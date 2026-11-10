package dronesimulation.model.missions;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import java.util.Objects;

/** Flies to a point. Done when the drone gets there. */
public class GoTo implements DroneCommand {

  /** How close counts as arrived, in m. */
  public static final double ARRIVAL_RADIUS = 0.75;

  private final Vector3D target;

  /**
   * Creates the command.
   *
   * @param target the point to fly to; its z must be positive
   * @throws IllegalArgumentException if the target is at or below the ground
   */
  public GoTo(Vector3D target) {
    this.target = Objects.requireNonNull(target, "target");
    if (target.z() <= 0) {
      throw new IllegalArgumentException("GoTo target must be above the ground, got z = "
          + target.z() + " (use land to come down)");
    }
  }

  /**
   * Returns the target.
   *
   * @return the target point
   */
  public Vector3D getTarget() {
    return target;
  }

  @Override
  public void start(Drone drone) {
    Commands.requireFlying(drone, "go to a waypoint");
    drone.setController(drone.navigateTo(target, ARRIVAL_RADIUS));
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireAirborne("go to a waypoint");
    plan.requireAltitude(target.z());
  }

  @Override
  public boolean isComplete(Drone drone) {
    return drone.hasArrived();
  }

  @Override
  public String describe() {
    return "goto " + Commands.format(target.x()) + " " + Commands.format(target.y()) + " "
        + Commands.format(target.z());
  }
}
