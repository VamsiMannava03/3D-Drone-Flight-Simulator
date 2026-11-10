package dronesimulation.model.world;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * A cylinder of airspace drones must stay out of, from the ground up to a ceiling.
 *
 * @param name a short name used in event messages
 * @param centreX the x of the centre, in m
 * @param centreY the y of the centre, in m
 * @param radius the radius, in m
 * @param ceiling the top of the zone, in m
 */
public record NoFlyZone(String name, double centreX, double centreY, double radius,
    double ceiling) {

  /**
   * Creates a zone.
   *
   * @throws IllegalArgumentException if the name is blank, or radius or ceiling is not positive
   */
  public NoFlyZone {
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("zone name must not be blank");
    }
    if (!(radius > 0) || !(ceiling > 0)) {
      throw new IllegalArgumentException("radius and ceiling must be positive");
    }
  }

  /**
   * Tells whether a point is inside the zone.
   *
   * @param p the point
   * @return true if inside
   */
  public boolean contains(Vector3D p) {
    return Math.hypot(p.x() - centreX, p.y() - centreY) < radius && p.z() < ceiling;
  }
}
