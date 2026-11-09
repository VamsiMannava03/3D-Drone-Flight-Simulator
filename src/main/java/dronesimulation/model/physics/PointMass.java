package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * A plain point mass with no forces of its own.
 *
 * <p>A basic {@link PhysicsBody} that validates its state. Use it for passive objects (a dropped
 * payload, test bodies), or extend it and override {@link #ownForces()} for bodies that push
 * themselves.
 */
public class PointMass implements PhysicsBody {

  private Vector3D position;
  private Vector3D velocity;
  private double mass;

  /**
   * Creates a point mass.
   *
   * @param mass the mass in kilograms, must be positive and finite
   * @param position the starting position in metres, not {@code null}
   * @param velocity the starting velocity in m/s, not {@code null}
   * @throws IllegalArgumentException if the mass is not positive and finite
   * @throws NullPointerException if the position or velocity is {@code null}
   */
  public PointMass(double mass, Vector3D position, Vector3D velocity) {
    setMass(mass);
    setPosition(position);
    setVelocity(velocity);
  }

  /**
   * Creates a point mass at rest.
   *
   * @param mass the mass in kilograms, must be positive and finite
   * @param position the starting position in metres, not {@code null}
   * @throws IllegalArgumentException if the mass is not positive and finite
   * @throws NullPointerException if the position is {@code null}
   */
  public PointMass(double mass, Vector3D position) {
    this(mass, position, Vector3D.ZERO);
  }

  @Override
  public Vector3D getPosition() {
    return position;
  }

  @Override
  public void setPosition(Vector3D position) {
    this.position = Objects.requireNonNull(position, "position must not be null");
  }

  @Override
  public Vector3D getVelocity() {
    return velocity;
  }

  @Override
  public void setVelocity(Vector3D velocity) {
    this.velocity = Objects.requireNonNull(velocity, "velocity must not be null");
  }

  @Override
  public double getMass() {
    return mass;
  }

  /**
   * Changes the mass, for example when a payload is attached or released.
   *
   * @param mass the new mass in kilograms, must be positive and finite
   * @throws IllegalArgumentException if the mass is not positive and finite
   */
  public void setMass(double mass) {
    if (!Double.isFinite(mass) || mass <= 0) {
      throw new IllegalArgumentException("Mass must be positive and finite but was " + mass);
    }
    this.mass = mass;
  }
}
