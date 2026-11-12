package dronesimulation.model.physics;

/**
 * Two bodies that overlap.
 *
 * @param first one of the bodies
 * @param second the other body
 * @param depth how far they overlap, in metres, positive
 */
public record Collision(SolidBody first, SolidBody second, double depth) {
}
