package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * Wind that blows with the same velocity everywhere and at every moment.
 */
public final class ConstantWind implements WindField {

  private final Vector3D velocity;

  /**
   * Creates a constant wind.
   *
   * @param velocity the air velocity in m/s
   */
  public ConstantWind(Vector3D velocity) {
    this.velocity = Objects.requireNonNull(velocity, "velocity must not be null");
  }

  /**
   * Returns the air velocity.
   *
   * @return the wind velocity in m/s
   */
  public Vector3D getVelocity() {
    return velocity;
  }

  @Override
  public Vector3D airVelocityAt(Vector3D position) {
    return velocity;
  }
}
