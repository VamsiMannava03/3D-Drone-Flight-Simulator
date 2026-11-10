package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.Vector3D;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.obstacles.BoxObstacle;
import dronesimulation.model.obstacles.SphereObstacle;
import dronesimulation.model.world.Building;
import java.util.List;
import org.junit.jupiter.api.Test;

class TownBuilderTest {

  @Test
  void townHasStreetsHousesAndGardens() {
    ColourBatch town = TownBuilder.town(DemoScenario.create(42, new EventBus()).getObstacles());
    assertTrue(town.meshes().containsKey(TownBuilder.ROAD), "streets between the houses");
    assertTrue(town.meshes().keySet().stream()
        .anyMatch(c -> List.of(0x4C5159, 0x9A4B33, 0x5D6673, 0x6E4C3B).contains(c)), "roofs");
    assertTrue(town.meshes().containsKey(0xF3F1EA), "garden fences");
    assertTrue(town.meshes().containsKey(TownBuilder.BUILDING_COLOURS[0]), "the towers");
  }

  @Test
  void blockFillsItsBox() {
    ColourBatch town = TownBuilder.town(List.of(Building.block("B", 0, 0, 4, 6, 10)));
    float[] data = town.builder(TownBuilder.BUILDING_COLOURS[0]).toArray();
    double maxZ = 0;
    double maxX = 0;
    for (int i = 0; i < data.length; i += 6) {
      maxX = Math.max(maxX, data[i]);
      maxZ = Math.max(maxZ, data[i + 2]);
    }
    assertEquals(2, maxX, 1e-5);
    assertEquals(10, maxZ, 1e-5);
  }

  @Test
  void otherBoxesAreDrawnAndSpheresLeftToTheView() {
    ColourBatch town = TownBuilder.town(List.of(
        new SphereObstacle("ball", new Vector3D(0, 0, 5), 2),
        new BoxObstacle("crate", Vector3D.ZERO, new Vector3D(1, 1, 1))));
    assertEquals(36, town.builder(TownBuilder.BUILDING_COLOURS[1]).vertexCount());
    assertEquals(1, town.meshes().size());
  }

  @Test
  void groundIsColouredByWhereItIs() {
    assertEquals(TownBuilder.FIELD, TownBuilder.groundColour(0, 0, 0));
    assertEquals(TownBuilder.SAND, TownBuilder.groundColour(Landscape.riverCentreX(0), 0, -2));
    assertEquals(TownBuilder.GRASS, TownBuilder.groundColour(900, 0, 40));
    assertEquals(TownBuilder.ROCK, TownBuilder.groundColour(1500, 0, 150));
    assertEquals(TownBuilder.SNOW, TownBuilder.groundColour(1900, 0, 300));
  }

  @Test
  void landscapeAndWaterAreBuilt() {
    ColourBatch land = TownBuilder.landscape(20);
    assertTrue(land.meshes().containsKey(TownBuilder.FIELD));
    assertTrue(land.meshes().containsKey(TownBuilder.TRUNK));
    assertTrue(TownBuilder.water().vertexCount() > 0);
  }
}
