package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;

/**
 * One step of a mission, such as take off, go to a point, hover, drop a payload or land (Command
 * pattern).
 *
 * <p>A mission runs its steps in order. The current step is executed every simulation step until
 * it reports that it is complete.
 */
public interface MissionStep {

  /**
   * Checks whether the given drone is able to perform this step.
   *
   * @param drone the drone to check, not {@code null}
   * @return {@code true} if the drone can perform this step
   */
  boolean isValidFor(Drone drone);

  /**
   * Runs one simulation step of this command on the drone.
   *
   * @param drone the drone to act on, not {@code null}
   */
  void execute(Drone drone);

  /**
   * Checks whether this step has finished.
   *
   * @param drone the drone performing the step, not {@code null}
   * @return {@code true} once the step is done
   */
  boolean isComplete(Drone drone);
}
