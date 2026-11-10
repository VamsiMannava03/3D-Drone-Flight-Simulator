package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraFrameTest {

  @Test
  void lookingNorthHasEastOnTheRightAndUpOnTop() {
    CameraFrame frame = new CameraFrame(new double[] {0, -10, 0}, new double[] {0, 0, 0});
    float[] m = frame.matrix();
    assertArrayEquals(new float[] {1, 0, 0}, new float[] {m[0], m[1], m[2]}, 1e-6f);
    assertArrayEquals(new float[] {0, 0, -1}, new float[] {m[4], m[5], m[6]}, 1e-6f);
    assertArrayEquals(new float[] {0, 1, 0}, new float[] {m[8], m[9], m[10]}, 1e-6f);
    assertEquals(-10, m[13], 1e-6);
  }

  @Test
  void projectsTheTargetToTheCentreAndHigherPointsUp() {
    CameraFrame frame = new CameraFrame(new double[] {0, -10, 0}, new double[] {0, 0, 0});
    assertArrayEquals(new double[] {400, 300},
        frame.project(new double[] {0, 0, 0}, 60, 800, 600), 1e-9);
    double[] east = frame.project(new double[] {1, 0, 0}, 60, 800, 600);
    double[] up = frame.project(new double[] {0, 0, 1}, 60, 800, 600);
    assertTrue(east[0] > 400);
    assertTrue(up[1] < 300);
    assertNull(frame.project(new double[] {0, -20, 0}, 60, 800, 600), "behind the camera");
  }

  @Test
  void straightDownStillHasAFrame() {
    CameraFrame frame = new CameraFrame(new double[] {0, 0, 10}, new double[] {0, 0, 0});
    float[] m = frame.matrix();
    assertEquals(-1, m[10], 1e-6);
    assertEquals(1, m[0], 1e-6);
  }
}
