package dronesimulation;

import dronesimulation.controller.CommandParser;
import dronesimulation.controller.HoverFacade;
import dronesimulation.controller.SimulationController;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.view.SimulationView;
import dronesimulation.view.TextView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Entry point for Hover, the 3D drone flight simulator.
 *
 * <p>Wires the MVC layers together: the model ({@link SimulationEngine} and its world), the
 * controller ({@link SimulationController} reading commands) and a view that receives
 * read-only snapshots.
 */
public final class Main {

  /** Frames per second at which snapshots are sent to the views. */
  static final double FRAME_RATE = 60;

  private Main() {
    // entry point only
  }

  /**
   * Starts the simulator.
   *
   * @param args options; see {@link Options#parse(String[])}
   */
  public static void main(String[] args) {
    Options options = Options.parse(args);
    EventBus events = new EventBus();
    SimulationEngine engine = createEngine(options, events);
    HoverFacade hover = new HoverFacade(engine);
    SimulationController controller = new SimulationController(engine, new CommandParser(),
        System.out);

    if (options.mission() != null) {
      System.out.println(loadMissions(options.mission(), hover));
    }
    engine.setPaused(options.paused());
    controller.setOnQuit(() -> System.exit(0));
    controller.startConsole(System.in);
    System.out.println("Hover drone flight simulator: " + engine.world().getDrones().size()
        + " drones. Type 'help' for commands, 'quit' to exit."
        + (options.paused() ? " Paused: type 'start' to begin." : ""));

    TextView view = new TextView();
    connect(view, engine, events);
    engine.runRealtime(FRAME_RATE);
  }

  /**
   * Builds the simulation for the chosen scenario. It holds a scenario, not a single world, so
   * reset can rebuild it for an identical rerun.
   *
   * @param options the parsed options
   * @param events the event bus the world reports to
   * @return the engine
   */
  static SimulationEngine createEngine(Options options, EventBus events) {
    return new SimulationEngine(
        () -> options.scenario().equals("empty") ? DemoScenario.empty(events)
            : DemoScenario.create(options.seed(), events), SimulationEngine.DEFAULT_DT);
  }

  /**
   * Sends snapshots and events to a view. Views that do not print events themselves get them
   * echoed to the terminal.
   *
   * @param view the view
   * @param engine the simulation
   * @param events the event bus
   */
  static void connect(SimulationView view, SimulationEngine engine, EventBus events) {
    engine.addSnapshotListener(view::render);
    boolean viewPrints = view instanceof TextView;
    events.subscribe(e -> {
      String line = format(e);
      view.showMessage(line);
      if (!viewPrints) {
        System.out.println(line);
      }
    });
  }

  /**
   * Loads a mission file and says what happened.
   *
   * @param file the mission file
   * @param hover the facade to load it through
   * @return a message for the user
   */
  static String loadMissions(Path file, HoverFacade hover) {
    if (!Files.exists(file)) {
      return "No mission file at " + file + "; type commands instead.";
    }
    try {
      int count = hover.loadMission(file);
      return "Loaded " + count + " commands from " + file + " (replayed on reset)";
    } catch (IOException | IllegalArgumentException e) {
      return "Could not load missions: " + e.getMessage();
    }
  }

  /**
   * Formats an event as one line of the event log.
   *
   * @param e the event
   * @return the line
   */
  static String format(SimEvent e) {
    return String.format(Locale.ROOT, "[%6.2f] %-4s %-16s %s", e.time(), e.droneId(),
        e.type(), e.message());
  }

  /**
   * Command-line options.
   *
   * @param mode how to show the simulation; only {@code text} so far
   * @param mission a mission file to run at start and after every reset, or null
   * @param seed the seed for gusts and sensor noise
   * @param paused whether to start paused
   * @param scenario {@code city} or {@code empty}
   */
  record Options(String mode, Path mission, long seed, boolean paused, String scenario) {

    /** The display modes this build supports. */
    static final List<String> MODES = List.of("text");

    /**
     * Parses the command line.
     *
     * @param args the arguments: a mode, {@code --mission=FILE}, {@code --seed=N},
     *     {@code --scenario=city|empty} and {@code --paused}
     * @return the options
     * @throws IllegalArgumentException for an unknown mode or scenario, or a bad seed
     */
    static Options parse(String[] args) {
      String mode = MODES.get(0);
      Path mission = null;
      long seed = 42;
      boolean paused = false;
      String scenario = "city";
      for (String arg : args) {
        if (arg.startsWith("--scenario=")) {
          scenario = arg.substring("--scenario=".length()).toLowerCase(Locale.ROOT);
        } else if (arg.equals("--paused")) {
          paused = true;
        } else if (arg.startsWith("--mission=")) {
          mission = Path.of(arg.substring("--mission=".length()));
        } else if (arg.startsWith("--seed=")) {
          seed = Long.parseLong(arg.substring("--seed=".length()));
        } else {
          mode = arg.toLowerCase(Locale.ROOT);
        }
      }
      if (!MODES.contains(mode)) {
        throw new IllegalArgumentException("Unknown mode '" + mode + "'. Use "
            + String.join(" or ", MODES) + ".");
      }
      if (!List.of("city", "empty").contains(scenario)) {
        throw new IllegalArgumentException("Unknown scenario '" + scenario
            + "'. Use city or empty.");
      }
      if (mission == null && scenario.equals("city")) {
        mission = Path.of("missions", "demo.txt"); // the empty world starts with nothing
      }
      return new Options(mode, mission, seed, paused, scenario);
    }
  }
}
