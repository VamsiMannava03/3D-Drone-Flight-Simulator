package dronesimulation.view.scene;

import dronesimulation.model.Vector3D;
import dronesimulation.model.engine.DroneSnapshot;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * How one drone looks from frame to frame: its tilt, heading, bank and rotor spin, eased
 * toward what its latest snapshot implies, plus the trail it leaves. The model has no
 * orientation (drones are point masses), so these come from the thrust and velocity alone.
 */
public final class DroneMotion {

  /** How much larger than life multirotors are drawn, so they can be seen from afar. */
  public static final double QUAD_SCALE = 3.0;
  /** How much larger than life planes are drawn. */
  public static final double PLANE_SCALE = 2.2;
  /** The most trail points kept. */
  public static final int TRAIL_POINTS = 700;
  /** Distance between trail points, in m. */
  public static final double TRAIL_SPACING = 0.5;

  private final int colour;
  private final Deque<double[]> trail = new ArrayDeque<>();
  private double[] up = {0, 0, 1};
  private double yaw;
  private double pitch;
  private double roll;
  private double rotorAngle;
  private double rotorRate;

  /**
   * Creates the motion of a drone.
   *
   * @param colour the drone's colour as 0xRRGGBB
   */
  public DroneMotion(int colour) {
    this.colour = colour;
  }

  /**
   * Eases the look toward a new snapshot of the drone.
   *
   * @param d the drone's snapshot
   * @param dt the real time since the last frame, in s
   */
  public void update(DroneSnapshot d, double dt) {
    double[] p = vec(d.position());
    double[] last = trail.peekLast();
    if (last == null || distance(last, p) > TRAIL_SPACING) {
      trail.addLast(p);
      if (trail.size() > TRAIL_POINTS) {
        trail.removeFirst();
      }
    }
    double k = 1 - Math.exp(-dt * 8);
    boolean plane = isPlane(d);
    boolean crashed = isCrashed(d);
    Vector3D thrust = d.thrust();
    double[] wantUp = {0, 0, 1};
    if (crashed) {
      wantUp = normalize(new double[] {0.9, 0.2, 0.35});
    } else if (!plane && thrust.magnitude() > 0.5) {
      wantUp = normalize(vec(thrust));
    }
    for (int i = 0; i < 3; i++) {
      up[i] += (wantUp[i] - up[i]) * k;
    }
    up = normalize(up);
    Vector3D vel = d.velocity();
    double horizontal = Math.hypot(vel.x(), vel.y());
    double yawRate = 0;
    if (horizontal > 0.7 && !crashed) {
      double delta = wrapAngle(Math.atan2(vel.y(), vel.x()) - yaw);
      double step = delta * (1 - Math.exp(-dt * (plane ? 5 : 2.5)));
      yaw += step;
      yawRate = step / Math.max(dt, 1e-4);
    }
    if (plane) {
      double wantRoll = crashed ? 1.2 : Math.max(-0.9, Math.min(0.9,
          -Math.atan(horizontal * yawRate / 9.81)));
      roll += (wantRoll - roll) * k;
      double wantPitch = crashed ? 0 : Math.atan2(vel.z(), Math.max(horizontal, 1));
      pitch += (wantPitch - pitch) * k;
    }
    boolean spinning = !crashed && thrust.magnitude() > 0.05;
    double wantRate = spinning ? 28 + thrust.magnitude() * 2.2 : 0;
    rotorRate += (wantRate - rotorRate) * (1 - Math.exp(-dt * 2));
    rotorAngle += rotorRate * dt;
  }

  /**
   * Returns where to draw the drone's model: position, orientation and scale.
   *
   * @param d the drone's snapshot
   * @return the model matrix
   */
  public float[] model(DroneSnapshot d) {
    Vector3D p = d.position();
    if (isPlane(d)) {
      return Mat4.mul(Mat4.translation(p.x(), p.y(), p.z() + liftOf(d)),
          Mat4.rotationZ(yaw), Mat4.rotationY(-pitch), Mat4.rotationX(roll),
          Mat4.scale(PLANE_SCALE, PLANE_SCALE, PLANE_SCALE));
    }
    return Mat4.mul(Mat4.translation(p.x(), p.y(), p.z() + liftOf(d)),
        Mat4.alignZ(up[0], up[1], up[2]), Mat4.rotationZ(yaw),
        Mat4.scale(QUAD_SCALE, QUAD_SCALE, QUAD_SCALE));
  }

  /**
   * Returns the centre of the drawn model, a little above the drone's point.
   *
   * @param d the drone's snapshot
   * @return the centre
   */
  public static double[] visualCentre(DroneSnapshot d) {
    Vector3D p = d.position();
    return new double[] {p.x(), p.y(), p.z() + liftOf(d)};
  }

  /**
   * Returns the rotor positions in the model, for a multirotor.
   *
   * @param d the drone's snapshot
   * @return the motor positions
   */
  public static double[][] motorsOf(DroneSnapshot d) {
    return isHex(d) ? DroneModels.HEX_MOTORS : DroneModels.QUAD_MOTORS;
  }

  /**
   * Tells whether the drone is a fixed-wing plane.
   *
   * @param d the drone's snapshot
   * @return true for a plane
   */
  public static boolean isPlane(DroneSnapshot d) {
    return d.type().equals("FixedWing");
  }

  /**
   * Tells whether the drone is a hexacopter.
   *
   * @param d the drone's snapshot
   * @return true for a hexacopter
   */
  public static boolean isHex(DroneSnapshot d) {
    return d.type().equals("Hexacopter");
  }

  /**
   * Tells whether the drone has crashed.
   *
   * @param d the drone's snapshot
   * @return true if crashed
   */
  public static boolean isCrashed(DroneSnapshot d) {
    return d.state().equals("Crashed");
  }

  /**
   * Tells whether the drone is on its way somewhere, so its target is worth showing.
   *
   * @param d the drone's snapshot
   * @return true while it flies
   */
  public static boolean isActive(DroneSnapshot d) {
    return !List.of("Crashed", "Landed", "Idle", "Charging").contains(d.state());
  }

  /**
   * Returns the drone's colour.
   *
   * @return 0xRRGGBB
   */
  public int colour() {
    return colour;
  }

  /**
   * Returns the trail, oldest point first.
   *
   * @return the points as {x, y, z}
   */
  public List<double[]> trail() {
    return List.copyOf(trail);
  }

  /**
   * Returns the rotor angle, which grows as the rotors spin.
   *
   * @return the angle in radians
   */
  public double rotorAngle() {
    return rotorAngle;
  }

  /**
   * Returns how fast the rotors spin.
   *
   * @return the rate in radians per second
   */
  public double rotorRate() {
    return rotorRate;
  }

  /**
   * Returns the heading.
   *
   * @return the yaw in radians
   */
  public double yaw() {
    return yaw;
  }

  /**
   * Returns which way the drone's top points.
   *
   * @return a unit vector {x, y, z}
   */
  public double[] up() {
    return up.clone();
  }

  private static double liftOf(DroneSnapshot d) {
    return isPlane(d) ? 0.14 * PLANE_SCALE : 0.08 * QUAD_SCALE;
  }

  static double wrapAngle(double a) {
    double r = (a + Math.PI) % (2 * Math.PI);
    if (r < 0) {
      r += 2 * Math.PI;
    }
    return r - Math.PI;
  }

  private static double[] vec(Vector3D v) {
    return new double[] {v.x(), v.y(), v.z()};
  }

  private static double distance(double[] a, double[] b) {
    return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1])
        + (a[2] - b[2]) * (a[2] - b[2]));
  }

  private static double[] normalize(double[] a) {
    double len = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    return len == 0 ? new double[] {0, 0, 1} : new double[] {a[0] / len, a[1] / len, a[2] / len};
  }
}
