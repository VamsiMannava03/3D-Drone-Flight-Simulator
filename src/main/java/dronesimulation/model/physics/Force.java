package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;

/**
 * A force that acts on a body (Strategy pattern).
 *
 * <p>Each kind of force, such as gravity or drag, is its own implementation, so new forces can be
 * added to the engine without changing existing classes.
 */
@FunctionalInterface
public interface Force {

  /**
   * Computes this force on a body in its current state. Must not modify the body.
   *
   * @param body the body the force acts on
   * @return the force in newtons
   */
  Vector3D compute(PhysicsBody body);
}
