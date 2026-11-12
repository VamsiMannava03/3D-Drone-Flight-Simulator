package dronesimulation.model.drones;

/**
 * Performance limits of a drone model.
 *
 * @param maxThrust the strongest thrust the motors can give, in N
 * @param cruiseSpeed the normal travel speed, in m/s
 * @param safeLandingSpeed the fastest touchdown that still counts as a landing, in m/s
 * @param chargePower how fast the battery charges on the ground, in W
 * @param idlePower power used by electronics while the motors run, in W
 * @param maxPower power drawn by the motors at full (100%) throttle, in W
 */
public record DroneSpec(double maxThrust, double cruiseSpeed, double safeLandingSpeed,
    double chargePower, double idlePower, double maxPower) {

  /** Fastest safe touchdown required by the SRS, in m/s. */
  public static final double DEFAULT_SAFE_LANDING_SPEED = 3.0;

  /**
   * Creates a spec.
   *
   * @throws IllegalArgumentException if any value is not positive (idlePower may be 0)
   */
  public DroneSpec {
    requirePositive("maxThrust", maxThrust);
    requirePositive("cruiseSpeed", cruiseSpeed);
    requirePositive("safeLandingSpeed", safeLandingSpeed);
    requirePositive("chargePower", chargePower);
    requirePositive("maxPower", maxPower);
    if (!(idlePower >= 0) || !Double.isFinite(idlePower)) {
      throw new IllegalArgumentException("idlePower must be >= 0, got " + idlePower);
    }
  }

  private static void requirePositive(String name, double value) {
    if (!(value > 0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be positive, got " + value);
    }
  }
}
