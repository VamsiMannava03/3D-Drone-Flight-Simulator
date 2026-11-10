package dronesimulation.model.drones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.WaypointController;
import dronesimulation.model.physics.Gravity;
import dronesimulation.model.physics.PhysicsConstants;
import dronesimulation.model.physics.QuadraticDrag;
import dronesimulation.model.physics.TestWorld;
import dronesimulation.model.states.Flying;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DroneTypesTest {

  private static final double G = PhysicsConstants.GRAVITY;
  private static final double DT = 0.01;

  private static TestWorld world(Drone d) {
    TestWorld w = new TestWorld();
    w.addForce(new Gravity());
    w.addForce(new QuadraticDrag(0.02));
    w.addBody(d);
    return w;
  }

  private static Drone quad() {
    return DroneFactory.create(DroneType.QUADCOPTER, "q1", Vector3D.ZERO, new Random(1));
  }

  private static FixedWingDrone plane() {
    return (FixedWingDrone) DroneFactory.create(DroneType.FIXED_WING, "fw1", Vector3D.ZERO,
        new Random(1));
  }

  @Test
  void quadcopterLimitsTiltAndTotalThrust() {
    QuadcopterDrone q = (QuadcopterDrone) quad();
    Vector3D sideways = q.limitThrust(new Vector3D(100, 0, 10));
    double tilt = Math.toDegrees(Math.atan2(sideways.x(), sideways.z()));
    assertEquals(MultirotorDrone.MAX_TILT_DEGREES, tilt, 1e-9);

    assertEquals(32, q.limitThrust(new Vector3D(0, 0, 500)).magnitude(), 1e-9);
    assertEquals(0, q.limitThrust(new Vector3D(0, 0, -20)).z(), 1e-12); // cannot push down
  }

  @Test
  void quadcopterPowerGrowsWithThrottle() {
    QuadcopterDrone q = (QuadcopterDrone) quad();
    double hover = q.powerFor(new Vector3D(0, 0, 1.5 * G));
    double climb = q.powerFor(new Vector3D(0, 0, 25));
    assertTrue(climb > hover && hover > q.getSpec().idlePower());
    // Full throttle draws idle + max power.
    assertEquals(8 + 450, q.powerFor(new Vector3D(0, 0, 32)), 1e-9);
    assertEquals(1.5 * G / 32, q.throttleFor(new Vector3D(0, 0, 1.5 * G)), 1e-12);
    assertEquals(1.0, q.throttleFor(new Vector3D(0, 0, 999)), 1e-12);
  }

  @Test
  void reportedThrottleFollowsLastThrust() {
    Drone q = quad();
    assertEquals(0, q.getThrottle());
    q.takeOff(5);
    q.setController((d, dt) -> new Vector3D(0, 0, 16));
    q.update(DT);
    assertEquals(0.5, q.getThrottle(), 1e-12);
  }

  @Test
  void fixedWingThrottleCountsOnlyThePropeller() {
    FixedWingDrone p = plane();
    double maxProp = 40 * FixedWingDrone.PROPELLER_SHARE;
    assertEquals(0.5, p.throttleFor(new Vector3D(maxProp / 2, 0, 15)), 1e-12);
    assertTrue(p.powerFor(new Vector3D(0, 0, 10)) > p.powerFor(Vector3D.ZERO), "lift costs power");
  }

  @Test
  void presetsUseSrsThresholds() {
    for (Drone d : new Drone[] {quad(), plane()}) {
      assertEquals(3.0, d.getSpec().safeLandingSpeed(), 1e-12);
      assertEquals(0.2, d.getBattery().getLowLevel(), 1e-12);
    }
  }

  @Test
  void landsAtThreeMetresPerSecondAndCrashesAbove() {
    Drone soft = quad();
    soft.takeOff(5);
    soft.setPosition(new Vector3D(0, 0, 6));
    soft.update(DT);
    soft.onGroundImpact(3.0);
    assertEquals("Landing", soft.getState().name()); // touchdown while Flying, then Landed

    Drone hard = quad();
    hard.takeOff(5);
    hard.setPosition(new Vector3D(0, 0, 6));
    hard.update(DT);
    hard.onGroundImpact(3.01);
    assertEquals("Crashed", hard.getState().name());
  }

  @Test
  void quadcopterTakesOffStraightUp() {
    Drone q = quad();
    q.takeOff(10);
    q.setController(new WaypointController(q.takeOffTarget(10), 6, 0.5));
    TestWorld w = world(q);
    for (int i = 0; i < 1000 && q.getState() != Flying.INSTANCE; i++) {
      q.update(DT);
      w.step(DT);
    }
    assertSame(Flying.INSTANCE, q.getState());
    assertTrue(Math.hypot(q.getPosition().x(), q.getPosition().y()) < 0.3);
  }

  @Test
  void fixedWingCannotLiftOffWithoutAirspeed() {
    FixedWingDrone p = plane();
    assertEquals(0, p.getAvailableLift(), 1e-12);
    assertEquals(0, p.limitThrust(new Vector3D(0, 0, 50)).z(), 1e-12);
    Vector3D prop = p.limitThrust(new Vector3D(100, 0, 0));
    assertEquals(40 * FixedWingDrone.PROPELLER_SHARE, prop.magnitude(), 1e-9);
    assertEquals(9 * FixedWingDrone.SAFETY_FACTOR, p.getMinFlightSpeed(), 1e-9);
  }

  @Test
  void fixedWingRollsThenClimbsToAltitude() {
    FixedWingDrone p = plane();
    p.takeOff(20);
    p.setController(new WaypointController(p.takeOffTarget(20), 16, 5));
    TestWorld w = world(p);

    for (int i = 0; i < 100; i++) { // first second: still rolling on the runway
      p.update(DT);
      w.step(DT);
    }
    assertEquals(0, p.getAltitude(), 1e-9);
    assertTrue(p.getVelocity().x() > 3, "accelerating along the runway");

    for (int i = 0; i < 6000 && p.getState() != Flying.INSTANCE; i++) {
      p.update(DT);
      w.step(DT);
    }
    assertSame(Flying.INSTANCE, p.getState());
    assertTrue(p.getVelocity().magnitude() > p.getStallSpeed());
    assertTrue(p.getPosition().x() > 30, "needed a run-up");
  }

  @Test
  void builderValidatesConfiguration() {
    assertThrows(IllegalStateException.class,
        () -> new DroneBuilder(DroneType.QUADCOPTER, "x").maxThrust(30).build());
    assertThrows(IllegalStateException.class,
        () -> new DroneBuilder(DroneType.QUADCOPTER, "x").mass(1).build());
    assertThrows(IllegalStateException.class,
        () -> new DroneBuilder(DroneType.FIXED_WING, "x").mass(1).maxThrust(30).build());
    assertThrows(IllegalStateException.class, () -> new DroneBuilder(DroneType.QUADCOPTER, "x")
        .mass(1).maxThrust(30).stallSpeed(9).build());
    assertThrows(IllegalArgumentException.class, // too weak to lift itself
        () -> new DroneBuilder(DroneType.QUADCOPTER, "x").mass(5).maxThrust(30).build());
    assertThrows(IllegalStateException.class, // too weak to lift the payload
        () -> new DroneBuilder(DroneType.QUADCOPTER, "x").mass(1).maxThrust(30)
            .payload(2).build());
    assertThrows(IllegalArgumentException.class, // cruise below minimum flight speed
        () -> new DroneBuilder(DroneType.FIXED_WING, "x").mass(1).maxThrust(30)
            .stallSpeed(9).cruiseSpeed(10).build());
  }

  @Test
  void builderSetsParts() {
    Drone d = new DroneBuilder(DroneType.QUADCOPTER, "q9").mass(1).maxThrust(30)
        .at(new Vector3D(1, 2, 0)).battery(2, 0.3).batteryLevel(0.5).payload(0.5)
        .cruiseSpeed(7).maxPower(200).build();
    assertInstanceOf(QuadcopterDrone.class, d);
    assertEquals(new Vector3D(1, 2, 0), d.getHome());
    assertEquals(7200, d.getBattery().getCapacity(), 1e-9);
    assertEquals(0.5, d.getBattery().getLevel(), 1e-12);
    assertEquals(200, d.getSpec().maxPower(), 1e-12);
    assertEquals(1.5, d.getMass(), 1e-9);
    assertEquals(7, d.getSpec().cruiseSpeed(), 1e-9);
  }

  @Test
  void hexacopterIsAStrongerMultirotor() {
    Drone hex = DroneFactory.create(DroneType.HEXACOPTER, "h1", Vector3D.ZERO, new Random(1));
    assertInstanceOf(HexacopterDrone.class, hex);
    assertEquals(6, ((MultirotorDrone) hex).getRotorCount());
    assertEquals(4, ((MultirotorDrone) quad()).getRotorCount());
    assertEquals("Hexacopter", hex.getTypeName());
    assertEquals(0, hex.getMinFlightSpeed());

    hex.loadPayload(2.0); // a quadcopter cannot lift this much
    assertThrows(IllegalStateException.class, () -> quad().loadPayload(2.0));

    hex.takeOff(10);
    hex.setController(hex.navigateTo(hex.takeOffTarget(10), 0.5));
    TestWorld w = world(hex);
    for (int i = 0; i < 1500 && hex.getState() != Flying.INSTANCE; i++) {
      hex.update(DT);
      w.step(DT);
    }
    assertSame(Flying.INSTANCE, hex.getState());
  }

  @Test
  void factoryPresetsAreValidAndTyped() {
    assertEquals("Quadcopter", quad().getTypeName());
    assertEquals("FixedWing", plane().getTypeName());
    assertEquals(FixedWingDrone.WHEEL_FRICTION, plane().getGroundFriction(), 1e-12);
  }
}
