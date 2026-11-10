package dronesimulation.model.events;

/**
 * Receives events from the simulation (Observer pattern). Lets the model report what happens
 * without knowing who is listening.
 */
@FunctionalInterface
public interface SimulationListener {

  /**
   * Called when an event happens.
   *
   * @param event the event
   */
  void onEvent(SimEvent event);
}
