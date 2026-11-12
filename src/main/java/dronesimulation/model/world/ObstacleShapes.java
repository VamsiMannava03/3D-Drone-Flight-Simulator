package dronesimulation.model.world;

import dronesimulation.model.Vector3D;
import dronesimulation.model.obstacles.BoxObstacle;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.obstacles.SphereObstacle;
import java.util.Optional;

/**
 * Shape questions the world asks about obstacles, answered for each kind of obstacle it knows.
 */
public final class ObstacleShapes {

  private ObstacleShapes() {
    // static helpers only
  }

  /**
   * Returns the height of an obstacle's highest point.
   *
   * @param obstacle the obstacle
   * @return the top in m, or 0 for an unknown kind of obstacle
   */
  public static double top(Obstacle obstacle) {
    if (obstacle instanceof Building building) {
      return building.top();
    }
    if (obstacle instanceof BoxObstacle box) {
      return box.getMax().z();
    }
    if (obstacle instanceof SphereObstacle sphere) {
      return sphere.getCenter().z() + sphere.getRadius();
    }
    return 0;
  }

  /**
   * Returns the point on or in an obstacle closest to a given point.
   *
   * @param obstacle the obstacle
   * @param point the point
   * @return the closest point, the point itself if it is inside, or empty for an unknown kind
   *     of obstacle
   */
  public static Optional<Vector3D> closestPoint(Obstacle obstacle, Vector3D point) {
    if (obstacle instanceof Building building) {
      return Optional.of(clamp(point, building.min(), building.max()));
    }
    if (obstacle instanceof BoxObstacle box) {
      return Optional.of(clamp(point, box.getMin(), box.getMax()));
    }
    if (obstacle instanceof SphereObstacle sphere) {
      Vector3D fromCentre = point.subtract(sphere.getCenter());
      double distance = fromCentre.magnitude();
      if (distance <= sphere.getRadius()) {
        return Optional.of(point);
      }
      return Optional.of(sphere.getCenter().add(fromCentre.scale(sphere.getRadius() / distance)));
    }
    return Optional.empty();
  }

  private static Vector3D clamp(Vector3D p, Vector3D min, Vector3D max) {
    return new Vector3D(clamp(p.x(), min.x(), max.x()), clamp(p.y(), min.y(), max.y()),
        clamp(p.z(), min.z(), max.z()));
  }

  private static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
  }
}
