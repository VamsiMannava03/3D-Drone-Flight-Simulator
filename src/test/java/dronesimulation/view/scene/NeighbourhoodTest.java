package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.world.Building;
import java.util.List;
import org.junit.jupiter.api.Test;

class NeighbourhoodTest {

  private static boolean overlaps(Neighbourhood.Street s, Building o) {
    return o.min().x() < s.x1() && o.max().x() > s.x0()
        && o.min().y() < s.y1() && o.max().y() > s.y0();
  }

  @Test
  void elmStreetAndOakCourtRunBetweenTheirRowsOfHouses() {
    List<Building> town =
        Scenery.buildings(DemoScenario.create(42, new EventBus()).getObstacles());
    List<Neighbourhood.Street> streets = Neighbourhood.streets(town);
    assertEquals(2, streets.size(), "Elm Street and Oak Court");
    for (Neighbourhood.Street s : streets) {
      for (Building o : town) {
        assertFalse(overlaps(s, o), "a street runs into " + o.name());
      }
    }
    Neighbourhood.Street oak = streets.stream().filter(s -> !s.alongX()).findFirst()
        .orElseThrow();
    // Odd-numbered houses end at x = -36, even-numbered ones start at x = -16.
    assertEquals(-36 + Neighbourhood.YARD, oak.x0(), 1e-9);
    assertEquals(-16 - Neighbourhood.YARD, oak.x1(), 1e-9);

    Neighbourhood.Street elm = streets.stream().filter(Neighbourhood.Street::alongX)
        .findFirst().orElseThrow();
    // North-side houses start at y = -22, south-side ones end at y = -34.
    assertEquals(-34 + Neighbourhood.YARD, elm.y0(), 1e-9);
    assertEquals(-22 - Neighbourhood.YARD, elm.y1(), 1e-9);
    double firstHouse = town.stream().filter(o -> o.name().startsWith("Elm"))
        .mapToDouble(o -> o.min().x()).min().orElseThrow();
    double lastHouse = town.stream().filter(o -> o.name().startsWith("Elm"))
        .mapToDouble(o -> o.max().x()).max().orElseThrow();
    assertEquals(firstHouse - Neighbourhood.RUN_OUT, elm.x0(), 1e-9);
    assertEquals(lastHouse + Neighbourhood.RUN_OUT, elm.x1(), 1e-9);
    // q1's doorstep drop point is on the pavement, so the package lands in the street.
    assertTrue(elm.x0() < 28 && 28 < elm.x1() && elm.y0() < -25 && -25 < elm.y1());
  }

  @Test
  void streetsAlsoRunAlongY() {
    List<Building> houses = List.of(
        Building.house("West 1", -10, 0, 8, 10, 7), Building.house("East 1", 10, 0, 8, 10, 7),
        Building.house("West 2", -10, 14, 8, 10, 7), Building.house("East 2", 10, 14, 8, 10, 7));
    List<Neighbourhood.Street> streets = Neighbourhood.streets(houses);
    assertEquals(1, streets.size());
    Neighbourhood.Street s = streets.get(0);
    assertFalse(s.alongX());
    assertEquals(-6 + Neighbourhood.YARD, s.x0(), 1e-9);
    assertEquals(6 - Neighbourhood.YARD, s.x1(), 1e-9);
    assertEquals(-5 - Neighbourhood.RUN_OUT, s.y0(), 1e-9); // joined into one street
    assertEquals(19 + Neighbourhood.RUN_OUT, s.y1(), 1e-9);
  }

  @Test
  void noStreetWithoutFacingHouses() {
    assertTrue(Neighbourhood.streets(List.of()).isEmpty());
    assertTrue(Neighbourhood.streets(List.of( // towers do not make a street
        Building.block("A", 0, 0, 8, 8, 30), Building.block("B", 0, 20, 8, 8, 30)))
        .isEmpty());
    assertTrue(Neighbourhood.streets(List.of( // too far apart
        Building.house("A", 0, 0, 8, 8, 7), Building.house("B", 0, 40, 8, 8, 7))).isEmpty());
    assertTrue(Neighbourhood.streets(List.of( // touching, no room for a street
        Building.house("A", 0, 0, 8, 8, 7), Building.house("B", 0, 10, 8, 8, 7))).isEmpty());
  }
}
