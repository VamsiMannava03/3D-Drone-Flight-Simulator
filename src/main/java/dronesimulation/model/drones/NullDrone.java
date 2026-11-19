package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import dronesimulation.model.missions.MissionStep;
import dronesimulation.model.missions.NullMissionStep;

/**
 * A drone that does nothing (Null Object pattern).
 *
 * <p>It is never active, so the physics engine skips it. Setters and lifecycle actions are ignored.
 * Use it where code needs a {@link Drone} before a real one exists.
 */
public final class NullDrone implements Drone {

  /** The single shared instance. */
  public static final NullDrone INSTANCE = new NullDrone();

  private NullDrone() {
    // use INSTANCE
  }

  @Override
  public String getId() {
    return "none";
  }

  @Override
  public DroneState getState() {
    return NullDroneState.INSTANCE;
  }

  @Override
  public double getBatteryLevel() {
    return 0;
  }

  @Override
  public double getMaxThrust() {
    return 0;
  }

  @Override
  public double getThrottle() {
    return 0;
  }

  @Override
  public void setThrottle(double throttle) {
    // ignored
  }

  @Override
  public MissionStep getCurrentStep() {
    return NullMissionStep.INSTANCE;
  }

  @Override
  public void takeOff() {
    // ignored
  }

  @Override
  public void land() {
    // ignored
  }

  @Override
  public void crash() {
    // ignored
  }

  @Override
  public Vector3D getPosition() {
    return Vector3D.ZERO;
  }

  @Override
  public void setPosition(Vector3D position) {
    // ignored
  }

  @Override
  public Vector3D getVelocity() {
    return Vector3D.ZERO;
  }

  @Override
  public void setVelocity(Vector3D velocity) {
    // ignored
  }

  /**
   * Returns a placeholder mass of 1 kg, because a body mass must be positive.
   *
   * @return 1.0
   */
  @Override
  public double getMass() {
    return 1.0;
  }

  @Override
  public boolean isActive() {
    return false;
  }
}
