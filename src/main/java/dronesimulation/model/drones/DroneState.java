package dronesimulation.model.drones;

/**
 * One stage of a drone's lifecycle (State pattern).
 *
 * <p>The stages are idle, taking off, flying, landing, landed and crashed. Each transition returns
 * the next state, or throws if the action is not allowed in the current state.
 */
public interface DroneState {

  /**
   * Returns the name of this state, for example {@code "FLYING"}.
   *
   * @return the state name, never {@code null}
   */
  String name();

  /**
   * Starts a take-off.
   *
   * @return the next state
   * @throws IllegalStateException if the drone cannot take off from this state
   */
  DroneState takeOff();

  /**
   * Starts a landing.
   *
   * @return the next state
   * @throws IllegalStateException if the drone cannot land from this state
   */
  DroneState land();

  /**
   * Marks the drone as crashed.
   *
   * @return the next state
   * @throws IllegalStateException if the drone cannot crash from this state
   */
  DroneState crash();
}
