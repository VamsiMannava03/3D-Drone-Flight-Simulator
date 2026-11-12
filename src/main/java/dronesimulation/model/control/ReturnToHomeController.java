package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;

/**
 * Flies a drone back above its home position at a safe altitude, ready to land. It uses the
 * drone's own navigation ({@link Drone#navigateTo}), so it works for multirotors and fixed-wing
 * drones alike.
 */
public class ReturnToHomeController implements FlightController {

  /** How close above home counts as arrived, in m. */
  public static final double ARRIVAL_RADIUS = 1.0;

  private final FlightController navigation;

  /**
   * Creates a return-to-home controller for a drone.
   *
   * @param drone the drone to bring home
   * @param altitude the altitude to fly home at, in m, must be positive
   * @throws IllegalArgumentException if altitude is not positive
   */
  public ReturnToHomeController(Drone drone, double altitude) {
    if (!(altitude > 0) || !Double.isFinite(altitude)) {
      throw new IllegalArgumentException("altitude must be positive, got " + altitude);
    }
    Vector3D home = drone.getHome();
    navigation = drone.navigateTo(new Vector3D(home.x(), home.y(), home.z() + altitude),
        ARRIVAL_RADIUS);
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    return navigation.desiredThrust(drone, dt);
  }

  @Override
  public boolean hasArrived(Drone drone) {
    return navigation.hasArrived(drone);
  }

  @Override
  public Vector3D getTarget() {
    return navigation.getTarget();
  }
}
