package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import java.util.Objects;

/**
 * Flies a fixed-wing drone to a target. It never slows below the drone's minimum flight speed:
 * far from the target it flies straight at it, and once there it circles it at a radius the
 * drone can turn at.
 */
public class FixedWingController implements FlightController {

  /** Smallest circling radius, in m. */
  public static final double MIN_ORBIT_RADIUS = 25;
  /** Height error that still counts as at the target, in m. */
  public static final double ALTITUDE_TOLERANCE = 3;

  private final Vector3D target;
  private final double cruiseSpeed;
  private final double arrivalRadius;

  /**
   * Creates a controller.
   *
   * @param target the point to fly to
   * @param cruiseSpeed the travel speed, in m/s, must be positive
   * @param arrivalRadius the smallest distance that counts as arrived, in m, must be positive;
   *     the circling radius is used if it is larger
   * @throws IllegalArgumentException if cruiseSpeed or arrivalRadius is not positive
   */
  public FixedWingController(Vector3D target, double cruiseSpeed, double arrivalRadius) {
    this.target = Objects.requireNonNull(target, "target");
    if (!(cruiseSpeed > 0) || !(arrivalRadius > 0)) {
      throw new IllegalArgumentException("cruiseSpeed and arrivalRadius must be positive");
    }
    this.cruiseSpeed = cruiseSpeed;
    this.arrivalRadius = arrivalRadius;
  }

  /**
   * Returns the circling radius for a drone: a turn it can make at its current speed (or its
   * minimum speed, if faster) within the controller's acceleration limit, with a margin, but at
   * least {@link #MIN_ORBIT_RADIUS}. A plane cannot brake, so a fast one circles wider.
   *
   * @param drone the drone
   * @return the radius in m
   */
  public static double orbitRadius(Drone drone) {
    Vector3D v = drone.getVelocity();
    double speed = Math.max(drone.getMinFlightSpeed(), Math.hypot(v.x(), v.y()));
    return Math.max(MIN_ORBIT_RADIUS,
        1.3 * speed * speed / WaypointController.MAX_ACCELERATION);
  }

  @Override
  public Vector3D getTarget() {
    return target;
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    Vector3D position = drone.getSensedPosition();
    Vector3D toTarget = new Vector3D(target.x() - position.x(), target.y() - position.y(), 0);
    double distance = toTarget.magnitude();
    double radius = orbitRadius(drone);
    double minSpeed = drone.getMinFlightSpeed();
    double speed = Math.max(minSpeed, Math.min(distance * WaypointController.POSITION_GAIN,
        Math.max(cruiseSpeed, minSpeed)));
    // Vector-field loiter guidance: blend "fly along the circle" with "close the distance to
    // the circle". Far away the inward part dominates, so the plane heads for the target; near
    // the circle it follows it. The heading changes smoothly, so it does not overshoot.
    Vector3D direction;
    if (distance < 1e-6) {
      direction = getHeadingOrX(drone);
    } else {
      Vector3D inward = toTarget.scale(1 / distance);
      Vector3D tangent = new Vector3D(inward.y(), -inward.x(), 0);
      double pull = 1.5 * (distance - radius) / radius;
      direction = tangent.add(inward.scale(pull)).normalize();
    }
    Vector3D horizontal = direction.scale(speed);
    double vz = WaypointController.clampVertical(
        (target.z() - position.z()) * WaypointController.POSITION_GAIN);
    return WaypointController.thrustFor(drone,
        new Vector3D(horizontal.x(), horizontal.y(), vz));
  }

  private static Vector3D getHeadingOrX(Drone drone) {
    Vector3D v = drone.getVelocity();
    double speed = Math.hypot(v.x(), v.y());
    return speed > 1e-6 ? new Vector3D(v.x() / speed, v.y() / speed, 0) : new Vector3D(1, 0, 0);
  }

  @Override
  public boolean hasArrived(Drone drone) {
    Vector3D error = target.subtract(drone.getPosition());
    double horizontal = Math.hypot(error.x(), error.y());
    return horizontal < Math.max(arrivalRadius, orbitRadius(drone) * 1.5)
        && Math.abs(error.z()) < ALTITUDE_TOLERANCE;
  }
}
