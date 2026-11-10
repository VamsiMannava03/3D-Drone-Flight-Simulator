package dronesimulation.view.scene;

/**
 * 4x4 float matrices for OpenGL, stored column-major in a {@code float[16]} as OpenGL expects.
 * Every method returns a new array; inputs are never changed.
 */
public final class Mat4 {

  private Mat4() {
  }

  /**
   * Returns the identity matrix.
   *
   * @return a new identity matrix
   */
  public static float[] identity() {
    float[] m = new float[16];
    m[0] = 1;
    m[5] = 1;
    m[10] = 1;
    m[15] = 1;
    return m;
  }

  /**
   * Multiplies two matrices: the result applies {@code b} first, then {@code a}.
   *
   * @param a the left matrix
   * @param b the right matrix
   * @return a * b
   */
  public static float[] mul(float[] a, float[] b) {
    float[] r = new float[16];
    for (int col = 0; col < 4; col++) {
      for (int row = 0; row < 4; row++) {
        float sum = 0;
        for (int k = 0; k < 4; k++) {
          sum += a[k * 4 + row] * b[col * 4 + k];
        }
        r[col * 4 + row] = sum;
      }
    }
    return r;
  }

  /**
   * Multiplies several matrices left to right: mul(a, b, c) = a * b * c.
   *
   * @param first the leftmost matrix
   * @param rest the others, in order
   * @return the product
   */
  public static float[] mul(float[] first, float[]... rest) {
    float[] r = first;
    for (float[] m : rest) {
      r = mul(r, m);
    }
    return r;
  }

  /**
   * Returns a translation.
   *
   * @param x the x offset
   * @param y the y offset
   * @param z the z offset
   * @return the matrix
   */
  public static float[] translation(double x, double y, double z) {
    float[] m = identity();
    m[12] = (float) x;
    m[13] = (float) y;
    m[14] = (float) z;
    return m;
  }

  /**
   * Returns a scale.
   *
   * @param x the x factor
   * @param y the y factor
   * @param z the z factor
   * @return the matrix
   */
  public static float[] scale(double x, double y, double z) {
    float[] m = identity();
    m[0] = (float) x;
    m[5] = (float) y;
    m[10] = (float) z;
    return m;
  }

  /**
   * Returns a rotation about the x axis.
   *
   * @param angle the angle in radians, counter-clockwise looking down the axis
   * @return the matrix
   */
  public static float[] rotationX(double angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    float[] m = identity();
    m[5] = c;
    m[6] = s;
    m[9] = -s;
    m[10] = c;
    return m;
  }

  /**
   * Returns a rotation about the y axis.
   *
   * @param angle the angle in radians
   * @return the matrix
   */
  public static float[] rotationY(double angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    float[] m = identity();
    m[0] = c;
    m[2] = -s;
    m[8] = s;
    m[10] = c;
    return m;
  }

  /**
   * Returns a rotation about the z axis.
   *
   * @param angle the angle in radians
   * @return the matrix
   */
  public static float[] rotationZ(double angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    float[] m = identity();
    m[0] = c;
    m[1] = s;
    m[4] = -s;
    m[5] = c;
    return m;
  }

  /**
   * Returns a rotation about any axis (Rodrigues' formula). A zero axis gives the identity.
   *
   * @param ax the axis x
   * @param ay the axis y
   * @param az the axis z
   * @param angle the angle in radians
   * @return the matrix
   */
  public static float[] rotation(double ax, double ay, double az, double angle) {
    double len = Math.sqrt(ax * ax + ay * ay + az * az);
    if (len < 1e-12 || angle == 0) {
      return identity();
    }
    double x = ax / len;
    double y = ay / len;
    double z = az / len;
    double c = Math.cos(angle);
    double s = Math.sin(angle);
    double t = 1 - c;
    float[] m = identity();
    m[0] = (float) (t * x * x + c);
    m[1] = (float) (t * x * y + s * z);
    m[2] = (float) (t * x * z - s * y);
    m[4] = (float) (t * x * y - s * z);
    m[5] = (float) (t * y * y + c);
    m[6] = (float) (t * y * z + s * x);
    m[8] = (float) (t * x * z + s * y);
    m[9] = (float) (t * y * z - s * x);
    m[10] = (float) (t * z * z + c);
    return m;
  }

  /**
   * Returns the rotation that turns the +z axis to point along a direction.
   *
   * @param dx the direction x
   * @param dy the direction y
   * @param dz the direction z
   * @return the matrix
   */
  public static float[] alignZ(double dx, double dy, double dz) {
    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 1e-12) {
      return identity();
    }
    if (dz < 0 && Math.hypot(dx, dy) < 1e-12 * len) {
      return rotationX(Math.PI); // straight down: the axis below is zero, so flip explicitly
    }
    double cos = Math.max(-1, Math.min(1, dz / len));
    // axis = z x d = (-dy, dx, 0)
    return rotation(-dy, dx, 0, Math.acos(cos));
  }

  /**
   * Returns a perspective projection.
   *
   * @param fovY the vertical field of view in radians
   * @param aspect width / height
   * @param near the near plane distance, positive
   * @param far the far plane distance, beyond near
   * @return the matrix
   */
  public static float[] perspective(double fovY, double aspect, double near, double far) {
    float f = (float) (1 / Math.tan(fovY / 2));
    float[] m = new float[16];
    m[0] = (float) (f / aspect);
    m[5] = f;
    m[10] = (float) ((far + near) / (near - far));
    m[11] = -1;
    m[14] = (float) (2 * far * near / (near - far));
    return m;
  }

  /**
   * Returns a camera (view) matrix looking from an eye point toward a centre point.
   *
   * @param eye the camera position
   * @param centre the point looked at
   * @param up the up direction
   * @return the matrix
   */
  public static float[] lookAt(double[] eye, double[] centre, double[] up) {
    double[] f = normalize(sub(centre, eye));
    double[] s = normalize(cross(f, up));
    double[] u = cross(s, f);
    float[] m = identity();
    m[0] = (float) s[0];
    m[4] = (float) s[1];
    m[8] = (float) s[2];
    m[1] = (float) u[0];
    m[5] = (float) u[1];
    m[9] = (float) u[2];
    m[2] = (float) -f[0];
    m[6] = (float) -f[1];
    m[10] = (float) -f[2];
    m[12] = (float) -dot(s, eye);
    m[13] = (float) -dot(u, eye);
    m[14] = (float) dot(f, eye);
    return m;
  }

  /**
   * Returns the inverse of a matrix.
   *
   * @param m the matrix
   * @return its inverse
   * @throws ArithmeticException if the matrix cannot be inverted
   */
  public static float[] invert(float[] m) {
    double[] inv = new double[16];
    inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
        + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
    inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
        - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
    inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
        + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
    inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
        - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
    inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
        - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
    inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
        + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
    inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
        - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
    inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
        + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
    inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
        + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
    inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
        - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
    inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
        + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
    inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
        - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
    inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
        - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
    inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
        + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
    inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
        - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
    inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
        + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
    double det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
    if (Math.abs(det) < 1e-20) {
      throw new ArithmeticException("matrix is not invertible");
    }
    float[] r = new float[16];
    for (int i = 0; i < 16; i++) {
      r[i] = (float) (inv[i] / det);
    }
    return r;
  }

  /**
   * Transforms a point (x, y, z, w) by a matrix.
   *
   * @param m the matrix
   * @param x the x
   * @param y the y
   * @param z the z
   * @param w the w (1 for points, 0 for directions)
   * @return the transformed {x, y, z, w}
   */
  public static double[] transform(float[] m, double x, double y, double z, double w) {
    return new double[] {
        m[0] * x + m[4] * y + m[8] * z + m[12] * w,
        m[1] * x + m[5] * y + m[9] * z + m[13] * w,
        m[2] * x + m[6] * y + m[10] * z + m[14] * w,
        m[3] * x + m[7] * y + m[11] * z + m[15] * w
    };
  }

  private static double[] sub(double[] a, double[] b) {
    return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
  }

  private static double[] cross(double[] a, double[] b) {
    return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]};
  }

  private static double dot(double[] a, double[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  private static double[] normalize(double[] a) {
    double len = Math.sqrt(dot(a, a));
    return new double[] {a[0] / len, a[1] / len, a[2] / len};
  }
}
