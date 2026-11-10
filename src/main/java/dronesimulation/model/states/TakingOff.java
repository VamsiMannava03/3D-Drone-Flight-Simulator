package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/** Climbing to the take-off altitude. Becomes Flying once it gets there. */
public final class TakingOff implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final TakingOff INSTANCE = new TakingOff();

  /** How close to the target altitude counts as arrived, in m. */
  public static final double ALTITUDE_TOLERANCE = 0.5;

  private TakingOff() {
  }

  @Override
  public String name() {
    return "TakingOff";
  }

  @Override
  public boolean canFly() {
    return true;
  }

  /** It is airborne, so hitting an obstacle or another drone is a crash (SRS FR13). */
  @Override
  public boolean canCrash() {
    return true;
  }

  @Override
  public DroneState crash(Drone drone) {
    return Crashed.INSTANCE;
  }

  /**
   * A light bump during the run-up is ignored, but falling back to the ground (for example when
   * the battery dies mid-climb) faster than a safe landing is a crash, as when Flying.
   */
  @Override
  public DroneState touchdown(Drone drone, double impactSpeed) {
    return impactSpeed > drone.getSpec().safeLandingSpeed() ? Crashed.INSTANCE : this;
  }

  @Override
  public DroneState update(Drone drone, double dt) {
    if (drone.getAltitude() >= drone.getTargetAltitude() - ALTITUDE_TOLERANCE) {
      return Flying.INSTANCE;
    }
    return this;
  }
}
