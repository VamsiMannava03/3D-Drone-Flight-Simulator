package dronesimulation.model;

/**
 * An immutable vector in three-dimensional space.
 *
 * <p>Used for positions, velocities, accelerations, forces and wind. Every operation returns a
 * new vector and leaves the original unchanged. The class makes no assumption about which axis
 * points up. Components must be finite, so a {@code NaN} or infinite value fails where it is
 * created instead of spreading through the physics.
 *
 * @param x the x component
 * @param y the y component
 * @param z the z component
 */
public record Vector3D(double x, double y, double z) {

  /** The zero vector, {@code (0, 0, 0)}. */
  public static final Vector3D ZERO = new Vector3D(0, 0, 0);

  /**
   * The unit vector pointing up, {@code (0, 0, 1)}.
   *
   * <p>The simulation uses x east, y north and z up, with the ground at {@code z = 0}.
   */
  public static final Vector3D UP = new Vector3D(0, 0, 1);

  /**
   * Creates a vector from its components.
   *
   * @throws IllegalArgumentException if any component is {@code NaN} or infinite
   */
  public Vector3D {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
      throw new IllegalArgumentException(
          "Components must be finite numbers but were (" + x + ", " + y + ", " + z + ")");
    }
  }

  /**
   * Returns the sum of this vector and another.
   *
   * @param other the vector to add
   * @return a new vector {@code this + other}
   */
  public Vector3D add(Vector3D other) {
    return new Vector3D(x + other.x, y + other.y, z + other.z);
  }

  /**
   * Returns the difference between this vector and another.
   *
   * @param other the vector to subtract
   * @return a new vector {@code this - other}
   */
  public Vector3D subtract(Vector3D other) {
    return new Vector3D(x - other.x, y - other.y, z - other.z);
  }

  /**
   * Returns this vector multiplied by a scalar.
   *
   * @param factor the scalar to multiply by
   * @return a new vector {@code this * factor}
   */
  public Vector3D scale(double factor) {
    return new Vector3D(x * factor, y * factor, z * factor);
  }

  /**
   * Returns the dot product of this vector and another.
   *
   * @param other the other vector
   * @return {@code this . other}
   */
  public double dot(Vector3D other) {
    return x * other.x + y * other.y + z * other.z;
  }

  /**
   * Returns the cross product of this vector and another, using the right-hand rule.
   *
   * @param other the other vector
   * @return a new vector {@code this x other}, perpendicular to both
   */
  public Vector3D cross(Vector3D other) {
    return new Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x);
  }

  /**
   * Returns the length of this vector.
   *
   * @return the Euclidean length, never negative
   */
  public double magnitude() {
    return Math.sqrt(dot(this));
  }

  /**
   * Returns the unit vector pointing in the same direction as this one.
   *
   * @return a new vector of length 1
   * @throws IllegalStateException if this is the zero vector, which has no direction
   */
  public Vector3D normalize() {
    double length = magnitude();
    if (length == 0) {
      throw new IllegalStateException("Cannot normalize the zero vector");
    }
    return scale(1 / length);
  }
}
