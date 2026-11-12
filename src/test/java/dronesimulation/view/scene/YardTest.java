package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.world.Building;
import dronesimulation.model.world.World;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YardTest {

  private final World world = DemoScenario.create(42, new EventBus());
  private final List<Building> town = Scenery.buildings(world.getObstacles());
  private final List<Neighbourhood.Street> streets = Neighbourhood.streets(town);
  private final List<Yard.Item> items = town.stream()
      .filter(o -> o.style() == Building.Style.HOUSE)
      .flatMap(o -> Yard.of(HousePlan.of(o, streets)).stream()).toList();

  private static boolean overlap(Yard.Item i, double x0, double y0, double x1, double y1) {
    return i.x0() < x1 && i.x1() > x0 && i.y0() < y1 && i.y1() > y0;
  }

  @Test
  void gardensKeepOutOfStreetsBuildingsPadsAndTheRunway() {
    assertTrue(items.size() > 100, "a garden for every house: " + items.size());
    for (Yard.Item i : items) {
      String what = i.kind() + " at " + i.x0() + ", " + i.y0();
      for (Neighbourhood.Street s : streets) {
        assertFalse(overlap(i, s.x0(), s.y0(), s.x1(), s.y1()), what + " in the street");
      }
      for (Building o : town) {
        assertFalse(overlap(i, o.min().x(), o.min().y(), o.max().x(), o.max().y()),
            what + " inside " + o.name());
      }
      for (Drone d : world.getDrones()) {
        double hx = d.getHome().x();
        double hy = d.getHome().y();
        assertFalse(overlap(i, hx - 3, hy - 3, hx + 3, hy + 3), what + " on " + d.getId()
            + "'s pad");
      }
      assertFalse(overlap(i, -400, -15, -35, 5), what + " on the fixed-wing's runway");
      assertTrue(i.height() <= Yard.MAX_TREE, what + " is " + i.height() + " m tall");
    }
  }

  @Test
  void everyHouseHasAPathAFenceAndATree() {
    for (Building o : town) {
      if (o.style() != Building.Style.HOUSE) {
        continue;
      }
      List<Yard.Item> garden = Yard.of(HousePlan.of(o, streets));
      for (Yard.Kind k : new Yard.Kind[] {Yard.Kind.PATH, Yard.Kind.FENCE, Yard.Kind.TREE,
          Yard.Kind.MAILBOX}) {
        assertTrue(garden.stream().anyMatch(i -> i.kind() == k), o.name() + " has a " + k);
      }
      boolean hasGarage = HousePlan.of(o, streets).garage() != null;
      assertTrue(hasGarage == garden.stream().anyMatch(i -> i.kind() == Yard.Kind.DRIVEWAY),
          o.name() + " has a driveway exactly when it has a garage");
    }
  }

  /** Every straight leg the mission flies, as {x0, y0, z0, x1, y1, z1}. */
  private List<double[]> legs(String file) throws IOException {
    Map<String, double[]> at = new HashMap<>();
    Map<String, double[]> home = new HashMap<>();
    for (Drone d : world.getDrones()) {
      home.put(d.getId(), new double[] {d.getHome().x(), d.getHome().y(), 0});
      at.put(d.getId(), home.get(d.getId()).clone());
    }
    double cruise = world.returnAltitude();
    List<double[]> legs = new ArrayList<>();
    for (String line : Files.readAllLines(Path.of("missions", file))) {
      String[] w = line.trim().split("\\s+");
      if (w.length < 2 || !at.containsKey(w[0])) {
        continue;
      }
      double[] p = at.get(w[0]);
      List<double[]> next = new ArrayList<>();
      switch (w[1]) {
        case "takeoff" -> next.add(new double[] {p[0], p[1], Double.parseDouble(w[2])});
        case "goto" -> next.add(new double[] {Double.parseDouble(w[2]),
            Double.parseDouble(w[3]), Double.parseDouble(w[4])});
        case "deliver" -> next.add(new double[] {Double.parseDouble(w[2]),
            Double.parseDouble(w[3]), Double.parseDouble(w[4]) + 2.5});
        case "home" -> { // climb to the return altitude, then fly home at it
          double[] h = home.get(w[0]);
          next.add(new double[] {p[0], p[1], Math.max(p[2], cruise)});
          next.add(new double[] {h[0], h[1], Math.max(p[2], cruise)});
        }
        case "land" -> next.add(new double[] {p[0], p[1], 0});
        default -> { }
      }
      for (double[] q : next) {
        legs.add(new double[] {p[0], p[1], p[2], q[0], q[1], q[2]});
        p = q;
      }
      at.put(w[0], p);
    }
    return legs;
  }

  @ParameterizedTest
  @ValueSource(strings = {"demo.txt", "inspection.txt"})
  void missionsFlyWellAboveEveryGarden(String file) throws IOException {
    List<double[]> legs = legs(file);
    assertTrue(legs.size() > 15, "read the mission: " + legs.size() + " legs");
    for (double[] l : legs) {
      for (int s = 0; s <= 400; s++) {
        double t = s / 400.0;
        double x = l[0] + (l[3] - l[0]) * t;
        double y = l[1] + (l[4] - l[1]) * t;
        double z = l[2] + (l[5] - l[2]) * t;
        if (z <= 0.01) {
          continue; // on the ground at home or in the field
        }
        for (Yard.Item i : items) {
          double reach = 1.0; // the drone's radius and a margin
          if (overlap(i, x - reach, y - reach, x + reach, y + reach)) {
            assertTrue(z > i.height() + 1, String.format(
                "%s leg passes %.1f m over a %.1f m %s at %.1f, %.1f", file, z, i.height(),
                i.kind(), x, y));
          }
        }
      }
    }
  }
}
