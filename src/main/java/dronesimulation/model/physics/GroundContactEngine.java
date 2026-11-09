package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adds a flat ground plane at {@code z = 0} to another {@link PhysicsEngine} (Decorator).
 *
 * <p>After the wrapped engine moves the bodies, any body that ended up at or below the ground
 * is put back on it, loses its downward speed, and is slowed by sliding friction. A body that
 * was in the air before the step counts as a {@link GroundImpact}; {@link SolidBody solid
 * bodies} are told about it through {@link SolidBody#onGroundImpact(double)} once every body
 * has been moved, so all bodies in a step see the same world.
 *
 * <p>The engine also keeps the simulation clock, since every step goes through it.
 */
public final class GroundContactEngine implements PhysicsEngine {

  private final PhysicsEngine inner;
  private double time;
  private List<GroundImpact> lastImpacts = List.of();

  /**
   * Wraps an engine with a ground plane.
   *
   * @param inner the engine that applies forces and moves the bodies
   */
  public GroundContactEngine(PhysicsEngine inner) {
    this.inner = Objects.requireNonNull(inner, "inner engine must not be null");
  }

  @Override
  public void addBody(PhysicsBody body) {
    inner.addBody(body);
  }

  @Override
  public boolean removeBody(PhysicsBody body) {
    return inner.removeBody(body);
  }

  @Override
  public List<PhysicsBody> getBodies() {
    return inner.getBodies();
  }

  /**
   * Returns the simulated time, the sum of all step sizes so far.
   *
   * @return the time in seconds
   */
  public double getTime() {
    return time;
  }

  /**
   * Returns the ground impacts of the most recent step.
   *
   * @return the impacts, in body order; empty before the first step
   */
  public List<GroundImpact> getLastImpacts() {
    return lastImpacts;
  }

  @Override
  public void step(double dt) {
    List<PhysicsBody> bodies = List.copyOf(inner.getBodies());
    boolean[] airborne = new boolean[bodies.size()];
    double[] verticalSpeed = new double[bodies.size()];
    for (int i = 0; i < bodies.size(); i++) {
      airborne[i] = bodies.get(i).getPosition().z() > 0;
      verticalSpeed[i] = bodies.get(i).getVelocity().z();
    }
    inner.step(dt);
    List<GroundImpact> impacts = new ArrayList<>();
    for (int i = 0; i < bodies.size(); i++) {
      PhysicsBody body = bodies.get(i);
      if (body.getPosition().z() > 0) {
        continue;
      }
      double vz = body.getVelocity().z();
      // The net vertical force this step, recovered from the change in vertical speed.
      double verticalForce = body.getMass() * (vz - verticalSpeed[i]) / dt;
      resolveContact(body, Math.max(0, -verticalForce), dt);
      if (airborne[i]) {
        impacts.add(new GroundImpact(body, Math.max(0, -vz)));
      }
    }
    time += dt;
    lastImpacts = List.copyOf(impacts);
    for (GroundImpact impact : lastImpacts) {
      if (impact.body() instanceof SolidBody solid) {
        solid.onGroundImpact(impact.impactSpeed());
      }
    }
  }

  private static void resolveContact(PhysicsBody body, double normalForce, double dt) {
    Vector3D p = body.getPosition();
    Vector3D v = body.getVelocity();
    body.setPosition(new Vector3D(p.x(), p.y(), 0));
    Vector3D horizontal = new Vector3D(v.x(), v.y(), 0);
    double speed = horizontal.magnitude();
    double friction = body instanceof SolidBody solid
        ? solid.getGroundFriction() : SolidBody.DEFAULT_GROUND_FRICTION;
    double slowdown = friction * normalForce / body.getMass() * dt;
    if (speed <= slowdown) {
      body.setVelocity(Vector3D.ZERO);
    } else {
      body.setVelocity(horizontal.scale((speed - slowdown) / speed));
    }
  }
}
