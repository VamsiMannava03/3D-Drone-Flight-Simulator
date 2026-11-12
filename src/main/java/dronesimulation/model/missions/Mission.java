package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.states.Crashed;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A drone's queue of commands, run one after another. When a command cannot start, it is reported
 * as COMMAND_REJECTED and the rest of the mission is cancelled, because later commands usually
 * depend on it. Each finished command is reported as MISSION_STEP_DONE, and the last one also as
 * MISSION_COMPLETE.
 */
public class Mission {

  private final Deque<DroneCommand> queue = new ArrayDeque<>();
  private DroneCommand current;

  /** Creates an empty mission. */
  public Mission() {
  }

  /**
   * Adds a command to the end of the queue.
   *
   * @param command the command
   */
  public void add(DroneCommand command) {
    queue.addLast(Objects.requireNonNull(command, "command"));
  }

  /**
   * Checks a command against the drone's capabilities and everything already planned, and
   * queues it only if it can run.
   *
   * @param drone the drone this mission belongs to
   * @param command the command
   * @throws IllegalStateException or IllegalArgumentException if the drone could not run it
   */
  public void addChecked(Drone drone, DroneCommand command) {
    Objects.requireNonNull(command, "command");
    command.check(planAhead(drone));
    add(command);
  }

  /**
   * Returns the drone's planned situation once the running and queued commands are done.
   *
   * @param drone the drone this mission belongs to
   * @return the plan
   */
  public MissionPlan planAhead(Drone drone) {
    MissionPlan plan = MissionPlan.from(drone);
    if (current != null) {
      current.finish(plan);
    }
    for (DroneCommand queued : queue) {
      queued.finish(plan);
    }
    return plan;
  }

  /**
   * Adds several commands to the end of the queue, in order.
   *
   * @param commands the commands
   */
  public void addAll(Collection<? extends DroneCommand> commands) {
    commands.forEach(this::add);
  }

  /**
   * Drops the current command and everything queued, then queues new commands. Used for
   * emergencies such as returning home on low battery.
   *
   * @param commands the new commands
   */
  public void replaceWith(Collection<? extends DroneCommand> commands) {
    clear();
    addAll(commands);
  }

  /**
   * Keeps the running command but drops everything queued after it, then queues new commands.
   *
   * @param commands the new commands to run after the current one
   */
  public void replaceQueued(Collection<? extends DroneCommand> commands) {
    queue.clear();
    addAll(commands);
  }

  /** Drops the current command and everything queued. */
  public void clear() {
    queue.clear();
    current = null;
  }

  /**
   * Returns the command running now.
   *
   * @return the command, or null if none
   */
  public DroneCommand getCurrent() {
    return current;
  }

  /**
   * Returns the commands waiting to run, in order.
   *
   * @return a copy of the queue
   */
  public List<DroneCommand> getQueued() {
    return new ArrayList<>(queue);
  }

  /**
   * Returns how many commands are waiting to run, without copying the queue.
   *
   * @return the number of queued commands
   */
  public int queuedCount() {
    return queue.size();
  }

  /**
   * Tells whether nothing is running or queued.
   *
   * @return true if idle
   */
  public boolean isEmpty() {
    return current == null && queue.isEmpty();
  }

  /**
   * Runs the mission for one step: starts the next command if none is running, updates the
   * running one, and moves on when it completes. A crashed drone's mission is cleared.
   *
   * @param drone the drone this mission belongs to
   * @param dt the time step in s
   */
  public void update(Drone drone, double dt) {
    if (drone.getState() == Crashed.INSTANCE) {
      clear();
      return;
    }
    if (current == null && !queue.isEmpty()) {
      DroneCommand next = queue.pollFirst();
      try {
        next.start(drone);
        current = next;
      } catch (IllegalStateException | IllegalArgumentException e) {
        int dropped = queue.size();
        queue.clear();
        drone.getEvents().publish(SimEventType.COMMAND_REJECTED, drone.getId(),
            "'" + next.describe() + "' rejected: " + e.getMessage()
                + (dropped > 0 ? " (" + dropped + " queued commands cancelled)" : ""));
        return;
      }
    }
    if (current != null) {
      current.update(drone, dt);
      if (current.isComplete(drone)) {
        drone.getEvents().publish(SimEventType.MISSION_STEP_DONE, drone.getId(),
            "'" + current.describe() + "' done, " + queue.size() + " steps left");
        current = null;
        if (queue.isEmpty()) {
          drone.getEvents().publish(SimEventType.MISSION_COMPLETE, drone.getId(),
              "mission complete");
        }
      }
    }
  }
}
