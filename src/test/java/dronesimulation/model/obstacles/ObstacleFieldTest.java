package dronesimulation.model.obstacles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ObstacleField}
 */
public class ObstacleFieldTest {

  private static final double DRONE_RADIUS = 0.5;

  private ObstacleField field;
  private BoxObstacle tower;
  private SphereObstacle balloon;

  @BeforeEach
  public void setUp() {
    field = new ObstacleField();
    tower = new BoxObstacle("tower", new Vector3D(0, 0, 0), new Vector3D(4, 4, 30));
    balloon = new SphereObstacle("balloon", new Vector3D(20, 20, 15), 3);
    field.add(tower);
    field.add(balloon);
  }

  @Test
  public void testStartsEmpty() {
    ObstacleField empty = new ObstacleField();
    assertEquals(0, empty.size());
    assertFalse(empty.hasCollision(Vector3D.ZERO, DRONE_RADIUS));
  }

  @Test
  public void testAddAndFind() {
    assertEquals(2, field.size());
    assertSame(tower, field.find("tower"));
    assertSame(balloon, field.find("balloon"));
    assertNull(field.find("missing"));
  }

  @Test
  public void testRejectsNullObstacle() {
    assertThrows(IllegalArgumentException.class, () -> field.add(null));
  }

  @Test
  public void testRejectsDuplicateId() {
    SphereObstacle copy = new SphereObstacle("tower", Vector3D.ZERO, 1);
    assertThrows(IllegalArgumentException.class, () -> field.add(copy));
    assertEquals(2, field.size());
  }

  @Test
  public void testRemove() {
    assertTrue(field.remove("tower"));
    assertNull(field.find("tower"));
    assertEquals(1, field.size());
    assertFalse(field.remove("tower"));
  }

  @Test
  public void testFindCollisionReturnsObstacleHit() {
    assertSame(tower, field.findCollision(new Vector3D(4.2, 2, 10), DRONE_RADIUS));
    assertSame(balloon, field.findCollision(new Vector3D(20, 20, 18.5), DRONE_RADIUS));
  }

  @Test
  public void testNoCollisionInOpenSpace() {
    Vector3D openSpace = new Vector3D(10, 10, 10);
    assertNull(field.findCollision(openSpace, DRONE_RADIUS));
    assertFalse(field.hasCollision(openSpace, DRONE_RADIUS));
  }

  @Test
  public void testHasCollision() {
    assertTrue(field.hasCollision(new Vector3D(2, 2, 31.5), 2));
  }

  @Test
  public void testDroneFlyingIntoTowerCrashes() {
    // Drone flies toward the tower one 0.01 s step at a time at 5 m/s
    Vector3D position = new Vector3D(-10, 2, 10);
    Vector3D stepMove = new Vector3D(0.05, 0, 0);
    int steps = 0;
    while (!field.hasCollision(position, DRONE_RADIUS) && steps < 1000) {
      position = position.add(stepMove);
      steps++;
    }
    assertSame(tower, field.findCollision(position, DRONE_RADIUS));
    // Contact happens when the drone edge reaches x = 0
    assertEquals(-DRONE_RADIUS, position.x(), 0.05);
  }

  @Test
  public void testObstacleListIsReadOnly() {
    List<Obstacle> obstacles = field.getObstacles();
    assertEquals(2, obstacles.size());
    assertSame(tower, obstacles.get(0));
    assertThrows(UnsupportedOperationException.class, () -> obstacles.add(balloon));
  }
}
