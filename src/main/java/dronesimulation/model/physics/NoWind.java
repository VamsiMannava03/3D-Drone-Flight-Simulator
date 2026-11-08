package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;

/**
 * Still air everywhere (Null Object pattern).
 *
 * <p>Lets code that needs a {@link WindField} work without null checks when there is no wind.
 */
public final class NoWind implements WindField {

  /** The single shared instance. */
  public static final NoWind INSTANCE = new NoWind();

  private NoWind() {
    // use INSTANCE
  }

  /**
   * Returns zero air velocity.
   *
   * @param position the position in metres (ignored)
   * @return {@link Vector3D#ZERO}
   */
  @Override
  public Vector3D airVelocityAt(Vector3D position) {
    return Vector3D.ZERO;
  }
}
