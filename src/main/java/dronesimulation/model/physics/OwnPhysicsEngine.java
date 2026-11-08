package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The team's own physics engine: point masses integrated with semi-implicit Euler.
 *
 * <p>Each step, for every active body in the order it was added:
 *
 * <ol>
 *   <li>sum the body's own forces and every global force, in the order they were given;
 *   <li>compute the acceleration {@code a = F / m};
 *   <li>update the velocity first, {@code v = v + a * dt};
 *   <li>then update the position with the <em>new</em> velocity, {@code x = x + v * dt}.
 * </ol>
 *
 * <p>Using the new velocity for the position is what makes this semi-implicit Euler instead of
 * plain Euler, which slowly gains energy and makes a hovering body drift. The fixed order of bodies
 * and forces keeps the results deterministic.
 */
public final class OwnPhysicsEngine implements PhysicsEngine {

  private final List<Force> globalForces;
  private final List<PhysicsBody> bodies = new ArrayList<>();

  /**
   * Creates an engine that applies the given forces to every body.
   *
   * @param globalForces forces applied to every body each step, such as gravity and drag; the list
   *     is copied, so later changes to it have no effect
   * @throws NullPointerException if the list or any force in it is {@code null}
   */
  public OwnPhysicsEngine(List<Force> globalForces) {
    this.globalForces = List.copyOf(globalForces);
  }

  @Override
  public void addBody(PhysicsBody body) {
    Objects.requireNonNull(body, "body must not be null");
    if (bodies.contains(body)) {
      throw new IllegalArgumentException("Body was already added to the engine");
    }
    bodies.add(body);
  }

  @Override
  public boolean removeBody(PhysicsBody body) {
    return bodies.remove(body);
  }

  @Override
  public List<PhysicsBody> getBodies() {
    return Collections.unmodifiableList(bodies);
  }

  /**
   * Returns the forces applied to every body.
   *
   * @return a read-only list of the global forces, in the order they are applied
   */
  public List<Force> getGlobalForces() {
    return globalForces;
  }

  @Override
  public void step(double dt) {
    if (!Double.isFinite(dt) || dt <= 0) {
      throw new IllegalArgumentException("Time step must be positive and finite but was " + dt);
    }
    for (PhysicsBody body : bodies) {
      if (!body.isActive()) {
        continue;
      }
      integrate(body, netForce(body), dt);
    }
  }

  /**
   * Returns the total force on a body: its own forces plus every global force.
   *
   * @param body the body
   * @return the net force in newtons
   */
  Vector3D netForce(PhysicsBody body) {
    Vector3D net = body.ownForces();
    for (Force force : globalForces) {
      net = net.add(force.compute(body));
    }
    return net;
  }

  private static void integrate(PhysicsBody body, Vector3D netForce, double dt) {
    Vector3D acceleration = netForce.scale(1.0 / body.getMass());
    Vector3D velocity = body.getVelocity().add(acceleration.scale(dt));
    Vector3D position = body.getPosition().add(velocity.scale(dt));
    body.setVelocity(velocity);
    body.setPosition(position);
  }
}
