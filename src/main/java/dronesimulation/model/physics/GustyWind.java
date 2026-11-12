package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * A steady base wind with random horizontal gusts that blend smoothly from one to the next.
 *
 * <p>A new gust is drawn every {@code gustInterval} seconds from a seeded {@link Random}, so
 * the same seed always gives the same wind. Gusts are drawn in time order and remembered, so
 * asking for the wind out of order still gives the same answers. The current time comes from
 * a clock, normally the simulation time.
 */
public final class GustyWind implements WindField {

  private final Vector3D base;
  private final double gustStrength;
  private final double gustInterval;
  private final Random random;
  private final DoubleSupplier clock;
  private final List<Vector3D> gusts = new ArrayList<>();

  /**
   * Creates a gusty wind.
   *
   * @param base the steady wind velocity in m/s
   * @param gustStrength the largest gust speed added to the base, in m/s, not negative
   * @param gustInterval seconds between two gusts, positive
   * @param random the seeded source of the gusts
   * @param clock gives the current time in seconds
   * @throws IllegalArgumentException if the strength is negative or the interval not positive
   */
  public GustyWind(Vector3D base, double gustStrength, double gustInterval, Random random,
      DoubleSupplier clock) {
    this.base = Objects.requireNonNull(base, "base must not be null");
    if (!Double.isFinite(gustStrength) || gustStrength < 0) {
      throw new IllegalArgumentException(
          "Gust strength must be finite and not negative but was " + gustStrength);
    }
    if (!Double.isFinite(gustInterval) || gustInterval <= 0) {
      throw new IllegalArgumentException(
          "Gust interval must be positive and finite but was " + gustInterval);
    }
    this.gustStrength = gustStrength;
    this.gustInterval = gustInterval;
    this.random = Objects.requireNonNull(random, "random must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  /**
   * Returns the steady part of the wind.
   *
   * @return the base velocity in m/s
   */
  public Vector3D getBase() {
    return base;
  }

  /**
   * Returns the largest gust speed.
   *
   * @return the gust strength in m/s
   */
  public double getGustStrength() {
    return gustStrength;
  }

  @Override
  public Vector3D airVelocityAt(Vector3D position) {
    return velocityAt(clock.getAsDouble());
  }

  /**
   * Returns the wind at the given time.
   *
   * @param time the time in seconds; negative times count as 0
   * @return the air velocity in m/s
   */
  public Vector3D velocityAt(double time) {
    double t = Math.max(0, time) / gustInterval;
    int index = (int) Math.floor(t);
    double blend = t - index;
    double smooth = blend * blend * (3 - 2 * blend);
    Vector3D from = gust(index);
    Vector3D to = gust(index + 1);
    return base.add(from.scale(1 - smooth)).add(to.scale(smooth));
  }

  private Vector3D gust(int index) {
    while (gusts.size() <= index) {
      double angle = random.nextDouble() * 2 * Math.PI;
      double strength = random.nextDouble() * gustStrength;
      gusts.add(new Vector3D(Math.cos(angle) * strength, Math.sin(angle) * strength, 0));
    }
    return gusts.get(index);
  }
}
