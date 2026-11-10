package dronesimulation.view.scene;

import dronesimulation.model.world.Building;
import java.util.List;

/**
 * How the 3D view builds one house, so the neighbourhood is not a row of clones: which way it
 * faces, its roof type, and whether it has an attached garage, a front porch and a chimney.
 * Everything follows from the house's name and the streets around it, so a house looks the
 * same every run and across resets. Pure geometry, so it is tested without a GPU.
 *
 * <p>The house body and the garage stay inside the obstacle's footprint, which is what drones
 * collide with. Only the porch stands in front of it, in the front yard.
 *
 * @param house the obstacle being drawn
 * @param frontX the x of the unit vector the front door faces: -1, 0 or 1
 * @param frontY the y of the unit vector the front door faces: -1, 0 or 1
 * @param roof the main roof's type
 * @param main the main body's footprint {x0, y0, x1, y1}
 * @param garage the attached garage's footprint {x0, y0, x1, y1}, or null for none
 * @param porch the porch's footprint {x0, y0, x1, y1} in front of the door, or null for none
 * @param chimney whether the main roof has a chimney
 * @param look a number from the name for picking colours
 */
record HousePlan(Building house, int frontX, int frontY, Roof roof, double[] main,
    double[] garage, double[] porch, boolean chimney, int look) {

  /** The shape of the main roof. */
  enum Roof {
    /** Ridge parallel to the street; the slopes face the street and the back garden. */
    SIDE_GABLE,
    /** Ridge running back from the street; the gable end faces the street. */
    FRONT_GABLE,
    /** Four slopes: a short ridge along the longer side, sloped ends instead of gables. */
    HIP
  }

  /** Eaves height of a garage, in m: one storey. */
  static final double GARAGE_EAVES = 2.8;
  /** How tall a garage's roof is, in m. */
  static final double GARAGE_ROOF = 1.1;
  /** Share of the frontage a garage takes. */
  static final double GARAGE_SHARE = 0.36;
  /** How far a porch stands out from the front wall, in m; less than the front yard. */
  static final double PORCH_DEPTH = 1.3;
  /** How wide a porch is, along the front wall, in m. */
  static final double PORCH_WIDTH = 2.6;
  /** Height of a porch's roof, in m. */
  static final double PORCH_HEIGHT = 2.6;
  /** A street farther than this from a house's wall is not the street it faces, in m. */
  private static final double FACES_STREET_WITHIN = 6;

  /**
   * Plans a house.
   *
   * @param house a house obstacle
   * @param streets the streets around it, for which way it faces
   * @return the plan
   */
  static HousePlan of(Building house, List<Neighbourhood.Street> streets) {
    double x0 = house.min().x();
    double y0 = house.min().y();
    double x1 = house.max().x();
    double y1 = house.max().y();
    int[] front = facing(x0, y0, x1, y1, streets);
    // Names like "Elm St 1" and "Elm St 2" have nearly the same hashCode, so mix it well and
    // take each feature from different bits; otherwise neighbours would follow a pattern.
    int look = mix(house.name().hashCode()) & 0x7fff_ffff;
    Roof roof = Roof.values()[(look >>> 3) % 3];
    boolean facesY = front[1] != 0; // the front wall runs along x
    double frontage = facesY ? x1 - x0 : y1 - y0;
    boolean hasGarage = ((look >>> 9) & 3) < 2 && frontage >= 9;
    boolean hasPorch = ((look >>> 12) & 3) < 2;
    boolean chimney = ((look >>> 15) % 5) < 2;

    double[] main = {x0, y0, x1, y1};
    double[] garage = null;
    if (hasGarage) {
      double gw = frontage * GARAGE_SHARE;
      boolean onLowSide = ((look >>> 18) & 1) == 0; // which end of the frontage
      // The garage is flush with the front wall and a little shallower than the house.
      double depthAll = facesY ? y1 - y0 : x1 - x0;
      double gd = Math.max(5, depthAll - 1.5);
      if (facesY) {
        double gx0 = onLowSide ? x0 : x1 - gw;
        double gy0 = front[1] < 0 ? y0 : y1 - gd;
        garage = new double[] {gx0, gy0, gx0 + gw, gy0 + gd};
        main = onLowSide ? new double[] {x0 + gw, y0, x1, y1}
            : new double[] {x0, y0, x1 - gw, y1};
      } else {
        double gy0 = onLowSide ? y0 : y1 - gw;
        double gx0 = front[0] < 0 ? x0 : x1 - gd;
        garage = new double[] {gx0, gy0, gx0 + gd, gy0 + gw};
        main = onLowSide ? new double[] {x0, y0 + gw, x1, y1}
            : new double[] {x0, y0, x1, y1 - gw};
      }
    }
    double[] porch = null;
    if (hasPorch) {
      double cx = (main[0] + main[2]) / 2;
      double cy = (main[1] + main[3]) / 2;
      double half = PORCH_WIDTH / 2;
      porch = switch (front[0] * 10 + front[1]) {
        case -1 -> new double[] {cx - half, y0 - PORCH_DEPTH, cx + half, y0};
        case 1 -> new double[] {cx - half, y1, cx + half, y1 + PORCH_DEPTH};
        case -10 -> new double[] {x0 - PORCH_DEPTH, cy - half, x0, cy + half};
        default -> new double[] {x1, cy - half, x1 + PORCH_DEPTH, cy + half};
      };
    }
    return new HousePlan(house, front[0], front[1], roof, main, garage, porch, chimney, look);
  }

  /** A well-mixed 32-bit hash (the "lowbias32" finaliser): nearby inputs come out unrelated. */
  private static int mix(int h) {
    h ^= h >>> 16;
    h *= 0x7feb352d;
    h ^= h >>> 15;
    h *= 0x846ca68b;
    h ^= h >>> 16;
    return h;
  }

  /** The way a footprint faces: toward the nearest street, else toward the town centre. */
  private static int[] facing(double x0, double y0, double x1, double y1,
      List<Neighbourhood.Street> streets) {
    int[] best = null;
    double bestGap = FACES_STREET_WITHIN;
    for (Neighbourhood.Street s : streets) {
      boolean besideX = s.x0() < x1 && s.x1() > x0; // street level with the house along x
      boolean besideY = s.y0() < y1 && s.y1() > y0;
      double[][] candidates = {
          {besideX ? s.y0() - y1 : -1, 0, 1}, {besideX ? y0 - s.y1() : -1, 0, -1},
          {besideY ? s.x0() - x1 : -1, 1, 0}, {besideY ? x0 - s.x1() : -1, -1, 0}};
      for (double[] c : candidates) {
        if (c[0] >= 0 && c[0] <= bestGap) {
          bestGap = c[0];
          best = new int[] {(int) c[1], (int) c[2]};
        }
      }
    }
    if (best != null) {
      return best;
    }
    // No street: the longer side faces the middle of town.
    double cx = (x0 + x1) / 2;
    double cy = (y0 + y1) / 2;
    return x1 - x0 >= y1 - y0 ? new int[] {0, cy > 0 ? -1 : 1} : new int[] {cx > 0 ? -1 : 1, 0};
  }

  /**
   * Picks a wall colour.
   *
   * @param count how many colours there are to pick from
   * @return an index from 0 to count - 1
   */
  int wallColour(int count) {
    return (look >>> 20) % count;
  }

  /**
   * Picks a roof colour, independently of the walls.
   *
   * @param count how many colours there are to pick from
   * @return an index from 0 to count - 1
   */
  int roofColour(int count) {
    return (look >>> 25) % count;
  }

  /**
   * Returns the eaves height of the main body.
   *
   * @return the height in m
   */
  double eaves() {
    return house.eavesHeight();
  }

  /**
   * Tells whether the main roof's ridge runs along x.
   *
   * @return true if along x, false if along y
   */
  boolean ridgeAlongX() {
    boolean frontWallAlongX = frontY != 0;
    return switch (roof) {
      case SIDE_GABLE -> frontWallAlongX;
      case FRONT_GABLE -> !frontWallAlongX;
      case HIP -> main[2] - main[0] >= main[3] - main[1];
    };
  }
}
