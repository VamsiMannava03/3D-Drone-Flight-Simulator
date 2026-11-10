package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** The 3D view's math runs without a GPU, so it is tested like any other code. */
class GlMathTest {

  private static final float EPS = 1e-4f;

  private static double dot(double[] a, double[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  private static double[] apply(float[] m, double x, double y, double z) {
    return Mat4.transform(m, x, y, z, 1);
  }

  @Test
  void identityAndMultiplication() {
    float[] t = Mat4.translation(1, 2, 3);
    assertArrayEquals(t, Mat4.mul(Mat4.identity(), t), EPS);
    double[] p = apply(Mat4.mul(Mat4.translation(1, 0, 0), Mat4.scale(2, 2, 2)), 1, 1, 1);
    assertArrayEquals(new double[] {3, 2, 2, 1}, p, EPS); // scale first, then translate
  }

  @Test
  void rotationsTurnTheRightWay() {
    assertArrayEquals(new double[] {0, 1, 0, 1}, apply(Mat4.rotationZ(Math.PI / 2), 1, 0, 0),
        EPS);
    assertArrayEquals(new double[] {0, 0, 1, 1}, apply(Mat4.rotationX(Math.PI / 2), 0, 1, 0),
        EPS);
    assertArrayEquals(new double[] {0, 0, -1, 1}, apply(Mat4.rotationY(Math.PI / 2), 1, 0, 0),
        EPS);
    assertArrayEquals(Mat4.rotationZ(0.7), Mat4.rotation(0, 0, 1, 0.7), EPS);
  }

  @Test
  void alignZPointsUpAxisAlongDirection() {
    double[] d = apply(Mat4.alignZ(1, 0, 1), 0, 0, 1);
    assertEquals(Math.sqrt(0.5), d[0], EPS);
    assertEquals(Math.sqrt(0.5), d[2], EPS);
    assertArrayEquals(Mat4.identity(), Mat4.alignZ(0, 0, 5), EPS);
    double[] down = apply(Mat4.alignZ(0, 0, -2), 0, 0, 1);
    assertEquals(0, down[0], EPS);
    assertEquals(0, down[1], EPS);
    assertEquals(-1, down[2], EPS);
  }

  @Test
  void lookAtPutsEyeAtOriginLookingDownMinusZ() {
    float[] view = Mat4.lookAt(new double[] {10, 0, 5}, new double[] {0, 0, 5},
        new double[] {0, 0, 1});
    assertArrayEquals(new double[] {0, 0, 0, 1}, apply(view, 10, 0, 5), EPS);
    assertArrayEquals(new double[] {0, 0, -10, 1}, apply(view, 0, 0, 5), EPS);
    assertArrayEquals(new double[] {0, 1, -10, 1}, apply(view, 0, 0, 6), EPS); // z is up
  }

  @Test
  void perspectiveMapsNearAndFarPlanes() {
    float[] p = Mat4.perspective(Math.toRadians(60), 1.5, 1, 100);
    double[] near = apply(p, 0, 0, -1);
    double[] far = apply(p, 0, 0, -100);
    assertEquals(-1, near[2] / near[3], EPS);
    assertEquals(1, far[2] / far[3], EPS);
  }

  @Test
  void inverseUndoesTheMatrix() {
    float[] m = Mat4.mul(Mat4.translation(3, -2, 7), Mat4.rotation(1, 2, 3, 0.8),
        Mat4.scale(2, 3, 4));
    assertArrayEquals(Mat4.identity(), Mat4.mul(m, Mat4.invert(m)), 1e-4f);
    assertThrows(ArithmeticException.class, () -> Mat4.invert(new float[16]));
  }

  @Test
  void orbitCameraKeepsDistanceAndClampsPitch() {
    OrbitCamera cam = new OrbitCamera(new double[] {5, 5, 0}, 0, Math.toRadians(30), 20);
    double[] eye = cam.eye();
    double d = Math.sqrt(Math.pow(eye[0] - 5, 2) + Math.pow(eye[1] - 5, 2) + eye[2] * eye[2]);
    assertEquals(20, d, 1e-9);
    assertEquals(10, eye[2], 1e-9);

    cam.orbit(0, 10);
    assertEquals(OrbitCamera.MAX_PITCH, cam.getPitch(), 1e-12);
    cam.zoom(-1000);
    assertEquals(OrbitCamera.MAX_DISTANCE, cam.getDistance(), 1e-9);
  }

  @Test
  void cameraFollowMovesTowardThePoint() {
    OrbitCamera cam = new OrbitCamera(new double[] {0, 0, 0}, 0, 0.5, 20);
    for (int i = 0; i < 120; i++) {
      cam.follow(new double[] {10, 0, 0}, 1.0 / 60);
    }
    assertEquals(10, cam.target()[0], 0.01);
  }

  @Test
  void panDragsTheGroundWithTheMouseOnBothAxes() {
    // Yaw -90 degrees: the camera sits south of the target and looks north, so screen-right is
    // east (+x) and screen-up is north (+y).
    OrbitCamera cam = new OrbitCamera(new double[] {0, 0, 0}, -Math.PI / 2, 0.5, 20);
    cam.pan(10, 0); // drag right: the ground follows, so the target moves west
    assertEquals(-0.3, cam.target()[0], 1e-9);
    assertEquals(0, cam.target()[1], 1e-9);
    cam.pan(-10, 10); // drag back and down: the ground follows, so the target moves north
    assertEquals(0, cam.target()[0], 1e-9);
    assertEquals(0.3, cam.target()[1], 1e-9);
  }

  @Test
  void meshBuilderMakesClosedShapes() {
    MeshBuilder b = new MeshBuilder().box(Mat4.identity(), 1, 2, 3);
    assertEquals(36, b.vertexCount()); // 6 faces x 2 triangles x 3 vertices
    float[] data = b.toArray();
    for (int v = 0; v < 36; v++) { // every corner of a 1-wide box has x = +-0.5
      assertEquals(0.5, Math.abs(data[v * 6]), 1e-6);
    }
  }

  @Test
  void cylinderAndDiscVertexCounts() {
    assertEquals(16 * 6, new MeshBuilder().cylinder(Mat4.identity(), 1, 0, 1, 16, false)
        .vertexCount());
    assertEquals(16 * 12, new MeshBuilder().cylinder(Mat4.identity(), 1, 0, 1, 16, true)
        .vertexCount());
    assertEquals(8 * 3, new MeshBuilder().disc(Mat4.identity(), 1, 0, 8).vertexCount());
  }

  @Test
  void gableRoofSlopesUpToItsRidgeAndClosesAtTheEnds() {
    float[] roof = new MeshBuilder().gableRoof().toArray();
    assertEquals(12 * 6, roof.length); // 2 slopes x 2 triangles x 3 vertices
    for (int i = 0; i < roof.length; i += 6) {
      double y = roof[i + 1];
      double z = roof[i + 2];
      assertEquals(1 - 2 * Math.abs(y), z, 1e-6, "on a slope from eave to ridge");
      assertEquals(1, Math.sqrt(roof[i + 3] * roof[i + 3] + roof[i + 4] * roof[i + 4]
          + roof[i + 5] * roof[i + 5]), 1e-5);
      assertTrue(roof[i + 5] > 0, "roof faces up");
      assertEquals(Math.signum(y == 0 ? roof[i + 4] : y), Math.signum(roof[i + 4]),
          "each slope faces its own side");
    }
    float[] ends = new MeshBuilder().gableEnds().toArray();
    assertEquals(6 * 6, ends.length); // 2 triangles
    for (int i = 0; i < ends.length; i += 6) {
      assertEquals(0.5, Math.abs(ends[i]), 1e-6);
      assertEquals(Math.signum(ends[i]), ends[i + 3], 1e-6, "faces outward along x");
    }
  }

  @Test
  void hipRoofSlopesOnAllFourSides() {
    float[] d = new MeshBuilder().hipRoof(0.25).toArray();
    assertEquals((2 * 6 + 2 * 3) * 6, d.length); // two trapezoids and two triangles
    boolean[] faces = new boolean[4]; // which way the slopes face: -y, +y, +x, -x
    for (int i = 0; i < d.length; i += 6) {
      double z = d[i + 2];
      assertTrue(z == 0 || z == 1, "on the eaves or the ridge");
      if (z == 1) {
        assertEquals(0, d[i + 1], 1e-6);
        assertTrue(Math.abs(d[i]) <= 0.25 + 1e-6, "ridge from -0.25 to +0.25");
      }
      assertEquals(1, Math.sqrt(d[i + 3] * d[i + 3] + d[i + 4] * d[i + 4] + d[i + 5] * d[i + 5]),
          1e-5);
      assertTrue(d[i + 5] > 0, "every slope faces up");
      faces[d[i + 4] < -0.1 ? 0 : d[i + 4] > 0.1 ? 1 : d[i + 3] > 0.1 ? 2 : 3] = true;
    }
    for (boolean f : faces) {
      assertTrue(f, "slopes toward all four sides");
    }
  }

  @Test
  void coneNormalsArePerpendicularToItsSides() {
    for (double tip : new double[] {3, -3}) { // pointing up and pointing down
      MeshBuilder b = new MeshBuilder().cone(Mat4.identity(), 2, 0, tip, 8);
      assertEquals(8 * 3, b.vertexCount());
      float[] d = b.toArray();
      for (int f = 0; f < 8; f++) {
        int i = f * 18; // three vertices of 6 floats each: two base corners, then the tip
        double[] n = {d[i + 3], d[i + 4], d[i + 5]};
        double[] edge = {d[i + 6] - d[i], d[i + 7] - d[i + 1], d[i + 8] - d[i + 2]};
        double[] slant = {d[i + 12] - (d[i] + d[i + 6]) / 2, d[i + 13] - (d[i + 1] + d[i + 7]) / 2,
            d[i + 14] - (d[i + 2] + d[i + 8]) / 2}; // base edge midpoint to tip
        double slantLength = Math.sqrt(dot(slant, slant));
        assertEquals(1, Math.sqrt(dot(n, n)), 1e-5);
        assertEquals(0, dot(n, edge), 1e-5);
        // The smooth cone's normal: off the flat facet's by at most 1 - cos(half a facet).
        assertEquals(0, dot(n, slant) / slantLength, 1 - Math.cos(Math.PI / 8));
        assertEquals(Math.signum(tip), Math.signum(n[2]), "faces toward the tip's side");
      }
    }
  }

  @Test
  void heightFieldCoversTheSquareWithSlopeNormals() {
    MeshBuilder b = new MeshBuilder().heightField((x, y) -> 0.5 * x, 100, 10);
    assertEquals(10 * 10 * 6, b.vertexCount());
    float[] data = b.toArray();
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double len = Math.sqrt(0.25 + 1);
    for (int v = 0; v < b.vertexCount(); v++) {
      int i = v * 6;
      minX = Math.min(minX, data[i]);
      maxX = Math.max(maxX, data[i]);
      assertEquals(0.5 * data[i], data[i + 2], 1e-3); // on the surface
      assertEquals(-0.5 / len, data[i + 3], 1e-5);   // normal of the slope z = x / 2
      assertEquals(0, data[i + 4], 1e-6);
      assertEquals(1 / len, data[i + 5], 1e-5);
    }
    assertEquals(-100, minX, 1e-4);
    assertEquals(100, maxX, 1e-4);
    assertThrows(IllegalArgumentException.class,
        () -> new MeshBuilder().heightField((x, y) -> 0, 10, 1));
  }

  @Test
  void heightFieldPutsDetailAtTheCentre() {
    float[] data = new MeshBuilder().heightField((x, y) -> 0, 1000, 20).toArray();
    TreeSet<Float> xs = new TreeSet<>();
    for (int i = 0; i < data.length; i += 6) {
      xs.add(data[i]);
    }
    Float[] at = xs.toArray(new Float[0]);
    assertEquals(21, at.length);
    double centreGap = at[11] - at[10];
    double edgeGap = at[20] - at[19];
    assertTrue(edgeGap > 5 * centreGap, "centre " + centreGap + ", edge " + edgeGap);
  }
}
