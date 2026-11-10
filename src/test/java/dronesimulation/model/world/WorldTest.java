package dronesimulation.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Battery;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.drones.StubDrone;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.missions.GoTo;
import dronesimulation.model.missions.Land;
import dronesimulation.model.missions.ReturnHome;
import dronesimulation.model.missions.TakeOff;
import dronesimulation.model.physics.NoWind;
import dronesimulation.model.physics.OwnPhysicsEngine;
import dronesimulation.model.physics.PhysicsBody;
import dronesimulation.model.physics.PhysicsEngine;
import dronesimulation.model.physics.SolidBody;
import dronesimulation.model.states.Crashed;
import dronesimulation.model.states.Flying;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldTest {

  private static final double DT = 0.01;

  private final EventBus bus = new EventBus();
  private final List<SimEvent> events = new ArrayList<>();
  private final World world = new World(NoWind.INSTANCE, bus);

  WorldTest() {
    bus.subscribe(events::add);
  }

  private void run(double seconds) {
    for (int i = 0; i < Math.round(seconds / DT); i++) {
      world.step(DT);
    }
  }

  private long count(SimEventType type) {
    return events.stream().filter(e -> e.type() == type).count();
  }

  private Drone add(String id, Vector3D home, Vector3D... route) {
    Drone d = new StubDrone(id, home);
    world.addDrone(d);
    world.mission(id).add(new TakeOff(home.z() + 5));
    for (Vector3D p : route) {
      world.mission(id).add(new GoTo(p));
    }
    return d;
  }

  @Test
  void runsMissionsAndStampsEventsWithWorldTime() {
    add("d1", Vector3D.ZERO, new Vector3D(5, 0, 5));
    run(10);
    assertEquals(SimEventType.TOOK_OFF, events.get(0).type());
    assertTrue(events.get(0).time() > 0.5);
    assertEquals(1, count(SimEventType.MISSION_COMPLETE));
    assertEquals(10, world.getTime(), 1e-6);
  }

  @Test
  void hittingABuildingCrashesAndPushesOut() {
    Building tower = Building.block("Tower", 10, 0, 4, 4, 30);
    world.addObstacle(tower);
    Drone d = add("d1", Vector3D.ZERO, new Vector3D(20, 0, 5));

    run(15);

    assertSame(Crashed.INSTANCE, d.getState());
    assertEquals(1, count(SimEventType.COLLISION));
    assertTrue(events.stream().anyMatch(e -> e.message().equals("hit Tower")));
    assertTrue(tower.distanceTo(d.getPosition()) >= d.getRadius() - 1e-9);
  }

  @Test
  void hittingAnObstacleWhileTakingOffCrashes() {
    world.addObstacle(new Building("Bridge", new Vector3D(-3, -3, 3), new Vector3D(3, 3, 4)));
    Drone d = new StubDrone("d1", Vector3D.ZERO);
    world.addDrone(d);
    world.mission("d1").add(new TakeOff(10)); // straight up into the deck at 3 m

    run(5);

    assertSame(Crashed.INSTANCE, d.getState());
    assertEquals(1, count(SimEventType.COLLISION));
    assertEquals(1, count(SimEventType.CRASH));
  }

  @Test
  void dronesCollidingBothCrash() {
    Drone a = add("a", new Vector3D(-10, 0, 0), new Vector3D(10, 0, 5));
    Drone b = add("b", new Vector3D(10, 0, 0), new Vector3D(-10, 0, 5));

    run(10);

    assertSame(Crashed.INSTANCE, a.getState());
    assertSame(Crashed.INSTANCE, b.getState());
    assertEquals(1, count(SimEventType.COLLISION));
    assertEquals(2, count(SimEventType.CRASH));
  }

  @Test
  void stayingInANoFlyZoneIsReportedOnce() {
    world.addNoFlyZone(new NoFlyZone("Airport", 10, 0, 3, 50));
    add("d1", Vector3D.ZERO, new Vector3D(10, 0, 5)); // ends up hovering inside
    run(30);
    assertEquals(1, count(SimEventType.ZONE_ENTERED));
    assertEquals("entered Airport", events.stream()
        .filter(e -> e.type() == SimEventType.ZONE_ENTERED).findFirst().orElseThrow().message());
  }

  @Test
  void leavingAndReenteringIsReportedAgain() {
    world.addNoFlyZone(new NoFlyZone("Airport", 10, 0, 3, 50));
    add("d1", Vector3D.ZERO, new Vector3D(20, 0, 5), new Vector3D(10, 0, 5));
    run(30);
    assertEquals(2, count(SimEventType.ZONE_ENTERED));
    assertEquals(1, count(SimEventType.ZONE_EXITED));
  }

  @Test
  void removedDroneLeavesNoContactStateBehind() {
    world.addNoFlyZone(new NoFlyZone("Airport", 0, 0, 3, 50));
    add("d1", Vector3D.ZERO);
    run(2);
    world.removeDrone("d1");
    Drone again = add("d1", Vector3D.ZERO);
    run(2);

    assertEquals(List.of(again), world.getDrones());
    assertEquals(2, count(SimEventType.ZONE_ENTERED)); // the new drone is reported too
    assertEquals(0, count(SimEventType.ZONE_EXITED)); // removing is not leaving
  }

  @Test
  void lowBatteryTriggersReturnHome() {
    Drone d = new StubDrone("d1", Vector3D.ZERO, new Battery(0.05, 0.5)); // 180 J, low at 90 J
    world.addDrone(d);
    world.mission("d1").add(new TakeOff(5));
    world.mission("d1").add(new GoTo(new Vector3D(100, 0, 5)));

    run(6);

    assertEquals(1, count(SimEventType.LOW_BATTERY));
    assertEquals(1, count(SimEventType.RETURN_HOME));
    assertInstanceOf(ReturnHome.class, world.mission("d1").getCurrent());
    // The policy reacts within the same step: RETURN_HOME right after LOW_BATTERY.
    List<SimEventType> order = events.stream().map(SimEvent::type)
        .filter(t -> t == SimEventType.LOW_BATTERY || t == SimEventType.RETURN_HOME).toList();
    assertEquals(List.of(SimEventType.LOW_BATTERY, SimEventType.RETURN_HOME), order);
  }

  @Test
  void droneThatTakesOffOnALowBatteryStillReturnsHome() {
    // Low from the start, so LOW_BATTERY fires while Idle, before the policy would act.
    Drone d = new StubDrone("d1", Vector3D.ZERO, new Battery(1, 0.2, 0.15));
    world.addDrone(d);
    world.mission("d1").add(new TakeOff(5));
    world.mission("d1").add(new GoTo(new Vector3D(100, 0, 5)));

    run(6);

    assertEquals(1, count(SimEventType.LOW_BATTERY));
    assertEquals(1, count(SimEventType.RETURN_HOME));
    // Home is right below the take-off point, so the 100 m leg is dropped and it lands there.
    assertTrue(d.getPosition().x() < 1, "x " + d.getPosition().x());
  }

  @Test
  void returnHomeMessagesSayWhatIsAbandoned() {
    add("d1", Vector3D.ZERO, new Vector3D(30, 0, 5));
    world.mission("d1").add(new ReturnHome()); // home is the last step: nothing to abandon
    for (int i = 0; i < 6000 && !(world.mission("d1").getCurrent() instanceof ReturnHome); i++) {
      world.step(DT);
    }
    bus.publish(SimEventType.LOW_BATTERY, "d1", "battery at 19%");
    bus.flush();
    assertEquals(List.of("low battery: landing after returning home"), events.stream()
        .filter(e -> e.type() == SimEventType.RETURN_HOME).map(SimEvent::message).toList());

    add("d2", new Vector3D(5, 0, 0), new Vector3D(5, 30, 5), new Vector3D(5, 60, 5));
    for (int i = 0; i < 6000 && !(world.mission("d2").getCurrent() instanceof GoTo); i++) {
      world.step(DT); // until it flies the first goto, with the second still queued
    }
    bus.publish(SimEventType.LOW_BATTERY, "d2", "battery at 19%");
    bus.flush();
    assertEquals("low battery: abandoning 2 mission steps, returning home", events.stream()
        .filter(e -> e.type() == SimEventType.RETURN_HOME && e.droneId().equals("d2"))
        .map(SimEvent::message).findFirst().orElseThrow());
  }

  @Test
  void lowBatteryDuringAQueuedReturnDropsTheLegsAfterIt() {
    Drone d = add("d1", Vector3D.ZERO, new Vector3D(30, 0, 5));
    world.mission("d1").add(new ReturnHome());
    world.mission("d1").add(new GoTo(new Vector3D(100, 0, 5)));
    world.mission("d1").add(new GoTo(new Vector3D(0, 100, 5)));
    for (int i = 0; i < 6000 && !(world.mission("d1").getCurrent() instanceof ReturnHome); i++) {
      world.step(DT);
    }
    assertInstanceOf(ReturnHome.class, world.mission("d1").getCurrent());

    bus.publish(SimEventType.LOW_BATTERY, "d1", "battery at 19%");
    bus.flush();

    assertEquals(1, count(SimEventType.RETURN_HOME));
    assertInstanceOf(ReturnHome.class, world.mission("d1").getCurrent());
    assertEquals(1, world.mission("d1").getQueued().size());
    assertInstanceOf(Land.class, world.mission("d1").getQueued().get(0));
    assertSame(Flying.INSTANCE, d.getState());
  }

  @Test
  void listenersSeeTheFinishedStep() {
    Drone d = add("d1", new Vector3D(0, 0, 0));
    List<String> stateWhenToldOfTakeOff = new ArrayList<>();
    bus.subscribe(e -> {
      if (e.type() == SimEventType.TOOK_OFF) {
        stateWhenToldOfTakeOff.add(d.getState().name() + "@" + world.getTime());
      }
    });
    run(5);
    // Delivered after the whole step: the drone is already Flying and the clock has advanced.
    assertEquals(1, stateWhenToldOfTakeOff.size());
    assertTrue(stateWhenToldOfTakeOff.get(0).startsWith("Flying@"));
    assertEquals(0, bus.pendingCount());
  }

  @Test
  void failingListenerDoesNotStopTheSimulationOrOthers() {
    List<String> errors = new ArrayList<>();
    bus.setErrorHandler((listener, e) -> errors.add(e.getMessage()));
    bus.subscribe(e -> {
      throw new IllegalStateException("broken view");
    });
    List<SimEvent> after = new ArrayList<>();
    bus.subscribe(after::add);
    add("d1", Vector3D.ZERO, new Vector3D(5, 0, 5));

    run(10);

    assertEquals(events.size(), after.size());
    assertTrue(errors.size() > 0 && errors.stream().allMatch("broken view"::equals));
    assertEquals(1, count(SimEventType.MISSION_COMPLETE));
  }

  @Test
  void lowBatteryReturnsAtOnceEvenIfHomeIsQueuedLater() {
    Drone d = new StubDrone("d1", Vector3D.ZERO, new Battery(0.05, 0.5));
    world.addDrone(d);
    world.mission("d1").addAll(List.of(new TakeOff(5), new GoTo(new Vector3D(100, 0, 5)),
        new GoTo(new Vector3D(100, 100, 5)), new ReturnHome()));

    run(6);

    assertInstanceOf(ReturnHome.class, world.mission("d1").getCurrent());
    assertEquals(1, world.mission("d1").getQueued().size()); // only the Land
  }

  @Test
  void returnAltitudeClearsTallestBuilding() {
    Drone d = new StubDrone("d1", Vector3D.ZERO);
    world.addDrone(d);
    assertEquals(Drone.DEFAULT_RETURN_ALTITUDE, d.getReturnAltitude());
    world.addObstacle(Building.block("Tall", 50, 50, 5, 5, 30));
    assertEquals(30 + World.RETURN_CLEARANCE, d.getReturnAltitude());
  }

  @Test
  void returnHomeClimbsBeforeFlyingHome() {
    world.addObstacle(Building.block("Wall", 10, 0, 2, 40, 20));
    Drone d = new StubDrone("d1", new Vector3D(0, 0, 0));
    world.addDrone(d);
    // Go over the wall, then drop low behind it.
    world.mission("d1").addAll(List.of(new TakeOff(25), new GoTo(new Vector3D(20, 0, 25)),
        new GoTo(new Vector3D(20, 0, 5)), new ReturnHome()));

    run(60); // a straight line home at 5 m would hit the 20 m wall

    assertTrue(world.mission("d1").isEmpty(), "mission finished without crashing");
    assertEquals(25, d.getAltitude(), 0.5);
    assertEquals(0, Math.hypot(d.getPosition().x(), d.getPosition().y()), 1.0);
  }

  @Test
  void lowBatteryReturnCanBeTurnedOff() {
    world.setReturnHomeOnLowBattery(false);
    Drone d = new StubDrone("d1", Vector3D.ZERO, new Battery(0.05, 0.5));
    world.addDrone(d);
    world.mission("d1").add(new TakeOff(5));
    world.mission("d1").add(new GoTo(new Vector3D(100, 0, 5)));
    run(6);
    assertInstanceOf(GoTo.class, world.mission("d1").getCurrent());
  }

  @Test
  void unknownDroneGivesHelpfulError() {
    add("q1", Vector3D.ZERO);
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> world.drone("zz"));
    assertEquals("No drone called 'zz'. Drones: q1", e.getMessage());
    assertThrows(IllegalArgumentException.class,
        () -> world.addDrone(new StubDrone("q1", Vector3D.ZERO)));
  }

  @Test
  void queueCommandRejectsStepsTheDroneCannotDo() {
    add("q1", Vector3D.ZERO); // queues a take-off
    assertTrue(world.queueCommand("q1", new GoTo(new Vector3D(3, 3, 3))));
    assertTrue(!world.queueCommand("q1", new dronesimulation.model.missions.DropPayload()));
    run(DT);

    SimEvent rejected = events.stream().filter(e -> e.type() == SimEventType.COMMAND_REJECTED)
        .findFirst().orElseThrow();
    assertEquals("q1", rejected.droneId());
    assertEquals("'drop' rejected: q1 has no payload to drop", rejected.message());
    // Take-off is running; only the valid goto is queued after it, not the drop.
    assertEquals(1, world.mission("q1").getQueued().size());
  }

  @Test
  void physicsEngineCanBeReplaced() {
    List<String> calls = new ArrayList<>();
    World custom = new World(NoWind.INSTANCE, new EventBus(), forces -> {
      calls.add(forces.size() + " forces");
      PhysicsEngine real = new OwnPhysicsEngine(forces);
      return new PhysicsEngine() {
        @Override
        public void addBody(PhysicsBody body) {
          calls.add("add " + ((SolidBody) body).getId());
          real.addBody(body);
        }

        @Override
        public boolean removeBody(PhysicsBody body) {
          return real.removeBody(body);
        }

        @Override
        public List<PhysicsBody> getBodies() {
          return real.getBodies();
        }

        @Override
        public void step(double dt) {
          calls.add("step");
          real.step(dt);
        }
      };
    });
    custom.addDrone(new StubDrone("d1", Vector3D.ZERO));
    custom.step(DT);

    assertEquals(List.of("2 forces", "add d1", "step"), calls);
    assertEquals(DT, custom.getTime(), 1e-12);
  }

  @Test
  void obstacleAndZoneGeometry() {
    Building box = Building.block("B", 0, 0, 2, 2, 10);
    assertEquals(0, box.distanceTo(new Vector3D(0, 0, 5)));
    assertEquals(3, box.distanceTo(new Vector3D(4, 0, 5)), 1e-9);
    assertEquals(Math.sqrt(9 + 16 + 100), box.distanceTo(new Vector3D(-4, 5, 20)), 1e-9);
    assertEquals(new Vector3D(1, 0, 10),
        ObstacleShapes.closestPoint(box, new Vector3D(5, 0, 20)).orElseThrow());
    assertThrows(IllegalArgumentException.class,
        () -> new Building("X", new Vector3D(1, 1, 1), Vector3D.ZERO));

    NoFlyZone zone = new NoFlyZone("Z", 0, 0, 5, 20);
    assertTrue(zone.contains(new Vector3D(3, 3, 10)));
    assertTrue(!zone.contains(new Vector3D(3, 3, 25)));
    assertThrows(IllegalArgumentException.class, () -> new NoFlyZone("Z", 0, 0, 0, 20));
  }

  @Test
  void housesAreObstaclesUpToTheirRidge() {
    Building house = Building.house("Elm St 1", 0, 0, 10, 8, 8.5);
    assertEquals(Building.Style.HOUSE, house.style());
    assertEquals(new Vector3D(-5, -4, 0), house.min());
    assertEquals(new Vector3D(5, 4, 8.5), house.max());
    assertEquals(8.5, house.top());
    assertEquals(3.2, house.roofHeight(), 1e-9); // 0.4 x the 8 m side
    assertEquals(5.3, house.eavesHeight(), 1e-9);
    assertEquals(2, Building.house("Hut", 0, 0, 10, 10, 4).roofHeight(), 1e-9); // half the house
    assertEquals(0, Building.block("B", 0, 0, 2, 2, 10).roofHeight());
    assertEquals(10, Building.block("B", 0, 0, 2, 2, 10).eavesHeight());
    assertEquals(Building.Style.BLOCK, Building.block("B", 0, 0, 2, 2, 10).style());
    assertEquals(Building.Style.BLOCK,
        new Building("Bridge", Vector3D.ZERO, new Vector3D(1, 1, 1)).style());
    assertThrows(NullPointerException.class,
        () -> new Building("X", Vector3D.ZERO, new Vector3D(1, 1, 1), null));

    world.addObstacle(Building.house("Elm St 2", 10, 0, 6, 6, 8));
    Drone d = add("d1", Vector3D.ZERO, new Vector3D(20, 0, 5)); // straight through it
    run(15);
    assertSame(Crashed.INSTANCE, d.getState());
    assertTrue(events.stream().anyMatch(e -> e.message().equals("hit Elm St 2")));
  }
}
