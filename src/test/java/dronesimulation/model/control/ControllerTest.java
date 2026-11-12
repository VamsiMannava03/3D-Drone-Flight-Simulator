package dronesimulation.model.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.drones.DroneFactory;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.drones.StubDrone;
import dronesimulation.model.physics.Gravity;
import dronesimulation.model.physics.PhysicsConstants;
import dronesimulation.model.physics.TestWorld;
import dronesimulation.model.states.Flying;
import dronesimulation.model.states.Landed;
import org.junit.jupiter.api.Test;

class ControllerTest {

  private static final double DT = 0.01;

  private static TestWorld world(Drone d) {
    TestWorld w = new TestWorld();
    w.addForce(new Gravity());
    w.addBody(d);
    return w;
  }

  private static void fly(TestWorld w, Drone d, double seconds) {
    for (int i = 0; i < Math.round(seconds / DT); i++) {
      d.update(DT);
      w.step(DT);
    }
  }

  private static Drone airborne(Vector3D at) {
    Drone d = new StubDrone("d1", at);
    d.takeOff(Math.max(0.1, at.z() - 1));
    d.update(DT); // TakingOff -> Flying, already high enough
    return d;
  }

  @Test
  void waypointControllerFliesToTargetAndStops() {
    Drone d = airborne(new Vector3D(0, 0, 10));
    Vector3D target = new Vector3D(20, -10, 15);
    WaypointController c = new WaypointController(target, 5, 0.5);
    d.setController(c);
    TestWorld w = world(d);

    fly(w, d, 2);
    assertFalse(c.hasArrived(d));
    assertTrue(d.getVelocity().magnitude() <= 5.5, "respects cruise speed");
    fly(w, d, 20);

    assertTrue(c.hasArrived(d));
    assertEquals(0, d.getPosition().distanceTo(target), 0.05);
    assertSame(target, c.getTarget());
  }

  @Test
  void hoverTargetHoldsPosition() {
    Vector3D spot = new Vector3D(3, 4, 8);
    Drone d = airborne(spot);
    d.setController(new WaypointController(spot, 5, 0.5));
    TestWorld w = world(d);

    fly(w, d, 10);

    assertEquals(0, d.getPosition().distanceTo(spot), 1e-6);
    assertEquals(StubDrone.MASS * PhysicsConstants.GRAVITY, d.getLastThrust().z(), 1e-6);
  }

  @Test
  void landingControllerTouchesDownGently() {
    Drone d = airborne(new Vector3D(5, 5, 12));
    d.land();
    d.setController(new LandingController(2));
    TestWorld w = world(d);
    double[] impactSpeed = {-1};

    for (int i = 0; i < 3000 && d.getState() != Landed.INSTANCE; i++) {
      d.update(DT);
      w.step(DT).forEach(x -> impactSpeed[0] = x.impactSpeed());
    }

    // Landed (not Crashed) means touchdown was under the 2 m/s safe speed.
    assertSame(Landed.INSTANCE, d.getState());
    // Aims for TOUCHDOWN_RATE (0.4 m/s); the velocity loop lags a little, so allow up to 1 m/s.
    assertTrue(impactSpeed[0] > 0 && impactSpeed[0] < 1.0, "impact " + impactSpeed[0]);
    assertEquals(5, d.getPosition().x(), 0.05);
    assertEquals(5, d.getPosition().y(), 0.05);
  }

  @Test
  void returnToHomeFliesAboveHome() {
    Drone d = new StubDrone("d1", new Vector3D(-5, 2, 0));
    d.takeOff(6);
    d.setController(new WaypointController(new Vector3D(15, 15, 6), 5, 0.5));
    TestWorld w = world(d);
    fly(w, d, 12);
    assertSame(Flying.INSTANCE, d.getState());

    ReturnToHomeController home = new ReturnToHomeController(d, 6);
    d.setController(home);
    fly(w, d, 15);

    assertTrue(home.hasArrived(d));
    assertEquals(new Vector3D(-5, 2, 6).distanceTo(d.getPosition()), 0, 0.1);
  }

  @Test
  void hoverControllerHoldsAgainstAPush() {
    Vector3D spot = new Vector3D(0, 0, 10);
    Drone d = airborne(spot);
    HoverController hover = new HoverController(spot);
    d.setController(hover);
    TestWorld w = world(d);
    d.setVelocity(new Vector3D(4, -3, 0)); // a gust shoves it

    fly(w, d, 12);

    assertEquals(0, d.getPosition().distanceTo(spot), 0.01);
    assertFalse(hover.hasArrived(d), "hovering never ends by itself");
    assertSame(spot, hover.getTarget());
  }

  @Test
  void altitudeHoldKeepsHeightAndStopsDrift() {
    Drone d = airborne(new Vector3D(0, 0, 6));
    d.setController(new AltitudeHoldController(15));
    d.setVelocity(new Vector3D(3, 0, 0));
    TestWorld w = world(d);

    fly(w, d, 15);

    assertEquals(15, d.getAltitude(), 0.01);
    assertEquals(0, d.getVelocity().magnitude(), 0.01);
    assertTrue(d.getPosition().x() > 0.5, "it holds height, not position");
    assertEquals(15, new AltitudeHoldController(15).getAltitude());
  }

  @Test
  void nullControllerAsksForNothing() {
    Drone d = airborne(new Vector3D(0, 0, 5));
    assertEquals(Vector3D.ZERO, NullFlightController.INSTANCE.desiredThrust(d, DT));
    assertFalse(NullFlightController.INSTANCE.hasArrived(d));
    d.setController(null);
    assertSame(NullFlightController.INSTANCE, d.getController());
  }

  @Test
  void fixedWingControllerCirclesItsTarget() {
    Drone plane = DroneFactory.create(DroneType.FIXED_WING, "fw", new Vector3D(0, 0, 0),
        new java.util.Random(3));
    plane.takeOff(30);
    plane.setController(plane.navigateTo(plane.takeOffTarget(30), 1));
    TestWorld w = world(plane);
    fly(w, plane, 40);
    Vector3D centre = new Vector3D(plane.getPosition().x() + 60, plane.getPosition().y(), 30);
    FlightController circle = plane.navigateTo(centre, 1);
    plane.setController(circle);

    fly(w, plane, 30);
    double minSpeed = Double.MAX_VALUE;
    double maxDistance = 0;
    for (int i = 0; i < 2000; i++) {
      plane.update(DT);
      w.step(DT);
      minSpeed = Math.min(minSpeed, Math.hypot(plane.getVelocity().x(),
          plane.getVelocity().y()));
      maxDistance = Math.max(maxDistance, Math.hypot(plane.getPosition().x() - centre.x(),
          plane.getPosition().y() - centre.y()));
    }

    assertInstanceOf(FixedWingController.class, circle);
    assertTrue(circle.hasArrived(plane), "arrived? pos=" + plane.getPosition() + " centre="
        + centre + " maxDist=" + maxDistance + " minSpeed=" + minSpeed + " state="
        + plane.getState().name());
    assertTrue(minSpeed >= plane.getMinFlightSpeed() * 0.95, "never slows to stall: "
        + minSpeed);
    assertTrue(maxDistance < FixedWingController.orbitRadius(plane) * 1.5, "stays near: max "
        + maxDistance + " radius " + FixedWingController.orbitRadius(plane));
    assertEquals(30, plane.getAltitude(), 1.5);
  }

  @Test
  void returnToHomeUsesTheDronesOwnNavigation() {
    Drone plane = DroneFactory.create(DroneType.FIXED_WING, "fw", Vector3D.ZERO,
        new java.util.Random(3));
    ReturnToHomeController home = new ReturnToHomeController(plane, 40);
    assertEquals(new Vector3D(0, 0, 40), home.getTarget());
    assertInstanceOf(WaypointController.class, new StubDrone("s", Vector3D.ZERO)
        .navigateTo(Vector3D.ZERO, 1));
  }

  @Test
  void rejectsBadParameters() {
    assertThrows(IllegalArgumentException.class, () -> new AltitudeHoldController(0));
    assertThrows(IllegalArgumentException.class,
        () -> new FixedWingController(Vector3D.ZERO, 10, 0));
    assertThrows(IllegalArgumentException.class,
        () -> new WaypointController(Vector3D.ZERO, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new LandingController(-1));
    Drone d = new StubDrone("d1", Vector3D.ZERO);
    assertThrows(IllegalArgumentException.class, () -> new ReturnToHomeController(d, 0));
  }
}
