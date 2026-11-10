package dronesimulation.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Bounds2DTest {

  private static final double EPS = 1e-9;

  @Test
  void includeGrowsToCoverPoints() {
    Bounds2D b = Bounds2D.around(1, 2).include(-3, 5).include(4, 0);
    assertEquals(new Bounds2D(-3, 4, 0, 5), b);
  }

  @Test
  void paddedEnforcesMinimumSpanAboutCentre() {
    Bounds2D b = Bounds2D.around(10, 10).padded(4, 0);
    assertEquals(new Bounds2D(8, 12, 8, 12), b);
  }

  @Test
  void paddedAddsMarginOnEachSide() {
    Bounds2D b = new Bounds2D(0, 10, 0, 20).padded(1, 0.1);
    assertEquals(-1, b.minH(), EPS);
    assertEquals(11, b.maxH(), EPS);
    assertEquals(-2, b.minV(), EPS);
    assertEquals(22, b.maxV(), EPS);
  }

  @Test
  void withFloorShiftsUpKeepingSpan() {
    Bounds2D b = new Bounds2D(0, 1, -2, 8).withFloor(-1);
    assertEquals(new Bounds2D(0, 1, -1, 9), b);
    Bounds2D above = new Bounds2D(0, 1, 3, 8);
    assertEquals(above, above.withFloor(-1));
  }

  @Test
  void niceStepPicksOneTwoOrFive() {
    assertEquals(2, Bounds2D.niceStep(20, 10), EPS);
    assertEquals(5, Bounds2D.niceStep(40, 8), EPS);
    assertEquals(10, Bounds2D.niceStep(80, 8), EPS);
    assertEquals(0.5, Bounds2D.niceStep(4, 8), EPS);
  }

  @Test
  void rejectsInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> new Bounds2D(1, 0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> Bounds2D.niceStep(0, 5));
  }
}
