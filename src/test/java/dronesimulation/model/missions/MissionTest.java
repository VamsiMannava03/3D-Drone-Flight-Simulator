package dronesimulation.model.missions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.drones.StubDrone;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.physics.Gravity;
import dronesimulation.model.physics.TestWorld;
import dronesimulation.model.states.Crashed;
import dronesimulation.model.states.Flying;
import dronesimulation.model.states.Idle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissionTest {

  private static final double DT = 0.01;

  private final List<SimEvent> events = new ArrayList<>();
  private final Drone drone = new StubDrone("d1", Vector3D.ZERO);
  private final Mission mission = new Mission();
  private final TestWorld world = new TestWorld();

  MissionTest() {
    EventBus bus = new EventBus();
    bus.subscribe(events::add);
    drone.setEvents(bus);
    world.addForce(new Gravity());
    world.addBody(drone);
  }

  private void run(double seconds) {
    for (int i = 0; i < Math.round(seconds / DT); i++) {
      mission.update(drone, DT);
      drone.update(DT);
      world.step(DT);
      drone.getEvents().flush(); // events are delivered at the end of each step
    }
  }

  private List<SimEventType> types() {
    return events.stream().map(SimEvent::type).toList();
  }

  private List<SimEventType> typesWithoutSteps() {
    return types().stream().filter(t -> t != SimEventType.MISSION_STEP_DONE).toList();
  }

  @Test
  void dropMessageGivesThePointInMetres() {
    drone.loadPayload(0.5);
    mission.addAll(List.of(new TakeOff(5), new DropPayload()));

    run(20);

    String message = events.stream().filter(e -> e.type() == SimEventType.PAYLOAD_DROPPED)
        .map(SimEvent::message).findFirst().orElseThrow();
    assertTrue(message.matches("dropped 0\\.50 kg at \\(-?\\d+\\.\\d\\d, -?\\d+\\.\\d\\d, "
        + "\\d+\\.\\d\\d\\)"), message);
  }

  @Test
  void fullDeliveryMissionRunsInOrder() {
    drone.loadPayload(0.5);
    mission.addAll(List.of(new TakeOff(5), new GoTo(new Vector3D(10, 0, 5)), new Hover(1),
        new Deliver(new Vector3D(10, 6, 0)), new ReturnHome(), new Land(), new Charge()));

    run(120);

    assertEquals(List.of(SimEventType.TOOK_OFF, SimEventType.PAYLOAD_DROPPED,
        SimEventType.LANDED, SimEventType.MISSION_COMPLETE), typesWithoutSteps());
    List<String> steps = events.stream().filter(e -> e.type() == SimEventType.MISSION_STEP_DONE)
        .map(SimEvent::message).toList();
    assertEquals(7, steps.size());
    assertEquals("'takeoff 5' done, 6 steps left", steps.get(0));
    assertEquals("'charge' done, 0 steps left", steps.get(6));
    assertEquals(List.of("dropped 0.50 kg at (10.00, 6.00, 0.00)"), events.stream()
        .filter(e -> e.type() == SimEventType.PAYLOAD_DROPPED).map(SimEvent::message).toList());
    // The deliver step is reported right after the drop that finished it.
    assertEquals(SimEventType.MISSION_STEP_DONE,
        types().get(types().indexOf(SimEventType.PAYLOAD_DROPPED) + 1));
    assertSame(Idle.INSTANCE, drone.getState());
    assertTrue(drone.getBattery().isFull());
    assertEquals(0, drone.getPayloadMass());
    assertTrue(Math.hypot(drone.getPosition().x(), drone.getPosition().y()) < 0.5,
        "landed at home");
    assertTrue(mission.isEmpty());
  }

  @Test
  void hoverHoldsForItsDuration() {
    mission.addAll(List.of(new TakeOff(4), new Hover(3)));
    run(4);
    assertSame(Flying.INSTANCE, drone.getState());
    Vector3D before = drone.getPosition();
    run(2);
    assertEquals(0, before.distanceTo(drone.getPosition()), 0.1);
    run(3);
    assertTrue(types().contains(SimEventType.MISSION_COMPLETE));
  }

  @Test
  void rejectedCommandCancelsTheRest() {
    mission.addAll(List.of(new GoTo(new Vector3D(5, 5, 5)), new Land()));

    run(0.1);

    assertEquals(List.of(SimEventType.COMMAND_REJECTED), types());
    assertEquals("'goto 5 5 5' rejected: Cannot go to a waypoint while Idle "
        + "(1 queued commands cancelled)", events.get(0).message());
    assertTrue(mission.isEmpty());
  }

  @Test
  void deliverWithoutPayloadIsRejected() {
    mission.addAll(List.of(new TakeOff(3), new Deliver(new Vector3D(1, 1, 0))));
    run(5);
    assertTrue(events.get(events.size() - 1).message().contains("has no payload"));
  }

  @Test
  void crashClearsTheMission() {
    mission.addAll(List.of(new TakeOff(5), new Hover(30), new Land()));
    run(3);
    drone.crash("hit a tree");
    run(0.1);

    assertSame(Crashed.INSTANCE, drone.getState());
    assertTrue(mission.isEmpty());
    assertNull(mission.getCurrent());
  }

  @Test
  void replaceWithDropsCurrentAndQueued() {
    mission.addAll(List.of(new TakeOff(5), new Hover(30)));
    run(1);
    mission.replaceWith(List.of(new Hover(1)));
    assertNull(mission.getCurrent());
    assertEquals(1, mission.getQueued().size());
    assertEquals(1, mission.queuedCount());
  }

  @Test
  void describeUsesCommandLanguage() {
    assertEquals("takeoff 10", new TakeOff(10).describe());
    assertEquals("goto 1.5 -2 3", new GoTo(new Vector3D(1.5, -2, 3)).describe());
    assertEquals("hover 2", new Hover(2).describe());
    assertEquals("deliver 4 5 0", new Deliver(new Vector3D(4, 5, 0)).describe());
    assertEquals("land", new Land().describe());
    assertEquals("home", new ReturnHome().describe());
    assertEquals("charge", new Charge().describe());
  }

  @Test
  void describeKeepsFullPrecisionSoSavedFilesLoadBack() {
    // Rounded to 2 decimals these became "goto 1 1 0" and "hover 0", which are invalid.
    assertEquals("goto 1 1 0.004", new GoTo(new Vector3D(1, 1, 0.004)).describe());
    assertEquals("hover 0.004", new Hover(0.004).describe());
    assertEquals("takeoff 12.345", new TakeOff(12.345).describe());
  }

  @Test
  void commandsValidateArguments() {
    assertThrows(IllegalArgumentException.class, () -> new TakeOff(0));
    assertThrows(IllegalArgumentException.class, () -> new GoTo(new Vector3D(1, 1, 0)));
    assertThrows(IllegalArgumentException.class, () -> new Hover(-1));
    assertThrows(IllegalArgumentException.class, () -> new Deliver(new Vector3D(0, 0, -1)));
  }
}
