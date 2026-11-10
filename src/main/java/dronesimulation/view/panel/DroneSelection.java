package dronesimulation.view.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The drone currently picked in the window, shared by every panel (Observer). Selecting a
 * drone in the status panel shows its mission in the editor and its ring in the views.
 */
public final class DroneSelection {

  private final List<Consumer<String>> listeners = new ArrayList<>();
  private String selected;

  /** Creates a selection with no drone picked. */
  public DroneSelection() {
    // nothing selected yet
  }

  /**
   * Returns the selected drone.
   *
   * @return its id, or null if none is selected
   */
  public String get() {
    return selected;
  }

  /**
   * Selects a drone and tells the listeners, if it changed.
   *
   * @param id the drone id, or null to select none
   */
  public void set(String id) {
    if (Objects.equals(id, selected)) {
      return;
    }
    selected = id;
    for (Consumer<String> listener : List.copyOf(listeners)) {
      listener.accept(id);
    }
  }

  /**
   * Adds a listener told about every change.
   *
   * @param listener receives the newly selected id, or null
   */
  public void onChange(Consumer<String> listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }
}
