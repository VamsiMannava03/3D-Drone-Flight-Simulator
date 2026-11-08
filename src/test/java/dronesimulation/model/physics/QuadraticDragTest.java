package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link QuadraticDrag}.
 */
public class QuadraticDragTest {

  private static final double EPS = 1e-12;

  @Test
  public void noDragAtRestInStillAir() {
    PointMass body = new PointMass(1.0, Vector3D.ZERO);
    assertEquals(0, new QuadraticDrag(0.1).compute(body).magnitude(), 0);
  }

  @Test
  public void opposesMotionWithSquaredSpeed() {
    PointMass body = new PointMass(1.0, Vector3D.ZERO, new Vector3D(0, 0, -10));
    Vector3D force = new QuadraticDrag(0.1).compute(body);
    // |F| = k v^2 = 0.1 * 100 = 10, pointing up
    assertEquals(0, force.x(), EPS);
    assertEquals(0, force.y(), EPS);
    assertEquals(10, force.z(), EPS);
  }

  @Test
  public void worksInAnyDirection() {
    PointMass body = new PointMass(1.0, Vector3D.ZERO, new Vector3D(3, 4, 0));
    Vector3D force = new QuadraticDrag(0.2).compute(body);
    // |v| = 5, F = -0.2 * 5 * (3, 4, 0)
    assertEquals(-3, force.x(), EPS);
    assertEquals(-4, force.y(), EPS);
    assertEquals(0, force.z(), EPS);
  }

  @Test
  public void bodyMovingWithTheWindFeelsNoDrag() {
    WindField wind = position -> new Vector3D(5, 0, 0);
    PointMass body = new PointMass(1.0, Vector3D.ZERO, new Vector3D(5, 0, 0));
    assertEquals(0, new QuadraticDrag(0.1, wind).compute(body).magnitude(), 0);
  }

  @Test
  public void hoveringInWindFeelsSameDragAsFlyingInStillAir() {
    WindField wind = position -> new Vector3D(5, 0, 0);
    PointMass hovering = new PointMass(1.0, Vector3D.ZERO);
    PointMass flying = new PointMass(1.0, Vector3D.ZERO, new Vector3D(-5, 0, 0));

    Vector3D inWind = new QuadraticDrag(0.1, wind).compute(hovering);
    Vector3D inStillAir = new QuadraticDrag(0.1).compute(flying);

    assertEquals(inStillAir, inWind);
    assertEquals(2.5, inWind.x(), EPS);
  }

  @Test
  public void windIsSampledAtTheBodyPosition() {
    WindField wind = position -> new Vector3D(position.z(), 0, 0);
    PointMass body = new PointMass(1.0, new Vector3D(0, 0, 2));
    // air moves at +2 m/s in x, so drag pushes the body along +x with k * 2 * 2
    assertEquals(0.4, new QuadraticDrag(0.1, wind).compute(body).x(), EPS);
  }

  @Test
  public void zeroCoefficientDisablesDrag() {
    PointMass body = new PointMass(1.0, Vector3D.ZERO, new Vector3D(10, 10, 10));
    assertEquals(0, new QuadraticDrag(0).compute(body).magnitude(), 0);
  }

  @Test
  public void exposesCoefficientAndWind() {
    QuadraticDrag stillAir = new QuadraticDrag(0.1);
    assertEquals(0.1, stillAir.getCoefficient(), 0);
    assertSame(NoWind.INSTANCE, stillAir.getWind());

    WindField wind = position -> Vector3D.ZERO;
    assertSame(wind, new QuadraticDrag(0.1, wind).getWind());
  }

  @Test
  public void rejectsInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> new QuadraticDrag(-0.1));
    assertThrows(IllegalArgumentException.class, () -> new QuadraticDrag(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> new QuadraticDrag(Double.NEGATIVE_INFINITY));
    assertThrows(NullPointerException.class, () -> new QuadraticDrag(0.1, null));
  }
}
