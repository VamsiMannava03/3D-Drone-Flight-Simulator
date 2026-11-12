package dronesimulation.model.physics;

import dronesimulation.model.Vector3D;
import java.util.Objects;

/**
 * A {@link PointMass} with an id, a collision radius and a drag area.
 *
 * <p>Useful for anything in the world that is not a drone, such as a dropped payload, and
 * for tests of the environment forces.
 */
public class SolidPointMass extends PointMass implements SolidBody {

  private final String id;
  private final double radius;
  private double dragArea = DEFAULT_DRAG_AREA;

  /**
   * Creates a solid point mass with the default radius.
   *
   * @param id unique name, not blank
   * @param mass mass in kilograms, positive
   * @param position starting position
   * @param velocity starting velocity
   * @throws IllegalArgumentException if the id is blank or the mass is not positive
   */
  public SolidPointMass(String id, double mass, Vector3D position, Vector3D velocity) {
    this(id, mass, DEFAULT_RADIUS, position, velocity);
  }

  /**
   * Creates a solid point mass.
   *
   * @param id unique name, not blank
   * @param mass mass in kilograms, positive
   * @param radius collision radius in metres, positive
   * @param position starting position
   * @param velocity starting velocity
   * @throws IllegalArgumentException if the id is blank or the mass or radius is not positive
   */
  public SolidPointMass(String id, double mass, double radius, Vector3D position,
      Vector3D velocity) {
    super(mass, position, velocity);
    Objects.requireNonNull(id, "id must not be null");
    if (id.isBlank()) {
      throw new IllegalArgumentException("Id must not be blank");
    }
    if (!Double.isFinite(radius) || radius <= 0) {
      throw new IllegalArgumentException("Radius must be positive and finite but was " + radius);
    }
    this.id = id;
    this.radius = radius;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public double getRadius() {
    return radius;
  }

  @Override
  public double getDragArea() {
    return dragArea;
  }

  /**
   * Sets the drag coefficient times the frontal area.
   *
   * @param dragArea the drag area in square metres, not negative
   * @throws IllegalArgumentException if the drag area is negative or not finite
   */
  public void setDragArea(double dragArea) {
    if (!Double.isFinite(dragArea) || dragArea < 0) {
      throw new IllegalArgumentException(
          "Drag area must be finite and not negative but was " + dragArea);
    }
    this.dragArea = dragArea;
  }
}
