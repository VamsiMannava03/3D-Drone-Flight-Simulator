package dronesimulation.model.missions;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.SimEventType;
import java.util.Locale;
import java.util.Objects;

/**
 * Flies to a drop point, drops the payload a few metres above it, and reports DELIVERED.
 */
public class Deliver implements DroneCommand {

  /** Height above the drop point at which the package is released, in m. */
  public static final double DROP_HEIGHT = 2.5;

  private final Vector3D dropPoint;
  private boolean dropped;

  /**
   * Creates the command.
   *
   * @param dropPoint where the package should land; z is the ground or roof height there
   * @throws IllegalArgumentException if the drop point is below the ground
   */
  public Deliver(Vector3D dropPoint) {
    this.dropPoint = Objects.requireNonNull(dropPoint, "dropPoint");
    if (dropPoint.z() < 0) {
      throw new IllegalArgumentException("drop point must not be below the ground");
    }
  }

  @Override
  public void start(Drone drone) {
    Commands.requireFlying(drone, "deliver");
    if (drone.getPayloadMass() <= 0) {
      throw new IllegalStateException(drone.getId() + " has no payload to deliver");
    }
    dropped = false;
    Vector3D release = dropPoint.add(new Vector3D(0, 0, DROP_HEIGHT));
    drone.setController(drone.navigateTo(release, GoTo.ARRIVAL_RADIUS));
  }

  @Override
  public void update(Drone drone, double dt) {
    if (!dropped && drone.hasArrived()) {
      double mass = drone.dropPayload();
      dropped = true;
      drone.getEvents().publish(SimEventType.PAYLOAD_DROPPED, drone.getId(),
          String.format(Locale.ROOT, "dropped %.2f kg at %s", mass,
          Commands.point(dropPoint)));
    }
  }

  @Override
  public void check(MissionPlan plan) {
    plan.requireAirborne("deliver");
    plan.requireAltitude(dropPoint.z() + DROP_HEIGHT);
    plan.requirePayload();
    finish(plan);
  }

  @Override
  public void finish(MissionPlan plan) {
    plan.droppedPayload();
  }

  @Override
  public boolean isComplete(Drone drone) {
    return dropped;
  }

  @Override
  public String describe() {
    return "deliver " + Commands.format(dropPoint.x()) + " " + Commands.format(dropPoint.y())
        + " " + Commands.format(dropPoint.z());
  }
}
