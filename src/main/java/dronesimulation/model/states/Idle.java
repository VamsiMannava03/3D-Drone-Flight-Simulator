package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/** On the ground, motors off, ready to take off. */
public final class Idle implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final Idle INSTANCE = new Idle();

  private Idle() {
  }

  @Override
  public String name() {
    return "Idle";
  }

  @Override
  public DroneState takeOff(Drone drone) {
    return TakingOff.INSTANCE;
  }
}
