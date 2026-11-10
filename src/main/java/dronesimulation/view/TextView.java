package dronesimulation.view;

import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.WorldSnapshot;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Prints the state of every drone: id, type, state, position, speed, battery and current
 * command. Prints at most once per {@code interval} of simulated time, so the terminal stays
 * readable.
 */
public class TextView implements SimulationView {

  private final PrintStream out;
  private final double interval;
  private double lastPrinted = Double.NEGATIVE_INFINITY;

  /** Creates a view that prints to standard output every simulated second. */
  public TextView() {
    this(System.out, 1.0);
  }

  /**
   * Creates a view.
   *
   * @param out where to print
   * @param interval simulated seconds between printouts, at least 0
   * @throws IllegalArgumentException if interval is negative
   */
  public TextView(PrintStream out, double interval) {
    this.out = Objects.requireNonNull(out, "out");
    if (!(interval >= 0)) {
      throw new IllegalArgumentException("interval must be >= 0, got " + interval);
    }
    this.interval = interval;
  }

  @Override
  public synchronized void render(WorldSnapshot snapshot) {
    if (snapshot.time() < lastPrinted) {
      lastPrinted = Double.NEGATIVE_INFINITY; // the clock went back: a reset
    }
    if (snapshot.time() - lastPrinted < interval - 1e-9) {
      return;
    }
    lastPrinted = snapshot.time();
    out.printf(Locale.ROOT, "--- t = %.2f s%s ---%n", snapshot.time(),
        snapshot.paused() ? " (paused)" : "");
    for (DroneSnapshot d : snapshot.drones()) {
      out.printf(Locale.ROOT, "%-5s %-10s %-9s pos=(%.2f, %.2f, %.2f)  speed=%5.2f m/s  throttle=%3.0f%%  "
              + "battery=%3.0f%%  mass=%.2f kg  %s%n",
          d.id(), d.type(), d.state(), d.position().x(), d.position().y(), d.position().z(),
          d.velocity().magnitude(), d.throttle() * 100,
          d.batteryLevel() * 100, d.mass(), d.command().isEmpty() ? "" : "[" + d.command() + "]");
    }
  }

  @Override
  public synchronized void showMessage(String message) {
    out.println(message);
  }
}
