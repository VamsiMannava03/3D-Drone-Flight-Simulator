package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * Quadratic air drag that uses the local air density and each body's own drag area.
 *
 * <p>The force is {@code -0.5 · ρ(z) · CdA · |v_rel| · v_rel}, where {@code v_rel} is the
 * body's velocity relative to the wind. Only {@link SolidBody solid bodies} have a drag area;
 * any other body feels no force from this class (use {@link QuadraticDrag} for those).
 */
public final class AtmosphericDrag implements Force {

  private final Atmosphere atmosphere;
  private final WindField wind;

  /**
   * Creates drag in still air.
   *
   * @param atmosphere gives the air density at each altitude
   */
  public AtmosphericDrag(Atmosphere atmosphere) {
    this(atmosphere, NoWind.INSTANCE);
  }

  /**
   * Creates drag relative to the given wind.
   *
   * @param atmosphere gives the air density at each altitude
   * @param wind gives the air velocity at each position
   */
  public AtmosphericDrag(Atmosphere atmosphere, WindField wind) {
    this.atmosphere = Objects.requireNonNull(atmosphere, "atmosphere must not be null");
    this.wind = Objects.requireNonNull(wind, "wind must not be null");
  }

  /**
   * Returns the atmosphere used for the air density.
   *
   * @return the atmosphere
   */
  public Atmosphere getAtmosphere() {
    return atmosphere;
  }

  /**
   * Returns the wind the drag is measured against.
   *
   * @return the wind field
   */
  public WindField getWind() {
    return wind;
  }

  @Override
  public Vector3D compute(PhysicsBody body) {
    if (!(body instanceof SolidBody solid)) {
      return Vector3D.ZERO;
    }
    Vector3D position = body.getPosition();
    Vector3D relative = body.getVelocity().subtract(wind.airVelocityAt(position));
    double k = 0.5 * atmosphere.densityAt(position.z()) * solid.getDragArea();
    return relative.scale(-k * relative.magnitude());
  }
}
