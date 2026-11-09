package dronesimulation.model.physics;

import java.util.List;

/**
 * Moves a set of bodies forward in time.
 *
 * <p>The rest of the simulation depends only on this interface, so the team's own engine can be
 * swapped for another one (for example an ode4j adapter) without changing other code.
 */
public interface PhysicsEngine {

  /**
   * Adds a body to the simulation.
   *
   * @param body the body to add, not {@code null}
   * @throws NullPointerException if the body is {@code null}
   * @throws IllegalArgumentException if the body was already added
   */
  void addBody(PhysicsBody body);

  /**
   * Removes a body from the simulation. Does nothing if the body was never added.
   *
   * @param body the body to remove
   * @return {@code true} if the body was removed
   */
  boolean removeBody(PhysicsBody body);

  /**
   * Returns the bodies in the simulation, in the order they were added.
   *
   * @return a read-only view of the bodies
   */
  List<PhysicsBody> getBodies();

  /**
   * Advances every active body by one time step. The simulation normally calls this with
   * {@link PhysicsConstants#TIME_STEP}.
   *
   * @param dt the time step in seconds, must be positive and finite
   * @throws IllegalArgumentException if {@code dt} is not positive and finite
   */
  void step(double dt);
}
