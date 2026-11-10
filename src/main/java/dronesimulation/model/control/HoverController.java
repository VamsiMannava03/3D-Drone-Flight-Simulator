package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import java.util.Objects;

/**
 * Holds a multirotor still at a point, correcting for wind gusts. Hovering never "arrives"; the
 * command using it decides how long to hover.
 */
public class HoverController implements FlightController {

  /** Speed used to return to the hover point after drifting, in m/s. */
  public static final double CORRECTION_SPEED = 3;

  private final Vector3D point;

  /**
   * Creates a controller.
   *
   * @param point where to hover
   */
  public HoverController(Vector3D point) {
    this.point = Objects.requireNonNull(point, "point");
  }

  @Override
  public Vector3D getTarget() {
    return point;
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    Vector3D error = point.subtract(drone.getSensedPosition());
    Vector3D wanted = WaypointController.clampLength(
        error.scale(WaypointController.POSITION_GAIN), CORRECTION_SPEED);
    return WaypointController.thrustFor(drone,
        new Vector3D(wanted.x(), wanted.y(), WaypointController.clampVertical(wanted.z())));
  }
}
