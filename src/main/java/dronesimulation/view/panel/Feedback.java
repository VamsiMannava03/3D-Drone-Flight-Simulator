package dronesimulation.view.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The one-line answer a panel shows after the user does something: what happened, and
 * whether it worked.
 */
public final class Feedback {

  /** Shown when an action needs a selected drone but none is. */
  public static final String SELECT_A_DRONE = "Select a drone in the status panel first.";

  private final List<Runnable> listeners = new ArrayList<>();
  private String message = "";
  private boolean ok = true;

  /** Creates empty feedback. */
  public Feedback() {
    // nothing to say yet
  }

  /**
   * Shows a message.
   *
   * @param text the message
   * @param success true if the action worked, false for an error
   */
  public void show(String text, boolean success) {
    this.message = Objects.requireNonNull(text, "text");
    this.ok = success;
    for (Runnable listener : List.copyOf(listeners)) {
      listener.run();
    }
  }

  /**
   * Returns the message.
   *
   * @return the message, empty before the first one
   */
  public String message() {
    return message;
  }

  /**
   * Tells whether the message reports an error.
   *
   * @return true for an error
   */
  public boolean isError() {
    return !ok;
  }

  /**
   * Adds a listener told about every new message.
   *
   * @param listener the listener
   */
  public void onChange(Runnable listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }
}
