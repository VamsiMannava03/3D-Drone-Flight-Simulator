package dronesimulation.model.physics;

/**
 * Gives the air density at a given altitude (Strategy).
 *
 * <p>The air gets thinner higher up, which lowers the drag a body feels there.
 */
@FunctionalInterface
public interface Atmosphere {

  /** Air density at sea level, in kg/m³. */
  double SEA_LEVEL_DENSITY = 1.225;

  /** Height over which the standard atmosphere's density falls by a factor of e, in metres. */
  double SCALE_HEIGHT = 8500;

  /**
   * Returns the air density at the given altitude.
   *
   * @param altitude height above the ground in metres
   * @return the density in kg/m³, positive
   */
  double densityAt(double altitude);

  /**
   * Returns an atmosphere whose density falls exponentially with altitude. Below the ground
   * it uses the sea-level density.
   *
   * @return the standard atmosphere
   */
  static Atmosphere standard() {
    return altitude -> SEA_LEVEL_DENSITY * Math.exp(-Math.max(0, altitude) / SCALE_HEIGHT);
  }

  /**
   * Returns an atmosphere with the same density everywhere.
   *
   * @param density the density in kg/m³, positive
   * @return the uniform atmosphere
   * @throws IllegalArgumentException if the density is not positive and finite
   */
  static Atmosphere uniform(double density) {
    if (!Double.isFinite(density) || density <= 0) {
      throw new IllegalArgumentException(
          "Density must be positive and finite but was " + density);
    }
    return altitude -> density;
  }
}
