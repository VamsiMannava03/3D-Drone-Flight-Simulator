package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import dronesimulation.model.physics.SolidBody;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Builds drones step by step (Builder pattern). Drones have many optional parts, so each one is
 * set by name and the whole configuration is checked in {@link #build()}.
 *
 * <pre>{@code
 * Drone d = new DroneBuilder(DroneType.QUADCOPTER, "q1")
 *     .mass(1.5).maxThrust(32).cruiseSpeed(6).battery(4, 0.25)
 *     .at(new Vector3D(0, 0, 0)).build();
 * }</pre>
 */
public class DroneBuilder {

  private final DroneType type;
  private final String id;
  private Double mass;
  private Double maxThrust;
  private double radius = 0.4;
  private Vector3D home = Vector3D.ZERO;
  private double cruiseSpeed = 5;
  private double safeLandingSpeed = DroneSpec.DEFAULT_SAFE_LANDING_SPEED;
  private double batteryWattHours = 5;
  private double lowBatteryLevel = 0.2;
  private double batteryLevel = 1.0;
  private double chargePower = 100;
  private double idlePower = 5;
  private Double maxPower;
  private double dragArea = SolidBody.DEFAULT_DRAG_AREA;
  private double sensorNoise;
  private Random random;
  private double payload;
  private Double stallSpeed;
  private Vector3D runwayHeading;

  /**
   * Starts a builder.
   *
   * @param type the kind of drone
   * @param id a unique, non-blank name
   * @throws NullPointerException if type or id is null
   */
  public DroneBuilder(DroneType type, String id) {
    this.type = Objects.requireNonNull(type, "type");
    this.id = Objects.requireNonNull(id, "id");
  }

  /**
   * Sets the empty mass. Required.
   *
   * @param kg the mass in kg
   * @return this builder
   */
  public DroneBuilder mass(double kg) {
    this.mass = kg;
    return this;
  }

  /**
   * Sets the strongest total thrust. Required.
   *
   * @param newtons the thrust in N
   * @return this builder
   */
  public DroneBuilder maxThrust(double newtons) {
    this.maxThrust = newtons;
    return this;
  }

  /**
   * Sets the collision radius. Default 0.4 m.
   *
   * @param metres the radius in m
   * @return this builder
   */
  public DroneBuilder radius(double metres) {
    this.radius = metres;
    return this;
  }

  /**
   * Sets the start and home position. Default the origin.
   *
   * @param position the home position
   * @return this builder
   */
  public DroneBuilder at(Vector3D position) {
    this.home = Objects.requireNonNull(position, "position");
    return this;
  }

  /**
   * Sets the travel speed. Default 5 m/s.
   *
   * @param metresPerSecond the speed
   * @return this builder
   */
  public DroneBuilder cruiseSpeed(double metresPerSecond) {
    this.cruiseSpeed = metresPerSecond;
    return this;
  }

  /**
   * Sets the fastest safe touchdown. Default 3 m/s, as the SRS requires.
   *
   * @param metresPerSecond the speed
   * @return this builder
   */
  public DroneBuilder safeLandingSpeed(double metresPerSecond) {
    this.safeLandingSpeed = metresPerSecond;
    return this;
  }

  /**
   * Sets the battery. Default 5 Wh with a 20% warning level.
   *
   * @param wattHours the capacity in Wh
   * @param lowLevel the warning level as a fraction
   * @return this builder
   */
  public DroneBuilder battery(double wattHours, double lowLevel) {
    this.batteryWattHours = wattHours;
    this.lowBatteryLevel = lowLevel;
    return this;
  }

  /**
   * Sets the drag area (drag coefficient times frontal area). Default 0.03 m^2.
   *
   * @param squareMetres the drag area in m^2
   * @return this builder
   */
  public DroneBuilder dragArea(double squareMetres) {
    this.dragArea = squareMetres;
    return this;
  }

  /**
   * Sets how full the battery is at the start. Default 100%.
   *
   * @param fraction the starting charge, from 0 to 1
   * @return this builder
   */
  public DroneBuilder batteryLevel(double fraction) {
    this.batteryLevel = fraction;
    return this;
  }

  /**
   * Sets the motor power at full throttle. Defaults to 15 W per N of max thrust.
   *
   * @param watts the power in W
   * @return this builder
   */
  public DroneBuilder maxPower(double watts) {
    this.maxPower = watts;
    return this;
  }

  /**
   * Sets the charging power. Default 100 W.
   *
   * @param watts the power in W
   * @return this builder
   */
  public DroneBuilder chargePower(double watts) {
    this.chargePower = watts;
    return this;
  }

  /**
   * Sets the power used by electronics. Default 5 W.
   *
   * @param watts the power in W
   * @return this builder
   */
  public DroneBuilder idlePower(double watts) {
    this.idlePower = watts;
    return this;
  }

  /**
   * Adds position-sensor noise. Default none.
   *
   * @param metres the standard deviation of each reading, in m
   * @param random the seeded source of noise
   * @return this builder
   */
  public DroneBuilder sensorNoise(double metres, Random random) {
    this.sensorNoise = metres;
    this.random = Objects.requireNonNull(random, "random");
    return this;
  }

  /**
   * Loads a package to deliver. Default none.
   *
   * @param kg the payload mass in kg
   * @return this builder
   */
  public DroneBuilder payload(double kg) {
    this.payload = kg;
    return this;
  }

  /**
   * Sets the stall speed. Required for, and only allowed on, fixed-wing drones.
   *
   * @param metresPerSecond the stall speed
   * @return this builder
   */
  public DroneBuilder stallSpeed(double metresPerSecond) {
    this.stallSpeed = metresPerSecond;
    return this;
  }

  /**
   * Sets the take-off direction of a fixed-wing drone. Default +x.
   *
   * @param heading a horizontal direction
   * @return this builder
   */
  public DroneBuilder runwayHeading(Vector3D heading) {
    this.runwayHeading = Objects.requireNonNull(heading, "heading");
    return this;
  }

  /**
   * Checks the configuration and creates the drone.
   *
   * @return the new drone, Idle at its home position
   * @throws IllegalStateException if a required part is missing, or a part does not fit this
   *     drone type
   * @throws IllegalArgumentException if a value is out of range, including thrust too weak to
   *     lift the drone and its payload
   */
  public Drone build() {
    if (mass == null) {
      throw new IllegalStateException(id + ": mass is required");
    }
    if (maxThrust == null) {
      throw new IllegalStateException(id + ": maxThrust is required");
    }
    if (type == DroneType.FIXED_WING && stallSpeed == null) {
      throw new IllegalStateException(id + ": a fixed-wing drone needs a stallSpeed");
    }
    if (type != DroneType.FIXED_WING && (stallSpeed != null || runwayHeading != null)) {
      throw new IllegalStateException(id + ": stallSpeed and runwayHeading are only for "
          + "fixed-wing drones");
    }
    if (type == DroneType.FIXED_WING
        && cruiseSpeed < FixedWingDrone.minFlightSpeed(stallSpeed)) {
      throw new IllegalArgumentException(String.format(Locale.ROOT,
          "%s: cruise speed %.1f m/s is below the minimum flight speed %.1f m/s",
          id, cruiseSpeed, FixedWingDrone.minFlightSpeed(stallSpeed)));
    }
    DroneSpec spec = new DroneSpec(maxThrust, cruiseSpeed, safeLandingSpeed, chargePower,
        idlePower, maxPower != null ? maxPower : 15 * maxThrust);
    Battery battery = new Battery(batteryWattHours, lowBatteryLevel, batteryLevel);
    Sensor sensor = random == null ? Sensor.perfect()
        : new NoisySensor(Sensor.perfect(), sensorNoise, random);
    Drone drone = switch (type) {
      case QUADCOPTER -> new QuadcopterDrone(id, mass, radius, home, spec, battery, sensor);
      case HEXACOPTER -> new HexacopterDrone(id, mass, radius, home, spec, battery, sensor);
      case FIXED_WING -> {
        FixedWingDrone plane = new FixedWingDrone(id, mass, radius, home, spec, battery,
            sensor, stallSpeed);
        if (runwayHeading != null) {
          plane.setRunwayHeading(runwayHeading);
        }
        yield plane;
      }
    };
    drone.setDragArea(dragArea);
    if (payload > 0) {
      drone.loadPayload(payload);
    } else if (payload < 0) {
      throw new IllegalArgumentException(id + ": payload must not be negative");
    }
    return drone;
  }
}
