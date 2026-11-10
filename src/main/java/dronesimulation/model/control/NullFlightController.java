package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;

/**
 * A controller that asks for no thrust (Null Object pattern). Drones start with it and fall back
 * to it after a crash, so code never has to check for a missing controller.
 */
public final class NullFlightController implements FlightController {

  /** The single instance. */
  public static final NullFlightController INSTANCE = new NullFlightController();

  private NullFlightController() {
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    return Vector3D.ZERO;
  }
}
