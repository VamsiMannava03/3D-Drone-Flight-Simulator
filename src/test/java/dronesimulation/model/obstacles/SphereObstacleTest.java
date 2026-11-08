package dronesimulation.model.obstacles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SphereObstacle}
 */
public class SphereObstacleTest {

  private static final double DELTA = 1e-9;

  private final SphereObstacle balloon =
      new SphereObstacle("balloon", new Vector3D(0, 0, 0), 2);

  @Test
  public void testGetters() {
    assertEquals("balloon", balloon.getId());
    assertEquals(Vector3D.ZERO, balloon.getCenter());
    assertEquals(2, balloon.getRadius(), DELTA);
  }

  @Test
  public void testDistanceIsZeroInside() {
    assertEquals(0, balloon.distanceTo(Vector3D.ZERO), DELTA);
    assertEquals(0, balloon.distanceTo(new Vector3D(1, 1, 0)), DELTA);
  }

  @Test
  public void testDistanceOutside() {
    assertEquals(3, balloon.distanceTo(new Vector3D(5, 0, 0)), DELTA);
    // Point (3, 4, 0) is 5 from the center
    assertEquals(3, balloon.distanceTo(new Vector3D(3, 4, 0)), DELTA);
  }

  @Test
  public void testContains() {
    assertTrue(balloon.contains(new Vector3D(0, 2, 0)));
    assertFalse(balloon.contains(new Vector3D(0, 2.1, 0)));
  }

  @Test
  public void testCollidesWhenDroneJustTouches() {
    assertTrue(balloon.collidesWith(new Vector3D(3, 0, 0), 1));
  }

  @Test
  public void testNoCollisionWhenDroneIsClear() {
    assertFalse(balloon.collidesWith(new Vector3D(3.1, 0, 0), 1));
  }

  @Test
  public void testRejectsNullCenter() {
    assertThrows(IllegalArgumentException.class, () -> new SphereObstacle("ball", null, 1));
  }

  @Test
  public void testRejectsBadRadius() {
    assertThrows(IllegalArgumentException.class,
        () -> new SphereObstacle("ball", Vector3D.ZERO, 0));
    assertThrows(IllegalArgumentException.class,
        () -> new SphereObstacle("ball", Vector3D.ZERO, -1));
    assertThrows(IllegalArgumentException.class,
        () -> new SphereObstacle("ball", Vector3D.ZERO, Double.NaN));
  }
}
