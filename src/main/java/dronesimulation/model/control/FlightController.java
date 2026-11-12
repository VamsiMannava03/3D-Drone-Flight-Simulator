package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;

/**
 * Decides how hard and in which direction a drone should push (Strategy pattern). Lets
 * navigation change, for example waypoint following or return-to-home, without editing drones.
 */
public interface FlightController {

  /**
   * Computes the thrust the drone should ask for this step.
   *
   * @param drone the drone being controlled
   * @param dt the time step in s
   * @return the desired thrust in N, before the drone applies its own limits
   */
  Vector3D desiredThrust(Drone drone, double dt);

  /**
   * Tells whether the controller has reached its goal. False by default, for controllers with
   * no end point.
   *
   * @param drone the drone being controlled
   * @return true when arrived
   */
  default boolean hasArrived(Drone drone) {
    return false;
  }

  /**
   * Returns the point this controller is steering toward, for display.
   *
   * @return the target, or null if there is none
   */
  default Vector3D getTarget() {
    return null;
  }
}
