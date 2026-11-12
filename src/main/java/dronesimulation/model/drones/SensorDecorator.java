package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * Base for sensor decorators: wraps another sensor and changes its readings. Each decorator adds
 * one effect, and they can be stacked in any order.
 */
public abstract class SensorDecorator implements Sensor {

  private final Sensor inner;

  /**
   * Wraps a sensor.
   *
   * @param inner the sensor whose readings this one changes
   */
  protected SensorDecorator(Sensor inner) {
    this.inner = Objects.requireNonNull(inner, "inner");
  }

  @Override
  public final Vector3D read(Vector3D truePosition) {
    return change(inner.read(truePosition));
  }

  /**
   * Applies this decorator's effect to the wrapped sensor's reading.
   *
   * @param reading what the wrapped sensor read
   * @return the changed reading
   */
  protected abstract Vector3D change(Vector3D reading);
}
