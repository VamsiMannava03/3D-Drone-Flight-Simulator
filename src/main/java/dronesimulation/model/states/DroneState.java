package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/**
 * One stage of the drone lifecycle (State pattern):
 * Idle, TakingOff, Flying, Landing, Landed, Charging, Crashed.
 *
 * <p>Each transition returns the next state. The defaults reject the transition, so each state
 * overrides only the transitions that are legal from it, and illegal actions such as taking off
 * after a crash fail with a clear exception.
 */
public interface DroneState {

  /**
   * Returns the display name of this state.
   *
   * @return the name, for example "Flying"
   */
  String name();

  /**
   * Tells whether the motors may produce thrust in this state.
   *
   * @return true if the drone may fly
   */
  default boolean canFly() {
    return false;
  }

  /**
   * Tells whether a collision can crash the drone in this state.
   *
   * @return true if {@link #crash(Drone)} is allowed
   */
  default boolean canCrash() {
    return false;
  }

  /**
   * Starts a take-off.
   *
   * @param drone the drone
   * @return the next state
   * @throws IllegalStateException if take-off is not allowed from this state
   */
  default DroneState takeOff(Drone drone) {
    throw illegal("take off");
  }

  /**
   * Starts a landing.
   *
   * @param drone the drone
   * @return the next state
   * @throws IllegalStateException if landing is not allowed from this state
   */
  default DroneState land(Drone drone) {
    throw illegal("land");
  }

  /**
   * Starts charging.
   *
   * @param drone the drone
   * @return the next state
   * @throws IllegalStateException if charging is not allowed from this state
   */
  default DroneState charge(Drone drone) {
    throw illegal("charge");
  }

  /**
   * Marks the drone as crashed.
   *
   * @param drone the drone
   * @return the next state
   * @throws IllegalStateException if a crash is not possible from this state
   */
  default DroneState crash(Drone drone) {
    throw illegal("crash");
  }

  /**
   * Reacts to the drone hitting the ground. Ignored by default.
   *
   * @param drone the drone
   * @param impactSpeed the downward speed at impact, in m/s
   * @return the next state
   */
  default DroneState touchdown(Drone drone, double impactSpeed) {
    return this;
  }

  /**
   * Advances time-based behaviour within this state, such as finishing a take-off.
   *
   * @param drone the drone
   * @param dt the time step in s
   * @return the state for the next step, which may be this one
   */
  default DroneState update(Drone drone, double dt) {
    return this;
  }

  private IllegalStateException illegal(String action) {
    return new IllegalStateException("Cannot " + action + " while " + name());
  }
}
