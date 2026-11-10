package dronesimulation.model.drones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.NullFlightController;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.physics.Gravity;
import dronesimulation.model.physics.PhysicsConstants;
import dronesimulation.model.physics.TestWorld;
import dronesimulation.model.states.Crashed;
import dronesimulation.model.states.Flying;
import dronesimulation.model.states.Idle;
import dronesimulation.model.states.Landed;
import dronesimulation.model.states.Landing;
import dronesimulation.model.states.TakingOff;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DroneTest {

  private static final double G = PhysicsConstants.GRAVITY;
  private static final double DT = 0.01;

  private final List<SimEvent> events = new ArrayList<>();

  private Drone drone(Vector3D home) {
    Drone d = new StubDrone("d1", home);
    EventBus bus = new EventBus();
    bus.subscribe(events::add);
    d.setEvents(bus);
    return d;
  }

  private static TestWorld world(Drone d) {
    TestWorld w = new TestWorld();
    w.addForce(new Gravity());
    w.addBody(d);
    return w;
  }

  private static void step(TestWorld w, Drone d, int steps) {
    for (int i = 0; i < steps; i++) {
      d.update(DT);
      w.step(DT);
      d.getEvents().flush(); // events are delivered at the end of each step
    }
  }

  private List<SimEventType> types() {
    return events.stream().map(SimEvent::type).toList();
  }

  @Test
  void idleDroneProducesNoThrust() {
    Drone d = drone(Vector3D.ZERO);
    d.setController((drone, dt) -> new Vector3D(0, 0, 20));
    d.update(DT);
    assertEquals(Vector3D.ZERO, d.getLastThrust());
    assertSame(Idle.INSTANCE, d.getState());
    assertTrue(d.getBattery().isFull());
  }

  @Test
  void takeOffClimbsReportsAndDrainsBattery() {
    Drone d = drone(Vector3D.ZERO);
    d.setController((drone, dt) -> new Vector3D(0, 0, 15));
    d.takeOff(5);
    assertSame(TakingOff.INSTANCE, d.getState());
    TestWorld w = world(d);

    step(w, d, 300);

    assertSame(Flying.INSTANCE, d.getState());
    assertEquals(List.of(SimEventType.TOOK_OFF), types());
    assertTrue(d.getBattery().getLevel() < 1);
  }

  @Test
  void thrustIsLimitedByTheDroneType() {
    Drone d = drone(Vector3D.ZERO);
    d.setController((drone, dt) -> new Vector3D(0, 0, 1000));
    d.takeOff(5);
    d.update(DT);
    assertEquals(30, d.getLastThrust().magnitude(), 1e-9);
  }

  @Test
  void hardTouchdownCrashesAndStopsTheController() {
    Drone d = drone(new Vector3D(0, 0, 20));
    d.setController((drone, dt) -> Vector3D.ZERO); // motors idle: falls
    d.takeOff(10); // already above 10 m, so it becomes Flying on the next update
    TestWorld w = world(d);

    step(w, d, 400);

    assertSame(Crashed.INSTANCE, d.getState());
    assertSame(NullFlightController.INSTANCE, d.getController());
    assertEquals(List.of(SimEventType.TOOK_OFF, SimEventType.CRASH), types());
    assertTrue(events.get(1).message().startsWith("hit the ground at"));
  }

  @Test
  void softTouchdownWhileLandingLands() {
    Drone d = drone(new Vector3D(0, 0, 0.5));
    // Hover thrust minus a little: sinks slowly.
    d.setController((drone, dt) -> new Vector3D(0, 0, drone.getMass() * G * 0.95));
    d.takeOff(0.2);
    d.update(DT);
    d.land();
    assertSame(Landing.INSTANCE, d.getState());
    TestWorld w = world(d);

    step(w, d, 500);

    assertSame(Landed.INSTANCE, d.getState());
    assertTrue(types().contains(SimEventType.LANDED));
  }

  @Test
  void lowBatteryIsReportedOnce() {
    Battery battery = new Battery(0.01, 0.5); // 36 J
    Drone d = new StubDrone("d1", Vector3D.ZERO, battery);
    EventBus bus = new EventBus();
    bus.subscribe(events::add);
    d.setEvents(bus);
    d.setController((drone, dt) -> new Vector3D(0, 0, 10));
    d.takeOff(100);
    TestWorld w = world(d);

    step(w, d, 300); // 20 W for 3 s = 60 J: runs out

    // Never reaches 100 m, so no TOOK_OFF: the warning once, the depletion, then the fall
    // back to the ground is a crash.
    assertEquals(List.of(SimEventType.LOW_BATTERY, SimEventType.BATTERY_DEPLETED,
        SimEventType.CRASH), types());
    assertTrue(battery.isEmpty());
    assertEquals(Vector3D.ZERO, d.getLastThrust());
  }

  @Test
  void payloadAddsMassAndIsValidated() {
    Drone d = drone(Vector3D.ZERO);
    d.loadPayload(0.5);
    assertEquals(1.5, d.getMass(), 1e-9);
    assertThrows(IllegalStateException.class, () -> d.loadPayload(5));
    assertEquals(0.5, d.dropPayload(), 1e-9);
    assertEquals(StubDrone.MASS, d.getMass(), 1e-9);
    assertThrows(IllegalArgumentException.class, () -> d.loadPayload(0));
  }

  @Test
  void rejectsInvalidCommands() {
    Drone d = drone(Vector3D.ZERO);
    assertThrows(IllegalArgumentException.class, () -> d.takeOff(-1));
    IllegalStateException e = assertThrows(IllegalStateException.class, d::land);
    assertEquals("Cannot land while Idle", e.getMessage());
  }

  @Test
  void rejectsUnderpoweredDrone() {
    assertThrows(IllegalArgumentException.class, () -> new Drone("x", 10, 0.3, Vector3D.ZERO,
        new DroneSpec(30, 5, 2, 100, 0, 100), new Battery(1, 0.2), Sensor.perfect()) {
      @Override
      protected Vector3D limitThrust(Vector3D desired) {
        return desired;
      }

      @Override
      protected double powerFor(Vector3D thrust) {
        return 0;
      }

      @Override
      public Vector3D takeOffTarget(double altitude) {
        return Vector3D.ZERO;
      }

      @Override
      public dronesimulation.model.control.FlightController navigateTo(Vector3D target, double radius) {
        return dronesimulation.model.control.NullFlightController.INSTANCE;
      }

      @Override
      public double getMinFlightSpeed() {
        return 0;
      }

      @Override
      public String getTypeName() {
        return "Brick";
      }
    });
  }
}
