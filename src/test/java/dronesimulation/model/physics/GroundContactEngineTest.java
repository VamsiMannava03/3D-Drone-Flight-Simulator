package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroundContactEngineTest {

  private static final double G = PhysicsConstants.GRAVITY;
  private static final double DT = 0.001;

  private static GroundContactEngine withGravity() {
    return new GroundContactEngine(new OwnPhysicsEngine(List.of(new Gravity())));
  }

  private static List<GroundImpact> run(GroundContactEngine engine, double seconds) {
    List<GroundImpact> impacts = new ArrayList<>();
    int steps = (int) Math.round(seconds / DT);
    for (int i = 0; i < steps; i++) {
      engine.step(DT);
      impacts.addAll(engine.getLastImpacts());
    }
    return impacts;
  }

  @Test
  void groundStopsBodyAndReportsImpactSpeed() {
    GroundContactEngine engine = withGravity();
    PointMass body = new PointMass(1, new Vector3D(0, 0, 5));
    engine.addBody(body);

    List<GroundImpact> impacts = run(engine, 3);

    assertEquals(1, impacts.size());
    assertSame(body, impacts.get(0).body());
    assertEquals(Math.sqrt(2 * G * 5), impacts.get(0).impactSpeed(), 0.05);
    assertEquals(0, body.getPosition().z(), 1e-12);
    assertEquals(0, body.getVelocity().z(), 1e-12);
  }

  @Test
  void stepEndingExactlyOnTheGroundStillReportsTheImpact() {
    GroundContactEngine engine = new GroundContactEngine(new OwnPhysicsEngine(List.of()));
    PointMass body = new PointMass(1, new Vector3D(0, 0, 0.03), new Vector3D(0, 0, -3));
    engine.addBody(body);

    engine.step(0.01);
    List<GroundImpact> impacts = new ArrayList<>(engine.getLastImpacts());
    engine.step(0.01);
    impacts.addAll(engine.getLastImpacts());

    assertEquals(1, impacts.size());
    assertEquals(3, impacts.get(0).impactSpeed(), 1e-9);
    assertEquals(0, body.getVelocity().z(), 1e-12);
  }

  @Test
  void groundFrictionStopsSlidingBody() {
    GroundContactEngine engine = withGravity();
    PointMass body = new PointMass(1, Vector3D.ZERO, new Vector3D(4, 0, 0));
    engine.addBody(body);

    run(engine, 1);

    assertEquals(Vector3D.ZERO, body.getVelocity());
    double expectedSlide = 4 * 4 / (2 * SolidBody.DEFAULT_GROUND_FRICTION * G);
    assertEquals(expectedSlide, body.getPosition().x(), 0.02);
  }

  @Test
  void lowFrictionBodyKeepsRolling() {
    GroundContactEngine engine = withGravity();
    SolidPointMass cart = new SolidPointMass("cart", 1, Vector3D.ZERO, new Vector3D(4, 0, 0)) {
      @Override
      public double getGroundFriction() {
        return 0.01;
      }
    };
    engine.addBody(cart);

    run(engine, 1);

    assertEquals(4 - 0.01 * G, cart.getVelocity().x(), 1e-3);
  }

  @Test
  void solidBodyIsToldAboutItsImpactOnce() {
    GroundContactEngine engine = withGravity();
    List<Double> impacts = new ArrayList<>();
    engine.addBody(new SolidPointMass("ball", 1, new Vector3D(0, 0, 2), Vector3D.ZERO) {
      @Override
      public void onGroundImpact(double impactSpeed) {
        impacts.add(impactSpeed);
      }
    });

    run(engine, 2);

    assertEquals(1, impacts.size());
    assertEquals(Math.sqrt(2 * G * 2), impacts.get(0), 0.05);
  }

  @Test
  void keepsTimeAndForwardsBodies() {
    GroundContactEngine engine = withGravity();
    PointMass body = new PointMass(1, new Vector3D(0, 0, 10));
    assertTrue(engine.getLastImpacts().isEmpty());
    engine.addBody(body);
    assertEquals(List.of(body), engine.getBodies());

    engine.step(0.01);
    engine.step(0.01);

    assertEquals(0.02, engine.getTime(), 1e-12);
    assertTrue(engine.removeBody(body));
    assertFalse(engine.removeBody(body));
    assertThrows(IllegalArgumentException.class, () -> engine.step(0));
    assertThrows(NullPointerException.class, () -> new GroundContactEngine(null));
  }
}
