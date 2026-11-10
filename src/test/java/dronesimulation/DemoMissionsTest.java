package dronesimulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.controller.HoverFacade;
import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.states.Idle;
import dronesimulation.model.states.Landed;
import dronesimulation.model.world.Building;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The shipped mission files must fly cleanly through the demo city: no drone hits a house, a
 * tower or another drone, every package is delivered, and everyone ends up back on the ground.
 * Moving a building or a waypoint without checking the routes fails here.
 */
class DemoMissionsTest {

  private static List<SimEvent> fly(String file, SimulationEngine[] engineOut)
      throws IOException {
    EventBus events = new EventBus();
    List<SimEvent> log = new ArrayList<>();
    events.subscribe(log::add);
    SimulationEngine engine = new SimulationEngine(() -> DemoScenario.create(42, events),
        SimulationEngine.DEFAULT_DT);
    new HoverFacade(engine).loadMission(Path.of("missions", file));
    engine.runSteps(60_000); // 600 s, longer than either mission takes
    engineOut[0] = engine;
    return log;
  }

  private static long count(List<SimEvent> log, SimEventType type) {
    return log.stream().filter(e -> e.type() == type).count();
  }

  @ParameterizedTest
  @ValueSource(strings = {"demo.txt", "inspection.txt"})
  void missionFliesWithoutHittingAnything(String file) throws IOException {
    SimulationEngine[] engine = new SimulationEngine[1];
    List<SimEvent> log = fly(file, engine);

    assertEquals(0, count(log, SimEventType.COLLISION), () -> "collisions: " + log);
    assertEquals(0, count(log, SimEventType.CRASH), () -> "crashes: " + log);
    assertEquals(0, count(log, SimEventType.COMMAND_REJECTED), () -> "rejected: " + log);
    long delivers = Files.readAllLines(Path.of("missions", file)).stream()
        .filter(line -> line.matches("\\S+ deliver .*")).count();
    assertEquals(delivers, count(log, SimEventType.PAYLOAD_DROPPED), "every package dropped");
    for (Drone d : engine[0].world().getDrones()) {
      assertTrue(d.getState() == Landed.INSTANCE || d.getState() == Idle.INSTANCE,
          d.getId() + " ends on the ground, not " + d.getState().name());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"demo.txt", "inspection.txt"})
  void doorstepDeliveriesLandInTheStreetNotOnAHouse(String file) throws IOException {
    List<Building> houses = DemoScenario.create(42, new EventBus()).getObstacles().stream()
        .filter(o -> o instanceof Building b && b.style() == Building.Style.HOUSE)
        .map(o -> (Building) o).toList();
    assertTrue(houses.size() >= 10, "the neighbourhood is there");
    for (String line : Files.readAllLines(Path.of("missions", file))) {
      String[] w = line.trim().split("\\s+");
      if (w.length == 5 && w[1].equals("deliver") && Double.parseDouble(w[4]) == 0) {
        double x = Double.parseDouble(w[2]);
        double y = Double.parseDouble(w[3]);
        double nearest = houses.stream()
            .mapToDouble(h -> h.distanceTo(new Vector3D(x, y, 0)))
            .min().orElseThrow();
        assertTrue(nearest > 2 && nearest < 6, "drop " + x + ", " + y
            + " is at a front door, " + nearest + " m from the nearest house");
      }
    }
  }
}
