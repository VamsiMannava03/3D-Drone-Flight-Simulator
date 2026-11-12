package dronesimulation.controller;

import dronesimulation.model.engine.SimulationEngine;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A command line turned into an action the engine can run.
 *
 * @param text the normalised command text, for messages
 * @param action what to do on the simulation thread
 */
public record ParsedCommand(String text, Consumer<SimulationEngine> action) {

  /**
   * Creates a parsed command.
   *
   * @throws NullPointerException if an argument is null
   */
  public ParsedCommand {
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(action, "action");
  }
}
