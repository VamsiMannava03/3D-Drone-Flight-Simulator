package dronesimulation.view.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The scenery around the city: a height field with a river, and the trees on it. Used only by
 * the 3D view.
 *
 * <p>The physics ground is a flat plane at z = 0, so everywhere drones fly the landscape is flat
 * too: inside {@link #FLAT_RADIUS} the height is exactly 0, except in the river channel, which
 * dips below it. Farther out rolling hills rise, then mountains on the horizon, and the river
 * runs through a valley it has cut into them. Everything is a fixed function of position, so
 * the same landscape is drawn every run.
 */
public final class Landscape {

  /** Inside this distance from the centre the land is flat, in m. */
  public static final double FLAT_RADIUS = 420;
  /** Height of the river surface, in m; just below the ground so the banks show. */
  public static final double WATER_LEVEL = -0.6;
  /** Depth of the river bed, in m. */
  public static final double RIVER_DEPTH = 3.2;
  /** Distance from the river's centre line where the bed starts to rise, in m. */
  public static final double RIVER_BED_HALF_WIDTH = 6;
  /** Distance from the river's centre line where the banks reach the ground, in m. */
  public static final double RIVER_BANK = 17;
  /**
   * Distance from the river's centre line where the banks meet the water, in m. Inside
   * {@link #RIVER_BANK} the land is the channel alone, even in the hills, so this is the same
   * all along the river, and shaders can draw an exact shoreline from it.
   */
  public static final double WATERLINE = waterline();

  // The river's centre line: x = RIVER_X + BEND_A sin(y / BEND_A_LENGTH)
  //                                + BEND_B sin(y / BEND_B_LENGTH + BEND_B_PHASE).
  private static final double RIVER_X = 115;
  private static final double BEND_A = 28;
  private static final double BEND_A_LENGTH = 95;
  private static final double BEND_B = 12;
  private static final double BEND_B_LENGTH = 37;
  private static final double BEND_B_PHASE = 1;

  private static final double HILLS_FULL = 1100;
  private static final double MOUNTAINS_START = 900;
  private static final double MOUNTAINS_FULL = 1900;
  private static final double HILL_HEIGHT = 70;
  private static final double MOUNTAIN_HEIGHT = 330;
  private static final double VALLEY_WIDTH = 220;

  private Landscape() {
  }

  /**
   * Returns the x of the river's centre line at a given y. The river runs roughly north to
   * south, east of the city, with gentle bends.
   *
   * @param y the y, in m
   * @return the x of the centre line, in m
   */
  public static double riverCentreX(double y) {
    return RIVER_X + BEND_A * Math.sin(y / BEND_A_LENGTH)
        + BEND_B * Math.sin(y / BEND_B_LENGTH + BEND_B_PHASE);
  }

  /**
   * Builds the river's water surface: a flat ribbon at {@link #WATER_LEVEL} following the
   * centre line, wider than the river so the terrain's banks cut out the shoreline.
   *
   * @param halfLength how far it runs along y each way from the centre, in m
   * @param step the length of each piece, in m
   * @return the mesh
   */
  static MeshBuilder water(double halfLength, double step) {
    MeshBuilder b = new MeshBuilder();
    double half = RIVER_BANK + 8;
    double[] up = {0, 0, 1};
    for (double y = -halfLength; y < halfLength; y += step) {
      double y1 = Math.min(halfLength, y + step);
      double x0 = riverCentreX(y);
      double x1 = riverCentreX(y1);
      b.face(new double[][] {
          {x0 - half, y, WATER_LEVEL}, {x0 + half, y, WATER_LEVEL},
          {x1 + half, y1, WATER_LEVEL}, {x1 - half, y1, WATER_LEVEL}}, up);
    }
    return b;
  }

  /**
   * Returns the horizontal distance from a point to the river's centre line, measured along x.
   * The bends are gentle, so this is close to the true distance.
   *
   * @param x the x, in m
   * @param y the y, in m
   * @return the distance, in m
   */
  public static double riverDistance(double x, double y) {
    return Math.abs(x - riverCentreX(y));
  }

  /**
   * Returns the height of the land.
   *
   * @param x the x, in m
   * @param y the y, in m
   * @return the height, in m: 0 on the flat, below 0 in the river channel, above 0 in the hills
   */
  public static double heightAt(double x, double y) {
    double r = Math.hypot(x, y);
    double d = riverDistance(x, y);
    double land = 0;
    if (r > FLAT_RADIUS) {
      double hills = smoothstep(FLAT_RADIUS, HILLS_FULL, r)
          * (8 + HILL_HEIGHT * fbm(x / 420, y / 420, 5));
      double ridges = ridged(x / 900 + 7.3, y / 900 - 2.1, 5);
      double mountains = smoothstep(MOUNTAINS_START, MOUNTAINS_FULL, r)
          * MOUNTAIN_HEIGHT * ridges * ridges;
      land = (hills + mountains) * smoothstep(RIVER_BANK, VALLEY_WIDTH, d);
    }
    return land + channel(d);
  }

  /** The river channel's depth below the ground, as a (negative) height, at a distance d. */
  private static double channel(double d) {
    return -RIVER_DEPTH * (1 - smoothstep(RIVER_BED_HALF_WIDTH, RIVER_BANK, d));
  }

  /** Solves channel(d) = WATER_LEVEL by bisection; the channel rises steadily with d. */
  private static double waterline() {
    double lo = RIVER_BED_HALF_WIDTH;
    double hi = RIVER_BANK;
    for (int i = 0; i < 60; i++) {
      double mid = (lo + hi) / 2;
      if (channel(mid) < WATER_LEVEL) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    return (lo + hi) / 2;
  }

  /** One tree: where it stands, how tall it is, and whether it is a conifer. */
  record Tree(double x, double y, double z, double height, boolean conifer) {
  }

  /** The forest as three meshes, so it draws in three calls, one per colour. */
  record Forest(MeshBuilder trunks, MeshBuilder broadleaf, MeshBuilder conifers) {
  }

  /** No trees this close to the centre, so the city and its flying area stay open, in m. */
  static final double TREE_FREE_RADIUS = 160;
  /** No trees this close to the fixed-wing's runway (y = -5) or landing approach (y = -60). */
  static final double TREE_FREE_LANE = 35;

  /**
   * Places the trees: broadleaf trees along the river banks, conifer forests in patches on the
   * hills. None stand in the city, on the fixed-wing's runway and approach, in the water, on
   * steep rock or above the snow line. The same trees are placed every run.
   *
   * @return the trees
   */
  static List<Tree> trees() {
    List<Tree> trees = new ArrayList<>();
    Random random = new Random(7);
    // Riverside: a jittered 9 m grid in the 45 m strips beside both banks.
    for (double y = -1600; y < 1600; y += 9) {
      for (int side = -1; side <= 1; side += 2) {
        for (double off = RIVER_BANK + 4; off < RIVER_BANK + 49; off += 9) {
          if (random.nextDouble() > 0.45 - (off - RIVER_BANK) * 0.006) {
            continue; // thinner away from the water
          }
          double ty = y + random.nextDouble() * 9;
          double tx = riverCentreX(ty) + side * (off + random.nextDouble() * 9);
          addTree(trees, tx, ty, 7 + random.nextDouble() * 6, false);
        }
      }
    }
    // Hills: a jittered 24 m grid, kept where a large-scale noise says "forest".
    for (double x = -1800; x < 1800; x += 24) {
      for (double y = -1800; y < 1800; y += 24) {
        double tx = x + random.nextDouble() * 24;
        double ty = y + random.nextDouble() * 24;
        double r = Math.hypot(tx, ty);
        if (r < FLAT_RADIUS + 40 || r > 1800 || fbm(tx / 300 + 40, ty / 300 - 13, 3) < 0.56) {
          continue;
        }
        addTree(trees, tx, ty, 10 + random.nextDouble() * 8, true);
      }
    }
    return trees;
  }

  private static void addTree(List<Tree> trees, double x, double y, double height,
      boolean conifer) {
    if (Math.hypot(x, y) < TREE_FREE_RADIUS || riverDistance(x, y) < RIVER_BANK + 2
        || (x < 0 && x > -360 && (Math.abs(y + 5) < TREE_FREE_LANE
            || Math.abs(y + 60) < TREE_FREE_LANE))) {
      return;
    }
    double z = heightAt(x, y);
    double slope = Math.hypot(heightAt(x + 2, y) - heightAt(x - 2, y),
        heightAt(x, y + 2) - heightAt(x, y - 2)) / 4;
    if (z < 0 || z > 170 || slope > 0.6) {
      return;
    }
    trees.add(new Tree(x, y, z, height, conifer));
  }

  /**
   * Builds the meshes for all {@link #trees()}. A conifer is two stacked cones, a broadleaf
   * tree a wide double cone, each on a short trunk. Low-poly, so thousands draw cheaply.
   *
   * @return the trunks and crowns
   */
  static Forest forest() {
    MeshBuilder trunks = new MeshBuilder();
    MeshBuilder broadleaf = new MeshBuilder();
    MeshBuilder conifers = new MeshBuilder();
    for (Tree t : trees()) {
      double h = t.height();
      // Sink the trunk a little so it never floats on a slope.
      float[] at = Mat4.translation(t.x(), t.y(), t.z() - 0.5);
      trunks.cylinder(at, h * 0.035, 0, h * 0.35, 5, false);
      if (t.conifer()) {
        conifers.cone(at, h * 0.26, h * 0.22, h * 0.78, 7);
        conifers.cone(at, h * 0.19, h * 0.5, h + 0.5, 7);
      } else {
        broadleaf.cone(at, h * 0.32, h * 0.55, h + 0.5, 7);
        broadleaf.cone(at, h * 0.32, h * 0.55, h * 0.28, 7);
      }
    }
    return new Forest(trunks, broadleaf, conifers);
  }

  /** Fractal value noise in about [0, 1]. */
  private static double fbm(double x, double y, int octaves) {
    double sum = 0;
    double amplitude = 0.5;
    double norm = 0;
    for (int i = 0; i < octaves; i++) {
      sum += amplitude * noise(x, y);
      norm += amplitude;
      x = x * 2.03 + 17.1;
      y = y * 2.03 - 9.7;
      amplitude *= 0.5;
    }
    return sum / norm;
  }

  /** Ridged fractal noise in about [0, 1]: sharp crests, like mountain ranges. */
  private static double ridged(double x, double y, int octaves) {
    double sum = 0;
    double amplitude = 0.5;
    double norm = 0;
    for (int i = 0; i < octaves; i++) {
      double n = 1 - Math.abs(2 * noise(x, y) - 1);
      sum += amplitude * n * n;
      norm += amplitude;
      x = x * 2.07 - 3.3;
      y = y * 2.07 + 5.9;
      amplitude *= 0.5;
    }
    return sum / norm;
  }

  /** Smooth value noise in [0, 1] on a unit grid. */
  private static double noise(double x, double y) {
    double fx = Math.floor(x);
    double fy = Math.floor(y);
    int ix = (int) fx;
    int iy = (int) fy;
    double tx = fade(x - fx);
    double ty = fade(y - fy);
    double a = lerp(hash(ix, iy), hash(ix + 1, iy), tx);
    double b = lerp(hash(ix, iy + 1), hash(ix + 1, iy + 1), tx);
    return lerp(a, b, ty);
  }

  /** A repeatable pseudo-random value in [0, 1) for a grid point. */
  private static double hash(int x, int y) {
    int h = x * 374_761_393 + y * 668_265_263;
    h = (h ^ (h >>> 13)) * 1_274_126_177;
    h ^= h >>> 16;
    return (h & 0x7fff_ffff) / (double) 0x8000_0000L;
  }

  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }

  /**
   * Hermite step from 0 at {@code edge0} to 1 at {@code edge1}, exactly 0 below and 1 above.
   *
   * @param edge0 where the step starts
   * @param edge1 where the step ends
   * @param v the value
   * @return the step, in [0, 1]
   */
  static double smoothstep(double edge0, double edge1, double v) {
    double t = Math.max(0, Math.min(1, (v - edge0) / (edge1 - edge0)));
    return t * t * (3 - 2 * t);
  }
}
