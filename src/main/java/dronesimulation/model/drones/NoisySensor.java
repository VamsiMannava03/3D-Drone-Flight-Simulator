package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import java.util.Objects;
import java.util.Random;

/**
 * Adds Gaussian jitter to every reading, like GPS noise. The noise comes only from the injected
 * {@link Random}, so runs stay repeatable.
 */
public class NoisySensor extends SensorDecorator {

  private final double noise;
  private final Random random;

  /**
   * Wraps a sensor with noise.
   *
   * @param inner the sensor to add noise to
   * @param noise the standard deviation of each axis, in m, at least 0
   * @param random the source of noise; pass a seeded Random for repeatable runs
   * @throws IllegalArgumentException if noise is negative
   */
  public NoisySensor(Sensor inner, double noise, Random random) {
    super(inner);
    if (!(noise >= 0) || !Double.isFinite(noise)) {
      throw new IllegalArgumentException("noise must be >= 0, got " + noise);
    }
    this.noise = noise;
    this.random = Objects.requireNonNull(random, "random");
  }

  /**
   * Returns the noise level.
   *
   * @return the standard deviation in m
   */
  public double getNoise() {
    return noise;
  }

  @Override
  protected Vector3D change(Vector3D reading) {
    if (noise == 0) {
      return reading;
    }
    return reading.add(new Vector3D(random.nextGaussian() * noise,
        random.nextGaussian() * noise, random.nextGaussian() * noise));
  }
}
