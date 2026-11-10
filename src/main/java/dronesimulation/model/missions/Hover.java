package dronesimulation.model.missions;

import dronesimulation.model.control.HoverController;
import dronesimulation.model.drones.Drone;

/** Holds position for a while. Only drones that can hover may run it; fixed-wing drones cannot. */
public class Hover implements DroneCommand {

  private final double seconds;
  private double elapsed;

  /**
   * Creates the command.
   *
   * @param seconds how long to hover, in s, must be positive
   * @throws IllegalArgumentException if seconds is not positive
   */
  public Hover(double seconds) {
    if (!(seconds > 0) || !Double.isFinite(seconds)) {
      throw new IllegalArgumentException("hover time must be positive, got " + seconds);
    }
    this.seconds = seconds;
  }

  /**
   * Returns how long to hover.
   *
   * @return the duration in s
   */
  public double getSeconds() {
    return seconds;
  }

  @Override
  public void start(Drone drone) {
    Commands.requireFlying(drone, "hover");
    requireCanHover(drone);
    elapsed = 0;
    drone.setController(new HoverController(drone.getPosition()));
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireAirborne("hover");
    requireCanHover(plan.drone());
  }

  private static void requireCanHover(Drone drone) {
    if (drone.getMinFlightSpeed() > 0) {
      throw new IllegalStateException(drone.getId() + " is a " + drone.getTypeName()
          + " drone and cannot hover");
    }
  }

  @Override
  public void update(Drone drone, double dt) {
    elapsed += dt;
  }

  @Override
  public boolean isComplete(Drone drone) {
    return elapsed >= seconds - 1e-9;
  }

  @Override
  public String describe() {
    return "hover " + Commands.format(seconds);
  }
}
