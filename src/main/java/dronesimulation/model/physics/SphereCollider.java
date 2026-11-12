package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Optional;

/**
 * Treats every body as a sphere of its {@link SolidBody#getRadius() radius}. Spheres that
 * only touch do not collide.
 */
public final class SphereCollider implements Collider {

  /** Creates a sphere collider. */
  public SphereCollider() {
    // no state
  }

  @Override
  public Optional<Collision> check(SolidBody a, SolidBody b) {
    if (a == b) {
      return Optional.empty();
    }
    double reach = a.getRadius() + b.getRadius();
    Vector3D gap = a.getPosition().subtract(b.getPosition());
    double distanceSquared = gap.dot(gap);
    if (distanceSquared >= reach * reach) {
      return Optional.empty();
    }
    return Optional.of(new Collision(a, b, reach - Math.sqrt(distanceSquared)));
  }
}
