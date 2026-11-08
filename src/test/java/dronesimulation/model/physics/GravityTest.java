package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Gravity}.
 */
public class GravityTest {

  @Test
  public void defaultsToEarthGravity() {
    assertEquals(9.81, new Gravity().getAcceleration(), 0);
  }

  @Test
  public void pullsStraightDownWithWeight() {
    PointMass body = new PointMass(1.5, new Vector3D(3, 4, 50), new Vector3D(1, 2, 3));
    Vector3D force = new Gravity().compute(body);
    assertEquals(0, force.x(), 0);
    assertEquals(0, force.y(), 0);
    assertEquals(-14.715, force.z(), 1e-12);
  }

  @Test
  public void scalesWithMassAndAcceleration() {
    PointMass body = new PointMass(2.0, Vector3D.ZERO);
    Vector3D force = new Gravity(1.62).compute(body);
    assertEquals(-3.24, force.z(), 1e-12);
  }

  @Test
  public void zeroGravityGivesNoForce() {
    PointMass body = new PointMass(2.0, Vector3D.ZERO);
    assertEquals(0, new Gravity(0).compute(body).magnitude(), 0);
  }

  @Test
  public void rejectsNegativeOrNonFiniteAcceleration() {
    assertThrows(IllegalArgumentException.class, () -> new Gravity(-9.81));
    assertThrows(IllegalArgumentException.class, () -> new Gravity(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> new Gravity(Double.POSITIVE_INFINITY));
  }
}
