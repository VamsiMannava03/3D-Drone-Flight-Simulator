package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.FlightController;
import dronesimulation.model.control.NullFlightController;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.physics.PhysicsConstants;
import dronesimulation.model.physics.SolidPointMass;
import dronesimulation.model.states.Crashed;
import dronesimulation.model.states.DroneState;
import dronesimulation.model.states.Flying;
import dronesimulation.model.states.Idle;
import dronesimulation.model.states.Landed;
import dronesimulation.model.states.TakingOff;
import java.util.Locale;
import java.util.Objects;

/**
 * Base class for all drones: a point mass with a battery, a sensor, a flight controller and a
 * lifecycle state.
 *
 * <p>{@link #update(double)} is a template method. The update order is fixed here and each drone
 * type fills in the details through {@link #limitThrust(Vector3D)}, {@link #powerFor(Vector3D)}
 * and {@link #takeOffTarget(double)}.
 */
public abstract class Drone extends SolidPointMass {

  /** Thrust must exceed weight by this factor, so a drone can climb and not just hover. */
  public static final double LIFT_MARGIN = 1.2;
  /** Return-to-home altitude used until the world sets one, in m. */
  public static final double DEFAULT_RETURN_ALTITUDE = 15;

  private final DroneSpec spec;
  private final Battery battery;
  private final Sensor sensor;
  private final Vector3D home;
  private DroneState state = Idle.INSTANCE;
  private FlightController controller = NullFlightController.INSTANCE;
  private Vector3D lastThrust = Vector3D.ZERO;
  private double targetAltitude;
  private double payloadMass;
  private EventBus events = new EventBus();
  private boolean lowBatteryReported;
  private boolean emptyBatteryReported;
  private String crashReason = "crashed";
  private double returnAltitude = DEFAULT_RETURN_ALTITUDE;

  /** Tells whether motors with this spec can lift a mass, with {@link #LIFT_MARGIN} to spare. */
  private static boolean canLift(DroneSpec spec, double mass) {
    return spec.maxThrust() >= mass * LIFT_MARGIN * PhysicsConstants.GRAVITY;
  }

  /**
   * Creates a drone resting at its home position.
   *
   * @param id a unique, non-blank name
   * @param mass the empty mass in kg, must be positive
   * @param radius the collision radius in m, must be positive
   * @param home the starting position, which is also where it returns to
   * @param spec the performance limits
   * @param battery the battery
   * @param sensor the position sensor
   * @throws IllegalArgumentException if the motors cannot lift the drone
   */
  protected Drone(String id, double mass, double radius, Vector3D home, DroneSpec spec,
      Battery battery, Sensor sensor) {
    super(id, mass, radius, home, Vector3D.ZERO);
    Objects.requireNonNull(spec, "spec");
    if (!canLift(spec, mass)) {
      throw new IllegalArgumentException(String.format(Locale.ROOT,
          "%s: max thrust %.1f N cannot lift %.2f kg with a %.0f%% margin",
          id, spec.maxThrust(), mass, (LIFT_MARGIN - 1) * 100));
    }
    this.home = home;
    this.spec = spec;
    this.battery = Objects.requireNonNull(battery, "battery");
    this.sensor = Objects.requireNonNull(sensor, "sensor");
  }

  /**
   * Runs one drone update before the physics step, always in this order: ask the controller for
   * thrust, limit it to what this drone can do, apply it, drain the battery, advance the state,
   * then check the battery.
   *
   * @param dt the time step in s
   */
  public final void update(double dt) {
    Vector3D thrust = Vector3D.ZERO;
    if (state.canFly() && !battery.isEmpty()) {
      thrust = limitThrust(controller.desiredThrust(this, dt));
    }
    lastThrust = thrust;
    if (state.canFly()) {
      battery.drain(powerFor(thrust) * dt);
    }
    changeState(state.update(this, dt));
    checkBattery();
  }

  /**
   * Clamps the thrust a controller asks for to what this drone type can produce.
   *
   * @param desired the thrust the controller wants, in N
   * @return the thrust actually produced, in N
   */
  protected abstract Vector3D limitThrust(Vector3D desired);

  /**
   * Returns the throttle needed for a thrust: 0 is motors off, 1 is full power. Multirotors use
   * total thrust over max thrust; other types may count only part of the thrust.
   *
   * @param thrust the thrust being produced, in N
   * @return the throttle from 0 to 1
   */
  protected double throttleFor(Vector3D thrust) {
    return Math.min(1, thrust.magnitude() / spec.maxThrust());
  }

  /**
   * Returns the electrical power drawn at a thrust: idle power plus max power times
   * throttle^1.5 (rotor momentum theory). Climbing and fighting the wind need more throttle, so
   * they cost more energy.
   *
   * @param thrust the thrust being produced, in N
   * @return the power in W
   */
  protected double powerFor(Vector3D thrust) {
    return spec.idlePower() + spec.maxPower() * Math.pow(throttleFor(thrust), 1.5);
  }

  /**
   * Returns the throttle used in the last update.
   *
   * @return the throttle from 0 (off) to 1 (full)
   */
  public double getThrottle() {
    return throttleFor(lastThrust);
  }

  /**
   * Returns the point to fly toward when taking off to an altitude. A multicopter climbs
   * straight up; a fixed-wing drone needs a run-up.
   *
   * @param altitude the take-off altitude, in m
   * @return the take-off target
   */
  public abstract Vector3D takeOffTarget(double altitude);

  /**
   * Creates the controller this drone type uses to fly to a point (Factory Method): waypoint
   * navigation for multirotors, fixed-wing flight for planes. Commands call this, so they never
   * need to know the drone type.
   *
   * @param target the point to fly to
   * @param arrivalRadius how close counts as arrived, in m
   * @return a new controller
   */
  public abstract FlightController navigateTo(Vector3D target, double arrivalRadius);

  /**
   * Returns the slowest horizontal speed this drone can fly at. Zero for drones that can hover.
   *
   * @return the minimum flight speed in m/s
   */
  public abstract double getMinFlightSpeed();

  /**
   * Returns a short name for this drone type, such as "Quadcopter".
   *
   * @return the type name
   */
  public abstract String getTypeName();

  /**
   * Starts a take-off to an altitude.
   *
   * @param altitude the altitude to climb to, in m, must be positive
   * @throws IllegalArgumentException if the altitude is not positive
   * @throws IllegalStateException if the current state does not allow take-off
   */
  public void takeOff(double altitude) {
    if (!(altitude > 0) || !Double.isFinite(altitude)) {
      throw new IllegalArgumentException("altitude must be positive, got " + altitude);
    }
    changeState(state.takeOff(this));
    targetAltitude = altitude;
  }

  /**
   * Starts a landing.
   *
   * @throws IllegalStateException if the current state does not allow landing
   */
  public void land() {
    changeState(state.land(this));
  }

  /**
   * Starts charging the battery.
   *
   * @throws IllegalStateException if the current state does not allow charging
   */
  public void charge() {
    changeState(state.charge(this));
  }

  /**
   * Crashes the drone, for example after hitting an obstacle. Does nothing if a crash is not
   * possible in the current state.
   *
   * @param reason a short description, such as "hit building B2"
   */
  public void crash(String reason) {
    if (state.canCrash()) {
      crashReason = Objects.requireNonNull(reason, "reason");
      changeState(state.crash(this));
    }
  }

  @Override
  public void onGroundImpact(double impactSpeed) {
    crashReason = String.format(Locale.ROOT, "hit the ground at %.1f m/s", impactSpeed);
    changeState(state.touchdown(this, impactSpeed));
  }

  private void changeState(DroneState next) {
    if (next == state) {
      return;
    }
    DroneState previous = state;
    state = next;
    if (next == Crashed.INSTANCE) {
      controller = NullFlightController.INSTANCE;
      events.publish(SimEventType.CRASH, getId(), crashReason);
    } else if (next == Landed.INSTANCE) {
      events.publish(SimEventType.LANDED, getId(), "landed");
    } else if (next == Flying.INSTANCE && previous == TakingOff.INSTANCE) {
      events.publish(SimEventType.TOOK_OFF, getId(),
          String.format(Locale.ROOT, "reached %.1f m", getAltitude()));
    }
  }

  private void checkBattery() {
    if (battery.isLow() && !lowBatteryReported) {
      lowBatteryReported = true;
      events.publish(SimEventType.LOW_BATTERY, getId(),
          String.format(Locale.ROOT, "battery at %.0f%%", battery.getLevel() * 100));
    } else if (!battery.isLow()) {
      lowBatteryReported = false;
    }
    if (battery.isEmpty() && state.canFly() && !emptyBatteryReported) {
      emptyBatteryReported = true;
      events.publish(SimEventType.BATTERY_DEPLETED, getId(), "battery empty, motors stopped");
    } else if (!battery.isEmpty()) {
      emptyBatteryReported = false;
    }
  }

  /**
   * Returns the empty mass plus any payload.
   *
   * @return the total mass in kg
   */
  @Override
  public double getMass() {
    return super.getMass() + payloadMass;
  }

  /**
   * Loads a package.
   *
   * @param mass the package mass in kg, must be positive
   * @throws IllegalArgumentException if the mass is not positive
   * @throws IllegalStateException if the motors could not lift the extra weight
   */
  public void loadPayload(double mass) {
    if (!(mass > 0) || !Double.isFinite(mass)) {
      throw new IllegalArgumentException("payload mass must be positive, got " + mass);
    }
    double total = getMass() + mass;
    if (!canLift(spec, total)) {
      throw new IllegalStateException(String.format(Locale.ROOT,
          "%s cannot lift %.2f kg: max thrust %.1f N", getId(), total, spec.maxThrust()));
    }
    payloadMass += mass;
  }

  /**
   * Drops the whole payload.
   *
   * @return the mass dropped, in kg
   */
  public double dropPayload() {
    double dropped = payloadMass;
    payloadMass = 0;
    return dropped;
  }

  /**
   * Returns the mass of the carried payload.
   *
   * @return the payload in kg, 0 if none
   */
  public double getPayloadMass() {
    return payloadMass;
  }

  /**
   * Returns the current lifecycle state.
   *
   * @return the state
   */
  public DroneState getState() {
    return state;
  }

  /**
   * Returns the flight controller that is steering this drone.
   *
   * @return the controller; NullFlightController if none
   */
  public FlightController getController() {
    return controller;
  }

  /**
   * Sets the flight controller that steers this drone (Strategy pattern).
   *
   * @param controller the controller, or null to stop steering (uses NullFlightController)
   */
  public void setController(FlightController controller) {
    this.controller = controller == null ? NullFlightController.INSTANCE : controller;
  }

  /**
   * Tells whether the current controller has reached its goal.
   *
   * @return true if the controller has arrived
   */
  public boolean hasArrived() {
    return controller.hasArrived(this);
  }

  /**
   * Returns the thrust produced in the last update.
   *
   * @return the thrust in N
   */
  public Vector3D getLastThrust() {
    return lastThrust;
  }

  /**
   * Returns the thrust chosen in the last {@link #update(double)}, which the physics engine
   * adds to gravity and drag.
   *
   * @return the thrust in N
   */
  @Override
  public Vector3D ownForces() {
    return lastThrust;
  }

  /**
   * Tells whether the drone is resting on the ground.
   *
   * @return true at or below altitude 0
   */
  public boolean isOnGround() {
    return getPosition().z() <= 0;
  }

  /**
   * Returns where the sensor thinks the drone is. Controllers steer from this.
   *
   * @return the measured position
   */
  public Vector3D getSensedPosition() {
    return sensor.read(getPosition());
  }

  /**
   * Returns the height above the ground.
   *
   * @return the altitude in m
   */
  public double getAltitude() {
    return getPosition().z();
  }

  /**
   * Returns the altitude of the current take-off.
   *
   * @return the altitude in m
   */
  public double getTargetAltitude() {
    return targetAltitude;
  }

  /**
   * Returns the position this drone started from.
   *
   * @return the home position
   */
  public Vector3D getHome() {
    return home;
  }

  /**
   * Returns the altitude this drone climbs to before flying home, so it clears buildings.
   *
   * @return the altitude in m
   */
  public double getReturnAltitude() {
    return returnAltitude;
  }

  /**
   * Sets the return-to-home altitude.
   *
   * @param altitude the altitude in m, must be positive
   * @throws IllegalArgumentException if altitude is not positive
   */
  public void setReturnAltitude(double altitude) {
    if (!(altitude > 0) || !Double.isFinite(altitude)) {
      throw new IllegalArgumentException("return altitude must be positive, got " + altitude);
    }
    this.returnAltitude = altitude;
  }

  /**
   * Returns the performance limits.
   *
   * @return the spec
   */
  public DroneSpec getSpec() {
    return spec;
  }

  /**
   * Returns the battery.
   *
   * @return the battery
   */
  public Battery getBattery() {
    return battery;
  }

  /**
   * Returns the event bus this drone reports to.
   *
   * @return the event bus
   */
  public EventBus getEvents() {
    return events;
  }

  /**
   * Sets the event bus this drone reports to.
   *
   * @param events the event bus
   */
  public void setEvents(EventBus events) {
    this.events = Objects.requireNonNull(events, "events");
  }
}
