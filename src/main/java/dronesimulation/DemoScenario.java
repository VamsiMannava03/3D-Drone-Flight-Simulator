package dronesimulation;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.DroneFactory;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.physics.GustyWind;
import dronesimulation.model.world.Building;
import dronesimulation.model.world.NoFlyZone;
import dronesimulation.model.world.World;
import java.util.Random;

/**
 * The demo world: a small city block with buildings, a hospital no-fly zone, gusty wind, two
 * quadcopters and a cargo hexacopter at a depot, and a fixed-wing drone on a runway. Missions
 * come from {@code missions/demo.txt}.
 */
public final class DemoScenario {

  private DemoScenario() {
  }

  /**
   * Builds an empty world: a flat ground plane in calm air, with no drones, obstacles or zones.
   * The starting point of the SRS primary use case; drones and wind are then added with
   * commands, for example from {@code missions/usecase.txt}.
   *
   * @param events the event bus the world reports to
   * @return the empty world
   */
  public static World empty(EventBus events) {
    return new World(events);
  }

  /**
   * Builds the demo world. The same seed always gives the same world, weather and sensor noise.
   *
   * @param seed the random seed
   * @param events the event bus the world reports to
   * @return the world, with drones idle and no missions yet
   */
  public static World create(long seed, EventBus events) {
    Random random = new Random(seed);
    World world = new World(events);
    world.setWind(new GustyWind(new Vector3D(2.0, 1.0, 0), 2.5, 3, random, world::getTime));

    // A small downtown of two towers.
    world.addObstacle(Building.block("Tower A", 30, 10, 10, 10, 28));
    world.addObstacle(Building.block("Tower D", 60, 35, 8, 8, 36));
    // Elm Street runs east-west at y = -28, with a row of houses facing it on each side:
    // odd numbers on the north side, even on the south. Two-storey houses and bungalows
    // alternate, so the street is not a row of clones.
    double[] elmX = {15, 28, 41, 54, 67};
    for (int i = 0; i < elmX.length; i++) {
      boolean twoStorey = i % 2 == 0;
      world.addObstacle(Building.house("Elm St " + (2 * i + 1), elmX[i], -18,
          twoStorey ? 10 : 9, 8, twoStorey ? 8.5 : 6.5));
      world.addObstacle(Building.house("Elm St " + (2 * i + 2), elmX[i], -38,
          twoStorey ? 9 : 10, 8, twoStorey ? 6.5 : 8.5));
    }
    // Oak Court, north-west of the drone pads: a lane running north-south between x = -36
    // and -16, with two houses facing it on each side (odd numbers west, even east).
    world.addObstacle(Building.house("Oak Ct 1", -40, 21, 8, 10, 7.5));
    world.addObstacle(Building.house("Oak Ct 3", -40, 34, 8, 10, 8.5));
    world.addObstacle(Building.house("Oak Ct 2", -12, 21, 8, 10, 6.5));
    world.addObstacle(Building.house("Oak Ct 4", -12, 34, 8, 10, 8.5));
    world.addNoFlyZone(new NoFlyZone("Hospital", -30, -30, 12, 40));

    world.addDrone(DroneFactory.builder(DroneType.QUADCOPTER, "q1", new Vector3D(0, 0, 0),
        random).payload(0.4).build());
    // q2 starts half charged, so its long survey takes it below 20% and it returns home.
    world.addDrone(DroneFactory.builder(DroneType.QUADCOPTER, "q2", new Vector3D(4, 0, 0),
        random).batteryLevel(0.45).build());
    world.addDrone(DroneFactory.builder(DroneType.FIXED_WING, "fw1", new Vector3D(-40, -5, 0),
        random).runwayHeading(new Vector3D(-1, 0, 0)).build());
    // h1 carries a crate too heavy for a quadcopter.
    world.addDrone(DroneFactory.builder(DroneType.HEXACOPTER, "h1", new Vector3D(8, 0, 0),
        random).payload(1.5).build());
    return world;
  }
}
