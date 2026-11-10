package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.states.Flying;

/** Helpers shared by the commands. */
final class Commands {

  private Commands() {
  }

  /**
   * Checks that a drone is flying.
   *
   * @param drone the drone
   * @param action what the command wants to do, for the error message
   * @throws IllegalStateException if the drone is not flying
   */
  static void requireFlying(Drone drone, String action) {
    if (drone.getState() != Flying.INSTANCE) {
      throw new IllegalStateException(
          "Cannot " + action + " while " + drone.getState().name());
    }
  }

  /**
   * Formats a number without trailing zeros, for command descriptions. It is not rounded:
   * descriptions are saved to mission files, and must parse back to the same value.
   *
   * @param value the number
   * @return for example "10" or "2.5"
   */
  static String format(double value) {
    return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
  }
}
