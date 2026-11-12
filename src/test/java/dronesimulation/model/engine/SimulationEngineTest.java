package dronesimulation.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.DroneFactory;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.missions.GoTo;
import dronesimulation.model.missions.Hover;
import dronesimulation.model.missions.TakeOff;
import dronesimulation.model.physics.GustyWind;
import dronesimulation.model.physics.NoWind;
import dronesimulation.model.world.Building;
import dronesimulation.model.world.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SimulationEngineTest {

  /** Builds the same gusty, noisy scenario every time for a given seed. */
  private static SimulationEngine scenario(long seed) {
    Random random = new Random(seed);
    World world = new World(new EventBus());
    world.setWind(new GustyWind(new Vector3D(2, 1, 0), 3, 2, random, world::getTime));
    world.addObstacle(Building.block("B1", 20, 10, 6, 6, 12));
    world.addDrone(DroneFactory.create(DroneType.QUADCOPTER, "q1", Vector3D.ZERO, random));
    world.addDrone(DroneFactory.create(DroneType.FIXED_WING, "fw1", new Vector3D(0, 20, 0),
        random));
    world.mission("q1").addAll(List.of(new TakeOff(8), new GoTo(new Vector3D(30, -5, 15)),
        new Hover(2)));
    world.mission("fw1").add(new TakeOff(25));
    return new SimulationEngine(world, SimulationEngine.DEFAULT_DT);
  }

  @Test
  void sameInputsGiveExactlyTheSameResults() {
    SimulationEngine a = scenario(42);
    SimulationEngine b = scenario(42);
    a.runSteps(3000);
    b.runSteps(3000);
    assertEquals(a.snapshot(), b.snapshot());
  }

  @Test
  void differentSeedsGiveDifferentWeather() {
    SimulationEngine a = scenario(1);
    SimulationEngine b = scenario(2);
    a.runSteps(1000);
    b.runSteps(1000);
    assertNotEquals(a.snapshot().drones().get(0).position(),
        b.snapshot().drones().get(0).position());
  }

  @Test
  void submittedActionsRunBeforeTheNextStep() {
    SimulationEngine engine = scenario(3);
    List<Double> seenAt = new ArrayList<>();
    engine.submit(e -> seenAt.add(e.world().getTime()));
    engine.runSteps(5);
    assertEquals(List.of(0.0), seenAt);
  }

  @Test
  void failingActionIsReportedNotThrown() {
    SimulationEngine engine = scenario(3);
    List<SimEvent> events = new ArrayList<>();
    engine.world().getEvents().subscribe(events::add);
    engine.submit(e -> e.world().drone("nobody"));
    engine.runSteps(1);
    assertEquals(SimEventType.COMMAND_REJECTED, events.get(0).type());
    assertTrue(events.get(0).message().startsWith("No drone called 'nobody'"));
  }

  @Test
  void snapshotDescribesDronesAndWorld() {
    SimulationEngine engine = scenario(4);
    List<WorldSnapshot> frames = new ArrayList<>();
    engine.addSnapshotListener(frames::add);
    engine.runSteps(200);

    assertEquals(1, frames.size());
    WorldSnapshot s = frames.get(0);
    assertEquals(2.0, s.time(), 1e-9);
    assertEquals(1, s.obstacles().size());
    DroneSnapshot q1 = s.drones().get(0);
    assertEquals("q1", q1.id());
    assertEquals("Quadcopter", q1.type());
    assertEquals("takeoff 8", q1.command());
    assertEquals(2, q1.queuedCommands());
    assertTrue(q1.batteryLevel() < 1);
    assertFalse(s.paused());
  }

  @Test
  void statusShowsThrottleAndMassFallingAfterADrop() {
    World world = new World(NoWind.INSTANCE, new EventBus());
    world.addDrone(DroneFactory.builder(DroneType.QUADCOPTER, "q1", Vector3D.ZERO,
        new Random(1)).payload(0.5).build());
    SimulationEngine engine = new SimulationEngine(world, SimulationEngine.DEFAULT_DT);
    world.queueCommand("q1", new TakeOff(10));
    world.queueCommand("q1", new Hover(6));
    world.queueCommand("q1", new dronesimulation.model.missions.DropPayload());
    world.queueCommand("q1", new Hover(6));

    engine.runSteps(700); // about 3 s climbing, then hovering with the package
    DroneSnapshot loaded = engine.snapshot().drones().get(0);
    assertEquals("hover 6", loaded.command());
    engine.runSteps(1000); // hovering after the drop
    DroneSnapshot light = engine.snapshot().drones().get(0);

    assertEquals(2.0, loaded.mass(), 1e-9);
    assertEquals(1.5, light.mass(), 1e-9);
    assertEquals(2.0 * 9.81 / 32, loaded.throttle(), 0.01);
    assertEquals(1.5 * 9.81 / 32, light.throttle(), 0.01);
  }

  @Test
  void realtimeLoopStopsAndHonoursTimeScale() throws InterruptedException {
    SimulationEngine engine = scenario(5);
    engine.setTimeScale(10);
    Thread thread = new Thread(() -> engine.runRealtime(100));
    thread.start();
    Thread.sleep(300);
    engine.stop();
    thread.join(2000);
    assertFalse(thread.isAlive());
    assertTrue(engine.snapshot().time() > 1.0, "10x speed for 0.3 s is about 3 s");
  }

  @Test
  void validatesSettings() {
    SimulationEngine engine = scenario(6);
    assertThrows(IllegalArgumentException.class, () -> engine.setTimeScale(0));
    assertThrows(IllegalArgumentException.class, () -> engine.setTimeScale(100));
    assertThrows(IllegalArgumentException.class, () -> new SimulationEngine(engine.world(), 0));
    engine.setPaused(true);
    assertTrue(engine.snapshot().paused());
  }

  @Test
  void resetNeverShowsTheWorldBeforeTheScriptReplays() {
    SimulationEngine engine = new SimulationEngine(
        () -> new World(NoWind.INSTANCE, new EventBus()),
        SimulationEngine.DEFAULT_DT);
    engine.setScript(List.of(e -> e.world().addDrone(
        DroneFactory.create(DroneType.QUADCOPTER, "q1", Vector3D.ZERO, new Random(1)))));
    engine.runSteps(1);
    List<Integer> droneCounts = new ArrayList<>();
    engine.addSnapshotListener(s -> droneCounts.add(s.drones().size()));

    engine.submit(SimulationEngine::reset);
    engine.runSteps(1);

    // Only the finished frame, with the script's drone already back.
    assertEquals(List.of(1), droneCounts);
  }

  @Test
  void commandsQueuedBehindAResetRunAfterTheScriptReplays() {
    EventBus events = new EventBus();
    List<SimEvent> log = new ArrayList<>();
    events.subscribe(log::add);
    SimulationEngine engine = new SimulationEngine(() -> new World(NoWind.INSTANCE, events),
        SimulationEngine.DEFAULT_DT);
    engine.setScript(List.of(e -> e.world().addDrone(
        DroneFactory.create(DroneType.QUADCOPTER, "q1", Vector3D.ZERO, new Random(1)))));
    engine.runSteps(1);

    // In one frame: reset, then a command for the script's drone, as when "reset" and a
    // command are typed together.
    engine.submit(SimulationEngine::reset);
    engine.submit(e -> e.world().queueCommand("q1", new TakeOff(5)));
    engine.runSteps(1);

    assertTrue(log.stream().noneMatch(ev -> ev.type() == SimEventType.COMMAND_REJECTED),
        () -> "the command ran before the script brought q1 back: " + log);
    assertEquals(1, engine.world().mission("q1").queuedCount()
        + (engine.world().mission("q1").getCurrent() == null ? 0 : 1));
  }

  @Test
  void aFailingScriptStepIsReportedAndTheRestStillRuns() {
    EventBus events = new EventBus();
    List<SimEvent> log = new ArrayList<>();
    events.subscribe(log::add);
    SimulationEngine engine = new SimulationEngine(() -> new World(NoWind.INSTANCE, events),
        SimulationEngine.DEFAULT_DT);
    engine.setScript(List.of(
        e -> e.world().drone("missing"),
        e -> e.world().addDrone(
            DroneFactory.create(DroneType.QUADCOPTER, "q1", Vector3D.ZERO, new Random(1)))));
    engine.runSteps(1);

    assertEquals(1, log.stream().filter(ev -> ev.type() == SimEventType.COMMAND_REJECTED)
        .count());
    assertTrue(engine.world().findDrone("q1").isPresent());
  }

  @Test
  void aFailingSnapshotListenerDoesNotStopTheSimulationOrOtherViews() {
    SimulationEngine engine = scenario(3);
    List<Double> seen = new ArrayList<>();
    engine.addSnapshotListener(s -> {
      throw new IllegalStateException("broken view");
    });
    engine.addSnapshotListener(s -> seen.add(s.time()));

    engine.runSteps(10);
    engine.runSteps(10);

    assertEquals(2, seen.size(), "the second view still gets every frame");
    assertEquals(0.2, engine.world().getTime(), 1e-9, "and the simulation went on");
  }

  @Test
  void aScriptThatResetsIsRejectedInsteadOfLoopingForever() {
    EventBus events = new EventBus();
    List<SimEvent> log = new ArrayList<>();
    events.subscribe(log::add);
    SimulationEngine engine = new SimulationEngine(() -> new World(NoWind.INSTANCE, events),
        SimulationEngine.DEFAULT_DT);
    engine.setScript(List.of(SimulationEngine::reset));
    engine.runSteps(1); // returns: the inner replay is refused

    assertTrue(log.stream().anyMatch(ev -> ev.type() == SimEventType.COMMAND_REJECTED
        && ev.message().contains("cannot reset")), () -> log.toString());
  }
}
