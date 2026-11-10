package dronesimulation.model.control;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;

/**
 * Brings a drone down gently. It descends at a set rate and slows to about 0.4 m/s near the
 * ground, so touchdown stays under the safe landing speed. A hovering drone holds the spot where
 * the landing began; a fixed-wing drone keeps flying straight ahead at its minimum speed.
 */
public class LandingController implements FlightController {

  /** Descent rate just above the ground, in m/s. */
  public static final double TOUCHDOWN_RATE = 0.4;

  private final double descentRate;
  private Vector3D spot;
  private Vector3D heading;

  /**
   * Creates a landing controller.
   *
   * @param descentRate the descent speed high above the ground, in m/s, must be positive
   * @throws IllegalArgumentException if descentRate is not positive
   */
  public LandingController(double descentRate) {
    if (!(descentRate > 0) || !Double.isFinite(descentRate)) {
      throw new IllegalArgumentException("descentRate must be positive, got " + descentRate);
    }
    this.descentRate = descentRate;
  }

  @Override
  public Vector3D getTarget() {
    return spot;
  }

  @Override
  public Vector3D desiredThrust(Drone drone, double dt) {
    Vector3D position = drone.getSensedPosition();
    if (spot == null) {
      spot = new Vector3D(position.x(), position.y(), 0);
      Vector3D v = drone.getVelocity();
      double speed = Math.hypot(v.x(), v.y());
      heading = speed > 0.5 ? new Vector3D(v.x() / speed, v.y() / speed, 0)
          : new Vector3D(1, 0, 0);
    }
    double altitude = Math.max(0, position.z());
    double vz = -Math.min(descentRate, TOUCHDOWN_RATE + 0.5 * altitude);
    Vector3D horizontal;
    if (drone.getMinFlightSpeed() > 0) {
      horizontal = heading.scale(drone.getMinFlightSpeed());
    } else {
      Vector3D drift = spot.subtract(new Vector3D(position.x(), position.y(), 0));
      horizontal = drift.scale(WaypointController.POSITION_GAIN);
    }
    return WaypointController.thrustFor(drone,
        new Vector3D(horizontal.x(), horizontal.y(), vz));
  }

  @Override
  public boolean hasArrived(Drone drone) {
    return drone.isOnGround();
  }
}
