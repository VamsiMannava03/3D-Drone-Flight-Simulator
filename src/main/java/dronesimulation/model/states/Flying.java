package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/** In the air and following commands. */
public final class Flying implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final Flying INSTANCE = new Flying();

  private Flying() {
  }

  @Override
  public String name() {
    return "Flying";
  }

  @Override
  public boolean canFly() {
    return true;
  }

  @Override
  public boolean canCrash() {
    return true;
  }

  @Override
  public DroneState land(Drone drone) {
    return Landing.INSTANCE;
  }

  @Override
  public DroneState crash(Drone drone) {
    return Crashed.INSTANCE;
  }

  /** A gentle, unplanned touchdown counts as landing; a hard one is a crash. */
  @Override
  public DroneState touchdown(Drone drone, double impactSpeed) {
    return impactSpeed > drone.getSpec().safeLandingSpeed() ? Crashed.INSTANCE : Landing.INSTANCE;
  }
}
