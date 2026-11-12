package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;

/**
 * One step of a mission, such as TakeOff, GoTo, Hover, Deliver or Land (Command pattern).
 * Commands can be queued, loaded from a file and replayed.
 */
public interface DroneCommand {

  /**
   * Called once when the command becomes the active one.
   *
   * @param drone the drone running the command
   * @throws IllegalStateException if the drone cannot run this command now
   */
  void start(Drone drone);

  /**
   * Called every step while the command is active. Does nothing by default.
   *
   * @param drone the drone running the command
   * @param dt the time step in s
   */
  default void update(Drone drone, double dt) {
  }

  /**
   * Tells whether the command has finished, so the mission can move to the next one.
   *
   * @param drone the drone running the command
   * @return true when done
   */
  boolean isComplete(Drone drone);

  /**
   * Returns a short description, in the same words as the command language, such as
   * "goto 10 0 5".
   *
   * @return the description
   */
  String describe();

  /**
   * Checks that the drone will be able to run this step, given the plan of the steps before it,
   * then updates the plan with this step's effect. Lets a mission be validated against a drone's
   * capabilities before it runs.
   *
   * @param plan the drone's planned situation; updated by this call
   * @throws IllegalStateException or IllegalArgumentException if the step cannot run
   */
  void check(MissionPlan plan);

  /**
   * Updates a plan with this step's effect only, without checking it, for a step that is
   * already running. Nothing by default.
   *
   * @param plan the drone's planned situation
   */
  default void finish(MissionPlan plan) {
  }
}
