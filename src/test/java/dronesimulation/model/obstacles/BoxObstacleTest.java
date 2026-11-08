package dronesimulation.model.obstacles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BoxObstacle}
 */
public class BoxObstacleTest {

  private static final double DELTA = 1e-9;

  // 10 x 10 x 10 box from the origin
  private final BoxObstacle box =
      new BoxObstacle("building", new Vector3D(0, 0, 0), new Vector3D(10, 10, 10));

  @Test
  public void testGetters() {
    assertEquals("building", box.getId());
    assertEquals(new Vector3D(0, 0, 0), box.getMin());
    assertEquals(new Vector3D(10, 10, 10), box.getMax());
  }

  @Test
  public void testDistanceIsZeroInside() {
    assertEquals(0, box.distanceTo(new Vector3D(5, 5, 5)), DELTA);
  }

  @Test
  public void testDistanceToFace() {
    assertEquals(3, box.distanceTo(new Vector3D(13, 5, 5)), DELTA);
    assertEquals(2, box.distanceTo(new Vector3D(5, -2, 5)), DELTA);
  }

  @Test
  public void testDistanceToCorner() {
    // 3-4-5 triangle off the corner at (10, 10, z)
    assertEquals(5, box.distanceTo(new Vector3D(13, 14, 5)), DELTA);
  }

  @Test
  public void testContains() {
    assertTrue(box.contains(new Vector3D(5, 5, 5)));
    assertTrue(box.contains(new Vector3D(10, 10, 10)));
    assertFalse(box.contains(new Vector3D(10.1, 5, 5)));
  }

  @Test
  public void testCollidesWhenDroneOverlaps() {
    assertTrue(box.collidesWith(new Vector3D(10.4, 5, 5), 0.5));
  }

  @Test
  public void testCollidesWhenDroneJustTouches() {
    assertTrue(box.collidesWith(new Vector3D(10.5, 5, 5), 0.5));
  }

  @Test
  public void testNoCollisionWhenDroneIsClear() {
    assertFalse(box.collidesWith(new Vector3D(10.6, 5, 5), 0.5));
  }

  @Test
  public void testRejectsBlankId() {
    assertThrows(IllegalArgumentException.class,
        () -> new BoxObstacle(" ", Vector3D.ZERO, new Vector3D(1, 1, 1)));
    assertThrows(IllegalArgumentException.class,
        () -> new BoxObstacle(null, Vector3D.ZERO, new Vector3D(1, 1, 1)));
  }

  @Test
  public void testRejectsNullCorner() {
    assertThrows(IllegalArgumentException.class,
        () -> new BoxObstacle("box", null, new Vector3D(1, 1, 1)));
    assertThrows(IllegalArgumentException.class,
        () -> new BoxObstacle("box", Vector3D.ZERO, null));
  }

  @Test
  public void testRejectsFlatOrFlippedBox() {
    assertThrows(IllegalArgumentException.class,
        () -> new BoxObstacle("box", Vector3D.ZERO, new Vector3D(1, 0, 1)));
    assertThrows(IllegalArgumentException.class,
        () -> new BoxObstacle("box", new Vector3D(1, 1, 1), Vector3D.ZERO));
  }

  @Test
  public void testRejectsBadDroneInput() {
    assertThrows(IllegalArgumentException.class, () -> box.collidesWith(null, 1));
    assertThrows(IllegalArgumentException.class, () -> box.collidesWith(Vector3D.ZERO, -1));
    assertThrows(IllegalArgumentException.class,
        () -> box.collidesWith(Vector3D.ZERO, Double.NaN));
    assertThrows(IllegalArgumentException.class,
        () -> box.collidesWith(Vector3D.ZERO, Double.POSITIVE_INFINITY));
  }
}
