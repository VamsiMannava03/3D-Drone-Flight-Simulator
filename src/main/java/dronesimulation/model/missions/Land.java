package dronesimulation.model.missions;

import dronesimulation.model.control.LandingController;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.states.Landed;

/** Comes down gently and lands. Done when the drone is Landed. */
public class Land implements DroneCommand {

  /** Descent speed high above the ground, in m/s. */
  public static final double DESCENT_RATE = 2.0;

  /** Creates the command. */
  public Land() {
  }

  @Override
  public void start(Drone drone) {
    drone.land();
    drone.setController(new LandingController(DESCENT_RATE));
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireAirborne("land");
    finish(plan);
  }

  @Override
  public void finish(MissionPlan plan) {
    plan.landed();
  }

  @Override
  public boolean isComplete(Drone drone) {
    return drone.getState() == Landed.INSTANCE;
  }

  @Override
  public String describe() {
    return "land";
  }
}
