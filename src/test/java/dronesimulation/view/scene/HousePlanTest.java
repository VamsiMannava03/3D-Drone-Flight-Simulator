package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.world.Building;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HousePlanTest {

  private final List<Building> town =
      Scenery.buildings(DemoScenario.create(42, new EventBus()).getObstacles());
  private final List<Neighbourhood.Street> streets = Neighbourhood.streets(town);
  private final List<HousePlan> plans = town.stream()
      .filter(o -> o.style() == Building.Style.HOUSE)
      .map(o -> HousePlan.of(o, streets)).toList();

  private static boolean inside(double[] inner, double[] outer) {
    double e = 1e-9;
    return inner[0] >= outer[0] - e && inner[1] >= outer[1] - e
        && inner[2] <= outer[2] + e && inner[3] <= outer[3] + e;
  }

  private static double[] footprint(Building o) {
    return new double[] {o.min().x(), o.min().y(), o.max().x(), o.max().y()};
  }

  private static boolean overlap(double[] a, double[] b) {
    return a[0] < b[2] - 1e-9 && a[2] > b[0] + 1e-9 && a[1] < b[3] - 1e-9 && a[3] > b[1] + 1e-9;
  }

  @Test
  void everyHouseFacesItsStreet() {
    for (HousePlan p : plans) {
      String name = p.house().name();
      double cx = (p.house().min().x() + p.house().max().x()) / 2;
      double cy = (p.house().min().y() + p.house().max().y()) / 2;
      if (name.startsWith("Elm")) { // Elm Street runs along y = -28
        assertEquals(0, p.frontX(), name);
        assertEquals(cy > -28 ? -1 : 1, p.frontY(), name);
      } else { // Oak Court's lane runs along x = -26
        assertEquals(0, p.frontY(), name);
        assertEquals(cx < -26 ? 1 : -1, p.frontX(), name);
      }
    }
  }

  @Test
  void bodyAndGarageStayInsideTheFootprintAndPorchStaysInTheYard() {
    for (HousePlan p : plans) {
      double[] lot = footprint(p.house());
      String name = p.house().name();
      assertTrue(inside(p.main(), lot), name + " body inside its obstacle");
      if (p.garage() != null) {
        assertTrue(inside(p.garage(), lot), name + " garage inside its obstacle");
        assertFalse(overlap(p.garage(), p.main()), name + " garage beside the body");
      }
      if (p.porch() != null) {
        assertFalse(overlap(p.porch(), lot), name + " porch outside the walls");
        for (Neighbourhood.Street s : streets) {
          assertFalse(overlap(p.porch(), new double[] {s.x0(), s.y0(), s.x1(), s.y1()}),
              name + " porch stays out of the street");
        }
        // It stands against the front wall, on the side the house faces.
        double pcx = (p.porch()[0] + p.porch()[2]) / 2 - (lot[0] + lot[2]) / 2;
        double pcy = (p.porch()[1] + p.porch()[3]) / 2 - (lot[1] + lot[3]) / 2;
        assertTrue(pcx * p.frontX() + pcy * p.frontY() > 0, name + " porch at the front");
      }
    }
  }

  @Test
  void theNeighbourhoodIsVaried() {
    Set<HousePlan.Roof> roofs = new HashSet<>();
    Set<String> kinds = new HashSet<>();
    int garages = 0;
    int porches = 0;
    for (HousePlan p : plans) {
      roofs.add(p.roof());
      garages += p.garage() == null ? 0 : 1;
      porches += p.porch() == null ? 0 : 1;
      kinds.add(p.roof() + "/" + (p.garage() != null) + "/" + (p.porch() != null) + "/"
          + p.chimney());
    }
    assertEquals(3, roofs.size(), "every roof type is used");
    assertTrue(garages >= 3, "garages: " + garages);
    assertTrue(porches >= 3, "porches: " + porches);
    assertTrue(kinds.size() >= 7, plans.size() + " houses, only " + kinds.size() + " kinds");
  }

  @Test
  void planIsTheSameEveryRun() {
    for (HousePlan p : plans) {
      HousePlan again = HousePlan.of(p.house(), streets);
      assertEquals(p.roof(), again.roof());
      assertEquals(p.look(), again.look());
      assertEquals(p.garage() == null, again.garage() == null);
    }
  }

  @Test
  void ridgeRunsAccordingToTheRoofType() {
    Building wide = Building.house("A", 0, 0, 10, 8, 8);
    List<Neighbourhood.Street> south = List.of(new Neighbourhood.Street(-20, -12, 20, -6));
    HousePlan p = HousePlan.of(wide, south);
    assertEquals(-1, p.frontY());
    boolean sideGableAlongX = new HousePlan(wide, 0, -1, HousePlan.Roof.SIDE_GABLE, p.main(),
        null, null, false, 0).ridgeAlongX();
    boolean frontGableAlongX = new HousePlan(wide, 0, -1, HousePlan.Roof.FRONT_GABLE, p.main(),
        null, null, false, 0).ridgeAlongX();
    assertTrue(sideGableAlongX, "parallel to the street");
    assertFalse(frontGableAlongX, "running back from the street");
    assertNull(HousePlan.of(Building.house("Tiny", 0, 0, 6, 6, 6), List.of()).garage(),
        "a narrow house has no room for a garage");
  }
}
