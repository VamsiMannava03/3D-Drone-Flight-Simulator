package dronesimulation.view;

import dronesimulation.model.engine.WorldSnapshot;
import java.util.List;

/**
 * Shows the simulation to the user. Text, Swing and 3D views all implement this, so new views
 * never need changes to the model. Views only read snapshots.
 */
public interface SimulationView {

  /** The simulation speeds views offer: the 2D speed box and the 3D view's + and - keys. */
  List<Double> SPEED_STEPS = List.of(0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0);

  /**
   * Shows the world as it is in the snapshot. May be called from the simulation thread.
   *
   * @param snapshot the state to show
   */
  void render(WorldSnapshot snapshot);

  /**
   * Shows a short status message, such as a landing or crash report. Does nothing by default.
   *
   * @param message the message to show
   */
  default void showMessage(String message) {
  }
}
