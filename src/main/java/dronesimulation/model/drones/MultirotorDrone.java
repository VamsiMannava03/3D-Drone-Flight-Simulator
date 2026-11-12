package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.FlightController;
import dronesimulation.model.control.WaypointController;

/**
 * A drone lifted by several rotors. It can hover, climbs straight up, and can push in any
 * direction as long as it does not tilt more than {@link #MAX_TILT_DEGREES} from upright.
 * Subclasses only say how many rotors they have.
 */
public abstract class MultirotorDrone extends Drone {

  /** The furthest the thrust may lean from vertical, in degrees. */
  public static final double MAX_TILT_DEGREES = 35;

  private static final double MAX_TILT_TAN = Math.tan(Math.toRadians(MAX_TILT_DEGREES));

  /**
   * Creates a multirotor.
   *
   * @param id a unique, non-blank name
   * @param mass the empty mass in kg
   * @param radius the collision radius in m
   * @param home the start position
   * @param spec the performance limits
   * @param battery the battery
   * @param sensor the position sensor
   */
  protected MultirotorDrone(String id, double mass, double radius, Vector3D home,
      DroneSpec spec, Battery battery, Sensor sensor) {
    super(id, mass, radius, home, spec, battery, sensor);
  }

  /**
   * Returns how many rotors lift this drone.
   *
   * @return the rotor count
   */
  public abstract int getRotorCount();

  /** Rotors only push upward, the lean is capped, then the total is capped at max thrust. */
  @Override
  protected Vector3D limitThrust(Vector3D desired) {
    double up = Math.max(0, desired.z());
    double sideways = Math.hypot(desired.x(), desired.y());
    double maxSideways = up * MAX_TILT_TAN;
    double sideScale = sideways > maxSideways ? maxSideways / sideways : 1;
    Vector3D limited = new Vector3D(desired.x() * sideScale, desired.y() * sideScale, up);
    double max = getSpec().maxThrust();
    double length = limited.magnitude();
    return length > max ? limited.scale(max / length) : limited;
  }

  @Override
  public Vector3D takeOffTarget(double altitude) {
    Vector3D p = getPosition();
    return new Vector3D(p.x(), p.y(), altitude);
  }

  @Override
  public FlightController navigateTo(Vector3D target, double arrivalRadius) {
    return new WaypointController(target, getSpec().cruiseSpeed(), arrivalRadius);
  }

  @Override
  public double getMinFlightSpeed() {
    return 0;
  }
}
