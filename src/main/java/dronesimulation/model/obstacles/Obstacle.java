package dronesimulation.model.obstacles;

import dronesimulation.model.Vector3D;

/**
 * A fixed solid object in the world that drones can crash into
 *
 * <p>Each shape only defines how far a point is from its surface. Collision checks are shared
 * here so every shape follows the same rules
 */
public abstract class Obstacle {

  private final String id;

  /**
   * Creates an obstacle with a unique name
   *
   * @param id the name of the obstacle, such as "tower-1"
   * @throws IllegalArgumentException if the id is null or blank
   */
  protected Obstacle(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Obstacle id must not be blank");
    }
    this.id = id;
  }

  /**
   * Returns the name of this obstacle
   *
   * @return the obstacle id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the shortest distance from a point to the surface of this obstacle
   *
   * @param point the point to measure from
   * @return the distance in meters, or 0 if the point is inside
   */
  public abstract double distanceTo(Vector3D point);

  /**
   * Checks if a point is inside or on the surface of this obstacle
   *
   * @param point the point to check
   * @return true if the point is inside or touching
   */
  public boolean contains(Vector3D point) {
    return distanceTo(point) == 0;
  }

  /**
   * Checks if a drone, treated as a sphere, touches this obstacle
   *
   * @param center the center of the drone
   * @param radius the radius of the drone in meters
   * @return true if the drone touches or overlaps the obstacle
   * @throws IllegalArgumentException if the center is null or the radius is negative or not finite
   */
  public boolean collidesWith(Vector3D center, double radius) {
    if (center == null) {
      throw new IllegalArgumentException("Center must not be null");
    }
    if (!Double.isFinite(radius) || radius < 0) {
      throw new IllegalArgumentException("Radius must be a finite number >= 0 but was " + radius);
    }
    return distanceTo(center) <= radius;
  }
}
