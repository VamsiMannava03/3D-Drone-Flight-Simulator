package dronesimulation.view.panel;

import dronesimulation.model.Vector3D;
import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.WorldSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * What the drone status panel shows, without any widgets: the list of drones to pick from and
 * the live values of the selected one.
 */
public final class DroneStatusModel {

  /** The rows of the panel, in order. */
  public static final List<String> ROWS = List.of("Type", "State", "Altitude", "Speed",
      "Throttle", "Battery", "Mass", "Payload", "Step", "Queued", "Position", "Home");

  /** Below this battery level the battery shows as low. */
  public static final double LOW_LEVEL = 0.2;

  /** How full the battery looks. */
  public enum BatteryLook {
    /** No drone is selected. */
    NONE,
    /** Above {@link #LOW_LEVEL}. */
    OK,
    /** Below {@link #LOW_LEVEL}. */
    LOW,
    /** Empty. */
    EMPTY
  }

  private final DroneSelection selection;
  private final Map<String, String> values = new LinkedHashMap<>();
  private List<String> listed = List.of();
  private double throttle = -1;
  private double battery = -1;

  /**
   * Creates the status panel logic.
   *
   * @param selection the drone picked in the window
   */
  public DroneStatusModel(DroneSelection selection) {
    this.selection = Objects.requireNonNull(selection, "selection");
    clear();
  }

  /**
   * Reads the drone list and the selected drone's values. If the selected drone is gone, the
   * selection moves to the first drone, or to none.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    List<String> now = snapshot.drones().stream().map(DroneSnapshot::id).toList();
    listed = now;
    if (!now.contains(selection.get())) {
      selection.set(now.isEmpty() ? null : now.get(0));
    }
    String id = selection.get();
    DroneSnapshot drone = snapshot.drones().stream().filter(d -> d.id().equals(id))
        .findFirst().orElse(null);
    if (drone == null) {
      clear();
      return;
    }
    values.put("Type", drone.type());
    values.put("State", drone.state());
    values.put("Altitude", format("%.1f m", drone.position().z()));
    values.put("Speed", format("%.1f m/s", drone.velocity().magnitude()));
    values.put("Mass", format("%.2f kg", drone.mass()));
    values.put("Payload", drone.payload() > 0 ? format("%.2f kg", drone.payload()) : "none");
    values.put("Step", drone.command().isEmpty() ? "none" : drone.command());
    values.put("Queued", String.valueOf(drone.queuedCommands()));
    values.put("Position", point(drone.position()));
    values.put("Home", point(drone.home()));
    throttle = drone.throttle();
    battery = drone.batteryLevel();
    values.put("Throttle", Math.round(throttle * 100) + "%");
    values.put("Battery", Math.round(battery * 100) + "%");
  }

  /**
   * Picks a drone from the list.
   *
   * @param id the drone id
   */
  public void choose(String id) {
    selection.set(id);
  }

  /**
   * Returns the drones that can be picked.
   *
   * @return the ids, in world order
   */
  public List<String> listedDrones() {
    return listed;
  }

  /**
   * Returns the text of one row.
   *
   * @param row one of {@link #ROWS}
   * @return the value, or {@code -} with no drone selected
   */
  public String valueOf(String row) {
    String value = values.get(row);
    if (value == null) {
      throw new IllegalArgumentException("No row '" + row + "'");
    }
    return value;
  }

  /**
   * Returns the throttle as a fraction, for a bar.
   *
   * @return 0 to 1, or -1 with no drone selected
   */
  public double throttle() {
    return throttle;
  }

  /**
   * Returns the battery level as a fraction, for a bar.
   *
   * @return 0 to 1, or -1 with no drone selected
   */
  public double battery() {
    return battery;
  }

  /**
   * Tells how full the battery looks, for its colour.
   *
   * @return the look
   */
  public BatteryLook batteryLook() {
    if (battery < 0) {
      return BatteryLook.NONE;
    }
    return battery <= 0 ? BatteryLook.EMPTY : battery < LOW_LEVEL ? BatteryLook.LOW
        : BatteryLook.OK;
  }

  private void clear() {
    ROWS.forEach(row -> values.put(row, "-"));
    throttle = -1;
    battery = -1;
  }

  private static String point(Vector3D p) {
    return format("%.0f, %.0f, %.0f", p.x(), p.y(), p.z());
  }

  private static String format(String pattern, Object... args) {
    return String.format(Locale.ROOT, pattern, args);
  }
}
