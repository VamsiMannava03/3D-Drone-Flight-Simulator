package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OwnPhysicsEngine}, including the accuracy targets from the SRS.
 */
public class OwnPhysicsEngineTest {

  private static final double DT = PhysicsConstants.TIME_STEP;

  /** Reference quadcopter from the physics plan. */
  private static final double DRONE_MASS = 1.5;
  private static final double MAX_THRUST = 30.0;
  private static final double DRAG_K = 0.1;

  /** A point mass that pushes itself with a fixed throttle, straight up. */
  private static final class ThrustingBody extends PointMass {
    private final double throttle;

    ThrustingBody(double mass, Vector3D position, double throttle) {
      super(mass, position);
      this.throttle = throttle;
    }

    @Override
    public Vector3D ownForces() {
      return Vector3D.UP.scale(throttle * MAX_THRUST);
    }
  }

  /** A point mass the engine must not move. */
  private static final class InactiveBody extends PointMass {
    InactiveBody(Vector3D position) {
      super(1.0, position);
    }

    @Override
    public boolean isActive() {
      return false;
    }
  }

  private static void run(PhysicsEngine engine, int steps) {
    for (int i = 0; i < steps; i++) {
      engine.step(DT);
    }
  }

  @Test
  public void freeFallMatchesDiscreteSemiImplicitEuler() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(new Gravity()));
    PointMass body = new PointMass(DRONE_MASS, new Vector3D(0, 0, 100));
    engine.addBody(body);

    run(engine, 200);

    // v_n = -n g dt, z_n = z0 - g dt^2 n (n + 1) / 2
    assertEquals(-19.62, body.getVelocity().z(), 1e-9);
    assertEquals(80.2819, body.getPosition().z(), 1e-9);
    assertEquals(0, body.getPosition().x(), 0);
    assertEquals(0, body.getPosition().y(), 0);
  }

  @Test
  public void freeFallMatchesTextbookWithinTenCentimetres() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(new Gravity()));
    PointMass body = new PointMass(DRONE_MASS, new Vector3D(0, 0, 100));
    engine.addBody(body);

    run(engine, 200);

    double t = 2.0;
    double expected = 100 - 0.5 * PhysicsConstants.GRAVITY * t * t;
    assertEquals(expected, body.getPosition().z(), 0.1);
  }

  @Test
  public void hoverThrottleHoldsAltitudeForTenSeconds() {
    OwnPhysicsEngine engine =
        new OwnPhysicsEngine(List.of(new Gravity(), new QuadraticDrag(DRAG_K)));
    double hoverThrottle = DRONE_MASS * PhysicsConstants.GRAVITY / MAX_THRUST;
    ThrustingBody drone = new ThrustingBody(DRONE_MASS, new Vector3D(0, 0, 10), hoverThrottle);
    engine.addBody(drone);

    run(engine, 1000);

    assertEquals(10, drone.getPosition().z(), 1e-6);
  }

  @Test
  public void fallingBodyReachesTerminalSpeed() {
    OwnPhysicsEngine engine =
        new OwnPhysicsEngine(List.of(new Gravity(), new QuadraticDrag(DRAG_K)));
    PointMass body = new PointMass(DRONE_MASS, new Vector3D(0, 0, 5000));
    engine.addBody(body);

    run(engine, 3000);

    double terminal = Math.sqrt(DRONE_MASS * PhysicsConstants.GRAVITY / DRAG_K);
    assertEquals(12.13, terminal, 0.01);
    assertEquals(terminal, body.getVelocity().magnitude(), terminal * 0.001);
    assertTrue(body.getVelocity().z() < 0);
  }

  @Test
  public void fullThrottleClimbs() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(new Gravity()));
    ThrustingBody drone = new ThrustingBody(DRONE_MASS, Vector3D.ZERO, 1.0);
    engine.addBody(drone);

    engine.step(DT);

    // a = (30 - 14.715) / 1.5 = 10.19 m/s^2
    double a = (MAX_THRUST - DRONE_MASS * PhysicsConstants.GRAVITY) / DRONE_MASS;
    assertEquals(a * DT, drone.getVelocity().z(), 1e-12);
    assertEquals(a * DT * DT, drone.getPosition().z(), 1e-12);
  }

  @Test
  public void positionUsesTheNewVelocity() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(body -> new Vector3D(2, 0, 0)));
    PointMass body = new PointMass(1.0, Vector3D.ZERO, new Vector3D(1, 0, 0));
    engine.addBody(body);

    engine.step(0.5);

    // v = 1 + 2 * 0.5 = 2; plain Euler would give x = 0.5, semi-implicit gives 2 * 0.5 = 1
    assertEquals(2.0, body.getVelocity().x(), 1e-12);
    assertEquals(1.0, body.getPosition().x(), 1e-12);
  }

  @Test
  public void netForceAddsOwnAndGlobalForces() {
    OwnPhysicsEngine engine =
        new OwnPhysicsEngine(List.of(new Gravity(), body -> new Vector3D(1, 2, 0)));
    ThrustingBody drone = new ThrustingBody(DRONE_MASS, Vector3D.ZERO, 1.0);

    Vector3D net = engine.netForce(drone);

    assertEquals(1, net.x(), 1e-12);
    assertEquals(2, net.y(), 1e-12);
    assertEquals(MAX_THRUST - DRONE_MASS * PhysicsConstants.GRAVITY, net.z(), 1e-12);
  }

  @Test
  public void heavierBodyNeedsMoreForceForSameAcceleration() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(body -> new Vector3D(6, 0, 0)));
    PointMass light = new PointMass(1.0, Vector3D.ZERO);
    PointMass heavy = new PointMass(3.0, Vector3D.ZERO);
    engine.addBody(light);
    engine.addBody(heavy);

    engine.step(1.0);

    assertEquals(6.0, light.getVelocity().x(), 1e-12);
    assertEquals(2.0, heavy.getVelocity().x(), 1e-12);
  }

  @Test
  public void inactiveBodiesDoNotMove() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(new Gravity()));
    InactiveBody body = new InactiveBody(new Vector3D(1, 2, 3));
    engine.addBody(body);

    run(engine, 100);

    assertEquals(new Vector3D(1, 2, 3), body.getPosition());
    assertEquals(Vector3D.ZERO, body.getVelocity());
  }

  @Test
  public void noForcesMeansConstantVelocity() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of());
    PointMass body = new PointMass(1.0, Vector3D.ZERO, new Vector3D(3, -4, 0));
    engine.addBody(body);

    run(engine, 100);

    assertEquals(new Vector3D(3, -4, 0), body.getVelocity());
    assertEquals(3.0, body.getPosition().x(), 1e-9);
    assertEquals(-4.0, body.getPosition().y(), 1e-9);
  }

  @Test
  public void sameScenarioGivesIdenticalResults() {
    Vector3D first = runScenario();
    Vector3D second = runScenario();
    assertEquals(first, second);
  }

  private static Vector3D runScenario() {
    WindField wind = position -> new Vector3D(5, 0, 0);
    OwnPhysicsEngine engine =
        new OwnPhysicsEngine(List.of(new Gravity(), new QuadraticDrag(DRAG_K, wind)));
    ThrustingBody drone = new ThrustingBody(DRONE_MASS, new Vector3D(0, 0, 10), 0.6);
    engine.addBody(drone);
    run(engine, 1000);
    return drone.getPosition();
  }

  @Test
  public void addRemoveAndListBodies() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of());
    PointMass a = new PointMass(1.0, Vector3D.ZERO);
    PointMass b = new PointMass(1.0, Vector3D.ZERO);
    engine.addBody(a);
    engine.addBody(b);

    assertEquals(List.of(a, b), engine.getBodies());
    assertTrue(engine.removeBody(a));
    assertFalse(engine.removeBody(a));
    assertEquals(List.of(b), engine.getBodies());
  }

  @Test
  public void bodyListIsReadOnly() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of());
    assertThrows(
        UnsupportedOperationException.class,
        () -> engine.getBodies().add(new PointMass(1.0, Vector3D.ZERO)));
  }

  @Test
  public void rejectsNullAndDuplicateBodies() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of());
    PointMass body = new PointMass(1.0, Vector3D.ZERO);
    engine.addBody(body);

    assertThrows(NullPointerException.class, () -> engine.addBody(null));
    assertThrows(IllegalArgumentException.class, () -> engine.addBody(body));
  }

  @Test
  public void globalForcesAreCopied() {
    List<Force> forces = new ArrayList<>(List.of(new Gravity()));
    OwnPhysicsEngine engine = new OwnPhysicsEngine(forces);
    forces.clear();

    assertEquals(1, engine.getGlobalForces().size());
    assertThrows(
        UnsupportedOperationException.class, () -> engine.getGlobalForces().add(new Gravity()));
  }

  @Test
  public void rejectsNullForces() {
    List<Force> forces = new ArrayList<>();
    forces.add(null);
    assertThrows(NullPointerException.class, () -> new OwnPhysicsEngine(forces));
    assertThrows(NullPointerException.class, () -> new OwnPhysicsEngine(null));
  }

  @Test
  public void rejectsInvalidTimeStep() {
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of());
    assertThrows(IllegalArgumentException.class, () -> engine.step(0));
    assertThrows(IllegalArgumentException.class, () -> engine.step(-0.01));
    assertThrows(IllegalArgumentException.class, () -> engine.step(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> engine.step(Double.POSITIVE_INFINITY));
  }
}
