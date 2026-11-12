package dronesimulation.model.world;

import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.obstacles.ObstacleField;
import dronesimulation.model.physics.Collider;
import dronesimulation.model.physics.Collision;
import dronesimulation.model.physics.SolidBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Finds bodies touching each other or touching obstacles. */
public class CollisionDetector {

  private final Collider collider;

  /**
   * Creates a detector.
   *
   * @param collider decides when two bodies touch
   */
  public CollisionDetector(Collider collider) {
    this.collider = Objects.requireNonNull(collider, "collider");
  }

  /**
   * Checks every pair of bodies.
   *
   * @param bodies the bodies
   * @return each touching pair once
   */
  public List<Collision> between(List<? extends SolidBody> bodies) {
    List<Collision> found = new ArrayList<>();
    for (int i = 0; i < bodies.size(); i++) {
      for (int j = i + 1; j < bodies.size(); j++) {
        collider.check(bodies.get(i), bodies.get(j)).ifPresent(found::add);
      }
    }
    return found;
  }

  /**
   * Finds the first obstacle a body touches, treating the body as a sphere of its radius.
   *
   * @param body the body
   * @param obstacles the obstacles
   * @return the obstacle touched, or empty
   */
  public Optional<Obstacle> obstacleHit(SolidBody body, ObstacleField obstacles) {
    return Optional.ofNullable(obstacles.findCollision(body.getPosition(), body.getRadius()));
  }

}
