package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.states.Flying;

/** Climbs to an altitude. Done when the drone is Flying. */
public class TakeOff implements DroneCommand {

  private final double altitude;

  /**
   * Creates the command.
   *
   * @param altitude the altitude to climb to, in m, must be positive
   * @throws IllegalArgumentException if altitude is not positive
   */
  public TakeOff(double altitude) {
    if (!(altitude > 0) || !Double.isFinite(altitude)) {
      throw new IllegalArgumentException("altitude must be positive, got " + altitude);
    }
    this.altitude = altitude;
  }

  /**
   * Returns the target altitude.
   *
   * @return the altitude in m
   */
  public double getAltitude() {
    return altitude;
  }

  @Override
  public void start(Drone drone) {
    drone.takeOff(altitude);
    drone.setController(drone.navigateTo(drone.takeOffTarget(altitude), 1.0));
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireReadyToTakeOff();
    plan.requireAltitude(altitude);
    finish(plan);
  }

  @Override
  public void finish(MissionPlan plan) {
    plan.tookOff();
  }

  @Override
  public boolean isComplete(Drone drone) {
    return drone.getState() == Flying.INSTANCE;
  }

  @Override
  public String describe() {
    return "takeoff " + Commands.format(altitude);
  }
}
