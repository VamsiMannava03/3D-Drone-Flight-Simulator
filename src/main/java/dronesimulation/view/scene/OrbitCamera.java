package dronesimulation.view.scene;

/**
 * A camera that circles a target point: drag to orbit, scroll to zoom, right-drag to pan, and it
 * can smoothly follow a moving drone. World z is up.
 */
public class OrbitCamera {

  /** Lowest camera angle above the horizon, in radians. */
  public static final double MIN_PITCH = Math.toRadians(4);
  /** Highest camera angle, in radians (just short of straight down). */
  public static final double MAX_PITCH = Math.toRadians(88);
  /** Closest zoom, in m. */
  public static final double MIN_DISTANCE = 3;
  /** Farthest zoom, in m. */
  public static final double MAX_DISTANCE = 900;

  private final double[] target;
  private double yaw;
  private double pitch;
  private double distance;

  /**
   * Creates a camera.
   *
   * @param target the point to look at {x, y, z}
   * @param yaw the compass angle of the camera around the target, in radians
   * @param pitch the angle above the horizon, in radians
   * @param distance the distance from the target, in m
   */
  public OrbitCamera(double[] target, double yaw, double pitch, double distance) {
    this.target = target.clone();
    this.yaw = yaw;
    this.pitch = clamp(pitch, MIN_PITCH, MAX_PITCH);
    this.distance = clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
  }

  /**
   * Returns the camera position.
   *
   * @return {x, y, z}
   */
  public double[] eye() {
    double horizontal = distance * Math.cos(pitch);
    return new double[] {
        target[0] + horizontal * Math.cos(yaw),
        target[1] + horizontal * Math.sin(yaw),
        target[2] + distance * Math.sin(pitch)
    };
  }

  /**
   * Returns the point looked at.
   *
   * @return a copy of {x, y, z}
   */
  public double[] target() {
    return target.clone();
  }

  /**
   * Returns the view matrix.
   *
   * @return the matrix
   */
  public float[] viewMatrix() {
    return Mat4.lookAt(eye(), target, new double[] {0, 0, 1});
  }

  /**
   * Rotates around the target.
   *
   * @param deltaYaw change in compass angle, in radians
   * @param deltaPitch change in height angle, in radians
   */
  public void orbit(double deltaYaw, double deltaPitch) {
    yaw += deltaYaw;
    pitch = clamp(pitch + deltaPitch, MIN_PITCH, MAX_PITCH);
  }

  /**
   * Zooms in (positive steps) or out (negative steps), by 12% per step.
   *
   * @param steps scroll steps
   */
  public void zoom(double steps) {
    distance = clamp(distance * Math.pow(0.88, steps), MIN_DISTANCE, MAX_DISTANCE);
  }

  /**
   * Slides the target across the ground, relative to the way the camera faces, so the ground
   * follows the mouse on both axes.
   *
   * @param right screen-right mouse movement, in pixels
   * @param down screen-down mouse movement, in pixels
   */
  public void pan(double right, double down) {
    double speed = distance * 0.0015;
    double fx = -Math.cos(yaw);
    double fy = -Math.sin(yaw);
    double leftX = -fy;
    double leftY = fx;
    target[0] += (leftX * right + fx * down) * speed;
    target[1] += (leftY * right + fy * down) * speed;
  }

  /**
   * Moves the target smoothly toward a point, for following a drone.
   *
   * @param point the point to follow {x, y, z}
   * @param dt the real time since the last frame, in s
   */
  public void follow(double[] point, double dt) {
    double k = 1 - Math.exp(-dt * 6);
    for (int i = 0; i < 3; i++) {
      target[i] += (point[i] - target[i]) * k;
    }
  }

  /**
   * Jumps to a new view.
   *
   * @param point the new target {x, y, z}
   * @param newDistance the new distance, in m
   */
  public void moveTo(double[] point, double newDistance) {
    System.arraycopy(point, 0, target, 0, 3);
    distance = clamp(newDistance, MIN_DISTANCE, MAX_DISTANCE);
  }

  /**
   * Returns the distance to the target.
   *
   * @return the distance in m
   */
  public double getDistance() {
    return distance;
  }

  /**
   * Returns the height angle.
   *
   * @return the pitch in radians
   */
  public double getPitch() {
    return pitch;
  }

  private static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
  }
}
