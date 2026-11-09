package dronesimulation.model.physics;

/**
 * A {@link PhysicsBody} with a name and a physical size, so it can feel air drag, touch the
 * ground and collide with other bodies.
 *
 * <p>Plain bodies such as {@link PointMass} only have mass. Drones and other objects that
 * live in the simulated world implement this interface to give the environment forces and
 * the {@link GroundContactEngine} the extra numbers they need.
 */
public interface SolidBody extends PhysicsBody {

  /** Radius used for collisions when a body does not give its own, in metres. */
  double DEFAULT_RADIUS = 0.25;

  /** Drag coefficient times frontal area used when a body does not give its own, in m². */
  double DEFAULT_DRAG_AREA = 0.03;

  /** Sliding friction coefficient against the ground used by default. */
  double DEFAULT_GROUND_FRICTION = 0.8;

  /**
   * Returns the unique name of this body.
   *
   * @return the body id, never blank
   */
  String getId();

  /**
   * Returns the radius of the sphere used for collisions.
   *
   * @return the radius in metres, positive
   */
  default double getRadius() {
    return DEFAULT_RADIUS;
  }

  /**
   * Returns the drag coefficient times the frontal area (Cd·A).
   *
   * @return the drag area in square metres, not negative
   */
  default double getDragArea() {
    return DEFAULT_DRAG_AREA;
  }

  /**
   * Returns how strongly the ground slows this body down when it slides.
   *
   * @return the friction coefficient, not negative
   */
  default double getGroundFriction() {
    return DEFAULT_GROUND_FRICTION;
  }

  /**
   * Called once when the body reaches the ground after being in the air.
   *
   * @param impactSpeed the downward speed at the moment of contact, in m/s
   */
  default void onGroundImpact(double impactSpeed) {
    // most bodies do not care
  }
}
