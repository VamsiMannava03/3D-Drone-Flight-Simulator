package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * Builds wind fields from the numbers a user types: a speed, the compass direction the wind
 * blows from, and a gust strength.
 */
public final class Winds {

  /** Seconds between two gusts. */
  public static final double GUST_INTERVAL = 3;

  private Winds() {
    // factory methods only
  }

  /**
   * Turns a speed and a compass direction into a velocity. The direction is where the wind
   * comes from, like a weather report: 0 is north (+y), 90 is east (+x), 270 is west.
   *
   * @param speed the wind speed in m/s
   * @param fromDegrees the compass direction the wind blows from, in degrees
   * @return the air velocity in m/s
   */
  public static Vector3D velocity(double speed, double fromDegrees) {
    double r = Math.toRadians(fromDegrees);
    return new Vector3D(-speed * Math.sin(r), -speed * Math.cos(r), 0);
  }

  /**
   * Creates a wind. Without gusts it is a {@link ConstantWind}; with gusts it is a
   * {@link GustyWind} seeded from the three numbers, so the same settings always give the
   * same gusts.
   *
   * @param speed the base wind speed in m/s, not negative
   * @param fromDegrees the compass direction the wind blows from, in degrees
   * @param gustStrength the largest gust speed in m/s, not negative
   * @param clock gives the simulation time in seconds, used for gusts
   * @return the wind field
   * @throws IllegalArgumentException if the speed or gust strength is negative
   */
  public static WindField from(double speed, double fromDegrees, double gustStrength,
      DoubleSupplier clock) {
    if (!(speed >= 0) || !(gustStrength >= 0)) {
      throw new IllegalArgumentException(
          "Wind speed and gust strength must not be negative but were " + speed + " and "
              + gustStrength);
    }
    Vector3D base = velocity(speed, fromDegrees);
    if (gustStrength == 0) {
      return new ConstantWind(base);
    }
    long seed = Double.hashCode(speed) * 31L + Double.hashCode(fromDegrees) * 17L
        + Double.hashCode(gustStrength);
    return new GustyWind(base, gustStrength, GUST_INTERVAL, new Random(seed), clock);
  }
}
