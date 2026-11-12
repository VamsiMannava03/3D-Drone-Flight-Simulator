package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks a whole mission against a drone's capabilities before it runs, such as hovering with a
 * fixed-wing drone, dropping a payload it does not carry, or flying before taking off.
 */
public final class MissionValidator {

  private MissionValidator() {
  }

  /**
   * Validates steps for a drone, starting from its current state.
   *
   * @param drone the drone
   * @param steps the steps in order
   * @return one message per invalid step, such as "step 3 'hover 5': fw1 is a FixedWing drone
   *     and cannot hover"; empty if every step can run
   */
  public static List<String> validate(Drone drone, List<? extends DroneCommand> steps) {
    List<String> problems = new ArrayList<>();
    problemsByStep(drone, steps).forEach((i, problem) ->
        problems.add("step " + (i + 1) + " '" + steps.get(i).describe() + "': " + problem));
    return problems;
  }

  /**
   * Validates steps for a drone, starting from its current state, and says which steps fail.
   * A mission editor uses this to flag the exact rows that cannot run.
   *
   * @param drone the drone
   * @param steps the steps in order
   * @return the reason each invalid step cannot run, keyed by its index in {@code steps} (from
   *     0), in step order; empty if every step can run
   */
  public static Map<Integer, String> problemsByStep(Drone drone,
      List<? extends DroneCommand> steps) {
    Map<Integer, String> problems = new LinkedHashMap<>();
    MissionPlan plan = MissionPlan.from(drone);
    for (int i = 0; i < steps.size(); i++) {
      DroneCommand step = steps.get(i);
      try {
        step.check(plan);
      } catch (IllegalStateException | IllegalArgumentException e) {
        problems.put(i, e.getMessage());
        step.finish(plan); // assume it happened, to keep checking the rest sensibly
      }
    }
    return Collections.unmodifiableMap(problems);
  }
}
