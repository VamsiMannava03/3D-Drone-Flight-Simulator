package dronesimulation.view.scene;

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;

/**
 * Builds triangle meshes on the CPU from simple shapes. Each vertex is six floats: position
 * (x, y, z) then normal (nx, ny, nz). The JavaFX view turns the result into triangle meshes.
 */
public class MeshBuilder {

  private static final int FLOATS_PER_VERTEX = 6;
  /** For {@link #heightField}: grid spacing at the centre, as a share of the average spacing. */
  private static final double CENTRE_SPACING = 0.15;

  private float[] data = new float[1024];
  private int size;

  /** Creates an empty builder. */
  public MeshBuilder() {
  }

  /**
   * Adds a box.
   *
   * @param transform where to put it (rotation and translation; no scaling)
   * @param sx the size along x
   * @param sy the size along y
   * @param sz the size along z
   * @return this builder
   */
  public MeshBuilder box(float[] transform, double sx, double sy, double sz) {
    double[][] axes = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    double[] half = {sx / 2, sy / 2, sz / 2};
    for (int axis = 0; axis < 3; axis++) {
      for (int sign = -1; sign <= 1; sign += 2) {
        double[] n = scaled(axes[axis], sign);
        double[] u = axes[(axis + 1) % 3];
        double[] v = axes[(axis + 2) % 3];
        double hu = half[(axis + 1) % 3];
        double hv = half[(axis + 2) % 3];
        double hn = half[axis];
        double[][] corners = new double[4][];
        int[][] signs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int c = 0; c < 4; c++) {
          corners[c] = new double[3];
          for (int i = 0; i < 3; i++) {
            corners[c][i] = n[i] * hn + u[i] * hu * signs[c][0] + v[i] * hv * signs[c][1];
          }
        }
        quad(transform, corners, n);
      }
    }
    return this;
  }

  /**
   * Adds an upright cylinder around the z axis.
   *
   * @param transform where to put it (rotation and translation)
   * @param radius the radius
   * @param z0 the bottom height
   * @param z1 the top height
   * @param segments how many sides
   * @param caps whether to close the top and bottom
   * @return this builder
   */
  public MeshBuilder cylinder(float[] transform, double radius, double z0, double z1,
      int segments, boolean caps) {
    for (int i = 0; i < segments; i++) {
      double a0 = 2 * Math.PI * i / segments;
      double a1 = 2 * Math.PI * (i + 1) / segments;
      double c0 = Math.cos(a0);
      double s0 = Math.sin(a0);
      double c1 = Math.cos(a1);
      double s1 = Math.sin(a1);
      double[][] corners = {
          {c0 * radius, s0 * radius, z0}, {c1 * radius, s1 * radius, z0},
          {c1 * radius, s1 * radius, z1}, {c0 * radius, s0 * radius, z1}};
      double[] mid = {Math.cos((a0 + a1) / 2), Math.sin((a0 + a1) / 2), 0};
      quad(transform, corners, mid);
      if (caps) {
        triangle(transform, new double[] {0, 0, z1}, corners[3], corners[2],
            new double[] {0, 0, 1});
        triangle(transform, new double[] {0, 0, z0}, corners[1], corners[0],
            new double[] {0, 0, -1});
      }
    }
    return this;
  }

  /**
   * Adds an upright cone around the z axis, without a base: a tree crown, for example. With
   * {@code z1} below {@code z0} it points down.
   *
   * @param transform where to put it (rotation and translation)
   * @param radius the radius at {@code z0}
   * @param z0 the height of the wide end
   * @param z1 the height of the tip
   * @param segments how many sides
   * @return this builder
   */
  public MeshBuilder cone(float[] transform, double radius, double z0, double z1,
      int segments) {
    double rise = z1 - z0;
    double slant = Math.hypot(radius, rise);
    for (int i = 0; i < segments; i++) {
      double a0 = 2 * Math.PI * i / segments;
      double a1 = 2 * Math.PI * (i + 1) / segments;
      double mid = (a0 + a1) / 2;
      // Outward and toward the tip's side: perpendicular to the slanted face.
      double[] n = {Math.cos(mid) * Math.abs(rise) / slant, Math.sin(mid) * Math.abs(rise) / slant,
          Math.signum(rise) * radius / slant};
      triangle(transform, new double[] {Math.cos(a0) * radius, Math.sin(a0) * radius, z0},
          new double[] {Math.cos(a1) * radius, Math.sin(a1) * radius, z0},
          new double[] {0, 0, z1}, n);
    }
    return this;
  }

  /**
   * Adds the two sloping faces of a unit gable roof: ridge along x at z = 1, eaves along
   * y = -0.5 and y = +0.5 at z = 0, from x = -0.5 to +0.5. Scale it to fit a house.
   *
   * @return this builder
   */
  public MeshBuilder gableRoof() {
    double k = 1 / Math.sqrt(1.25); // normalises (0, +-1, 0.5)
    face(new double[][] {{-0.5, -0.5, 0}, {0.5, -0.5, 0}, {0.5, 0, 1}, {-0.5, 0, 1}},
        new double[] {0, -k, 0.5 * k});
    face(new double[][] {{0.5, 0.5, 0}, {-0.5, 0.5, 0}, {-0.5, 0, 1}, {0.5, 0, 1}},
        new double[] {0, k, 0.5 * k});
    return this;
  }

  /**
   * Adds a unit hip roof: four slopes from eaves at z = 0 around the square from -0.5 to +0.5,
   * up to a ridge along x at z = 1 from x = -ridgeHalf to +ridgeHalf. The ends slope too, so it
   * needs no gable ends. Scale it to fit a house.
   *
   * @param ridgeHalf half the ridge's length, from 0 (a pyramid) to below 0.5
   * @return this builder
   */
  public MeshBuilder hipRoof(double ridgeHalf) {
    double r = ridgeHalf;
    double[][][] faces = {
        {{-0.5, -0.5, 0}, {0.5, -0.5, 0}, {r, 0, 1}, {-r, 0, 1}},
        {{0.5, 0.5, 0}, {-0.5, 0.5, 0}, {-r, 0, 1}, {r, 0, 1}},
        {{0.5, -0.5, 0}, {0.5, 0.5, 0}, {r, 0, 1}},
        {{-0.5, 0.5, 0}, {-0.5, -0.5, 0}, {-r, 0, 1}}};
    float[] at = Mat4.identity();
    for (double[][] f : faces) {
      double[] n = upwardNormal(f[0], f[1], f[2]);
      if (f.length == 4) {
        face(f, n);
      } else {
        triangle(at, f[0], f[1], f[2], n);
      }
    }
    return this;
  }

  /** The unit normal of the plane through three points, turned to point up. */
  private static double[] upwardNormal(double[] a, double[] b, double[] c) {
    double[] u = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
    double[] v = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
    double[] n = {u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
    double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
    double sign = n[2] < 0 ? -1 : 1;
    return new double[] {sign * n[0] / len, sign * n[1] / len, sign * n[2] / len};
  }

  /**
   * Adds the two triangular gable ends that close a {@link #gableRoof()}, at x = -0.5 and
   * x = +0.5. They are kept apart from the roof so they can be drawn like the walls.
   *
   * @return this builder
   */
  public MeshBuilder gableEnds() {
    float[] at = Mat4.identity();
    for (int side = -1; side <= 1; side += 2) {
      triangle(at, new double[] {0.5 * side, -0.5, 0}, new double[] {0.5 * side, 0.5, 0},
          new double[] {0.5 * side, 0, 1}, new double[] {side, 0, 0});
    }
    return this;
  }

  /**
   * Adds a flat disc facing up.
   *
   * @param transform where to put it
   * @param radius the radius
   * @param z the height
   * @param segments how many sides
   * @return this builder
   */
  public MeshBuilder disc(float[] transform, double radius, double z, int segments) {
    for (int i = 0; i < segments; i++) {
      double a0 = 2 * Math.PI * i / segments;
      double a1 = 2 * Math.PI * (i + 1) / segments;
      triangle(transform, new double[] {0, 0, z},
          new double[] {Math.cos(a0) * radius, Math.sin(a0) * radius, z},
          new double[] {Math.cos(a1) * radius, Math.sin(a1) * radius, z},
          new double[] {0, 0, 1});
    }
    return this;
  }

  /**
   * Adds a flat rectangle facing up, centred on the origin.
   *
   * @param transform where to put it
   * @param sx the size along x
   * @param sy the size along y
   * @return this builder
   */
  public MeshBuilder plane(float[] transform, double sx, double sy) {
    double hx = sx / 2;
    double hy = sy / 2;
    quad(transform, new double[][] {{-hx, -hy, 0}, {hx, -hy, 0}, {hx, hy, 0}, {-hx, hy, 0}},
        new double[] {0, 0, 1});
    return this;
  }

  /**
   * Adds a smooth-shaded height field over the square from -halfSize to +halfSize on x and y.
   * Grid lines are closest together at the centre and spread out toward the edges, so detail
   * goes where the camera usually is. Each vertex gets its own normal from the slope, so hills
   * shade smoothly instead of showing facets.
   *
   * @param height the height at (x, y)
   * @param halfSize half the side of the square
   * @param cells how many cells along each side, at least 2
   * @return this builder
   */
  public MeshBuilder heightField(DoubleBinaryOperator height, double halfSize, int cells) {
    if (cells < 2) {
      throw new IllegalArgumentException("cells must be at least 2, got " + cells);
    }
    double[] at = new double[cells + 1];
    for (int i = 0; i <= cells; i++) {
      double u = 2.0 * i / cells - 1;
      at[i] = halfSize * (CENTRE_SPACING * u + (1 - CENTRE_SPACING) * u * u * u);
    }
    double[][] h = new double[cells + 1][cells + 1];
    for (int i = 0; i <= cells; i++) {
      for (int j = 0; j <= cells; j++) {
        h[i][j] = height.applyAsDouble(at[i], at[j]);
      }
    }
    double[][][] n = new double[cells + 1][cells + 1][];
    for (int i = 0; i <= cells; i++) {
      for (int j = 0; j <= cells; j++) {
        int i0 = Math.max(0, i - 1);
        int i1 = Math.min(cells, i + 1);
        int j0 = Math.max(0, j - 1);
        int j1 = Math.min(cells, j + 1);
        double dx = (h[i1][j] - h[i0][j]) / (at[i1] - at[i0]);
        double dy = (h[i][j1] - h[i][j0]) / (at[j1] - at[j0]);
        double len = Math.sqrt(dx * dx + dy * dy + 1);
        n[i][j] = new double[] {-dx / len, -dy / len, 1 / len};
      }
    }
    for (int i = 0; i < cells; i++) {
      for (int j = 0; j < cells; j++) {
        double[] p00 = {at[i], at[j], h[i][j]};
        double[] p10 = {at[i + 1], at[j], h[i + 1][j]};
        double[] p11 = {at[i + 1], at[j + 1], h[i + 1][j + 1]};
        double[] p01 = {at[i], at[j + 1], h[i][j + 1]};
        vertex(p00, n[i][j]);
        vertex(p10, n[i + 1][j]);
        vertex(p11, n[i + 1][j + 1]);
        vertex(p00, n[i][j]);
        vertex(p11, n[i + 1][j + 1]);
        vertex(p01, n[i][j + 1]);
      }
    }
    return this;
  }

  /**
   * Adds one quad, already in world coordinates, as two triangles.
   *
   * @param corners the four corners {x, y, z}, in order around the quad
   * @param normal the normal for all four
   * @return this builder
   */
  public MeshBuilder face(double[][] corners, double[] normal) {
    if (corners.length != 4) {
      throw new IllegalArgumentException("a face needs 4 corners, got " + corners.length);
    }
    vertex(corners[0], normal);
    vertex(corners[1], normal);
    vertex(corners[2], normal);
    vertex(corners[0], normal);
    vertex(corners[2], normal);
    vertex(corners[3], normal);
    return this;
  }

  /**
   * Adds another mesh's vertices, moved by a transform. Normals are transformed by the
   * inverse transpose, so they stay correct under non-uniform scaling.
   *
   * @param vertices interleaved position and normal floats, as {@link #toArray()} gives
   * @param transform where to put them
   * @return this builder
   */
  public MeshBuilder append(float[] vertices, float[] transform) {
    float[] inverse = Mat4.invert(transform);
    for (int i = 0; i + FLOATS_PER_VERTEX <= vertices.length; i += FLOATS_PER_VERTEX) {
      double[] p = Mat4.transform(transform, vertices[i], vertices[i + 1], vertices[i + 2], 1);
      double nx = vertices[i + 3];
      double ny = vertices[i + 4];
      double nz = vertices[i + 5];
      // (M^-1)^T n: row j of the transpose is column j of the inverse.
      double[] n = {
          inverse[0] * nx + inverse[1] * ny + inverse[2] * nz,
          inverse[4] * nx + inverse[5] * ny + inverse[6] * nz,
          inverse[8] * nx + inverse[9] * ny + inverse[10] * nz};
      double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
      if (len > 0) {
        n = scaled(n, 1 / len);
      }
      vertex(p, n);
    }
    return this;
  }

  /**
   * Returns how many vertices have been added.
   *
   * @return the vertex count
   */
  public int vertexCount() {
    return size / FLOATS_PER_VERTEX;
  }

  /**
   * Returns the vertex data.
   *
   * @return a copy of the interleaved position and normal floats
   */
  public float[] toArray() {
    return Arrays.copyOf(data, size);
  }

  private void quad(float[] t, double[][] c, double[] normal) {
    triangle(t, c[0], c[1], c[2], normal);
    triangle(t, c[0], c[2], c[3], normal);
  }

  private void triangle(float[] t, double[] a, double[] b, double[] c, double[] normal) {
    double[] n = Mat4.transform(t, normal[0], normal[1], normal[2], 0);
    vertex(Mat4.transform(t, a[0], a[1], a[2], 1), n);
    vertex(Mat4.transform(t, b[0], b[1], b[2], 1), n);
    vertex(Mat4.transform(t, c[0], c[1], c[2], 1), n);
  }

  private void vertex(double[] p, double[] n) {
    if (size + FLOATS_PER_VERTEX > data.length) {
      data = Arrays.copyOf(data, data.length * 2);
    }
    data[size++] = (float) p[0];
    data[size++] = (float) p[1];
    data[size++] = (float) p[2];
    data[size++] = (float) n[0];
    data[size++] = (float) n[1];
    data[size++] = (float) n[2];
  }

  private static double[] scaled(double[] v, double k) {
    return new double[] {v[0] * k, v[1] * k, v[2] * k};
  }
}
