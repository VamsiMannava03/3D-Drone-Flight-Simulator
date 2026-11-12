package dronesimulation.view.scene;

import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.world.Building;
import java.util.ArrayList;
import java.util.List;

/**
 * Picks out the parts of a world's obstacles that the scenery is built around.
 */
public final class Scenery {

  private Scenery() {
    // static helpers only
  }

  /**
   * Returns the buildings among the obstacles, in order. Other obstacle shapes are drawn on
   * their own and do not shape streets or gardens.
   *
   * @param obstacles the obstacles
   * @return the buildings
   */
  public static List<Building> buildings(List<? extends Obstacle> obstacles) {
    List<Building> buildings = new ArrayList<>();
    for (Obstacle o : obstacles) {
      if (o instanceof Building b) {
        buildings.add(b);
      }
    }
    return buildings;
  }
}
