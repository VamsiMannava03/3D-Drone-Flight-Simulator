package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;

/**
 * A position sensor, like GPS plus an altimeter. Controllers steer from what the sensor reads,
 * not from the true position.
 *
 * <p>Sensor effects are added with decorators (Decorator pattern): start from {@link #perfect()}
 * and wrap it, for example {@code new NoisySensor(new BiasedSensor(Sensor.perfect(), offset), 0.02,
 * random)} for a GPS that is both offset and jittery.
 */
@FunctionalInterface
public interface Sensor {

  /**
   * Reads a position.
   *
   * @param truePosition the actual position
   * @return the measured position
   */
  Vector3D read(Vector3D truePosition);

  /**
   * Returns a sensor that always reads the exact position.
   *
   * @return a perfect sensor
   */
  static Sensor perfect() {
    return truePosition -> truePosition;
  }
}
