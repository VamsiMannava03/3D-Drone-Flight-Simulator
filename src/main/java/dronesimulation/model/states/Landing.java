package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/** Descending to land. Touching down gently gives Landed; too fast gives Crashed. */
public final class Landing implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final Landing INSTANCE = new Landing();

  private Landing() {
  }

  @Override
  public String name() {
    return "Landing";
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
  public DroneState crash(Drone drone) {
    return Crashed.INSTANCE;
  }

  @Override
  public DroneState touchdown(Drone drone, double impactSpeed) {
    return impactSpeed > drone.getSpec().safeLandingSpeed() ? Crashed.INSTANCE : Landed.INSTANCE;
  }

  /** Covers a drone that is already resting on the ground when the landing starts. */
  @Override
  public DroneState update(Drone drone, double dt) {
    return drone.isOnGround() ? Landed.INSTANCE : this;
  }
}
