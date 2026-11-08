package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;

/**
 * Uniform gravity pulling straight down: {@code F = (0, 0, -m * g)}.
 */
public final class Gravity implements Force {

  private final double acceleration;

  /** Creates Earth gravity with {@code g = } {@value PhysicsConstants#GRAVITY} m/s². */
  public Gravity() {
    this(PhysicsConstants.GRAVITY);
  }

  /**
   * Creates gravity with a custom acceleration.
   *
   * @param acceleration the gravitational acceleration in m/s², must be finite and not negative
   * @throws IllegalArgumentException if the acceleration is negative or not finite
   */
  public Gravity(double acceleration) {
    if (!Double.isFinite(acceleration) || acceleration < 0) {
      throw new IllegalArgumentException(
          "Gravitational acceleration must be finite and not negative but was " + acceleration);
    }
    this.acceleration = acceleration;
  }

  /**
   * Returns the gravitational acceleration.
   *
   * @return {@code g} in m/s²
   */
  public double getAcceleration() {
    return acceleration;
  }

  @Override
  public Vector3D compute(PhysicsBody body) {
    return new Vector3D(0, 0, -body.getMass() * acceleration);
  }
}
