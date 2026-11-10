package dronesimulation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Vector3D}.
 */
public class Vector3DTest {

  private static final double EPS = 1e-9;

  private static void assertVector(double x, double y, double z, Vector3D actual) {
    assertEquals(x, actual.x(), EPS);
    assertEquals(y, actual.y(), EPS);
    assertEquals(z, actual.z(), EPS);
  }

  @Test
  public void storesComponents() {
    assertVector(1, 2, 3, new Vector3D(1, 2, 3));
  }

  @Test
  public void zeroIsAllZeros() {
    assertVector(0, 0, 0, Vector3D.ZERO);
  }

  @Test
  public void upIsUnitZ() {
    assertVector(0, 0, 1, Vector3D.UP);
  }

  @Test
  public void rejectsNaNAndInfinity() {
    assertThrows(IllegalArgumentException.class, () -> new Vector3D(Double.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new Vector3D(0, Double.POSITIVE_INFINITY, 0));
    assertThrows(IllegalArgumentException.class, () -> new Vector3D(0, 0, Double.NEGATIVE_INFINITY));
  }

  @Test
  public void addAndSubtract() {
    Vector3D a = new Vector3D(1, 2, 3);
    Vector3D b = new Vector3D(4, -5, 6);
    assertVector(5, -3, 9, a.add(b));
    assertVector(-3, 7, -3, a.subtract(b));
  }

  @Test
  public void scaleMultipliesEveryComponent() {
    assertVector(2, -4, 6, new Vector3D(1, -2, 3).scale(2));
    assertVector(0, 0, 0, new Vector3D(1, -2, 3).scale(0));
  }

  @Test
  public void operationsDoNotModifyTheOriginal() {
    Vector3D a = new Vector3D(1, 2, 3);
    a.add(new Vector3D(1, 1, 1));
    a.scale(10);
    a.normalize();
    assertVector(1, 2, 3, a);
  }

  @Test
  public void dotProduct() {
    assertEquals(32, new Vector3D(1, 2, 3).dot(new Vector3D(4, 5, 6)), EPS);
    assertEquals(0, new Vector3D(1, 0, 0).dot(new Vector3D(0, 1, 0)), EPS);
  }

  @Test
  public void crossProduct() {
    assertVector(-3, 6, -3, new Vector3D(1, 2, 3).cross(new Vector3D(4, 5, 6)));
    Vector3D x = new Vector3D(1, 0, 0);
    Vector3D y = new Vector3D(0, 1, 0);
    assertVector(0, 0, 1, x.cross(y));
    assertVector(0, 0, -1, y.cross(x));
  }

  @Test
  public void crossProductIsPerpendicularToBothInputs() {
    Vector3D a = new Vector3D(1, 2, 3);
    Vector3D b = new Vector3D(4, 5, 6);
    Vector3D c = a.cross(b);
    assertEquals(0, c.dot(a), EPS);
    assertEquals(0, c.dot(b), EPS);
  }

  @Test
  public void crossProductOfParallelVectorsIsZero() {
    Vector3D a = new Vector3D(1, 2, 3);
    assertVector(0, 0, 0, a.cross(a.scale(3)));
  }

  @Test
  public void magnitude() {
    assertEquals(5, new Vector3D(3, 4, 0).magnitude(), EPS);
    assertEquals(3, new Vector3D(1, 2, 2).magnitude(), EPS);
    assertEquals(0, Vector3D.ZERO.magnitude(), EPS);
  }

  @Test
  public void normalizeGivesUnitLengthAndKeepsDirection() {
    Vector3D n = new Vector3D(0, 3, 4).normalize();
    assertVector(0, 0.6, 0.8, n);
    assertEquals(1, n.magnitude(), EPS);
  }

  @Test
  public void normalizeOfZeroVectorThrows() {
    assertThrows(IllegalStateException.class, Vector3D.ZERO::normalize);
  }

  @Test
  public void equalityIsByValue() {
    assertEquals(new Vector3D(1, 2, 3), new Vector3D(1, 2, 3));
    assertEquals(new Vector3D(1, 2, 3).hashCode(), new Vector3D(1, 2, 3).hashCode());
    assertNotEquals(new Vector3D(1, 2, 3), new Vector3D(1, 2, 4));
  }

  @Test
  public void supportsSemiImplicitEulerFreeFall() {
    Vector3D gravity = new Vector3D(0, 0, -9.81);
    double dt = 0.01;
    Vector3D velocity = Vector3D.ZERO;
    Vector3D position = Vector3D.ZERO;
    for (int i = 0; i < 100; i++) {
      velocity = velocity.add(gravity.scale(dt));
      position = position.add(velocity.scale(dt));
    }
    assertVector(0, 0, -9.81, velocity);
    assertVector(0, 0, -4.95405, position);
  }

  @Test
  public void distanceToIsLengthOfDifference() {
    assertEquals(5, new Vector3D(1, 2, 3).distanceTo(new Vector3D(4, 6, 3)), 1e-12);
    assertEquals(0, Vector3D.UP.distanceTo(Vector3D.UP), 0);
  }
}
