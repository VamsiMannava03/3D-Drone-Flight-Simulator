package dronesimulation.view.panel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gives every drone its own colour, the same in every view: the first drone seen gets the
 * first colour, and so on.
 */
public final class DronePalette {

  /** The colours, as 0xRRGGBB. */
  public static final List<Integer> COLOURS = List.of(0x3B82F6, 0xF97316, 0x22C55E, 0xA855F7,
      0xEC4899, 0x06B6D4);

  private final Map<String, Integer> assigned = new HashMap<>();

  /** Creates a palette with no colours given out. */
  public DronePalette() {
    // nothing assigned yet
  }

  /**
   * Returns a drone's colour, giving it the next one the first time.
   *
   * @param droneId the drone
   * @return its colour as 0xRRGGBB
   */
  public int colourOf(String droneId) {
    return assigned.computeIfAbsent(droneId, id -> COLOURS.get(assigned.size() % COLOURS.size()));
  }

  /** Forgets every drone, for a reset. */
  public void clear() {
    assigned.clear();
  }
}
