package dronesimulation.model.engine;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.Mission;

/**
 * A read-only copy of one drone's state at a moment in time, for views and status panels.
 *
 * @param id the drone id
 * @param type the drone type name, such as "Quadcopter"
 * @param state the lifecycle state name, such as "Flying"
 * @param position the position in m
 * @param velocity the velocity in m/s
 * @param thrust the thrust produced in the last step, in N
 * @param throttle the throttle in the last step, from 0 to 1
 * @param batteryLevel the battery charge as a fraction from 0 to 1
 * @param mass the total mass including payload, in kg
 * @param payload the carried payload in kg
 * @param command the running command, such as "goto 10 0 5", or "" if none
 * @param queuedCommands how many commands are waiting
 * @param target the point the controller steers toward, or null
 * @param home the home position
 * @param radius the collision radius in m
 */
public record DroneSnapshot(String id, String type, String state, Vector3D position,
    Vector3D velocity, Vector3D thrust, double throttle, double batteryLevel, double mass,
    double payload, String command, int queuedCommands, Vector3D target, Vector3D home,
    double radius) {

  /**
   * Copies the current state of a drone and its mission.
   *
   * @param drone the drone
   * @param mission its mission
   * @return the snapshot
   */
  public static DroneSnapshot of(Drone drone, Mission mission) {
    DroneCommand current = mission.getCurrent();
    return new DroneSnapshot(drone.getId(), drone.getTypeName(), drone.getState().name(),
        drone.getPosition(), drone.getVelocity(), drone.getLastThrust(), drone.getThrottle(),
        drone.getBattery().getLevel(), drone.getMass(), drone.getPayloadMass(),
        current == null ? "" : current.describe(), mission.queuedCount(),
        drone.getController().getTarget(), drone.getHome(), drone.getRadius());
  }
}
