package dronesimulation.view.scene;

import java.util.ArrayList;
import java.util.List;

/**
 * The garden details around a house, for the 3D view: a path from the front door to the
 * pavement, a driveway in front of the garage, shrubs and a mailbox out front, and a back
 * garden with a picket fence and a tree. They are scenery, not obstacles, so they are kept low
 * and out of the street, and the demo missions are checked to fly well above them.
 */
final class Yard {

  /** What a garden item is. */
  enum Kind {
    /** A stone path, flat on the ground. */
    PATH,
    /** A concrete driveway, flat on the ground. */
    DRIVEWAY,
    /** One side of a picket fence: a thin strip. */
    FENCE,
    /** A round shrub. */
    SHRUB,
    /** A garden tree. */
    TREE,
    /** A mailbox on a post. */
    MAILBOX
  }

  /**
   * One garden item: what it is, its footprint and how tall it is.
   *
   * @param kind what it is
   * @param x0 the footprint's smallest x, in m
   * @param y0 the footprint's smallest y, in m
   * @param x1 the footprint's largest x, in m
   * @param y1 the footprint's largest y, in m
   * @param height how tall it is, in m; flat items are a few centimetres
   */
  record Item(Kind kind, double x0, double y0, double x1, double y1, double height) {
  }

  /** How deep a back garden is, from the back wall, in m. */
  static final double BACK_GARDEN = 6;
  /** Height of the picket fence, in m. */
  static final double FENCE_HEIGHT = 1.0;
  /** The tallest a garden tree grows, in m. */
  static final double MAX_TREE = 6;
  /** The shortest a garden tree grows, in m; heights step by 0.1 m up to {@link #MAX_TREE}. */
  static final double MIN_TREE = 4;

  private Yard() {
  }

  /**
   * Lays out the garden of a house.
   *
   * @param p the house's plan
   * @return the garden items
   */
  static List<Item> of(HousePlan p) {
    // Work in the house's own frame: "across" runs along the front wall, "out" from the front
    // wall toward the street. Then turn each item back into world coordinates.
    Frame f = new Frame(p);
    List<Item> items = new ArrayList<>();
    double yard = Neighbourhood.YARD;
    double doorAt = f.across((p.main()[0] + p.main()[2]) / 2, (p.main()[1] + p.main()[3]) / 2);
    double pathStart = p.porch() == null ? 0 : HousePlan.PORCH_DEPTH;
    items.add(f.item(Kind.PATH, doorAt - 0.55, pathStart, doorAt + 0.55, yard, 0.03));
    double[] garage = p.garage();
    double driveFrom = Double.NaN;
    double driveTo = Double.NaN;
    if (garage != null) {
      double a = f.across(garage[0], garage[1]);
      double b = f.across(garage[2], garage[3]);
      driveFrom = Math.min(a, b) + 0.2;
      driveTo = Math.max(a, b) - 0.2;
      items.add(f.item(Kind.DRIVEWAY, driveFrom, 0, driveTo, yard, 0.03));
    }
    for (int side = -1; side <= 1; side += 2) { // a shrub each side of the door
      double at = doorAt + side * 2.0;
      boolean onDrive = garage != null && at + 0.55 > driveFrom && at - 0.55 < driveTo;
      if (!onDrive && at - 0.55 > f.left && at + 0.55 < f.right) {
        items.add(f.item(Kind.SHRUB, at - 0.5, 0.15, at + 0.5, 1.15, 1.0));
      }
    }
    items.add(f.item(Kind.MAILBOX, doorAt + 0.8, yard - 0.4, doorAt + 1.1, yard - 0.1, 1.25));

    // The back garden, fenced on its two sides and at the back.
    double back = -f.depth;
    double end = back - BACK_GARDEN;
    double t = 0.06;
    items.add(f.item(Kind.FENCE, f.left, end, f.left + t, back, FENCE_HEIGHT));
    items.add(f.item(Kind.FENCE, f.right - t, end, f.right, back, FENCE_HEIGHT));
    items.add(f.item(Kind.FENCE, f.left, end, f.right, end + t, FENCE_HEIGHT));
    double spot = ((p.look() >>> 7) % 100) / 100.0; // where along the garden the tree stands
    double treeAt = f.left + 2 + spot * (f.right - f.left - 4);
    int steps = (int) Math.round((MAX_TREE - MIN_TREE) * 10); // 0.1 m apart
    double height = MIN_TREE + ((p.look() >>> 13) % (steps + 1)) / 10.0;
    items.add(f.item(Kind.TREE, treeAt - 1.6, end + 1.2, treeAt + 1.6, end + 4.4, height));
    return items;
  }

  /** A house's own frame: across its front wall, and out from it toward the street. */
  private static final class Frame {
    private final int fx;
    private final int fy;
    private final double frontLine; // the front wall's x or y
    /** The lot's extent along the front wall, and its depth behind the front wall. */
    private final double left;
    private final double right;
    private final double depth;

    Frame(HousePlan p) {
      fx = p.frontX();
      fy = p.frontY();
      double x0 = p.house().min().x();
      double y0 = p.house().min().y();
      double x1 = p.house().max().x();
      double y1 = p.house().max().y();
      if (fy != 0) {
        frontLine = fy > 0 ? y1 : y0;
        left = x0;
        right = x1;
        depth = y1 - y0;
      } else {
        frontLine = fx > 0 ? x1 : x0;
        left = y0;
        right = y1;
        depth = x1 - x0;
      }
    }

    /** The across coordinate of a world point. */
    double across(double x, double y) {
      return fy != 0 ? x : y;
    }

    /** Builds an item from across and out ranges in this frame. */
    Item item(Kind kind, double a0, double o0, double a1, double o1, double height) {
      int dir = fy != 0 ? fy : fx;
      double w0 = frontLine + dir * o0;
      double w1 = frontLine + dir * o1;
      double lo = Math.min(w0, w1);
      double hi = Math.max(w0, w1);
      return fy != 0 ? new Item(kind, a0, lo, a1, hi, height)
          : new Item(kind, lo, a0, hi, a1, height);
    }
  }
}
