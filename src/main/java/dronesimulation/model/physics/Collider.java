package dronesimulation.model.physics;

import java.util.Optional;

/**
 * Decides whether two bodies overlap (Strategy).
 */
@FunctionalInterface
public interface Collider {

  /**
   * Checks two bodies for overlap.
   *
   * @param a the first body
   * @param b the second body
   * @return the collision, or empty if they do not overlap or are the same body
   */
  Optional<Collision> check(SolidBody a, SolidBody b);
}
