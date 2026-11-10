package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;

/**
 * Holds a multirotor at an altitude and stops its sideways motion, without holding a position:
 * the wind may still carry it slowly. Like a pilot's altitude-hold mode, it never "arrives".
 */
public class AltitudeHoldController implements FlightController {

  private final double altitude;

  /**
   * Creates a controller.
   *
   * @param altitude the altitude to hold, in m, must be positive
   * @throws IllegalArgumentException if altitude is not positive
   */
  public AltitudeHoldController(double altitude) {
    if (!(altitude > 0) || !Double.isFinite(altitude)) {
      throw new IllegalArgumentException("altitude must be positive, got " + altitude);
    }
    this.altitude = altitude;
  }

  /**
   * Returns the held altitude.
   *
   * @return the altitude in m
   */
  public double getAltitude() {
    return altitude;
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    double vz = WaypointController.clampVertical(
        (altitude - drone.getSensedPosition().z()) * WaypointController.POSITION_GAIN);
    return WaypointController.thrustFor(drone, new Vector3D(0, 0, vz));
  }
}
