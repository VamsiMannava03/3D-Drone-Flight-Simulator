package dronesimulation.model.world;

import dronesimulation.model.Vector3D;
import dronesimulation.model.obstacles.BoxObstacle;
import dronesimulation.model.obstacles.Obstacle;
import java.util.Objects;

/**
 * A building standing on the ground: a {@link BoxObstacle} with a style, so views know how to
 * draw it.
 *
 * <p>A {@link Style#BLOCK} is a flat-roofed block or tower, a {@link Style#HOUSE} a house with
 * a pitched roof. Collisions always use the whole box. For a house that box reaches the roof's
 * ridge, so a drone is stopped a little above the sloping roof near the eaves instead of ever
 * passing through it.
 */
public final class Building extends Obstacle {

  /** What kind of building it is. */
  public enum Style {
    /** A flat-roofed block or tower. */
    BLOCK,
    /** A house with a pitched roof; the box reaches the ridge. */
    HOUSE
  }

  private final BoxObstacle box;
  private final Style style;

  /**
   * Creates a building from two corners.
   *
   * @param name a short name used in event messages, not blank
   * @param min the corner with the smallest x, y and z
   * @param max the corner with the largest x, y and z
   * @param style what kind of building it is
   * @throws IllegalArgumentException if the name is blank or min is not below max on every axis
   */
  public Building(String name, Vector3D min, Vector3D max, Style style) {
    super(name);
    this.style = Objects.requireNonNull(style, "style must not be null");
    this.box = new BoxObstacle(name, min, max);
  }

  /**
   * Creates a flat-roofed building from two corners.
   *
   * @param name a short name used in event messages, not blank
   * @param min the corner with the smallest x, y and z
   * @param max the corner with the largest x, y and z
   * @throws IllegalArgumentException if the name is blank or min is not below max on every axis
   */
  public Building(String name, Vector3D min, Vector3D max) {
    this(name, min, max, Style.BLOCK);
  }

  /**
   * Creates a flat-roofed block standing on the ground.
   *
   * @param name a short name, not blank
   * @param centreX the x of its centre in m
   * @param centreY the y of its centre in m
   * @param width its size along x in m
   * @param depth its size along y in m
   * @param height its height in m
   * @return the building
   */
  public static Building block(String name, double centreX, double centreY, double width,
      double depth, double height) {
    return onGround(name, centreX, centreY, width, depth, height, Style.BLOCK);
  }

  /**
   * Creates a house with a pitched roof standing on the ground.
   *
   * @param name a short name, not blank
   * @param centreX the x of its centre in m
   * @param centreY the y of its centre in m
   * @param width its size along x in m
   * @param depth its size along y in m
   * @param ridgeHeight the height of the top of the roof in m
   * @return the house
   */
  public static Building house(String name, double centreX, double centreY, double width,
      double depth, double ridgeHeight) {
    return onGround(name, centreX, centreY, width, depth, ridgeHeight, Style.HOUSE);
  }

  private static Building onGround(String name, double centreX, double centreY, double width,
      double depth, double height, Style style) {
    return new Building(name,
        new Vector3D(centreX - width / 2, centreY - depth / 2, 0),
        new Vector3D(centreX + width / 2, centreY + depth / 2, height), style);
  }

  /**
   * Returns the name, the same as {@link #getId()}.
   *
   * @return the name
   */
  public String name() {
    return getId();
  }

  /**
   * Returns the corner with the smallest x, y and z.
   *
   * @return the minimum corner
   */
  public Vector3D min() {
    return box.getMin();
  }

  /**
   * Returns the corner with the largest x, y and z.
   *
   * @return the maximum corner
   */
  public Vector3D max() {
    return box.getMax();
  }

  /**
   * Returns what kind of building it is.
   *
   * @return the style
   */
  public Style style() {
    return style;
  }

  /**
   * Returns the height of the highest point.
   *
   * @return the top in m
   */
  public double top() {
    return box.getMax().z();
  }

  /**
   * Returns how tall a house's pitched roof is; 0 for a flat-roofed block.
   *
   * @return the roof height in m
   */
  public double roofHeight() {
    if (style != Style.HOUSE) {
      return 0;
    }
    Vector3D min = box.getMin();
    Vector3D max = box.getMax();
    double shorter = Math.min(max.x() - min.x(), max.y() - min.y());
    return Math.min(0.4 * shorter, 0.5 * (max.z() - min.z()));
  }

  /**
   * Returns the height where the walls end and the roof begins.
   *
   * @return the eaves height in m
   */
  public double eavesHeight() {
    return top() - roofHeight();
  }

  @Override
  public double distanceTo(Vector3D point) {
    return box.distanceTo(point);
  }

  /**
   * Two buildings are equal when they have the same name, corners and style, so snapshots of
   * the same scenario compare equal.
   *
   * @param other the object to compare with
   * @return true if it is an equal building
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof Building b && name().equals(b.name()) && min().equals(b.min())
        && max().equals(b.max()) && style == b.style;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), min(), max(), style);
  }

  @Override
  public String toString() {
    return "Building[" + name() + ", " + style + ", " + min() + " to " + max() + "]";
  }
}
