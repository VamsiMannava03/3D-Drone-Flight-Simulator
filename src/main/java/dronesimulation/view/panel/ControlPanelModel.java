package dronesimulation.view.panel;

import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.view.SimulationView;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What the control panel does, without any widgets: start, pause, reset and speed, adding and
 * removing drones, setting the wind and loading mission files. Every action goes through the
 * {@link HoverFacade}, the same commands the terminal uses.
 */
public final class ControlPanelModel {

  /** The drone kinds offered, as the add command spells them. */
  public static final List<String> TYPES = List.of("quad", "hexa", "plane");
  /** Compass bearings offered for the wind; a number of degrees can be typed instead. */
  public static final List<String> BEARINGS = List.of("w", "nw", "n", "ne", "e", "se", "s",
      "sw");
  /** The buttons {@link #press(String)} knows. */
  public static final List<String> BUTTONS = List.of("Start", "Pause", "Reset",
      "Remove selected");

  private static final List<String> PREFIXES = List.of("q", "h", "fw");

  private final HoverFacade hover;
  private final DroneSelection selection;
  private final Feedback feedback = new Feedback();
  private Set<String> droneIds = Set.of();
  private String clock = "t = 0.0 s";
  private int speedIndex = SimulationView.SPEED_STEPS.indexOf(1.0);

  /**
   * Creates the control panel logic.
   *
   * @param hover drives the simulation
   * @param selection the drone picked in the window
   */
  public ControlPanelModel(HoverFacade hover, DroneSelection selection) {
    this.hover = Objects.requireNonNull(hover, "hover");
    this.selection = Objects.requireNonNull(selection, "selection");
  }

  /**
   * Reads the clock, the speed and the drone ids from a snapshot. A speed changed elsewhere,
   * for example in the terminal, is only shown, never sent back.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    clock = String.format(Locale.ROOT, "t = %.1f s%s", snapshot.time(),
        snapshot.paused() ? "  (paused)" : "");
    droneIds = snapshot.drones().stream().map(DroneSnapshot::id).collect(Collectors.toSet());
    int index = SimulationView.SPEED_STEPS.indexOf(snapshot.timeScale());
    if (index >= 0) {
      speedIndex = index;
    }
  }

  /**
   * Returns the clock text.
   *
   * @return for example {@code t = 12.3 s  (paused)}
   */
  public String clockText() {
    return clock;
  }

  /**
   * Returns which of {@link SimulationView#SPEED_STEPS} the simulation runs at.
   *
   * @return the index of the speed step
   */
  public int speedIndex() {
    return speedIndex;
  }

  /**
   * Presses one of the {@link #BUTTONS}.
   *
   * @param label the button label
   * @throws IllegalArgumentException if there is no such button
   */
  public void press(String label) {
    switch (label) {
      case "Start" -> run(hover::start, "started");
      case "Pause" -> run(hover::pause, "paused");
      case "Reset" -> run(hover::reset, "reset: scenario and missions rerun");
      case "Remove selected" -> removeSelected();
      default -> throw new IllegalArgumentException("No button '" + label + "'");
    }
  }

  /**
   * Changes the simulation speed.
   *
   * @param factor one of {@link SimulationView#SPEED_STEPS}
   */
  public void chooseSpeed(double factor) {
    int index = SimulationView.SPEED_STEPS.indexOf(factor);
    if (index < 0) {
      feedback.show("Unknown speed " + factor, false);
      return;
    }
    if (run(() -> hover.setSpeed(factor), "speed set")) {
      speedIndex = index;
    }
  }

  /**
   * Adds a drone, as the form's fields say.
   *
   * @param kind one of {@link #TYPES}
   * @param id the new id, or blank for a suggested one
   * @param x the home x in m, as typed
   * @param y the home y in m, as typed
   * @param payload the payload in kg, as typed
   * @param battery the starting battery level in percent, as typed
   * @return true if the drone was added
   */
  public boolean addDrone(String kind, String id, String x, String y, String payload,
      String battery) {
    String newId = id == null || id.isBlank() ? suggestId(kind, droneIds) : id.strip();
    return run(() -> hover.command(String.join(" ", "add", kind, newId, x.strip(), y.strip(),
        payload.strip(), battery.strip())), "added " + newId);
  }

  /**
   * Sets the wind, as the form's fields say.
   *
   * @param speed the speed in m/s, as typed
   * @param from a compass bearing such as {@code w}, or degrees
   * @param gust the gust strength in m/s, as typed
   */
  public void setWind(String speed, String from, String gust) {
    run(() -> hover.command(String.join(" ", "wind", speed.strip(), from.strip(),
        gust.strip())), "wind set");
  }

  /**
   * Loads a mission file; its commands run now and again after every reset.
   *
   * @param file the mission file
   */
  public void loadMission(Path file) {
    try {
      int count = hover.loadMission(file);
      feedback.show("loaded " + count + " commands from " + file.getFileName()
          + " (replayed on reset)", true);
    } catch (IOException | IllegalArgumentException e) {
      feedback.show("Could not load " + file.getFileName() + ": " + e.getMessage(), false);
    }
  }

  /**
   * Returns the answer to the last action.
   *
   * @return the feedback
   */
  public Feedback feedback() {
    return feedback;
  }

  /**
   * Suggests the first free id for a kind of drone: q1, q2, ... for quadcopters, h1, ... for
   * hexacopters and fw1, ... for planes.
   *
   * @param kind one of {@link #TYPES}
   * @param taken the ids already in use
   * @return the suggested id
   */
  public static String suggestId(String kind, Set<String> taken) {
    int type = TYPES.indexOf(kind);
    String prefix = PREFIXES.get(Math.max(0, type));
    int n = 1;
    while (taken.contains(prefix + n)) {
      n++;
    }
    return prefix + n;
  }

  /**
   * Formats a speed factor for the speed list.
   *
   * @param factor the factor
   * @return for example {@code 2x} or {@code 0.5x}
   */
  public static String speedLabel(double factor) {
    return (factor == Math.rint(factor) ? String.valueOf((long) factor)
        : String.valueOf(factor)) + "x";
  }

  private void removeSelected() {
    String selected = selection.get();
    if (selected == null) {
      feedback.show(Feedback.SELECT_A_DRONE, false);
      return;
    }
    run(() -> hover.removeDrone(selected), "removed " + selected);
  }

  private boolean run(Runnable action, String success) {
    try {
      action.run();
      feedback.show(success, true);
      return true;
    } catch (IllegalArgumentException e) {
      feedback.show(e.getMessage(), false);
      return false;
    }
  }
}
