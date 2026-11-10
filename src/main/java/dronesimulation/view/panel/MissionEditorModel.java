package dronesimulation.view.panel;

import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.WorldSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * What the mission editor does, without any widgets: build a mission step by step for the
 * selected drone, check every step against what that drone can do, and run, save or load it.
 *
 * <p>Steps are checked by the simulation itself, through {@link HoverFacade#checkMission},
 * which answers on the simulation thread. The answer is handed to the given UI executor, so
 * the editor only ever changes on one thread. An answer to an older list of steps is ignored.
 */
public final class MissionEditorModel {

  /** The step verbs, each with a hint about its arguments. */
  public static final Map<String, String> STEPS = orderedSteps();

  /** The buttons {@link #press(int, String)} knows. */
  public static final List<String> BUTTONS = List.of("Up", "Down", "Remove", "Clear",
      "Run on drone");

  private final HoverFacade hover;
  private final DroneSelection selection;
  private final Executor ui;
  private final Feedback feedback = new Feedback();
  private final List<String> steps = new ArrayList<>();
  private final List<Runnable> listeners = new ArrayList<>();
  private Map<Integer, String> problems = Map.of();
  private int selected = -1;
  private String droneState;
  private long checkNumber;
  private long answered;
  private String checkError;

  /**
   * Creates the mission editor logic.
   *
   * @param hover drives the simulation
   * @param selection the drone picked in the window
   * @param ui runs the answers of step checks on the UI thread
   */
  public MissionEditorModel(HoverFacade hover, DroneSelection selection, Executor ui) {
    this.hover = Objects.requireNonNull(hover, "hover");
    this.selection = Objects.requireNonNull(selection, "selection");
    this.ui = Objects.requireNonNull(ui, "ui");
    selection.onChange(id -> {
      droneState = null;
      check();
    });
  }

  private static Map<String, String> orderedSteps() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("takeoff", "altitude m");
    m.put("goto", "x y z");
    m.put("hover", "seconds");
    m.put("deliver", "x y z of drop point");
    m.put("drop", "");
    m.put("home", "");
    m.put("land", "");
    m.put("charge", "");
    return Collections.unmodifiableMap(m);
  }

  /**
   * Returns the editor's title.
   *
   * @return which drone the mission is for
   */
  public String title() {
    String id = selection.get();
    return id == null ? "Mission editor (no drone)" : "Mission editor for " + id;
  }

  /**
   * Checks the steps again when the selected drone's state changes, since what it can do
   * depends on it.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    String id = selection.get();
    String state = snapshot.drones().stream().filter(d -> d.id().equals(id))
        .map(DroneSnapshot::state).findFirst().orElse(null);
    if (!Objects.equals(state, droneState)) {
      droneState = state;
      check();
    }
  }

  /**
   * Adds a step at the end and selects it.
   *
   * @param verb one of {@link #STEPS}
   * @param arguments its arguments, as typed
   */
  public void addStep(String verb, String arguments) {
    steps.add((verb + " " + (arguments == null ? "" : arguments.strip())).strip());
    selected = steps.size() - 1;
    check();
  }

  /**
   * Selects a step.
   *
   * @param row the row, or -1 for none
   */
  public void select(int row) {
    selected = row >= 0 && row < steps.size() ? row : -1;
  }

  /**
   * Returns the selected step.
   *
   * @return its row, or -1
   */
  public int selected() {
    return selected;
  }

  /**
   * Selects a row and presses one of the {@link #BUTTONS}.
   *
   * @param row the row
   * @param action the button label
   * @throws IllegalArgumentException if there is no such button
   */
  public void press(int row, String action) {
    select(row);
    switch (action) {
      case "Up" -> move(-1);
      case "Down" -> move(1);
      case "Remove" -> removeStep();
      case "Clear" -> clearSteps();
      case "Run on drone" -> runMission();
      default -> throw new IllegalArgumentException("No button '" + action + "'");
    }
  }

  /**
   * Saves the steps as a mission file for the selected drone.
   *
   * @param file where to save
   */
  public void save(Path file) {
    String id = selection.get();
    if (id == null) {
      feedback.show(Feedback.SELECT_A_DRONE, false);
      return;
    }
    try {
      Files.write(file, toFile(id, steps), StandardCharsets.UTF_8);
      feedback.show("Saved " + steps.size() + " steps to " + file.getFileName() + ".", true);
    } catch (IOException e) {
      feedback.show("Could not save " + file.getFileName() + ": " + e.getMessage(), false);
    }
  }

  /**
   * Replaces the steps with those a mission file has for the selected drone.
   *
   * @param file the mission file
   */
  public void load(Path file) {
    String id = selection.get();
    if (id == null) {
      feedback.show(Feedback.SELECT_A_DRONE, false);
      return;
    }
    try {
      List<String> loaded = fromFile(id, Files.readAllLines(file, StandardCharsets.UTF_8));
      steps.clear();
      steps.addAll(loaded);
      selected = -1;
      feedback.show("Loaded " + loaded.size() + " steps from " + file.getFileName() + ".",
          true);
      check();
    } catch (IOException | IllegalArgumentException e) {
      feedback.show("Could not load " + file.getFileName() + ": " + e.getMessage(), false);
    }
  }

  /**
   * Writes steps as mission file lines for a drone.
   *
   * @param droneId the drone
   * @param steps the steps
   * @return the lines, starting with a comment
   */
  public static List<String> toFile(String droneId, List<String> steps) {
    List<String> lines = new ArrayList<>();
    lines.add("# Mission for " + droneId + ", written by the Hover mission editor.");
    steps.forEach(step -> lines.add(droneId + " " + step));
    return lines;
  }

  /**
   * Reads a drone's steps from mission file lines. If the file has no steps for the drone but
   * steps for exactly one other drone, those are used, so a mission saved for one drone can be
   * loaded onto another.
   *
   * @param droneId the drone
   * @param lines the file's lines
   * @return the steps
   * @throws IllegalArgumentException if no steps fit the drone
   */
  public static List<String> fromFile(String droneId, List<String> lines) {
    List<String> mine = new ArrayList<>();
    Set<String> owners = new LinkedHashSet<>();
    List<String> all = new ArrayList<>();
    for (String raw : lines) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      String[] words = line.split("\\s+", 2);
      String first = words.length < 2 ? "" : words[1].split("\\s+")[0];
      if (!STEPS.containsKey(first.toLowerCase(Locale.ROOT))) {
        continue; // not a drone step
      }
      owners.add(words[0]);
      all.add(words[1]);
      if (words[0].equals(droneId)) {
        mine.add(words[1]);
      }
    }
    if (!mine.isEmpty()) {
      return mine;
    }
    if (owners.size() == 1) {
      return all;
    }
    throw new IllegalArgumentException(owners.isEmpty() ? "no drone steps in the file"
        : "no steps for " + droneId + "; the file has steps for " + String.join(", ", owners));
  }

  /**
   * Returns the steps.
   *
   * @return the steps, in order
   */
  public List<String> steps() {
    return List.copyOf(steps);
  }

  /**
   * Returns the steps the selected drone cannot run, with the reason.
   *
   * @return reasons by row
   */
  public Map<Integer, String> problems() {
    return problems;
  }

  /**
   * Returns how a row is listed: its number, the step and any problem.
   *
   * @param row the row
   * @return the text
   */
  public String rowText(int row) {
    String problem = problems.get(row);
    return String.format(Locale.ROOT, "%2d  %s", row + 1, steps.get(row))
        + (problem == null ? "" : "    <- " + problem);
  }

  /**
   * Returns the answer to the last action or check.
   *
   * @return the feedback
   */
  public Feedback feedback() {
    return feedback;
  }

  /**
   * Adds a listener told whenever the steps, the selection or the problems change.
   *
   * @param listener the listener
   */
  public void onChange(Runnable listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }

  private void removeStep() {
    if (selected >= 0) {
      steps.remove(selected);
      selected = Math.min(selected, steps.size() - 1);
      check();
    }
  }

  private void clearSteps() {
    steps.clear();
    selected = -1;
    check();
  }

  private void move(int by) {
    int i = selected;
    int j = i + by;
    if (i < 0 || j < 0 || j >= steps.size()) {
      return;
    }
    Collections.swap(steps, i, j);
    selected = j;
    check();
  }

  private void check() {
    long number = ++checkNumber;
    String id = selection.get();
    changed();
    if (id == null || steps.isEmpty()) {
      apply(number, Map.of(), null);
      return;
    }
    hover.checkMission(id, steps()).whenComplete((result, error) ->
        ui.execute(() -> apply(number, result, error)));
  }

  private void apply(long number, Map<Integer, String> result, Throwable error) {
    if (number != checkNumber) {
      return; // the steps changed again since this check was asked for
    }
    answered = number;
    checkError = null;
    if (error != null) {
      Throwable cause = error instanceof CompletionException ? error.getCause() : error;
      problems = Map.of();
      checkError = cause.getMessage();
      feedback.show(checkError, false);
    } else {
      problems = result;
      if (steps.isEmpty()) {
        feedback.show("", true);
      } else if (problems.isEmpty()) {
        feedback.show("All " + steps.size() + " steps can run.", true);
      } else {
        Map.Entry<Integer, String> first = problems.entrySet().iterator().next();
        feedback.show(problems.size() + " invalid step" + (problems.size() == 1 ? "" : "s")
            + ". Step " + (first.getKey() + 1) + ": " + first.getValue(), false);
      }
    }
    changed();
  }

  private void runMission() {
    String id = selection.get();
    if (id == null) {
      feedback.show(Feedback.SELECT_A_DRONE, false);
      return;
    }
    if (answered != checkNumber) {
      feedback.show("Still checking the steps; try again in a moment.", false);
      return;
    }
    if (checkError != null) {
      feedback.show("The steps could not be checked: " + checkError, false);
      return;
    }
    if (!problems.isEmpty()) {
      feedback.show("Fix the steps marked in red first.", false);
      return;
    }
    try {
      hover.runMission(id, steps());
      feedback.show("Sent " + steps.size() + " steps to " + id + ".", true);
    } catch (IllegalArgumentException e) {
      feedback.show(e.getMessage(), false);
    }
  }

  private void changed() {
    for (Runnable listener : List.copyOf(listeners)) {
      listener.run();
    }
  }
}
