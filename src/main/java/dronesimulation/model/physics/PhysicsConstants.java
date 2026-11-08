package dronesimulation.model.physics;

/**
 * Physical constants and conventions shared by the whole physics package.
 *
 * <p>All values are in SI units: metres, seconds, kilograms and newtons. Axes are x east, y north
 * and z up, and the ground is the plane {@code z = 0}.
 */
public final class PhysicsConstants {

  /** Gravitational acceleration near the Earth's surface, in m/s². */
  public static final double GRAVITY = 9.81;

  /** Fixed simulation time step, in seconds (100 steps per simulated second). */
  public static final double TIME_STEP = 0.01;

  private PhysicsConstants() {
    // constants only
  }
}
