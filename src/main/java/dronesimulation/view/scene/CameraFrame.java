package dronesimulation.view.scene;

/**
 * A camera at an eye point looking at a target, with the world's z axis up. Gives the
 * camera's placement for a scene camera that looks along its own +z with +y down, and
 * projects world points onto the screen, for labels drawn over the 3D view.
 */
public final class CameraFrame {

  private final double[] eye;
  private final double[] right;
  private final double[] down;
  private final double[] forward;

  /**
   * Creates the frame.
   *
   * @param eye where the camera is {x, y, z}
   * @param target what it looks at; not straight above or below the eye
   */
  public CameraFrame(double[] eye, double[] target) {
    this.eye = eye.clone();
    forward = normalize(sub(target, eye));
    double[] r = cross(forward, new double[] {0, 0, 1});
    if (length(r) < 1e-9) {
      r = new double[] {1, 0, 0};
    }
    right = normalize(r);
    down = cross(forward, right);
  }

  /**
   * Returns the camera's placement: its columns are right, down, forward and the eye.
   *
   * @return the column-major matrix
   */
  public float[] matrix() {
    return new float[] {
        (float) right[0], (float) right[1], (float) right[2], 0,
        (float) down[0], (float) down[1], (float) down[2], 0,
        (float) forward[0], (float) forward[1], (float) forward[2], 0,
        (float) eye[0], (float) eye[1], (float) eye[2], 1};
  }

  /**
   * Projects a world point onto the screen.
   *
   * @param point the point {x, y, z}
   * @param fovY the vertical field of view in degrees
   * @param width the screen width in pixels
   * @param height the screen height in pixels
   * @return {x, y} in pixels from the top left, or null if the point is behind the camera
   */
  public double[] project(double[] point, double fovY, double width, double height) {
    double[] rel = sub(point, eye);
    double z = dot(rel, forward);
    if (z < 0.1) {
      return null;
    }
    double focal = (height / 2) / Math.tan(Math.toRadians(fovY) / 2);
    return new double[] {width / 2 + dot(rel, right) / z * focal,
        height / 2 + dot(rel, down) / z * focal};
  }

  private static double[] sub(double[] a, double[] b) {
    return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
  }

  private static double dot(double[] a, double[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  private static double[] cross(double[] a, double[] b) {
    return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]};
  }

  private static double length(double[] a) {
    return Math.sqrt(dot(a, a));
  }

  private static double[] normalize(double[] a) {
    double len = length(a);
    return new double[] {a[0] / len, a[1] / len, a[2] / len};
  }
}
