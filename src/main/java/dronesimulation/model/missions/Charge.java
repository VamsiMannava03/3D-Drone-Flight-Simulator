package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.states.Idle;

/** Charges a landed drone. Done when the battery is full and the drone is Idle again. */
public class Charge implements DroneCommand {

  /** Creates the command. */
  public Charge() {
  }

  @Override
  public void start(Drone drone) {
    drone.charge();
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireLandedForCharge();
    finish(plan);
  }

  @Override
  public void finish(MissionPlan plan) {
    plan.charged();
  }

  @Override
  public boolean isComplete(Drone drone) {
    return drone.getState() == Idle.INSTANCE;
  }

  @Override
  public String describe() {
    return "charge";
  }
}
