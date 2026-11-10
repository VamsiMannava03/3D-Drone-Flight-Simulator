package dronesimulation.view;

/**
 * A rectangle in world coordinates that a 2D view shows. The horizontal axis is h and the vertical
 * axis is v, so the same class serves the top view (x, y) and the side view (x, z).
 *
 * @param minH the left edge
 * @param maxH the right edge
 * @param minV the bottom edge
 * @param maxV the top edge
 */
public record Bounds2D(double minH, double maxH, double minV, double maxV) {

  /**
   * Creates bounds.
   *
   * @throws IllegalArgumentException if a minimum is greater than its maximum
   */
  public Bounds2D {
    if (minH > maxH || minV > maxV) {
      throw new IllegalArgumentException("min must not exceed max: " + minH + ".." + maxH
          + ", " + minV + ".." + maxV);
    }
  }

  /**
   * Returns bounds that cover a single point.
   *
   * @param h the horizontal coordinate
   * @param v the vertical coordinate
   * @return zero-size bounds at that point
   */
  public static Bounds2D around(double h, double v) {
    return new Bounds2D(h, h, v, v);
  }

  /**
   * Returns the smallest bounds that cover these bounds and a point.
   *
   * @param h the horizontal coordinate
   * @param v the vertical coordinate
   * @return the grown bounds
   */
  public Bounds2D include(double h, double v) {
    return new Bounds2D(Math.min(minH, h), Math.max(maxH, h),
        Math.min(minV, v), Math.max(maxV, v));
  }

  /**
   * Returns these bounds with a margin, and widened about their centre so each axis spans at
   * least {@code minSpan}. Keeps tiny or zero-size bounds readable.
   *
   * @param minSpan the smallest span of either axis, in m
   * @param padFraction the margin added on each side, as a fraction of the span
   * @return the padded bounds
   */
  public Bounds2D padded(double minSpan, double padFraction) {
    double spanH = Math.max(maxH - minH, minSpan);
    double spanV = Math.max(maxV - minV, minSpan);
    double midH = (minH + maxH) / 2;
    double midV = (minV + maxV) / 2;
    double halfH = spanH * (0.5 + padFraction);
    double halfV = spanV * (0.5 + padFraction);
    return new Bounds2D(midH - halfH, midH + halfH, midV - halfV, midV + halfV);
  }

  /**
   * Returns these bounds with the bottom edge raised to {@code floor} if it is below it, keeping
   * the span. Used by the side view so it never shows much below the ground.
   *
   * @param floor the lowest bottom edge allowed
   * @return the shifted bounds
   */
  public Bounds2D withFloor(double floor) {
    if (minV >= floor) {
      return this;
    }
    double shift = floor - minV;
    return new Bounds2D(minH, maxH, floor, maxV + shift);
  }

  /**
   * Picks a grid spacing of 1, 2 or 5 times a power of ten that gives about {@code targetLines}
   * lines across {@code span}.
   *
   * @param span the distance to cover, must be positive
   * @param targetLines about how many grid lines to draw, must be positive
   * @return the grid spacing
   * @throws IllegalArgumentException if span or targetLines is not positive
   */
  public static double niceStep(double span, int targetLines) {
    if (!(span > 0) || targetLines <= 0) {
      throw new IllegalArgumentException("span and targetLines must be positive");
    }
    double raw = span / targetLines;
    double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
    double fraction = raw / magnitude;
    double nice = fraction < 1.5 ? 1 : fraction < 3.5 ? 2 : fraction < 7.5 ? 5 : 10;
    return nice * magnitude;
  }
}
