package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.ArrayList;
import java.util.List;

/**
 * Test helper: an {@link OwnPhysicsEngine} with a ground plane whose forces can be added one
 * by one, so model tests can set up a small world in a couple of lines.
 */
public final class TestWorld {

  private final List<Force> forces = new ArrayList<>();
  private final GroundContactEngine engine = new GroundContactEngine(
      new OwnPhysicsEngine(List.of(this::sumOfForces)));

  /**
   * Adds a force that acts on every body from the next step on.
   *
   * @param force the force
   */
  public void addForce(Force force) {
    forces.add(force);
  }

  /**
   * Adds a body.
   *
   * @param body the body
   */
  public void addBody(PhysicsBody body) {
    engine.addBody(body);
  }

  /**
   * Advances the world by one step.
   *
   * @param dt the step in seconds
   * @return the ground impacts of the step
   */
  public List<GroundImpact> step(double dt) {
    engine.step(dt);
    return engine.getLastImpacts();
  }

  /**
   * Returns the simulated time.
   *
   * @return the time in seconds
   */
  public double getTime() {
    return engine.getTime();
  }

  private Vector3D sumOfForces(PhysicsBody body) {
    Vector3D sum = Vector3D.ZERO;
    for (Force force : forces) {
      sum = sum.add(force.compute(body));
    }
    return sum;
  }
}
