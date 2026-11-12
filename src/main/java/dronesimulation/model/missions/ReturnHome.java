package dronesimulation.model.missions;

import dronesimulation.model.Vector3D;
import dronesimulation.model.control.ReturnToHomeController;
import dronesimulation.model.drones.Drone;

/**
 * Flies back above the drone's home, like a real return-to-home: first climb straight up to the
 * drone's return altitude (or stay higher if already above it) so the path home clears
 * buildings, then fly home at that altitude. Usually followed by Land.
 */
public class ReturnHome implements DroneCommand {

  /** How close to the return altitude counts as done climbing, in m. */
  public static final double CLIMB_TOLERANCE = 1.0;

  private double altitude;
  private boolean climbing;

  /** Creates the command. */
  public ReturnHome() {
  }

  @Override
  public void start(Drone drone) {
    Commands.requireFlying(drone, "return home");
    altitude = Math.max(drone.getReturnAltitude(), drone.getAltitude());
    climbing = drone.getAltitude() < altitude - CLIMB_TOLERANCE;
    if (climbing) {
      Vector3D p = drone.getPosition();
      drone.setController(drone.navigateTo(new Vector3D(p.x(), p.y(), altitude),
          CLIMB_TOLERANCE));
    } else {
      drone.setController(new ReturnToHomeController(drone, altitude));
    }
  }

  @Override
  public void update(Drone drone, double dt) {
    if (climbing && drone.getAltitude() >= altitude - CLIMB_TOLERANCE) {
      climbing = false;
      drone.setController(new ReturnToHomeController(drone, altitude));
    }
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireAirborne("return home");
  }

  @Override
  public boolean isComplete(Drone drone) {
    return !climbing && drone.hasArrived();
  }

  @Override
  public String describe() {
    return "home";
  }
}
