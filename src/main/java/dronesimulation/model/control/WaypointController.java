package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.physics.PhysicsConstants;
import java.util.Objects;

/**
 * Flies a multirotor to a target point and holds it there (waypoint navigation).
 *
 * <p>A cascaded P controller: position error gives a wanted velocity (capped at the cruise speed
 * and at climb and descent limits), velocity error gives a wanted acceleration, and thrust is
 * mass times that acceleration plus gravity. Fixed-wing drones use {@link FixedWingController}.
 */
public class WaypointController implements FlightController {

  /** Wanted velocity per metre of position error, in 1/s. */
  public static final double POSITION_GAIN = 0.9;
  /** Wanted acceleration per m/s of velocity error, in 1/s. */
  public static final double VELOCITY_GAIN = 2.5;
  /** Largest acceleration asked for, in m/s^2. */
  public static final double MAX_ACCELERATION = 6;
  /** Fastest climb asked for, in m/s. */
  public static final double MAX_CLIMB_RATE = 4;
  /** Fastest descent asked for, in m/s. */
  public static final double MAX_DESCENT_RATE = 3;
  /** Speed below which a hovering drone counts as stopped, in m/s. */
  public static final double STOPPED_SPEED = 0.8;

  private final Vector3D target;
  private final double cruiseSpeed;
  private final double arrivalRadius;

  /**
   * Creates a controller.
   *
   * @param target the point to fly to
   * @param cruiseSpeed the travel speed, in m/s, must be positive
   * @param arrivalRadius how close counts as arrived, in m, must be positive
   * @throws IllegalArgumentException if cruiseSpeed or arrivalRadius is not positive
   */
  public WaypointController(Vector3D target, double cruiseSpeed, double arrivalRadius) {
    this.target = Objects.requireNonNull(target, "target");
    if (!(cruiseSpeed > 0) || !(arrivalRadius > 0)) {
      throw new IllegalArgumentException("cruiseSpeed and arrivalRadius must be positive");
    }
    this.cruiseSpeed = cruiseSpeed;
    this.arrivalRadius = arrivalRadius;
  }

  @Override
  public Vector3D getTarget() {
    return target;
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    Vector3D error = target.subtract(drone.getSensedPosition());
    Vector3D wanted = clampLength(error.scale(POSITION_GAIN), cruiseSpeed);
    return thrustFor(drone, new Vector3D(wanted.x(), wanted.y(), clampVertical(wanted.z())));
  }

  @Override
  public boolean hasArrived(Drone drone) {
    Vector3D error = target.subtract(drone.getPosition());
    return error.magnitude() < arrivalRadius && drone.getVelocity().magnitude() < STOPPED_SPEED;
  }

  /**
   * Limits a wanted climb or descent rate.
   *
   * @param vz the wanted vertical speed, in m/s
   * @return the speed within the climb and descent limits
   */
  static double clampVertical(double vz) {
    return Math.max(-MAX_DESCENT_RATE, Math.min(MAX_CLIMB_RATE, vz));
  }

  /**
   * Returns the thrust that turns the drone's velocity toward a wanted velocity.
   *
   * @param drone the drone
   * @param wantedVelocity the velocity to reach, in m/s
   * @return the thrust in N, before the drone's own limits
   */
  static Vector3D thrustFor(Drone drone, Vector3D wantedVelocity) {
    Vector3D acceleration = clampLength(
        wantedVelocity.subtract(drone.getVelocity()).scale(VELOCITY_GAIN), MAX_ACCELERATION);
    return acceleration.add(new Vector3D(0, 0, PhysicsConstants.GRAVITY)).scale(drone.getMass());
  }

  /**
   * Shortens a vector to at most a length.
   *
   * @param v the vector
   * @param max the longest allowed length
   * @return v, or v scaled down to length max
   */
  static Vector3D clampLength(Vector3D v, double max) {
    double length = v.magnitude();
    return length > max ? v.scale(max / length) : v;
  }
}
