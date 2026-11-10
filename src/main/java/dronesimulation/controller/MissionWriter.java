package dronesimulation.controller;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.Mission;
import dronesimulation.model.missions.TakeOff;
import dronesimulation.model.world.World;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Saves the planned missions of every drone as a mission file, in the same command language that
 * {@link MissionLoader} reads, so a saved file can be loaded and run again.
 */
public final class MissionWriter {

  private MissionWriter() {
  }

  /**
   * Returns the running and queued commands of every drone, one per line, grouped by drone.
   * Steps already finished are not saved; for a drone in the air, a take-off to its current
   * altitude comes first so that the file also runs from the ground.
   *
   * @param world the world
   * @return the mission file text
   */
  public static String script(World world) {
    List<String> lines = new ArrayList<>();
    lines.add(String.format(Locale.ROOT, "# Hover mission file, saved at t = %.2f s",
        world.getTime()));
    for (Drone drone : world.getDrones()) {
      Mission mission = world.mission(drone.getId());
      List<DroneCommand> steps = new ArrayList<>();
      if (mission.getCurrent() != null) {
        steps.add(mission.getCurrent());
      }
      steps.addAll(mission.getQueued());
      if (steps.isEmpty()) {
        continue;
      }
      lines.add("");
      lines.add("# " + drone.getId() + " (" + drone.getTypeName() + ")");
      if (drone.getState().canFly() && !(steps.get(0) instanceof TakeOff)) {
        // Its take-off is already done, but a loaded file starts every drone on the ground.
        TakeOff climb = new TakeOff(Math.max(1, Math.round(drone.getAltitude())));
        lines.add(drone.getId() + " " + climb.describe());
      }
      for (DroneCommand step : steps) {
        lines.add(drone.getId() + " " + step.describe());
      }
    }
    return String.join(System.lineSeparator(), lines) + System.lineSeparator();
  }

  /**
   * Writes the planned missions to a file, replacing it.
   *
   * @param world the world
   * @param file where to write
   * @throws UncheckedIOException if the file cannot be written
   */
  public static void save(World world, Path file) {
    try {
      Files.writeString(file, script(world), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("could not save missions to " + file + ": "
          + e.getMessage(), e);
    }
  }
}
