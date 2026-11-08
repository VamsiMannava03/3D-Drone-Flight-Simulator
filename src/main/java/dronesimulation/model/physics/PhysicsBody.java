package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;

/**
 * Something the physics engine can move: a point mass with a position and a velocity.
 *
 * <p>Bodies have no rotation. The engine reads the mass and the body's own forces (such as a
 * drone's thrust), adds the global forces (gravity, drag), and writes back the new velocity and
 * position each step. Drones and other simulated objects implement this interface.
 */
public interface PhysicsBody {

  /**
   * Returns the current position.
   *
   * @return the position in metres
   */
  Vector3D getPosition();

  /**
   * Sets the position. Called by the physics engine after integration.
   *
   * @param position the new position in metres, not {@code null}
   */
  void setPosition(Vector3D position);

  /**
   * Returns the current velocity.
   *
   * @return the velocity in m/s
   */
  Vector3D getVelocity();

  /**
   * Sets the velocity. Called by the physics engine after integration.
   *
   * @param velocity the new velocity in m/s, not {@code null}
   */
  void setVelocity(Vector3D velocity);

  /**
   * Returns the current mass.
   *
   * @return the mass in kilograms, always positive
   */
  double getMass();

  /**
   * Returns whether the engine should keep moving this body. A crashed drone, for example, is
   * inactive and stays where it is.
   *
   * @return {@code true} if the body takes part in the simulation; {@code true} by default
   */
  default boolean isActive() {
    return true;
  }

  /**
   * Returns the sum of the forces this body produces itself, such as thrust or lift. Global
   * forces like gravity and drag are added by the engine and must not be included here.
   *
   * @return the body's own net force in newtons; {@link Vector3D#ZERO} by default
   */
  default Vector3D ownForces() {
    return Vector3D.ZERO;
  }
}
