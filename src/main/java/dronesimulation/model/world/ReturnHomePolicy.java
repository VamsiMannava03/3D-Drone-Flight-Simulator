package dronesimulation.model.world;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.events.SimulationListener;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.Land;
import dronesimulation.model.missions.Mission;
import dronesimulation.model.missions.ReturnHome;
import dronesimulation.model.states.Flying;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An automatic reaction: when a flying drone reports low battery, it abandons the rest of its
 * mission, returns home and lands, and the decision is reported as RETURN_HOME.
 *
 * <p>Policies are ordinary event listeners, so new reactions can be added without changing the
 * world or the drones.
 */
public class ReturnHomePolicy implements SimulationListener {

  private final World world;

  /**
   * Creates the policy.
   *
   * @param world the world whose drones it steers
   */
  public ReturnHomePolicy(World world) {
    this.world = Objects.requireNonNull(world, "world");
  }

  @Override
  public void onEvent(SimEvent event) {
    // LOW_BATTERY fires once, maybe while the drone is still grounded or climbing; TOOK_OFF
    // catches a drone that reaches the air already low.
    boolean lowNow = event.type() == SimEventType.LOW_BATTERY;
    if (!lowNow && event.type() != SimEventType.TOOK_OFF) {
      return;
    }
    Optional<Drone> found = world.findDrone(event.droneId());
    if (found.isEmpty() || found.get().getState() != Flying.INSTANCE) {
      return;
    }
    Drone drone = found.get();
    if (!lowNow && !drone.getBattery().isLow()) {
      return;
    }
    Mission mission = world.mission(drone.getId());
    // Only a return that is running now counts; a "home" queued after more legs does not.
    if (mission.getCurrent() instanceof ReturnHome) {
      List<DroneCommand> queued = mission.getQueued();
      if (queued.size() == 1 && queued.get(0) instanceof Land) {
        return; // already going home to land
      }
      // Keep the running return, but land when it arrives instead of flying on.
      mission.replaceQueued(List.of(new Land()));
      world.getEvents().publish(SimEventType.RETURN_HOME, drone.getId(), queued.isEmpty()
          ? "low battery: landing after returning home"
          : "low battery: abandoning " + steps(queued.size()) + " after returning home");
      return;
    }
    if (mission.getCurrent() instanceof Land) {
      return;
    }
    int abandoned = mission.queuedCount() + (mission.getCurrent() == null ? 0 : 1);
    mission.replaceWith(List.of(new ReturnHome(), new Land()));
    world.getEvents().publish(SimEventType.RETURN_HOME, drone.getId(), abandoned == 0
        ? "low battery: returning home"
        : "low battery: abandoning " + steps(abandoned) + ", returning home");
  }

  private static String steps(int n) {
    return n + " mission step" + (n == 1 ? "" : "s");
  }
}
