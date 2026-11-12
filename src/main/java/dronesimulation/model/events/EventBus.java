package dronesimulation.model.events;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;

/**
 * Delivers simulation events to every subscribed listener (Observer pattern). The model publishes
 * here without knowing whether a view, the event log, a reaction policy or a test is listening.
 *
 * <p>Events are queued as they happen and delivered by {@link #flush()} at the end of each
 * simulation step, so listeners always see a consistent, finished step. Each event reaches each
 * listener exactly once, in publication order, and listeners in subscription order. A listener
 * that throws is reported to the error handler and does not stop the others.
 */
public class EventBus {

  private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();
  private final Queue<SimEvent> pending = new ArrayDeque<>();
  private DoubleSupplier clock = () -> 0;
  private BiConsumer<SimulationListener, RuntimeException> errorHandler =
      (listener, e) -> System.err.println("event listener failed: " + e);

  /** Creates an event bus with no listeners. */
  public EventBus() {
  }

  /**
   * Sets where event times come from, normally the simulation clock.
   *
   * @param clock returns the current simulation time in s
   */
  public void setClock(DoubleSupplier clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Sets what happens when a listener throws. By default the error is printed to stderr.
   *
   * @param errorHandler receives the failing listener and its exception
   */
  public void setErrorHandler(BiConsumer<SimulationListener, RuntimeException> errorHandler) {
    this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
  }

  /**
   * Adds a listener. It receives every event delivered after this call.
   *
   * @param listener the listener
   */
  public void subscribe(SimulationListener listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }

  /**
   * Removes a listener. Does nothing if it was not subscribed.
   *
   * @param listener the listener
   */
  public void unsubscribe(SimulationListener listener) {
    listeners.remove(listener);
  }

  /**
   * Queues an event for delivery at the next {@link #flush()}.
   *
   * @param event the event
   */
  public synchronized void publish(SimEvent event) {
    pending.add(Objects.requireNonNull(event, "event"));
  }

  /**
   * Queues an event stamped with the current clock time.
   *
   * @param type the kind of event
   * @param droneId the drone it concerns
   * @param message a short description
   */
  public void publish(SimEventType type, String droneId, String message) {
    publish(new SimEvent(type, droneId, clock.getAsDouble(), message));
  }

  /**
   * Delivers every queued event, including any that listeners publish while handling them.
   * Called at the end of each simulation step.
   */
  public void flush() {
    SimEvent event;
    while ((event = next()) != null) {
      for (SimulationListener listener : listeners) {
        try {
          listener.onEvent(event);
        } catch (RuntimeException e) {
          errorHandler.accept(listener, e);
        }
      }
    }
  }

  /**
   * Tells how many events are waiting for delivery.
   *
   * @return the number of queued events
   */
  public synchronized int pendingCount() {
    return pending.size();
  }

  private synchronized SimEvent next() {
    return pending.poll();
  }
}
