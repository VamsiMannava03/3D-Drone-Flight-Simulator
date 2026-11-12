package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/** Safely on the ground after a flight. Must charge before the next flight. */
public final class Landed implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final Landed INSTANCE = new Landed();

  private Landed() {
  }

  @Override
  public String name() {
    return "Landed";
  }

  @Override
  public DroneState charge(Drone drone) {
    return Charging.INSTANCE;
  }
}
