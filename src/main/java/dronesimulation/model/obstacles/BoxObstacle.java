package dronesimulation.model.obstacles;

import dronesimulation.model.Vector3D;

/**
 * A box shaped obstacle with its edges along the world axes, such as a building or a wall
 */
public final class BoxObstacle extends Obstacle {

  private final Vector3D min;
  private final Vector3D max;

  /**
   * Creates a box from two opposite corners
   *
   * @param id the name of the obstacle
   * @param min the corner with the smallest x, y and z values
   * @param max the corner with the largest x, y and z values
   * @throws IllegalArgumentException if a corner is null or min is not smaller than max on every
   *     axis
   */
  public BoxObstacle(String id, Vector3D min, Vector3D max) {
    super(id);
    if (min == null || max == null) {
      throw new IllegalArgumentException("Corners must not be null");
    }
    if (min.x() >= max.x() || min.y() >= max.y() || min.z() >= max.z()) {
      throw new IllegalArgumentException(
          "Min corner " + min + " must be smaller than max corner " + max + " on every axis");
    }
    this.min = min;
    this.max = max;
  }

  /**
   * Returns the corner with the smallest x, y and z values
   *
   * @return the min corner
   */
  public Vector3D getMin() {
    return min;
  }

  /**
   * Returns the corner with the largest x, y and z values
   *
   * @return the max corner
   */
  public Vector3D getMax() {
    return max;
  }

  @Override
  public double distanceTo(Vector3D point) {
    // Closest point on the box is the given point clamped to the box edges
    Vector3D closest = new Vector3D(
        clamp(point.x(), min.x(), max.x()),
        clamp(point.y(), min.y(), max.y()),
        clamp(point.z(), min.z(), max.z()));
    return point.subtract(closest).magnitude();
  }

  private static double clamp(double value, double low, double high) {
    return Math.max(low, Math.min(value, high));
  }
}
