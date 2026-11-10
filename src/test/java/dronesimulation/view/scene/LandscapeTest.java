package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.world.Building;
import dronesimulation.model.world.NoFlyZone;
import dronesimulation.model.world.World;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The scenery must not disagree with the flat physics ground where drones fly. */
class LandscapeTest {

  private final World demo = DemoScenario.create(42, new EventBus());

  /**
   * Every leg flown in the shipped mission files, as {x0, y0, x1, y1}: from each drone's home,
   * through its goto and deliver points in order.
   */
  private List<double[]> missionLegs() throws IOException {
    List<double[]> legs = new ArrayList<>();
    try (Stream<Path> files = Files.list(Path.of("missions"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".txt")).toList()) {
        Map<String, double[]> at = new HashMap<>();
        demo.getDrones().forEach(d -> at.put(d.getId(),
            new double[] {d.getHome().x(), d.getHome().y()}));
        for (String line : Files.readAllLines(file)) {
          String[] w = line.trim().split("\\s+");
          if (w.length >= 5 && w[0].equals("add")) { // add <type> <id> <x> <y> ...
            at.put(w[2], new double[] {Double.parseDouble(w[3]), Double.parseDouble(w[4])});
          } else if (w.length >= 5 && (w[1].equals("goto") || w[1].equals("deliver"))) {
            double[] to = {Double.parseDouble(w[2]), Double.parseDouble(w[3])};
            double[] from = at.getOrDefault(w[0], to);
            legs.add(new double[] {from[0], from[1], to[0], to[1]});
            at.put(w[0], to);
          }
        }
      }
    }
    return legs;
  }

  @Test
  void groundIsFlatWhereDronesLiveAndFly() throws IOException {
    for (Drone d : demo.getDrones()) {
      Vector3D home = d.getHome();
      assertEquals(0, Landscape.heightAt(home.x(), home.y()), "home of " + d.getId());
    }
    for (Building o : Scenery.buildings(demo.getObstacles())) {
      for (double x : new double[] {o.min().x(), o.max().x()}) {
        for (double y : new double[] {o.min().y(), o.max().y()}) {
          assertEquals(0, Landscape.heightAt(x, y), o.name() + " corner");
        }
      }
    }
    for (double x = -300; x <= -40; x += 5) { // fixed-wing runway and landing approach
      assertEquals(0, Landscape.heightAt(x, -5), "runway at x " + x);
      assertEquals(0, Landscape.heightAt(x, -60), "approach at x " + x);
    }
  }

  @Test
  void noHillRisesUnderAnyMissionLeg() throws IOException {
    List<double[]> legs = missionLegs();
    assertTrue(legs.size() > 20, "found the mission files");
    for (double[] leg : legs) {
      for (double t = 0; t <= 1; t += 0.01) { // a river below is fine, a hill is not
        double x = leg[0] + (leg[2] - leg[0]) * t;
        double y = leg[1] + (leg[3] - leg[1]) * t;
        assertTrue(Landscape.heightAt(x, y) <= 0, "land above ground at " + x + ", " + y);
      }
    }
  }

  @Test
  void riverKeepsClearOfTheCity() {
    for (Building o : Scenery.buildings(demo.getObstacles())) {
      assertTrue(Landscape.riverDistance(o.max().x(), o.min().y()) > Landscape.RIVER_BANK + 10,
          o.name());
      assertTrue(Landscape.riverDistance(o.max().x(), o.max().y()) > Landscape.RIVER_BANK + 10,
          o.name());
    }
    for (NoFlyZone z : demo.getNoFlyZones()) {
      assertTrue(Landscape.riverDistance(z.centreX() + z.radius(), z.centreY())
          > Landscape.RIVER_BANK + 10, z.name());
    }
  }

  @Test
  void riverHoldsWaterAlongItsWholeLength() {
    for (double y = -3000; y <= 3000; y += 25) {
      double x = Landscape.riverCentreX(y);
      assertTrue(Landscape.heightAt(x, y) < Landscape.WATER_LEVEL - 1, "bed at y " + y);
      // Past the banks, on the flat, the land is back above the water.
      if (Math.hypot(x, y) < Landscape.FLAT_RADIUS - 50) {
        assertFalse(Landscape.heightAt(x + Landscape.RIVER_BANK + 1, y) < 0, "east bank " + y);
        assertFalse(Landscape.heightAt(x - Landscape.RIVER_BANK - 1, y) < 0, "west bank " + y);
      }
    }
  }

  @Test
  void hillsAndMountainsRiseFarOut() {
    double ringMax = 0;
    double ringMin = Double.MAX_VALUE;
    for (int i = 0; i < 360; i++) {
      double a = Math.toRadians(i);
      double x = 1800 * Math.cos(a);
      double y = 1800 * Math.sin(a);
      if (Landscape.riverDistance(x, y) < 250) {
        continue; // the river valley
      }
      double h = Landscape.heightAt(x, y);
      assertTrue(Double.isFinite(h));
      ringMax = Math.max(ringMax, h);
      ringMin = Math.min(ringMin, h);
    }
    assertTrue(ringMin > 5, "no holes in the far land: " + ringMin);
    assertTrue(ringMax > 120, "mountains on the horizon: " + ringMax);
    assertTrue(Landscape.heightAt(600, 300) > 0, "hills start past the flat");
  }

  @Test
  void waterFollowsTheRiverAtWaterLevel() {
    MeshBuilder water = Landscape.water(100, 10);
    assertEquals(20 * 6, water.vertexCount()); // 20 pieces of 10 m, 2 triangles each
    float[] data = water.toArray();
    for (int i = 0; i < data.length; i += 6) {
      assertEquals(Landscape.WATER_LEVEL, data[i + 2], 1e-6);
      double fromCentre = Landscape.riverDistance(data[i], data[i + 1]);
      assertTrue(fromCentre > Landscape.RIVER_BANK && fromCentre < Landscape.RIVER_BANK + 10,
          "edge of the ribbon is past the banks: " + fromCentre);
      assertEquals(1, data[i + 5], 1e-6); // facing up
    }
  }

  @Test
  void waterlineIsWhereTheBanksMeetTheWater() {
    assertTrue(Landscape.WATERLINE > Landscape.RIVER_BED_HALF_WIDTH
        && Landscape.WATERLINE < Landscape.RIVER_BANK);
    for (double y : new double[] {0, 300, -1200, 2500}) { // on the flat and in the hills
      double x = Landscape.riverCentreX(y) + Landscape.WATERLINE;
      assertEquals(Landscape.WATER_LEVEL, Landscape.heightAt(x, y), 1e-6, "y " + y);
    }
  }

  @Test
  void treesGrowOnlyWhereTheyBelong() {
    List<Landscape.Tree> trees = Landscape.trees();
    assertTrue(trees.size() > 1000, "a forest, not a few trees: " + trees.size());
    assertTrue(trees.stream().anyMatch(Landscape.Tree::conifer));
    assertTrue(trees.stream().anyMatch(t -> !t.conifer()));
    for (Landscape.Tree t : trees) {
      String at = t.x() + ", " + t.y();
      assertEquals(Landscape.heightAt(t.x(), t.y()), t.z(), 1e-9, "on the ground at " + at);
      assertTrue(t.z() >= 0, "not in the river channel at " + at);
      assertTrue(Math.hypot(t.x(), t.y()) >= Landscape.TREE_FREE_RADIUS, "in the city at " + at);
      assertTrue(Landscape.riverDistance(t.x(), t.y()) > Landscape.RIVER_BANK, "in water " + at);
      boolean westOfHome = t.x() < 0 && t.x() > -360;
      assertFalse(westOfHome && Math.abs(t.y() + 5) < Landscape.TREE_FREE_LANE,
          "on the runway at " + at);
      assertFalse(westOfHome && Math.abs(t.y() + 60) < Landscape.TREE_FREE_LANE,
          "under the landing approach at " + at);
    }
    assertEquals(trees, Landscape.trees(), "the same trees every run");
  }

  @Test
  void forestMeshHoldsEveryTree() {
    List<Landscape.Tree> trees = Landscape.trees();
    long conifers = trees.stream().filter(Landscape.Tree::conifer).count();
    Landscape.Forest forest = Landscape.forest();
    assertEquals(trees.size() * 5 * 6, forest.trunks().vertexCount()); // 5-sided, no caps
    assertEquals(conifers * 2 * 7 * 3, forest.conifers().vertexCount()); // two 7-sided cones
    assertEquals((trees.size() - conifers) * 2 * 7 * 3, forest.broadleaf().vertexCount());
  }

  @Test
  void smoothstepIsExactAtItsEdges() {
    assertEquals(0, Landscape.smoothstep(10, 20, 5));
    assertEquals(0, Landscape.smoothstep(10, 20, 10));
    assertEquals(0.5, Landscape.smoothstep(10, 20, 15), 1e-12);
    assertEquals(1, Landscape.smoothstep(10, 20, 25));
  }
}
