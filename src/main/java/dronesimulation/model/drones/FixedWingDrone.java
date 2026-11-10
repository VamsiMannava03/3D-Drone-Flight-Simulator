package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.FixedWingController;
import dronesimulation.model.control.FlightController;
import dronesimulation.model.physics.PhysicsConstants;

/**
 * A plane-like drone. Its propeller pushes horizontally and its wings make lift that grows with
 * the square of its speed, so it cannot hover, needs a take-off run on its wheels, and must
 * keep above its stall speed.
 *
 * <p>Simplification: lift uses the horizontal ground speed, not the airspeed. Wind still pushes
 * the plane through air drag, but a tailwind does not cost it lift and a headwind does not add
 * any.
 */
public class FixedWingDrone extends Drone {

  /** Lift at stall speed, as a multiple of the drone's weight. */
  public static final double LIFT_AT_STALL = 1.6;
  /** Share of max thrust the propeller can give horizontally. */
  public static final double PROPELLER_SHARE = 0.4;
  /** Minimum flight speed as a multiple of the stall speed. */
  public static final double SAFETY_FACTOR = 1.3;
  /** Wheels roll easily, so ground friction is low. */
  public static final double WHEEL_FRICTION = 0.15;
  /** Power per N of lift, from induced drag, in W/N. */
  public static final double LIFT_POWER_PER_NEWTON = 0.6;

  private final double stallSpeed;
  private Vector3D runwayHeading = new Vector3D(1, 0, 0);

  /**
   * Creates a fixed-wing drone. Use {@link DroneBuilder} or {@link DroneFactory}.
   *
   * @param id a unique, non-blank name
   * @param mass the empty mass in kg
   * @param radius the collision radius (about half the wingspan) in m
   * @param home the start position
   * @param spec the performance limits
   * @param battery the battery
   * @param sensor the position sensor
   * @param stallSpeed the speed below which the wings cannot hold it up, in m/s
   * @throws IllegalArgumentException if stallSpeed is not positive
   */
  FixedWingDrone(String id, double mass, double radius, Vector3D home, DroneSpec spec,
      Battery battery, Sensor sensor, double stallSpeed) {
    super(id, mass, radius, home, spec, battery, sensor);
    if (!(stallSpeed > 0) || !Double.isFinite(stallSpeed)) {
      throw new IllegalArgumentException("stallSpeed must be positive, got " + stallSpeed);
    }
    this.stallSpeed = stallSpeed;
  }

  /**
   * Returns the stall speed.
   *
   * @return the stall speed in m/s
   */
  public double getStallSpeed() {
    return stallSpeed;
  }

  /**
   * Sets the direction of the take-off run used when the drone starts from rest.
   *
   * @param heading a horizontal direction, not zero
   * @throws IllegalArgumentException if the heading has no horizontal part
   */
  public void setRunwayHeading(Vector3D heading) {
    Vector3D flat = new Vector3D(heading.x(), heading.y(), 0);
    if (flat.magnitude() == 0) {
      throw new IllegalArgumentException("runway heading must be horizontal and non-zero");
    }
    runwayHeading = flat.normalize();
  }

  /**
   * Returns the most lift the wings make at the current horizontal ground speed (see the class
   * notes on why it is not airspeed).
   *
   * @return the lift in N
   */
  public double getAvailableLift() {
    Vector3D v = getVelocity();
    double ratio = Math.hypot(v.x(), v.y()) / stallSpeed;
    return LIFT_AT_STALL * getMass() * PhysicsConstants.GRAVITY * ratio * ratio;
  }

  /**
   * Returns the direction the drone is flying, or its runway heading when it is nearly still.
   *
   * @return a horizontal unit vector
   */
  public Vector3D getHeading() {
    Vector3D v = getVelocity();
    double speed = Math.hypot(v.x(), v.y());
    return speed > 1 ? new Vector3D(v.x() / speed, v.y() / speed, 0) : runwayHeading;
  }

  /**
   * Splits the desired force the way a plane can make it. The propeller only pulls forward along
   * the heading (it cannot brake; drag slows the plane). The wings make all other force: up to
   * the available lift, shared between holding height and turning by banking.
   */
  @Override
  protected Vector3D limitThrust(Vector3D desired) {
    Vector3D forward = getHeading();
    Vector3D side = new Vector3D(-forward.y(), forward.x(), 0);
    double maxProp = getSpec().maxThrust() * PROPELLER_SHARE;
    double along = Math.max(0, Math.min(maxProp, desired.dot(forward)));
    double lift = Math.min(getAvailableLift(), getSpec().maxThrust());
    double vertical = Math.max(-0.3 * lift, Math.min(lift, desired.z()));
    double maxTurn = Math.sqrt(Math.max(0, lift * lift - vertical * vertical));
    double turn = Math.max(-maxTurn, Math.min(maxTurn, desired.dot(side)));
    return forward.scale(along).add(side.scale(turn)).add(new Vector3D(0, 0, vertical));
  }

  /** Throttle drives the propeller only; lift, including turning, comes from speed. */
  @Override
  protected double throttleFor(Vector3D thrust) {
    double prop = Math.max(0, thrust.dot(getHeading()));
    return Math.min(1, prop / (getSpec().maxThrust() * PROPELLER_SHARE));
  }

  /** Propeller power from throttle, plus the induced drag of the wings' lift. */
  @Override
  protected double powerFor(Vector3D thrust) {
    Vector3D side = new Vector3D(-getHeading().y(), getHeading().x(), 0);
    double wingForce = Math.hypot(thrust.z(), thrust.dot(side));
    return super.powerFor(thrust) + LIFT_POWER_PER_NEWTON * wingForce;
  }

  /** Runs up along its heading, climbing as lift builds, to a point well ahead. */
  @Override
  public Vector3D takeOffTarget(double altitude) {
    Vector3D p = getPosition();
    Vector3D ahead = p.add(getHeading().scale(400));
    return new Vector3D(ahead.x(), ahead.y(), altitude);
  }

  @Override
  public FlightController navigateTo(Vector3D target, double arrivalRadius) {
    return new FixedWingController(target, getSpec().cruiseSpeed(), arrivalRadius);
  }

  @Override
  public double getMinFlightSpeed() {
    return minFlightSpeed(stallSpeed);
  }

  /**
   * Returns the slowest a fixed-wing drone with a given stall speed may fly: its stall speed
   * with the {@link #SAFETY_FACTOR} on top.
   *
   * @param stallSpeed the stall speed, in m/s
   * @return the minimum flight speed, in m/s
   */
  public static double minFlightSpeed(double stallSpeed) {
    return stallSpeed * SAFETY_FACTOR;
  }

  @Override
  public double getGroundFriction() {
    return WHEEL_FRICTION;
  }

  @Override
  public String getTypeName() {
    return "FixedWing";
  }
}
