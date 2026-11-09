package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * Quadratic air drag: {@code F = -k * |v_rel| * v_rel}, where {@code v_rel = v - w}.
 *
 * <p>Drag uses the body's velocity relative to the air, {@code w} being the wind's air velocity.
 * That is how wind affects a body: a drone hovering in a 5 m/s wind feels exactly the same drag as
 * a drone flying at 5 m/s in still air. A falling body reaches the terminal speed {@code sqrt(m * g
 * / k)}.
 */
public final class QuadraticDrag implements Force {

  private final double coefficient;
  private final WindField wind;

  /**
   * Creates drag in still air.
   *
   * @param coefficient the drag coefficient {@code k} in kg/m, must be finite and not negative
   * @throws IllegalArgumentException if the coefficient is negative or not finite
   */
  public QuadraticDrag(double coefficient) {
    this(coefficient, NoWind.INSTANCE);
  }

  /**
   * Creates drag in the given wind.
   *
   * @param coefficient the drag coefficient {@code k} in kg/m, must be finite and not negative
   * @param wind the wind that supplies the air velocity, not {@code null}
   * @throws IllegalArgumentException if the coefficient is negative or not finite
   * @throws NullPointerException if the wind is {@code null}
   */
  public QuadraticDrag(double coefficient, WindField wind) {
    if (!Double.isFinite(coefficient) || coefficient < 0) {
      throw new IllegalArgumentException(
          "Drag coefficient must be finite and not negative but was " + coefficient);
    }
    this.coefficient = coefficient;
    this.wind = Objects.requireNonNull(wind, "wind must not be null");
  }

  /**
   * Returns the drag coefficient.
   *
   * @return {@code k} in kg/m
   */
  public double getCoefficient() {
    return coefficient;
  }

  /**
   * Returns the wind this drag uses.
   *
   * @return the wind field
   */
  public WindField getWind() {
    return wind;
  }

  @Override
  public Vector3D compute(PhysicsBody body) {
    Vector3D relative = body.getVelocity().subtract(wind.airVelocityAt(body.getPosition()));
    return relative.scale(-coefficient * relative.magnitude());
  }
}
