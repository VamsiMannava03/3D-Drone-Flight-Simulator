package dronesimulation.model.engine;

import dronesimulation.model.Vector3D;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.world.NoFlyZone;
import dronesimulation.model.world.World;
import java.util.List;

/**
 * A read-only copy of the whole simulation at a moment in time. Views only ever see snapshots, so
 * they cannot change the model.
 *
 * @param time the simulation time in s
 * @param drones every drone, in the order they were added
 * @param obstacles the obstacles
 * @param noFlyZones the no-fly zones
 * @param wind the air velocity at the origin, in m/s
 * @param paused whether the simulation is paused
 * @param timeScale how many simulated seconds pass per real second
 */
public record WorldSnapshot(double time, List<DroneSnapshot> drones, List<Obstacle> obstacles,
    List<NoFlyZone> noFlyZones, Vector3D wind, boolean paused, double timeScale) {

  /**
   * Creates a snapshot; the lists are copied.
   */
  public WorldSnapshot {
    drones = List.copyOf(drones);
    obstacles = List.copyOf(obstacles);
    noFlyZones = List.copyOf(noFlyZones);
  }

  /**
   * Copies the current state of a world.
   *
   * @param world the world
   * @param paused whether the simulation is paused
   * @param timeScale the time scale
   * @return the snapshot
   */
  public static WorldSnapshot of(World world, boolean paused, double timeScale) {
    List<DroneSnapshot> drones = world.getDrones().stream()
        .map(d -> DroneSnapshot.of(d, world.mission(d.getId())))
        .toList();
    return new WorldSnapshot(world.getTime(), drones, world.getObstacles(),
        world.getNoFlyZones(), world.getWind().airVelocityAt(Vector3D.ZERO),
        paused, timeScale);
  }
}
