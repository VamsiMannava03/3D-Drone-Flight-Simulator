package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * Shifts every reading by a fixed offset, like a badly calibrated GPS. A drone using it flies to
 * the wrong place by exactly that offset.
 */
public class BiasedSensor extends SensorDecorator {

  private final Vector3D offset;

  /**
   * Wraps a sensor with a fixed offset.
   *
   * @param inner the sensor to offset
   * @param offset added to every reading, in m
   */
  public BiasedSensor(Sensor inner, Vector3D offset) {
    super(inner);
    this.offset = Objects.requireNonNull(offset, "offset");
  }

  @Override
  protected Vector3D change(Vector3D reading) {
    return reading.add(offset);
  }
}
