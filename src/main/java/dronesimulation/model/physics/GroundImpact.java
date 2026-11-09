package dronesimulation.model.physics;

/**
 * A body reaching the ground after being in the air.
 *
 * @param body the body that touched down
 * @param impactSpeed its downward speed at the moment of contact, in m/s, not negative
 */
public record GroundImpact(PhysicsBody body, double impactSpeed) {
}
