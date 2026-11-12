package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.FlightController;
import dronesimulation.model.control.WaypointController;

/** A minimal drone for tests: thrust limited by magnitude, power = 10 W + 1 W per N. */
public final class StubDrone extends Drone {

  /** Mass of every stub drone, in kg. */
  public static final double MASS = 1.0;

  /**
   * Creates a stub drone with a 1 Wh battery.
   *
   * @param id the id
   * @param home the start position
   */
  public StubDrone(String id, Vector3D home) {
    this(id, home, new Battery(1, 0.2));
  }

  /**
   * Creates a stub drone with the given battery.
   *
   * @param id the id
   * @param home the start position
   * @param battery the battery
   */
  public StubDrone(String id, Vector3D home, Battery battery) {
    this(id, home, battery, Sensor.perfect());
  }

  /**
   * Creates a stub drone with the given battery and sensor.
   *
   * @param id the id
   * @param home the start position
   * @param battery the battery
   * @param sensor the position sensor
   */
  public StubDrone(String id, Vector3D home, Battery battery, Sensor sensor) {
    super(id, MASS, 0.3, home, new DroneSpec(30, 5, 2, 1000, 10, 100), battery, sensor);
  }

  @Override
  protected Vector3D limitThrust(Vector3D desired) {
    double max = getSpec().maxThrust();
    return desired.magnitude() > max ? desired.normalize().scale(max) : desired;
  }

  @Override
  protected double powerFor(Vector3D thrust) {
    return getSpec().idlePower() + thrust.magnitude();
  }

  @Override
  public Vector3D takeOffTarget(double altitude) {
    return new Vector3D(getPosition().x(), getPosition().y(), altitude);
  }

  @Override
  public FlightController navigateTo(Vector3D target, double arrivalRadius) {
    return new WaypointController(target, getSpec().cruiseSpeed(), arrivalRadius);
  }

  @Override
  public double getMinFlightSpeed() {
    return 0;
  }

  @Override
  public String getTypeName() {
    return "Stub";
  }
}
