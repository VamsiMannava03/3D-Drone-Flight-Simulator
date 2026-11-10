package dronesimulation.view.scene;

import dronesimulation.model.Vector3D;
import dronesimulation.model.obstacles.BoxObstacle;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.world.Building;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the still parts of the 3D scene as meshes, one per colour: the landscape with its
 * river and forest, the streets and gardens, and every building. Nothing here moves, so it is
 * built once per set of obstacles.
 */
public final class TownBuilder {

  /** Half the side of the square of land drawn, in m. */
  public static final double TERRAIN_HALF_SIZE = 3000;
  /** Colour of water, drawn see-through. */
  public static final int WATER = 0x3E7FA8;

  static final int GRASS = 0x6F9B4E;
  static final int FIELD = 0x80A85B;
  static final int SAND = 0xC9B98C;
  static final int ROCK = 0x8D8A80;
  static final int SNOW = 0xEEF1F4;
  static final int ROAD = 0x4A4D53;
  static final int TRUNK = 0x5B4632;
  static final int LEAF = 0x4E7A34;
  static final int CONIFER = 0x2F5530;
  static final int[] BUILDING_COLOURS = {0xC9C4BA, 0xB4BAC2, 0xD3C7B1, 0xA9B0A6, 0xC2B8C4};
  static final int[] HOUSE_WALLS = {0xE9DEC4, 0xBCCCD8, 0xC9D3B2, 0xF2EFE8, 0xEBD9A2, 0xD9B9A3};
  static final int[] HOUSE_ROOFS = {0x4C5159, 0x9A4B33, 0x5D6673, 0x6E4C3B};

  private static final float[] CUBE =
      new MeshBuilder().box(Mat4.translation(0, 0, 0.5), 1, 1, 1).toArray();
  private static final float[] GABLE_ROOF = new MeshBuilder().gableRoof().toArray();
  private static final float[] HIP_ROOF = new MeshBuilder().hipRoof(0.25).toArray();
  private static final float[] GABLE_ENDS = new MeshBuilder().gableEnds().toArray();
  private static final float[] CROWN = new MeshBuilder().cone(Mat4.identity(), 0.5, 0.45, 1, 8)
      .cone(Mat4.identity(), 0.5, 0.45, 0, 8).toArray();
  private static final float[] STEM =
      new MeshBuilder().cylinder(Mat4.identity(), 0.5, 0, 1, 6, false).toArray();
  private static final float[] STREET = new MeshBuilder().plane(Mat4.identity(), 1, 1)
      .toArray();

  private TownBuilder() {
    // static builders only
  }

  /**
   * Builds the landscape: the ground coloured by height, and the forest. It does not depend on
   * the world, so it can be built once.
   *
   * @param cells how many grid cells along each side of the ground
   * @return the meshes by colour
   */
  public static ColourBatch landscape(int cells) {
    ColourBatch batch = new ColourBatch();
    float[] ground = new MeshBuilder()
        .heightField(Landscape::heightAt, TERRAIN_HALF_SIZE, cells).toArray();
    float[] identity = Mat4.identity();
    for (int i = 0; i + 18 <= ground.length; i += 18) {
      float[] triangle = Arrays.copyOfRange(ground, i, i + 18);
      double x = (triangle[0] + triangle[6] + triangle[12]) / 3;
      double y = (triangle[1] + triangle[7] + triangle[13]) / 3;
      double z = (triangle[2] + triangle[8] + triangle[14]) / 3;
      batch.add(groundColour(x, y, z), triangle, identity);
    }
    Landscape.Forest forest = Landscape.forest();
    batch.add(TRUNK, forest.trunks().toArray(), identity);
    batch.add(LEAF, forest.broadleaf().toArray(), identity);
    batch.add(CONIFER, forest.conifers().toArray(), identity);
    return batch;
  }

  /**
   * Builds the river's water surface.
   *
   * @return the water mesh
   */
  public static MeshBuilder water() {
    return Landscape.water(TERRAIN_HALF_SIZE, 8);
  }

  /**
   * Picks the ground colour for a spot.
   *
   * @param x the x in m
   * @param y the y in m
   * @param z the ground height in m
   * @return the colour as 0xRRGGBB
   */
  static int groundColour(double x, double y, double z) {
    if (Landscape.riverDistance(x, y) < Landscape.RIVER_BANK + 1 && z < 0.5) {
      return SAND;
    }
    if (z > 230) {
      return SNOW;
    }
    if (z > 110) {
      return ROCK;
    }
    return Math.hypot(x, y) < Landscape.FLAT_RADIUS ? FIELD : GRASS;
  }

  /**
   * Builds the town: streets between the houses, gardens, and every obstacle.
   *
   * @param obstacles the world's obstacles
   * @return the meshes by colour
   */
  public static ColourBatch town(List<? extends Obstacle> obstacles) {
    ColourBatch batch = new ColourBatch();
    List<Building> buildings = Scenery.buildings(obstacles);
    List<Neighbourhood.Street> streets = Neighbourhood.streets(buildings);
    for (Neighbourhood.Street st : streets) {
      batch.add(ROAD, STREET, footprint(st.x0(), st.y0(), st.x1(), st.y1(), 0.03, 1));
    }
    List<Yard.Item> yards = new ArrayList<>();
    int index = 0;
    for (Obstacle o : obstacles) {
      if (o instanceof Building b && b.style() == Building.Style.HOUSE) {
        HousePlan plan = HousePlan.of(b, streets);
        house(batch, plan);
        yards.addAll(Yard.of(plan));
      } else if (o instanceof Building b) {
        box(batch, b.min(), b.max(), BUILDING_COLOURS[index % BUILDING_COLOURS.length]);
      } else if (o instanceof BoxObstacle b) {
        box(batch, b.getMin(), b.getMax(), BUILDING_COLOURS[index % BUILDING_COLOURS.length]);
      }
      index++;
    }
    for (Yard.Item item : yards) {
      gardenItem(batch, item);
    }
    return batch;
  }

  private static void box(ColourBatch batch, Vector3D min, Vector3D max, int colour) {
    batch.add(colour, CUBE, footprint(min.x(), min.y(), max.x(), max.y(), min.z(),
        max.z() - min.z()));
  }

  private static void house(ColourBatch batch, HousePlan p) {
    double top = p.house().max().z();
    double eaves = p.eaves();
    int wall = HOUSE_WALLS[p.wallColour(HOUSE_WALLS.length)];
    int roof = HOUSE_ROOFS[p.roofColour(HOUSE_ROOFS.length)];
    double[] m = p.main();
    float[] at = body(batch, m, eaves, top - eaves, p.ridgeAlongX(), p.roof(), wall, roof);
    if (p.chimney()) {
      boolean alongX = p.ridgeAlongX();
      double length = alongX ? m[2] - m[0] : m[3] - m[1];
      double across = alongX ? m[3] - m[1] : m[2] - m[0];
      double roofHeight = top - eaves;
      double base = roofHeight * 0.45;
      batch.add(0x8E4B3B, CUBE, Mat4.mul(at, Mat4.translation(length * 0.28, across * 0.2, base),
          Mat4.scale(0.8, 0.8, roofHeight + 0.9 - base)));
    }
    if (p.garage() != null) {
      body(batch, p.garage(), HousePlan.GARAGE_EAVES, HousePlan.GARAGE_ROOF, p.frontY() != 0,
          HousePlan.Roof.SIDE_GABLE, wall, roof);
    }
    if (p.porch() != null) {
      porch(batch, p);
    }
  }

  private static float[] body(ColourBatch batch, double[] f, double eaves, double roofHeight,
      boolean ridgeAlongX, HousePlan.Roof type, int wall, int roof) {
    double w = f[2] - f[0];
    double d = f[3] - f[1];
    double cx = (f[0] + f[2]) / 2;
    double cy = (f[1] + f[3]) / 2;
    batch.add(wall, CUBE, footprint(f[0], f[1], f[2], f[3], 0, eaves));
    double length = ridgeAlongX ? w : d;
    double across = ridgeAlongX ? d : w;
    float[] at = Mat4.mul(Mat4.translation(cx, cy, eaves),
        Mat4.rotationZ(ridgeAlongX ? 0 : Math.PI / 2));
    batch.add(roof, type == HousePlan.Roof.HIP ? HIP_ROOF : GABLE_ROOF,
        Mat4.mul(at, Mat4.scale(length + 0.6, across + 0.9, roofHeight)));
    if (type != HousePlan.Roof.HIP) {
      batch.add(wall, GABLE_ENDS, Mat4.mul(at, Mat4.scale(length, across, roofHeight)));
    }
    return at;
  }

  private static void gardenItem(ColourBatch batch, Yard.Item i) {
    double w = i.x1() - i.x0();
    double d = i.y1() - i.y0();
    double cx = (i.x0() + i.x1()) / 2;
    double cy = (i.y0() + i.y1()) / 2;
    float[] at = Mat4.translation(cx, cy, 0);
    float[] fitted = footprint(i.x0(), i.y0(), i.x1(), i.y1(), 0, i.height());
    switch (i.kind()) {
      case PATH -> batch.add(0xC9C1AF, CUBE, fitted);
      case DRIVEWAY -> batch.add(0xAFADA7, CUBE, fitted);
      case FENCE -> batch.add(0xF3F1EA, CUBE, fitted);
      case SHRUB -> batch.add(0x46702F, CROWN, fitted);
      case TREE -> {
        double h = i.height();
        batch.add(TRUNK, STEM, Mat4.mul(at, Mat4.scale(0.28, 0.28, h * 0.45)));
        batch.add(LEAF, CROWN, Mat4.mul(Mat4.translation(cx, cy, h * 0.3),
            Mat4.scale(w, d, h * 0.7)));
      }
      case MAILBOX -> {
        batch.add(0x3B3B3B, CUBE, Mat4.mul(at, Mat4.scale(0.08, 0.08, i.height() - 0.25)));
        batch.add(0x2F5D8A, CUBE, Mat4.mul(Mat4.translation(cx, cy, i.height() - 0.25),
            Mat4.scale(w, d, 0.25)));
      }
      default -> {
        // nothing else grows in a garden
      }
    }
  }

  private static void porch(ColourBatch batch, HousePlan p) {
    double[] r = p.porch();
    double w = r[2] - r[0];
    double d = r[3] - r[1];
    double cx = (r[0] + r[2]) / 2;
    double cy = (r[1] + r[3]) / 2;
    int white = 0xF1F0EC;
    batch.add(0xB9B4AA, CUBE, Mat4.mul(Mat4.translation(cx, cy, 0), Mat4.scale(w, d, 0.15)));
    batch.add(white, CUBE, Mat4.mul(Mat4.translation(cx, cy, HousePlan.PORCH_HEIGHT),
        Mat4.scale(w + 0.2, d + 0.2, 0.16)));
    double edgeX = p.frontX() < 0 ? r[0] + 0.15 : p.frontX() > 0 ? r[2] - 0.15 : 0;
    double edgeY = p.frontY() < 0 ? r[1] + 0.15 : p.frontY() > 0 ? r[3] - 0.15 : 0;
    for (int side = -1; side <= 1; side += 2) {
      double px = p.frontX() != 0 ? edgeX : cx + side * (w / 2 - 0.15);
      double py = p.frontY() != 0 ? edgeY : cy + side * (d / 2 - 0.15);
      batch.add(white, CUBE, Mat4.mul(Mat4.translation(px, py, 0),
          Mat4.scale(0.16, 0.16, HousePlan.PORCH_HEIGHT)));
    }
  }

  /**
   * Returns the transform that stretches a unit cube standing on its base over a rectangle.
   *
   * @param x0 the smallest x
   * @param y0 the smallest y
   * @param x1 the largest x
   * @param y1 the largest y
   * @param z the base height
   * @param h the height
   * @return the transform
   */
  static float[] footprint(double x0, double y0, double x1, double y1, double z, double h) {
    return Mat4.mul(Mat4.translation((x0 + x1) / 2, (y0 + y1) / 2, z),
        Mat4.scale(x1 - x0, y1 - y0, h));
  }
}
