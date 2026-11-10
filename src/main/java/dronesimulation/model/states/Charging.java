package dronesimulation.model.states;

import dronesimulation.model.drones.Drone;

/** On the ground, refilling the battery. Becomes Idle when the battery is full. */
public final class Charging implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final Charging INSTANCE = new Charging();

  private Charging() {
  }

  @Override
  public String name() {
    return "Charging";
  }

  @Override
  public DroneState update(Drone drone, double dt) {
    drone.getBattery().recharge(drone.getSpec().chargePower() * dt);
    return drone.getBattery().isFull() ? Idle.INSTANCE : this;
  }
}
