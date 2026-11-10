package dronesimulation.view.scene;

import java.util.List;

/**
 * Turns lines into thin solid ribbons, since 3D scenes draw triangles, not lines. Each
 * segment becomes two crossed strips, so it can be seen from any side.
 */
public final class Lines {

  private Lines() {
    // static helpers only
  }

  /**
   * Adds one segment.
   *
   * @param out where the ribbon goes
   * @param a one end {x, y, z}
   * @param b the other end
   * @param width the ribbon width in m
   */
  public static void segment(MeshBuilder out, double[] a, double[] b, double width) {
    double[] dir = sub(b, a);
    double len = length(dir);
    if (len < 1e-9) {
      return;
    }
    dir = scale(dir, 1 / len);
    double[] side = cross(dir, new double[] {0, 0, 1});
    if (length(side) < 1e-6) {
      side = new double[] {1, 0, 0};
    }
    side = scale(side, 1 / length(side));
    double[] lift = cross(side, dir);
    strip(out, a, b, scale(side, width / 2), lift);
    strip(out, a, b, scale(lift, width / 2), side);
  }

  /**
   * Adds a line through points.
   *
   * @param out where the ribbons go
   * @param points the points {x, y, z}, in order
   * @param width the ribbon width in m
   */
  public static void polyline(MeshBuilder out, List<double[]> points, double width) {
    for (int i = 1; i < points.size(); i++) {
      segment(out, points.get(i - 1), points.get(i), width);
    }
  }

  /**
   * Adds a horizontal circle.
   *
   * @param out where the ribbons go
   * @param cx the centre x
   * @param cy the centre y
   * @param z the height
   * @param radius the radius
   * @param segments how many straight pieces
   * @param width the ribbon width in m
   */
  public static void ring(MeshBuilder out, double cx, double cy, double z, double radius,
      int segments, double width) {
    double[] prev = null;
    for (int i = 0; i <= segments; i++) {
      double a = 2 * Math.PI * i / segments;
      double[] p = {cx + radius * Math.cos(a), cy + radius * Math.sin(a), z};
      if (prev != null) {
        segment(out, prev, p, width);
      }
      prev = p;
    }
  }

  private static void strip(MeshBuilder out, double[] a, double[] b, double[] offset,
      double[] normal) {
    out.face(new double[][] {sub(a, offset), sub(b, offset), add(b, offset), add(a, offset)},
        normal);
  }

  private static double[] add(double[] a, double[] b) {
    return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
  }

  private static double[] sub(double[] a, double[] b) {
    return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
  }

  private static double[] scale(double[] a, double k) {
    return new double[] {a[0] * k, a[1] * k, a[2] * k};
  }

  private static double length(double[] a) {
    return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
  }

  private static double[] cross(double[] a, double[] b) {
    return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]};
  }
}
