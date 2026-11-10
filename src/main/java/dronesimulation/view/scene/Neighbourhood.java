package dronesimulation.view.scene;

import dronesimulation.model.world.Building;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lays streets for the 3D view between houses that face each other. Streets are scenery: the
 * simulation knows only the houses, so the view works out where the streets must be. Wherever
 * two houses stand opposite each other across a gap a street fits in, a street runs between
 * them, and stretches of street in line with each other join into one.
 */
final class Neighbourhood {

  /** A street: an axis-aligned rectangle on the ground, in m. */
  record Street(double x0, double y0, double x1, double y1) {

    /**
     * Tells whether the street runs along x rather than along y.
     *
     * @return true if it is longer along x
     */
    boolean alongX() {
      return x1 - x0 >= y1 - y0;
    }
  }

  /** Front yard left between a house and the street, in m; room for a porch and a path. */
  static final double YARD = 2;
  /** Narrowest and widest gap between two facing houses that holds a street, in m. */
  static final double MIN_GAP = 6;
  static final double MAX_GAP = 24;
  /** How far apart two facing houses' centres may be along the street, in m. */
  private static final double ALIGN = 2;
  /** Stretches of street in line, with ends at most this far apart, join into one, in m. */
  private static final double JOIN = 15;
  /** How far a street runs past its last houses, in m. */
  static final double RUN_OUT = 4;

  private Neighbourhood() {
  }

  /**
   * Returns the streets between the houses among some obstacles.
   *
   * @param obstacles the obstacles; only {@link Building.Style#HOUSE houses} count
   * @return the streets, possibly none
   */
  static List<Street> streets(List<Building> obstacles) {
    List<double[]> houses = new ArrayList<>(); // footprints {x0, y0, x1, y1}
    for (Building o : obstacles) {
      if (o.style() == Building.Style.HOUSE) {
        houses.add(new double[] {o.min().x(), o.min().y(), o.max().x(), o.max().y()});
      }
    }
    List<Street> streets = new ArrayList<>(streetsAlongX(houses));
    // Streets along y are streets along x with the axes swapped.
    for (Street s : streetsAlongX(houses.stream().map(Neighbourhood::swap).toList())) {
      streets.add(new Street(s.y0(), s.x0(), s.y1(), s.x1()));
    }
    return streets;
  }

  private static double[] swap(double[] f) {
    return new double[] {f[1], f[0], f[3], f[2]};
  }

  private static List<Street> streetsAlongX(List<double[]> houses) {
    List<double[]> pieces = new ArrayList<>();
    for (double[] north : houses) {
      for (double[] south : houses) {
        double gap = north[1] - south[3];
        double northMid = (north[0] + north[2]) / 2;
        double southMid = (south[0] + south[2]) / 2;
        if (gap >= MIN_GAP && gap <= MAX_GAP && Math.abs(northMid - southMid) <= ALIGN) {
          pieces.add(new double[] {Math.min(north[0], south[0]), south[3] + YARD,
              Math.max(north[2], south[2]), north[1] - YARD});
        }
      }
    }
    pieces.sort(Comparator.<double[]>comparingDouble(p -> p[1]).thenComparingDouble(p -> p[0]));
    List<Street> streets = new ArrayList<>();
    double[] open = null;
    for (double[] p : pieces) {
      boolean inLine = open != null && Math.abs(p[1] - open[1]) < 0.5
          && Math.abs(p[3] - open[3]) < 0.5 && p[0] <= open[2] + JOIN;
      if (inLine) {
        open[2] = Math.max(open[2], p[2]);
        open[1] = Math.max(open[1], p[1]); // stay clear of every house on the way
        open[3] = Math.min(open[3], p[3]);
      } else {
        addStreet(streets, open, houses);
        open = p.clone();
      }
    }
    addStreet(streets, open, houses);
    return streets;
  }

  /** Adds a finished street, run out past its ends, unless it would cross a house. */
  private static void addStreet(List<Street> streets, double[] s, List<double[]> houses) {
    if (s == null) {
      return;
    }
    Street street = new Street(s[0] - RUN_OUT, s[1], s[2] + RUN_OUT, s[3]);
    for (double[] h : houses) {
      if (h[0] < street.x1() && h[2] > street.x0() && h[1] < street.y1() && h[3] > street.y0()) {
        return;
      }
    }
    streets.add(street);
  }
}
