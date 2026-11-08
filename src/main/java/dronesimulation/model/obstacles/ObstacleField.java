package dronesimulation.model.obstacles;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds all obstacles in the world and checks drones against them
 */
public class ObstacleField {

  private final List<Obstacle> obstacles = new ArrayList<>();

  /**
   * Adds an obstacle to the world
   *
   * @param obstacle the obstacle to add
   * @throws IllegalArgumentException if the obstacle is null or its id is already used
   */
  public void add(Obstacle obstacle) {
    if (obstacle == null) {
      throw new IllegalArgumentException("Obstacle must not be null");
    }
    if (find(obstacle.getId()) != null) {
      throw new IllegalArgumentException("Obstacle id already used: " + obstacle.getId());
    }
    obstacles.add(obstacle);
  }

  /**
   * Removes the obstacle with the given id
   *
   * @param id the id of the obstacle to remove
   * @return true if an obstacle was removed
   */
  public boolean remove(String id) {
    Obstacle obstacle = find(id);
    if (obstacle == null) {
      return false;
    }
    return obstacles.remove(obstacle);
  }

  /**
   * Returns the obstacle with the given id
   *
   * @param id the id to look for
   * @return the matching obstacle, or null if there is none
   */
  public Obstacle find(String id) {
    for (Obstacle obstacle : obstacles) {
      if (obstacle.getId().equals(id)) {
        return obstacle;
      }
    }
    return null;
  }

  /**
   * Returns the first obstacle a drone, treated as a sphere, is touching
   *
   * @param center the center of the drone
   * @param radius the radius of the drone in meters
   * @return the obstacle hit, or null if the drone is clear
   * @throws IllegalArgumentException if the center is null or the radius is negative or not finite
   */
  public Obstacle findCollision(Vector3D center, double radius) {
    for (Obstacle obstacle : obstacles) {
      if (obstacle.collidesWith(center, radius)) {
        return obstacle;
      }
    }
    return null;
  }

  /**
   * Checks if a drone, treated as a sphere, is touching any obstacle
   *
   * @param center the center of the drone
   * @param radius the radius of the drone in meters
   * @return true if the drone hits at least one obstacle
   * @throws IllegalArgumentException if the center is null or the radius is negative or not finite
   */
  public boolean hasCollision(Vector3D center, double radius) {
    return findCollision(center, radius) != null;
  }

  /**
   * Returns a read only view of all obstacles, in the order they were added
   *
   * @return the obstacles
   */
  public List<Obstacle> getObstacles() {
    return Collections.unmodifiableList(obstacles);
  }

  /**
   * Returns the number of obstacles
   *
   * @return the obstacle count
   */
  public int size() {
    return obstacles.size();
  }
}
