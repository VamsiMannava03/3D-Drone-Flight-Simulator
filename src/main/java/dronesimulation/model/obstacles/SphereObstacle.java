package dronesimulation.model.obstacles;

import dronesimulation.model.Vector3D;

/**
 * A round obstacle, such as a balloon or a tree canopy
 */
public final class SphereObstacle extends Obstacle {

  private final Vector3D center;
  private final double radius;

  /**
   * Creates a sphere from its center and radius
   *
   * @param id the name of the obstacle
   * @param center the center of the sphere
   * @param radius the radius in meters
   * @throws IllegalArgumentException if the center is null or the radius is not a positive number
   */
  public SphereObstacle(String id, Vector3D center, double radius) {
    super(id);
    if (center == null) {
      throw new IllegalArgumentException("Center must not be null");
    }
    if (!Double.isFinite(radius) || radius <= 0) {
      throw new IllegalArgumentException("Radius must be a finite number > 0 but was " + radius);
    }
    this.center = center;
    this.radius = radius;
  }

  /**
   * Returns the center of the sphere
   *
   * @return the center point
   */
  public Vector3D getCenter() {
    return center;
  }

  /**
   * Returns the radius of the sphere
   *
   * @return the radius in meters
   */
  public double getRadius() {
    return radius;
  }

  @Override
  public double distanceTo(Vector3D point) {
    double distance = point.subtract(center).magnitude() - radius;
    // Points inside the sphere count as 0
    return Math.max(0, distance);
  }
}
