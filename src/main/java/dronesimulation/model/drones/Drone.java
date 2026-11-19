package dronesimulation.model.drones;

import dronesimulation.model.missions.MissionStep;
import dronesimulation.model.physics.PhysicsBody;

/**
 * A drone in the simulation.
 *
 * <p>A drone is a {@link PhysicsBody}, so the physics engine moves it. Its thrust is returned from
 * {@link #ownForces()}, and {@link #isActive()} is {@code false} once it has landed or crashed.
 * Flight controllers set the throttle, missions read and change its state, and views read it.
 */
public interface Drone extends PhysicsBody {

  /**
   * Returns the unique id of this drone.
   *
   * @return the id, never {@code null}
   */
  String getId();

  /**
   * Returns the current lifecycle state.
   *
   * @return the state, never {@code null}
   */
  DroneState getState();

  /**
   * Returns the remaining battery as a fraction.
   *
   * @return a value from 0.0 (empty) to 1.0 (full)
   */
  double getBatteryLevel();

  /**
   * Returns the maximum thrust the drone can produce.
   *
   * @return the thrust in newtons
   */
  double getMaxThrust();

  /**
   * Returns the current throttle.
   *
   * @return a value from 0.0 (off) to 1.0 (full)
   */
  double getThrottle();

  /**
   * Sets the throttle.
   *
   * @param throttle a value from 0.0 (off) to 1.0 (full)
   * @throws IllegalArgumentException if the throttle is outside 0.0 to 1.0
   */
  void setThrottle(double throttle);

  /**
   * Returns the mission step the drone is working on.
   *
   * @return the current step, never {@code null}
   */
  MissionStep getCurrentStep();

  /**
   * Starts a take-off.
   *
   * @throws IllegalStateException if the drone cannot take off in its current state
   */
  void takeOff();

  /**
   * Starts a landing.
   *
   * @throws IllegalStateException if the drone cannot land in its current state
   */
  void land();

  /**
   * Marks the drone as crashed.
   *
   * @throws IllegalStateException if the drone cannot crash in its current state
   */
  void crash();
}
