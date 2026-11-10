package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.SimEventType;
import java.util.Locale;

/**
 * Releases the payload right where the drone is, and reports PAYLOAD_DROPPED. The drone becomes
 * lighter at once, so it needs less thrust to hover. Use {@link Deliver} to fly to a drop point
 * first.
 */
public class DropPayload implements DroneCommand {

  private boolean dropped;

  /** Creates the command. */
  public DropPayload() {
  }

  @Override
  public void start(Drone drone) {
    Commands.requireFlying(drone, "drop its payload");
    if (drone.getPayloadMass() <= 0) {
      throw new IllegalStateException(drone.getId() + " has no payload to drop");
    }
    double mass = drone.dropPayload();
    dropped = true;
    drone.getEvents().publish(SimEventType.PAYLOAD_DROPPED, drone.getId(),
        String.format(Locale.ROOT, "dropped %.2f kg at %s", mass,
            Commands.point(drone.getPosition())));
  }

  @Override
  public boolean isComplete(Drone drone) {
    return dropped;
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireAirborne("drop its payload");
    plan.requirePayload();
    finish(plan);
  }

  @Override
  public void finish(MissionPlan plan) {
    plan.droppedPayload();
  }

  @Override
  public String describe() {
    return "drop";
  }
}
