package dronesimulation.view.scene;

/**
 * Procedural drone models built from boxes and cylinders, at real size (metres). Each model is
 * split into parts so the view can colour them and spin the rotors separately.
 */
final class DroneModels {

  /** Motor positions of the quadcopter, {x, y, z}. */
  static final double[][] QUAD_MOTORS = {
      {0.22, 0.22, 0.03}, {-0.22, 0.22, 0.03}, {-0.22, -0.22, 0.03}, {0.22, -0.22, 0.03}};
  /** Motor positions of the hexacopter: six arms 60 degrees apart, {x, y, z}. */
  static final double[][] HEX_MOTORS = hexMotors();

  /** Height of the rotor plane above a motor's base. */
  static final double ROTOR_HEIGHT = 0.05;
  /** Where the fixed-wing propeller sits. */
  static final double[] PLANE_PROPELLER = {0.56, 0, 0};

  private DroneModels() {
  }

  /**
   * Returns the quadcopter frame: arms, motors and landing legs (drawn dark).
   *
   * @return the builder
   */
  static MeshBuilder quadFrame() {
    MeshBuilder b = new MeshBuilder();
    b.box(Mat4.mul(Mat4.translation(0, 0, 0.03), Mat4.rotationZ(Math.PI / 4)), 0.64, 0.035,
        0.025);
    b.box(Mat4.mul(Mat4.translation(0, 0, 0.03), Mat4.rotationZ(-Math.PI / 4)), 0.64, 0.035,
        0.025);
    for (double[] m : QUAD_MOTORS) {
      b.cylinder(Mat4.translation(m[0], m[1], m[2]), 0.032, 0, ROTOR_HEIGHT, 14, true);
    }
    for (int sx = -1; sx <= 1; sx += 2) {
      b.box(Mat4.translation(0, sx * 0.09, -0.05), 0.2, 0.012, 0.012);
      b.box(Mat4.translation(0.07, sx * 0.09, -0.02), 0.012, 0.012, 0.06);
      b.box(Mat4.translation(-0.07, sx * 0.09, -0.02), 0.012, 0.012, 0.06);
    }
    return b;
  }

  private static double[][] hexMotors() {
    double[][] motors = new double[6][];
    for (int i = 0; i < 6; i++) {
      double a = Math.PI / 6 + i * Math.PI / 3;
      motors[i] = new double[] {0.34 * Math.cos(a), 0.34 * Math.sin(a), 0.03};
    }
    return motors;
  }

  /**
   * Returns the hexacopter frame: three crossing arms, six motors and landing legs.
   *
   * @return the builder
   */
  static MeshBuilder hexFrame() {
    MeshBuilder b = new MeshBuilder();
    for (int i = 0; i < 3; i++) {
      b.box(Mat4.mul(Mat4.translation(0, 0, 0.03), Mat4.rotationZ(Math.PI / 6 + i * Math.PI / 3)),
          0.7, 0.035, 0.025);
    }
    for (double[] m : HEX_MOTORS) {
      b.cylinder(Mat4.translation(m[0], m[1], m[2]), 0.034, 0, ROTOR_HEIGHT, 14, true);
    }
    for (int sx = -1; sx <= 1; sx += 2) {
      b.box(Mat4.translation(0, sx * 0.11, -0.05), 0.26, 0.014, 0.014);
      b.box(Mat4.translation(0.09, sx * 0.11, -0.02), 0.014, 0.014, 0.06);
      b.box(Mat4.translation(-0.09, sx * 0.11, -0.02), 0.014, 0.014, 0.06);
    }
    return b;
  }

  /**
   * Returns the hexacopter body shell: a larger hub with a cargo pod underneath.
   *
   * @return the builder
   */
  static MeshBuilder hexBody() {
    MeshBuilder b = new MeshBuilder();
    b.cylinder(Mat4.translation(0, 0, 0.02), 0.11, 0, 0.055, 6, true);
    b.box(Mat4.translation(0, 0, -0.015), 0.14, 0.1, 0.05);
    return b;
  }

  /**
   * Returns the quadcopter body shell (drawn in the drone's colour).
   *
   * @return the builder
   */
  static MeshBuilder quadBody() {
    MeshBuilder b = new MeshBuilder();
    b.box(Mat4.translation(0, 0, 0.045), 0.17, 0.12, 0.05);
    b.box(Mat4.translation(0.06, 0, 0.035), 0.08, 0.09, 0.03);
    return b;
  }

  /**
   * Returns one two-bladed rotor, centred on the origin, spinning around z.
   *
   * @return the builder
   */
  static MeshBuilder rotorBlades() {
    return new MeshBuilder().box(Mat4.identity(), 0.25, 0.022, 0.005);
  }

  /**
   * Returns the faint disc that shows a spinning rotor.
   *
   * @return the builder
   */
  static MeshBuilder rotorDisc() {
    return new MeshBuilder().disc(Mat4.identity(), 0.13, 0, 24);
  }

  /**
   * Returns the fixed-wing airframe in the drone's colour: fuselage, wings and tail. The nose
   * points along +x.
   *
   * @return the builder
   */
  static MeshBuilder planeBody() {
    MeshBuilder b = new MeshBuilder();
    b.box(Mat4.translation(0.0, 0, 0), 1.0, 0.13, 0.13);
    b.box(Mat4.translation(0.45, 0, 0), 0.14, 0.1, 0.1);
    b.box(Mat4.translation(0.06, 0, 0.05), 0.26, 1.8, 0.03);
    b.box(Mat4.translation(-0.44, 0, 0.02), 0.14, 0.52, 0.02);
    b.box(Mat4.translation(-0.46, 0, 0.12), 0.14, 0.02, 0.2);
    return b;
  }

  /**
   * Returns the fixed-wing dark details: canopy, wing tips and wheels.
   *
   * @return the builder
   */
  static MeshBuilder planeDetails() {
    MeshBuilder b = new MeshBuilder();
    b.box(Mat4.translation(0.18, 0, 0.08), 0.22, 0.09, 0.05);
    b.box(Mat4.translation(0.06, 0.9, 0.05), 0.26, 0.02, 0.06);
    b.box(Mat4.translation(0.06, -0.9, 0.05), 0.26, 0.02, 0.06);
    b.cylinder(Mat4.mul(Mat4.translation(0.2, 0.12, -0.1), Mat4.rotationX(Math.PI / 2)),
        0.04, -0.015, 0.015, 12, true);
    b.cylinder(Mat4.mul(Mat4.translation(0.2, -0.12, -0.1), Mat4.rotationX(Math.PI / 2)),
        0.04, -0.015, 0.015, 12, true);
    b.cylinder(Mat4.mul(Mat4.translation(-0.4, 0, -0.08), Mat4.rotationX(Math.PI / 2)),
        0.03, -0.01, 0.01, 12, true);
    return b;
  }

  /**
   * Returns the fixed-wing propeller, centred on the origin, spinning around x.
   *
   * @return the builder
   */
  static MeshBuilder propeller() {
    return new MeshBuilder().box(Mat4.identity(), 0.012, 0.035, 0.32);
  }
}
