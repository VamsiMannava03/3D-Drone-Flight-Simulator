package dronesimulation.controller;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.DroneBuilder;
import dronesimulation.model.drones.DroneFactory;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.missions.Charge;
import dronesimulation.model.missions.Deliver;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.DropPayload;
import dronesimulation.model.missions.GoTo;
import dronesimulation.model.missions.Hover;
import dronesimulation.model.missions.Land;
import dronesimulation.model.missions.ReturnHome;
import dronesimulation.model.missions.TakeOff;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Turns text commands into actions. The same language is used at the console and in mission
 * files:
 *
 * <pre>
 *   &lt;drone&gt; takeoff &lt;altitude&gt;
 *   &lt;drone&gt; goto &lt;x&gt; &lt;y&gt; &lt;z&gt;
 *   &lt;drone&gt; hover &lt;seconds&gt;
 *   &lt;drone&gt; deliver &lt;x&gt; &lt;y&gt; &lt;z&gt;
 *   &lt;drone&gt; land | home | charge | clear
 *   pause | resume | speed &lt;factor&gt;
 * </pre>
 *
 * <p>Arguments are checked while parsing, so typos fail at once with a clear
 * {@link IllegalArgumentException}. Whether the drone exists is checked when the action runs.
 */
public class CommandParser {

  /** Help text listing every command. */
  public static final String HELP = String.join(System.lineSeparator(),
      "Drone commands:",
      "  <drone> takeoff <altitude>     climb to an altitude",
      "  <drone> goto <x> <y> <z>       fly to a point",
      "  <drone> hover <seconds>        hold position",
      "  <drone> deliver <x> <y> <z>    fly to a point and drop the payload there",
      "  <drone> drop                   drop the payload where it is",
      "  <drone> home                   fly back above home",
      "  <drone> land                   land where it is",
      "  <drone> charge                 charge after landing",
      "  <drone> clear                  cancel its mission",
      "Every step is checked against the drone before it is queued.",
      "Missions: save <file>   (load one with --mission=<file>)",
      "Drones: add <quad|hexa|plane> <id> <x> <y> [payload kg] [battery %] | remove <id>",
      "Wind: wind <speed m/s> <from: degrees or n/ne/e/se/s/sw/w/nw> [gust m/s]",
      "Simulation: start | pause | resume | reset | speed <0.1-20> | help | quit");

  /** First words that {@link #parse} reads as simulation commands, never as drone ids. */
  private static final Set<String> KEYWORDS =
      Set.of("pause", "resume", "start", "reset", "wind", "add", "remove", "speed", "save");

  /** The mission steps, as listed in error messages. */
  private static final String STEPS = "takeoff, goto, hover, deliver, drop, home, land or charge";
  /** What may follow a drone id on the console: a step, or clearing its mission. */
  private static final String STEPS_OR_CLEAR =
      "takeoff, goto, hover, deliver, drop, home, land, charge or clear";

  /** Creates a parser. It holds no state, so one parser can be shared. */
  public CommandParser() {
  }

  /**
   * Parses one command line.
   *
   * @param line the command, for example "q1 goto 10 0 5"
   * @return the parsed command
   * @throws IllegalArgumentException if the line is empty, unknown, or has bad arguments
   */
  public ParsedCommand parse(String line) {
    String[] words = line == null ? new String[0] : line.trim().split("\\s+");
    if (words.length == 0 || words[0].isEmpty()) {
      throw new IllegalArgumentException("Empty command. Type 'help' for the list.");
    }
    String first = words[0].toLowerCase(Locale.ROOT);
    switch (first) {
      case "pause":
        expectArgs(words, 1, "pause");
        return new ParsedCommand("pause", e -> e.setPaused(true));
      case "resume":
      case "start":
        expectArgs(words, 1, first);
        return new ParsedCommand(first, e -> e.setPaused(false));
      case "reset":
        expectArgs(words, 1, "reset");
        return new ParsedCommand("reset", SimulationEngine::reset);
      case "wind":
        return parseWind(words);
      case "add":
        return parseAdd(words);
      case "remove":
        expectArgs(words, 2, "remove <id>");
        String removed = words[1];
        return new ParsedCommand("remove " + removed, e -> e.world().removeDrone(removed));
      case "speed":
        expectArgs(words, 2, "speed <factor>");
        double factor = number(words[1], "speed factor");
        return new ParsedCommand("speed " + words[1], e -> e.setTimeScale(factor));
      case "save":
        if (words.length < 2) {
          throw new IllegalArgumentException("Usage: save <file>");
        }
        // The rest of the line is the path, so folders such as "My Missions" work.
        String path = line.trim().substring(words[0].length()).trim();
        Path file = Path.of(path);
        return new ParsedCommand("save " + path, e -> MissionWriter.save(e.world(), file));
      default:
        return parseDroneCommand(words);
    }
  }

  private ParsedCommand parseDroneCommand(String[] words) {
    String id = words[0];
    if (words.length < 2) {
      throw new IllegalArgumentException("Missing command after '" + id
          + "'. Type 'help' for the list.");
    }
    String verb = words[1].toLowerCase(Locale.ROOT);
    if (verb.equals("clear")) {
      expectArgs(words, 2, id + " clear");
      return new ParsedCommand(id + " clear", e -> e.world().mission(id).clear());
    }
    DroneCommand command = step(words, STEPS_OR_CLEAR);
    return new ParsedCommand(id + " " + command.describe(),
        e -> e.world().queueCommand(id, command));
  }

  /**
   * Parses one mission step without a drone id, such as "goto 10 0 5", for mission editors.
   * Only syntax is checked here; whether a drone can perform it is checked against the drone.
   *
   * @param step the step, for example "takeoff 15"
   * @return the step as a command
   * @throws IllegalArgumentException if the step is empty, unknown, or has bad arguments
   */
  public DroneCommand parseStep(String step) {
    String text = step == null ? "" : step.trim();
    if (text.isEmpty()) {
      throw new IllegalArgumentException("Empty step. Expected " + STEPS + ".");
    }
    return step(("<drone> " + text).split("\\s+"), STEPS);
  }

  /**
   * Parses words[1..] as a mission step; words[0] is the drone id, used in usage messages, and
   * expected lists what may come instead, for the message about an unknown word.
   */
  private static DroneCommand step(String[] words, String expected) {
    String id = words[0];
    String verb = words[1].toLowerCase(Locale.ROOT);
    return switch (verb) {
      case "takeoff" -> {
        expectArgs(words, 3, id + " takeoff <altitude>");
        yield new TakeOff(number(words[2], "altitude"));
      }
      case "goto" -> {
        expectArgs(words, 5, id + " goto <x> <y> <z>");
        yield new GoTo(point(words));
      }
      case "hover" -> {
        expectArgs(words, 3, id + " hover <seconds>");
        yield new Hover(number(words[2], "seconds"));
      }
      case "deliver" -> {
        expectArgs(words, 5, id + " deliver <x> <y> <z>");
        yield new Deliver(point(words));
      }
      case "drop" -> {
        expectArgs(words, 2, id + " drop");
        yield new DropPayload();
      }
      case "land" -> {
        expectArgs(words, 2, id + " land");
        yield new Land();
      }
      case "home" -> {
        expectArgs(words, 2, id + " home");
        yield new ReturnHome();
      }
      case "charge" -> {
        expectArgs(words, 2, id + " charge");
        yield new Charge();
      }
      default -> throw new IllegalArgumentException("Unknown command '" + words[1]
          + "'. Expected " + expected + ".");
    };
  }

  private ParsedCommand parseWind(String[] words) {
    if (words.length != 3 && words.length != 4) {
      throw new IllegalArgumentException("Usage: wind <speed m/s> <from> [gust m/s]");
    }
    double speed = number(words[1], "wind speed");
    double from = bearing(words[2]);
    double gust = words.length == 4 ? number(words[3], "gust strength") : 0;
    if (speed < 0 || gust < 0) {
      throw new IllegalArgumentException("wind speed and gusts must not be negative");
    }
    String text = String.format(Locale.ROOT, "wind %s from %.0f deg, gusts %s", words[1], from,
        words.length == 4 ? words[3] : "0");
    return new ParsedCommand(text, e -> e.world().setWind(speed, from, gust));
  }

  private ParsedCommand parseAdd(String[] words) {
    if (words.length < 5 || words.length > 7) {
      throw new IllegalArgumentException(
          "Usage: add <quad|hexa|plane> <id> <x> <y> [payload kg] [battery %]");
    }
    DroneType type = switch (words[1].toLowerCase(Locale.ROOT)) {
      case "quad", "quadcopter" -> DroneType.QUADCOPTER;
      case "hexa", "hexacopter" -> DroneType.HEXACOPTER;
      case "plane", "fixedwing", "fixed-wing" -> DroneType.FIXED_WING;
      default -> throw new IllegalArgumentException("Unknown drone type '" + words[1]
          + "'. Expected quad, hexa or plane.");
    };
    String id = words[2];
    if (KEYWORDS.contains(id.toLowerCase(Locale.ROOT))) {
      // "speed takeoff 10" would parse as the speed command, so the drone could not be flown.
      throw new IllegalArgumentException(
          "'" + id + "' is a command word and cannot be a drone id");
    }
    Vector3D home = new Vector3D(number(words[3], "x"), number(words[4], "y"), 0);
    double payload = words.length >= 6 ? number(words[5], "payload") : 0;
    double battery = words.length == 7 ? number(words[6], "battery %") / 100 : 1;
    String text = "add " + type.name().toLowerCase(Locale.ROOT) + " " + id;
    return new ParsedCommand(text, e -> {
      // A seed from the id keeps runs repeatable without sharing the scenario's Random.
      DroneBuilder builder = DroneFactory.builder(type, id, home, new Random(id.hashCode()))
          .batteryLevel(battery);
      if (payload > 0) {
        builder.payload(payload);
      }
      e.world().addDrone(builder.build());
    });
  }

  private static double bearing(String word) {
    return switch (word.toLowerCase(Locale.ROOT)) {
      case "n", "north" -> 0;
      case "ne", "northeast" -> 45;
      case "e", "east" -> 90;
      case "se", "southeast" -> 135;
      case "s", "south" -> 180;
      case "sw", "southwest" -> 225;
      case "w", "west" -> 270;
      case "nw", "northwest" -> 315;
      default -> number(word, "wind direction");
    };
  }

  private static Vector3D point(String[] words) {
    return new Vector3D(number(words[2], "x"), number(words[3], "y"), number(words[4], "z"));
  }

  private static void expectArgs(String[] words, int count, String usage) {
    if (words.length != count) {
      throw new IllegalArgumentException("Usage: " + usage);
    }
  }

  private static double number(String word, String what) {
    try {
      double value = Double.parseDouble(word);
      if (!Double.isFinite(value)) {
        throw new NumberFormatException();
      }
      return value;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Expected a number for " + what + " but got '" + word + "'");
    }
  }
}
