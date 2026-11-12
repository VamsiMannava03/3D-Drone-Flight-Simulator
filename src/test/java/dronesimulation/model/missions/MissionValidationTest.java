package dronesimulation.model.missions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.drones.DroneFactory;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.drones.StubDrone;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.physics.Gravity;
import dronesimulation.model.physics.PhysicsConstants;
import dronesimulation.model.physics.TestWorld;
import dronesimulation.model.states.Flying;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** SRS FR11: missions are checked against each drone's capabilities. */
class MissionValidationTest {

  private static Drone quad() {
    return DroneFactory.builder(DroneType.QUADCOPTER, "q1", Vector3D.ZERO, new Random(1))
        .payload(0.5).build();
  }

  private static Drone plane() {
    return DroneFactory.create(DroneType.FIXED_WING, "fw1", Vector3D.ZERO, new Random(1));
  }

  private static Vector3D at(double x, double y, double z) {
    return new Vector3D(x, y, z);
  }

  @Test
  void validMissionHasNoProblems() {
    List<DroneCommand> steps = List.of(new TakeOff(15), new GoTo(at(10, 0, 15)),
        new GoTo(at(20, 10, 15)), new DropPayload(), new Hover(10), new ReturnHome(),
        new Land(), new Charge(), new TakeOff(5), new Land());
    assertEquals(List.of(), MissionValidator.validate(quad(), steps));
  }

  @Test
  void fixedWingCannotHover() {
    List<String> problems = MissionValidator.validate(plane(),
        List.of(new TakeOff(30), new Hover(10), new Land()));
    assertEquals(List.of("step 2 'hover 10': fw1 is a FixedWing drone and cannot hover"),
        problems);
  }

  @Test
  void problemsAreKeyedByStepIndexForEditors() {
    Map<Integer, String> problems = MissionValidator.problemsByStep(plane(),
        List.of(new TakeOff(30), new Hover(10), new GoTo(at(50, 0, 30)), new DropPayload()));
    assertEquals(List.of(1, 3), List.copyOf(problems.keySet()));
    assertEquals("fw1 is a FixedWing drone and cannot hover", problems.get(1));
    assertEquals(Map.of(), MissionValidator.problemsByStep(quad(), List.of()));
  }

  @Test
  void mustTakeOffBeforeFlying() {
    List<String> problems = MissionValidator.validate(quad(),
        List.of(new GoTo(at(5, 5, 5)), new Land()));
    assertEquals(2, problems.size());
    assertEquals("step 1 'goto 5 5 5': q1 must take off before it can go to a waypoint",
        problems.get(0));
  }

  @Test
  void payloadCanOnlyBeDroppedOnce() {
    List<String> problems = MissionValidator.validate(quad(), List.of(new TakeOff(10),
        new Deliver(at(5, 5, 0)), new DropPayload()));
    assertEquals(List.of("step 3 'drop': q1 has no payload to drop"), problems);
  }

  @Test
  void mustChargeAfterLandingBeforeTakingOffAgain() {
    List<String> problems = MissionValidator.validate(quad(), List.of(new TakeOff(10),
        new Land(), new TakeOff(10)));
    assertEquals(List.of("step 3 'takeoff 10': q1 must charge after landing before it can "
        + "take off again"), problems);
    assertTrue(MissionValidator.validate(quad(), List.of(new Charge())).get(0)
        .endsWith("q1 can only charge after landing"));
  }

  @Test
  void altitudeIsCappedAtTheFlightCeiling() {
    List<String> problems = MissionValidator.validate(quad(), List.of(new TakeOff(150)));
    assertEquals(List.of("step 1 'takeoff 150': 150 m is above the 120 m flight ceiling"),
        problems);
  }

  @Test
  void addCheckedPlansAfterWhatIsAlreadyQueued() {
    Drone d = quad();
    Mission mission = new Mission();
    mission.addChecked(d, new TakeOff(10));
    mission.addChecked(d, new GoTo(at(1, 1, 10))); // fine: the queued take-off comes first
    mission.addChecked(d, new Land());
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> mission.addChecked(d, new Hover(3)));
    assertEquals("q1 must take off before it can hover", e.getMessage());
    assertEquals(3, mission.getQueued().size());
  }

  @Test
  void dropMakesTheDroneLighterAtOnce() {
    Drone d = new StubDrone("d1", Vector3D.ZERO);
    d.loadPayload(0.5);
    List<SimEvent> events = new ArrayList<>();
    EventBus bus = new EventBus();
    bus.subscribe(events::add);
    d.setEvents(bus);
    TestWorld world = new TestWorld();
    world.addForce(new Gravity());
    world.addBody(d);
    Mission mission = new Mission();
    mission.addChecked(d, new TakeOff(5));
    mission.addChecked(d, new DropPayload());
    mission.addChecked(d, new Hover(2));

    for (int i = 0; i < 800; i++) {
      mission.update(d, 0.01);
      d.update(0.01);
      world.step(0.01);
      bus.flush();
    }

    assertSame(Flying.INSTANCE, d.getState());
    assertEquals(StubDrone.MASS, d.getMass(), 1e-12);
    assertEquals(StubDrone.MASS * PhysicsConstants.GRAVITY, d.getLastThrust().z(), 0.05,
        "hover thrust matches the lighter mass");
    assertTrue(events.stream().anyMatch(e -> e.type() == SimEventType.PAYLOAD_DROPPED));
    assertEquals("drop", new DropPayload().describe());
  }
}
