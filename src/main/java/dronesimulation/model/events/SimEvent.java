package dronesimulation.model.events;

import java.util.Objects;

/**
 * Something that happened during the simulation.
 *
 * @param type what kind of event it is
 * @param droneId the drone it concerns
 * @param time the simulation time in s
 * @param message a short human-readable description
 */
public record SimEvent(SimEventType type, String droneId, double time, String message) {

  /**
   * Creates an event.
   *
   * @throws NullPointerException if type, droneId or message is null
   */
  public SimEvent {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(droneId, "droneId");
    Objects.requireNonNull(message, "message");
  }
}
