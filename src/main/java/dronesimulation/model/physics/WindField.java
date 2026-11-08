package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;

/**
 * Supplies the velocity of the air at a point.
 *
 * <p>Wind is not a separate push on a body: it changes the body's velocity relative to the air,
 * which {@link QuadraticDrag} turns into a force. Implementations that use randomness, such as
 * gusts, must draw it from a seeded generator so runs are repeatable.
 */
@FunctionalInterface
public interface WindField {

  /**
   * Returns the air velocity at a position.
   *
   * @param position the position in metres
   * @return the air velocity in m/s
   */
  Vector3D airVelocityAt(Vector3D position);
}
