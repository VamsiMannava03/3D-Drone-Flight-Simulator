package dronesimulation.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.obstacles.BoxObstacle;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.obstacles.SphereObstacle;
import org.junit.jupiter.api.Test;

class BuildingTest {

  @Test
  void blockStandsOnTheGroundAroundItsCentre() {
    Building b = Building.block("B", 10, 20, 4, 6, 12);
    assertEquals("B", b.name());
    assertEquals(new Vector3D(8, 17, 0), b.min());
    assertEquals(new Vector3D(12, 23, 12), b.max());
    assertEquals(Building.Style.BLOCK, b.style());
    assertEquals(12, b.top());
    assertEquals(0, b.roofHeight());
    assertEquals(12, b.eavesHeight());
  }

  @Test
  void houseHasAPitchedRoof() {
    Building house = Building.house("Elm St 1", 0, 0, 10, 8, 8.5);
    assertEquals(Building.Style.HOUSE, house.style());
    assertEquals(3.2, house.roofHeight(), 1e-9);
    assertEquals(8.5 - 3.2, house.eavesHeight(), 1e-9);
    assertEquals(2, Building.house("Hut", 0, 0, 10, 10, 4).roofHeight(), 1e-9);
  }

  @Test
  void distanceUsesTheWholeBox() {
    Building b = Building.block("B", 0, 0, 2, 2, 10);
    assertEquals(0, b.distanceTo(new Vector3D(0, 0, 5)));
    assertEquals(3, b.distanceTo(new Vector3D(4, 0, 5)), 1e-9);
    assertTrue(b.collidesWith(new Vector3D(0, 0, 10.2), 0.25));
  }

  @Test
  void rejectsBadInput() {
    assertThrows(IllegalArgumentException.class,
        () -> new Building("X", new Vector3D(1, 1, 1), Vector3D.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> new Building(" ", Vector3D.ZERO, new Vector3D(1, 1, 1)));
    assertThrows(NullPointerException.class,
        () -> new Building("X", Vector3D.ZERO, new Vector3D(1, 1, 1), null));
    assertEquals(Building.Style.BLOCK,
        new Building("Bridge", Vector3D.ZERO, new Vector3D(1, 1, 1)).style());
  }

  @Test
  void buildingsWithTheSameValuesAreEqual() {
    Building a = Building.house("H", 0, 0, 8, 8, 6);
    assertEquals(a, Building.house("H", 0, 0, 8, 8, 6));
    assertEquals(a.hashCode(), Building.house("H", 0, 0, 8, 8, 6).hashCode());
    assertNotEquals(a, Building.block("H", 0, 0, 8, 8, 6));
    assertNotEquals(a, Building.house("G", 0, 0, 8, 8, 6));
    assertNotEquals(a, Building.house("H", 1, 0, 8, 8, 6));
    assertNotEquals(a, "H");
    assertTrue(a.toString().contains("HOUSE"));
  }

  @Test
  void shapesKnowTopAndClosestPoint() {
    Obstacle box = new BoxObstacle("box", Vector3D.ZERO, new Vector3D(2, 2, 3));
    Obstacle ball = new SphereObstacle("ball", new Vector3D(0, 0, 10), 2);
    Obstacle building = Building.block("B", 0, 0, 2, 2, 7);
    Obstacle unknown = new Obstacle("odd") {
      @Override
      public double distanceTo(Vector3D point) {
        return 1;
      }
    };

    assertEquals(3, ObstacleShapes.top(box));
    assertEquals(12, ObstacleShapes.top(ball));
    assertEquals(7, ObstacleShapes.top(building));
    assertEquals(0, ObstacleShapes.top(unknown));

    assertEquals(new Vector3D(2, 1, 3),
        ObstacleShapes.closestPoint(box, new Vector3D(5, 1, 4)).orElseThrow());
    assertEquals(new Vector3D(1, 0, 7),
        ObstacleShapes.closestPoint(building, new Vector3D(3, 0, 9)).orElseThrow());
    assertEquals(new Vector3D(0, 0, 12),
        ObstacleShapes.closestPoint(ball, new Vector3D(0, 0, 20)).orElseThrow());
    Vector3D inside = new Vector3D(0, 0, 10.5);
    assertEquals(inside, ObstacleShapes.closestPoint(ball, inside).orElseThrow());
    assertTrue(ObstacleShapes.closestPoint(unknown, Vector3D.ZERO).isEmpty());
  }
}
