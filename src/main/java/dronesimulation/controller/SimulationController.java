package dronesimulation.controller;

import dronesimulation.model.engine.SimulationEngine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * The controller of MVC: takes user input (console lines, or keys forwarded by a view), parses it
 * and hands the result to the engine. It never changes the model directly.
 */
public class SimulationController {

  private final SimulationEngine engine;
  private final CommandParser parser;
  private final PrintStream out;
  private volatile Runnable onQuit = () -> { };

  /**
   * Creates a controller.
   *
   * @param engine the engine to send commands to
   * @param parser the command parser
   * @param out where to print replies and errors
   */
  public SimulationController(SimulationEngine engine, CommandParser parser, PrintStream out) {
    this.engine = Objects.requireNonNull(engine, "engine");
    this.parser = Objects.requireNonNull(parser, "parser");
    this.out = Objects.requireNonNull(out, "out");
  }

  /**
   * Sets what happens when the user types {@code quit}, for example closing the window.
   *
   * @param onQuit the action
   */
  public void setOnQuit(Runnable onQuit) {
    this.onQuit = Objects.requireNonNull(onQuit, "onQuit");
  }

  /**
   * Handles one line of input. Errors are printed, never thrown.
   *
   * @param line the input
   * @return true if the line was accepted
   */
  public boolean execute(String line) {
    String text = line == null ? "" : line.strip();
    if (text.isEmpty()) {
      return false;
    }
    switch (text.toLowerCase(Locale.ROOT)) {
      case "help":
        out.println(CommandParser.HELP);
        return true;
      case "quit":
      case "exit":
        engine.stop();
        onQuit.run();
        return true;
      default:
        try {
          ParsedCommand command = parser.parse(text);
          engine.submit(command.action());
          out.println("> " + command.text());
          return true;
        } catch (IllegalArgumentException e) {
          out.println("! " + e.getMessage());
          return false;
        }
    }
  }

  /**
   * Starts a background thread that reads commands from a stream, one per line, until the
   * stream ends.
   *
   * @param in the input, normally System.in
   * @return the started daemon thread
   */
  public Thread startConsole(InputStream in) {
    Thread thread = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          execute(line);
        }
      } catch (IOException e) {
        out.println("! console closed: " + e.getMessage());
      }
    }, "console-input");
    thread.setDaemon(true);
    thread.start();
    return thread;
  }
}
