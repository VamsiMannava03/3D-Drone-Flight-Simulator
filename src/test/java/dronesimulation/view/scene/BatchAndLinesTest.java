package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BatchAndLinesTest {

  @Test
  void appendMovesVerticesAndKeepsNormalsUnit() {
    float[] cube = new MeshBuilder().box(Mat4.identity(), 1, 1, 1).toArray();
    MeshBuilder out = new MeshBuilder().append(cube,
        Mat4.mul(Mat4.translation(10, 0, 0), Mat4.scale(2, 4, 1)));
    float[] data = out.toArray();
    assertEquals(cube.length, data.length);
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    for (int i = 0; i < data.length; i += 6) {
      minX = Math.min(minX, data[i]);
      maxX = Math.max(maxX, data[i]);
      double n = Math.sqrt(data[i + 3] * data[i + 3] + data[i + 4] * data[i + 4]
          + data[i + 5] * data[i + 5]);
      assertEquals(1, n, 1e-5);
    }
    assertEquals(9, minX, 1e-5);
    assertEquals(11, maxX, 1e-5);
  }

  @Test
  void batchGroupsShapesByColour() {
    float[] cube = new MeshBuilder().box(Mat4.identity(), 1, 1, 1).toArray();
    ColourBatch batch = new ColourBatch()
        .add(0xFF0000, cube, Mat4.identity())
        .add(0x00FF00, cube, Mat4.identity())
        .add(0xFF0000, cube, Mat4.translation(5, 0, 0));
    assertEquals(List.of(0xFF0000, 0x00FF00), List.copyOf(batch.meshes().keySet()));
    assertEquals(2 * cube.length / 6, batch.builder(0xFF0000).vertexCount());
  }

  @Test
  void linesBecomeCrossedRibbons() {
    MeshBuilder out = new MeshBuilder();
    Lines.segment(out, new double[] {0, 0, 0}, new double[] {10, 0, 0}, 0.2);
    assertEquals(12, out.vertexCount(), "two quads of two triangles");
    Lines.segment(out, new double[] {0, 0, 0}, new double[] {0, 0, 5}, 0.2);
    assertEquals(24, out.vertexCount(), "vertical segments work too");
    Lines.segment(out, new double[] {1, 1, 1}, new double[] {1, 1, 1}, 0.2);
    assertEquals(24, out.vertexCount(), "a zero-length segment adds nothing");
    float[] data = out.toArray();
    for (int i = 0; i < 12 * 6; i += 6) {
      assertTrue(Math.abs(data[i + 1]) <= 0.1 + 1e-6 && Math.abs(data[i + 2]) <= 0.1 + 1e-6);
    }
  }

  @Test
  void polylinesAndRings() {
    MeshBuilder out = new MeshBuilder();
    Lines.polyline(out, List.of(new double[] {0, 0, 0}, new double[] {1, 0, 0},
        new double[] {1, 1, 0}), 0.1);
    assertEquals(24, out.vertexCount());
    MeshBuilder ring = new MeshBuilder();
    Lines.ring(ring, 0, 0, 2, 5, 16, 0.1);
    assertEquals(16 * 12, ring.vertexCount());
    float[] data = ring.toArray();
    double r = Math.hypot(data[0], data[1]);
    assertEquals(5, r, 0.1);
  }
}
