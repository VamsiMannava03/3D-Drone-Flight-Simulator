package dronesimulation.controller;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.MissionValidator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * One simple entry point to the whole simulator (Facade pattern), for user interfaces and
 * scripts: the SRS control panel actions (start, pause, reset, speed, add or remove drones, wind
 * settings) and mission files. Every action is applied on the simulation thread before the next
 * step, through the same command language users type, so the model is never touched directly.
 */
public class HoverFacade {

  private final SimulationEngine engine;
  private final CommandParser parser;

  /**
   * Creates a facade for an engine.
   *
   * @param engine the engine to drive
   */
  public HoverFacade(SimulationEngine engine) {
    this.engine = Objects.requireNonNull(engine, "engine");
    this.parser = new CommandParser();
  }

  /**
   * Returns the engine behind this facade.
   *
   * @return the engine
   */
  public SimulationEngine engine() {
    return engine;
  }

  /**
   * Runs one line of the command language, such as "q1 goto 10 0 5".
   *
   * @param line the command
   * @return the normalised command text
   * @throws IllegalArgumentException if the line cannot be parsed
   */
  public String command(String line) {
    ParsedCommand parsed = parser.parse(line);
    engine.submit(parsed.action());
    return parsed.text();
  }

  /** Starts (or resumes) the simulation. */
  public void start() {
    command("start");
  }

  /** Pauses the simulation. */
  public void pause() {
    command("pause");
  }

  /** Rebuilds the scenario and replays the loaded missions, for an identical rerun. */
  public void reset() {
    command("reset");
  }

  /**
   * Sets how many simulated seconds pass per real second.
   *
   * @param factor the speed, from 0.1 to 20
   */
  public void setSpeed(double factor) {
    command("speed " + factor);
  }

  /**
   * Adds a drone from its type's preset.
   *
   * @param type the drone type
   * @param id a new, unique id
   * @param x the home x, in m
   * @param y the home y, in m
   * @param payload the payload in kg, or 0 for none
   */
  public void addDrone(DroneType type, String id, double x, double y, double payload) {
    String word = switch (type) {
      case QUADCOPTER -> "quad";
      case HEXACOPTER -> "hexa";
      case FIXED_WING -> "plane";
    };
    command(String.format(Locale.ROOT, "add %s %s %s %s %s", word, id, x, y, payload));
  }

  /**
   * Removes a drone.
   *
   * @param id the drone id
   */
  public void removeDrone(String id) {
    command("remove " + id);
  }

  /**
   * Changes the wind.
   *
   * @param speed the average speed in m/s
   * @param fromDegrees where it blows from, as a compass bearing (270 = from the west)
   * @param gust the largest gust in m/s, 0 for a steady wind
   */
  public void setWind(double speed, double fromDegrees, double gust) {
    command(String.format(Locale.ROOT, "wind %s %s %s", speed, fromDegrees, gust));
  }

  /**
   * Loads a mission file. Its steps are validated and queued now, and again after every reset.
   *
   * @param file the mission file
   * @return how many commands it holds
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if a line is invalid
   */
  public int loadMission(Path file) throws IOException {
    List<ParsedCommand> commands = new MissionLoader(parser).load(file);
    engine.setScript(commands.stream().map(ParsedCommand::action).toList());
    return commands.size();
  }

  /**
   * Checks a draft mission against a drone, without running it: the SRS mission editor flags
   * invalid steps with this. Each step is written without the drone id, such as "hover 10". The
   * check starts from the drone's state now, as {@link #runMission} replaces its mission.
   *
   * <p>The check runs on the simulation thread, so the result arrives after the next frame
   * (also while paused). It fails with an {@link IllegalArgumentException} if there is no such
   * drone.
   *
   * @param droneId the drone
   * @param steps the steps, in order
   * @return the reason each invalid step cannot run, keyed by its index in {@code steps} (from
   *     0); empty when the whole mission can run
   */
  public CompletableFuture<Map<Integer, String>> checkMission(String droneId,
      List<String> steps) {
    Objects.requireNonNull(droneId, "droneId");
    Map<Integer, String> problems = new LinkedHashMap<>();
    List<DroneCommand> parsed = new ArrayList<>();
    List<Integer> indexOf = new ArrayList<>();
    for (int i = 0; i < steps.size(); i++) {
      try {
        parsed.add(parser.parseStep(steps.get(i)));
        indexOf.add(i);
      } catch (IllegalArgumentException e) {
        problems.put(i, e.getMessage());
      }
    }
    CompletableFuture<Map<Integer, String>> result = new CompletableFuture<>();
    engine.submit(e -> {
      try {
        Drone drone = e.world().drone(droneId);
        MissionValidator.problemsByStep(drone, parsed)
            .forEach((i, problem) -> problems.put(indexOf.get(i), problem));
        Map<Integer, String> sorted = new LinkedHashMap<>();
        problems.keySet().stream().sorted().forEach(i -> sorted.put(i, problems.get(i)));
        result.complete(Collections.unmodifiableMap(sorted));
      } catch (RuntimeException ex) { // any failure, or the caller would wait forever
        result.completeExceptionally(ex);
      }
    });
    return result;
  }

  /**
   * Replaces a drone's mission with new steps, written without the drone id. Each step is
   * checked again when it is queued, and a step the drone cannot do is reported as a
   * COMMAND_REJECTED event.
   *
   * @param droneId the drone
   * @param steps the steps, in order, such as "takeoff 15" and "goto 40 0 15"
   * @throws IllegalArgumentException if a step cannot be parsed; then nothing is changed
   */
  public void runMission(String droneId, List<String> steps) {
    Objects.requireNonNull(droneId, "droneId");
    List<DroneCommand> parsed = new ArrayList<>();
    for (int i = 0; i < steps.size(); i++) {
      try {
        parsed.add(parser.parseStep(steps.get(i)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("step " + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    // One action, so no other command can land between clearing the mission and queueing
    // the new steps, and an unknown drone is reported once, not once per step.
    engine.submit(e -> {
      e.world().mission(droneId).clear();
      parsed.forEach(step -> e.world().queueCommand(droneId, step));
    });
  }

  /**
   * Saves every drone's running and queued steps to a mission file.
   *
   * @param file where to write
   */
  public void saveMission(Path file) {
    command("save " + file);
  }

  /**
   * Registers something that receives a snapshot of the simulation every frame, such as a view.
   *
   * @param listener the listener
   */
  public void onSnapshot(Consumer<WorldSnapshot> listener) {
    engine.addSnapshotListener(listener);
  }
}
