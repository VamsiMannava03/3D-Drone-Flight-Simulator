package dronesimulation.model.world;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.Mission;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.obstacles.ObstacleField;
import dronesimulation.model.physics.Atmosphere;
import dronesimulation.model.physics.AtmosphericDrag;
import dronesimulation.model.physics.Collision;
import dronesimulation.model.physics.Force;
import dronesimulation.model.physics.Gravity;
import dronesimulation.model.physics.GroundContactEngine;
import dronesimulation.model.physics.NoWind;
import dronesimulation.model.physics.OwnPhysicsEngine;
import dronesimulation.model.physics.PhysicsEngine;
import dronesimulation.model.physics.SphereCollider;
import dronesimulation.model.physics.WindField;
import dronesimulation.model.physics.Winds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Everything that is simulated: drones with their missions, obstacles, no-fly zones and the
 * weather, on top of the {@link TestWorld}.
 *
 * <p>Each step runs every mission, then every drone, then the physics, then checks for
 * collisions and no-fly zones. Events go out through the {@link EventBus}.
 */
public class World {

  /** How far above the tallest obstacle drones fly home, in m. */
  public static final double RETURN_CLEARANCE = 5;

  private final GroundContactEngine physics;
  private final EventBus events;
  private WindField wind;
  private final Map<String, Drone> drones = new LinkedHashMap<>();
  /** The drones in the order they were added, kept so each step need not copy them. */
  private final List<Drone> droneList = new ArrayList<>();
  private final Map<String, Mission> missions = new LinkedHashMap<>();
  private final ObstacleField obstacles = new ObstacleField();
  private final List<NoFlyZone> noFlyZones = new ArrayList<>();
  private final CollisionDetector detector = new CollisionDetector(new SphereCollider());
  // Contact state is keyed by objects, not by id strings, so the checks that run on every step
  // do not build a new string per drone.
  private final Set<Drone> onObstacle = new HashSet<>();
  private Set<DronePair> touching = new HashSet<>();
  private final Set<DroneZone> inZone = new HashSet<>();
  private final ReturnHomePolicy returnHomePolicy = new ReturnHomePolicy(this);
  private boolean returnHomeOnLowBattery;

  /**
   * Creates an empty world in still air, with gravity and drag on the team's own physics
   * engine.
   *
   * @param events where events are published; its clock is set to this world's time
   */
  public World(EventBus events) {
    this(NoWind.INSTANCE, events);
  }

  /**
   * Creates an empty world with gravity and drag on the team's own physics engine.
   *
   * @param wind the weather
   * @param events where events are published; its clock is set to this world's time
   */
  public World(WindField wind, EventBus events) {
    this(wind, events, OwnPhysicsEngine::new);
  }

  /**
   * Creates an empty world on a physics engine of the caller's choice. The world hands the
   * factory its environment forces (gravity, and drag in an atmosphere that thins with
   * altitude, measured against the current wind) and puts a ground plane under the engine it
   * gets back.
   *
   * @param wind the weather
   * @param events where events are published; its clock is set to this world's time
   * @param engineFactory builds a physics engine with no bodies that applies the given forces
   */
  public World(WindField wind, EventBus events,
      Function<List<Force>, PhysicsEngine> engineFactory) {
    this.wind = Objects.requireNonNull(wind, "wind");
    this.events = Objects.requireNonNull(events, "events");
    // Read the field on every call, so setWind() takes effect immediately.
    WindField currentWind = position -> this.wind.airVelocityAt(position);
    List<Force> forces = List.of(new Gravity(),
        new AtmosphericDrag(Atmosphere.standard(), currentWind));
    this.physics = new GroundContactEngine(
        Objects.requireNonNull(engineFactory.apply(forces), "engine"));
    events.setClock(physics::getTime);
    setReturnHomeOnLowBattery(true);
  }

  /**
   * Changes the weather from the next step on.
   *
   * @param wind the new wind
   */
  public void setWind(WindField wind) {
    this.wind = Objects.requireNonNull(wind, "wind");
  }

  /**
   * Changes the weather from the next step on, from the numbers a user types. Gusts are seeded
   * from the settings and follow this world's clock.
   *
   * @param speed the base wind speed in m/s, not negative
   * @param fromDegrees the compass direction the wind blows from, in degrees
   * @param gustStrength the largest gust speed in m/s, not negative
   * @throws IllegalArgumentException if the speed or gust strength is negative
   */
  public void setWind(double speed, double fromDegrees, double gustStrength) {
    setWind(Winds.from(speed, fromDegrees, gustStrength, this::getTime));
  }

  /**
   * Removes a drone and its mission.
   *
   * @param id the drone id
   * @throws IllegalArgumentException if there is no such drone
   */
  public void removeDrone(String id) {
    Drone drone = drone(id);
    physics.removeBody(drone);
    drones.remove(id);
    droneList.remove(drone);
    missions.remove(id);
    onObstacle.remove(drone);
    touching.removeIf(pair -> pair.first() == drone || pair.second() == drone);
    inZone.removeIf(key -> key.drone() == drone);
  }

  /**
   * Detaches this world from its event bus, so a replacement world can use the same bus. Call it
   * when the world is thrown away, for example on reset.
   */
  public void detach() {
    setReturnHomeOnLowBattery(false);
  }

  /**
   * Adds a drone with an empty mission.
   *
   * @param drone the drone
   * @throws IllegalArgumentException if a drone with the same id exists
   */
  public void addDrone(Drone drone) {
    Objects.requireNonNull(drone, "drone");
    if (drones.containsKey(drone.getId())) {
      throw new IllegalArgumentException("Duplicate drone id: " + drone.getId());
    }
    physics.addBody(drone);
    drone.setEvents(events);
    drone.setReturnAltitude(returnAltitude());
    drones.put(drone.getId(), drone);
    droneList.add(drone);
    missions.put(drone.getId(), new Mission());
  }

  /**
   * Adds an obstacle. Every drone's return-to-home altitude is raised to clear it.
   *
   * @param obstacle the obstacle
   */
  public void addObstacle(Obstacle obstacle) {
    obstacles.add(Objects.requireNonNull(obstacle, "obstacle"));
    double altitude = returnAltitude();
    drones.values().forEach(d -> d.setReturnAltitude(altitude));
  }

  /**
   * Returns the altitude drones climb to before flying home: clear of the tallest obstacle.
   *
   * @return the altitude in m
   */
  public double returnAltitude() {
    double tallest = obstacles.getObstacles().stream().mapToDouble(ObstacleShapes::top).max()
        .orElse(0);
    return Math.max(Drone.DEFAULT_RETURN_ALTITUDE, tallest + RETURN_CLEARANCE);
  }

  /**
   * Adds a no-fly zone.
   *
   * @param zone the zone
   */
  public void addNoFlyZone(NoFlyZone zone) {
    noFlyZones.add(Objects.requireNonNull(zone, "zone"));
  }

  /**
   * Turns the {@link ReturnHomePolicy} on or off: whether a drone that reports low battery
   * abandons its mission to return home and land. On by default.
   *
   * @param enabled true to return home automatically
   */
  public void setReturnHomeOnLowBattery(boolean enabled) {
    if (enabled && !returnHomeOnLowBattery) {
      events.subscribe(returnHomePolicy);
    } else if (!enabled && returnHomeOnLowBattery) {
      events.unsubscribe(returnHomePolicy);
    }
    returnHomeOnLowBattery = enabled;
  }

  /**
   * Finds a drone by id, if it exists.
   *
   * @param id the id
   * @return the drone, or empty
   */
  public Optional<Drone> findDrone(String id) {
    return Optional.ofNullable(drones.get(id));
  }

  /**
   * Finds a drone by id.
   *
   * @param id the id
   * @return the drone
   * @throws IllegalArgumentException if there is no such drone
   */
  public Drone drone(String id) {
    Drone drone = drones.get(id);
    if (drone == null) {
      throw new IllegalArgumentException(
          "No drone called '" + id + "'. Drones: " + String.join(", ", drones.keySet()));
    }
    return drone;
  }

  /**
   * Returns a drone's mission.
   *
   * @param id the drone id
   * @return the mission
   * @throws IllegalArgumentException if there is no such drone
   */
  public Mission mission(String id) {
    drone(id);
    return missions.get(id);
  }

  /**
   * Queues a command for a drone after checking that the drone can run it, given everything it
   * is already doing. A step the drone cannot perform, such as hovering with a fixed-wing drone,
   * is not queued and is reported as COMMAND_REJECTED.
   *
   * @param id the drone id
   * @param command the command
   * @return true if the command was queued
   * @throws IllegalArgumentException if there is no such drone
   */
  public boolean queueCommand(String id, DroneCommand command) {
    Drone drone = drone(id);
    try {
      missions.get(id).addChecked(drone, command);
      return true;
    } catch (IllegalStateException | IllegalArgumentException e) {
      events.publish(SimEventType.COMMAND_REJECTED, id,
          "'" + command.describe() + "' rejected: " + e.getMessage());
      return false;
    }
  }

  /**
   * Advances the world by one time step, then delivers the step's events to listeners. Events
   * are delivered in the order they happened, after the whole step is done.
   *
   * @param dt the time step in s, must be positive
   */
  public void step(double dt) {
    for (Drone drone : drones.values()) {
      missions.get(drone.getId()).update(drone, dt);
      drone.update(dt);
    }
    physics.step(dt);
    checkObstacles();
    checkDroneCollisions();
    checkNoFlyZones();
    events.flush();
  }

  private void checkObstacles() {
    for (Drone drone : droneList) {
      Optional<Obstacle> hit = detector.obstacleHit(drone, obstacles);
      if (hit.isEmpty()) {
        onObstacle.remove(drone);
        continue;
      }
      Obstacle obstacle = hit.get();
      pushOut(drone, obstacle);
      if (onObstacle.add(drone)) {
        events.publish(SimEventType.COLLISION, drone.getId(), "hit " + obstacle.getId());
        drone.crash("hit " + obstacle.getId());
      }
    }
  }

  private static void pushOut(Drone drone, Obstacle obstacle) {
    Vector3D p = drone.getPosition();
    Optional<Vector3D> nearest = ObstacleShapes.closestPoint(obstacle, p);
    if (nearest.isEmpty()) {
      return; // an unknown shape: the crash still counts, the drone is just not moved
    }
    Vector3D closest = nearest.get();
    Vector3D normal = p.subtract(closest);
    if (normal.magnitude() < 1e-9) {
      normal = Vector3D.UP; // inside: push up onto the top
      closest = new Vector3D(p.x(), p.y(), ObstacleShapes.top(obstacle));
    } else {
      normal = normal.normalize();
    }
    drone.setPosition(closest.add(normal.scale(drone.getRadius())));
    Vector3D v = drone.getVelocity();
    double into = v.dot(normal);
    if (into < 0) {
      drone.setVelocity(v.subtract(normal.scale(into)));
    }
  }


  private void checkDroneCollisions() {
    List<Collision> hits = detector.between(droneList);
    if (hits.isEmpty()) {
      touching.clear(); // the usual case: nothing to allocate
      return;
    }
    // A pair stays in touching while it overlaps, so its collision is reported only once.
    Set<DronePair> now = new HashSet<>();
    for (Collision c : hits) {
      Drone first = (Drone) c.first();
      Drone second = (Drone) c.second();
      DronePair pair = new DronePair(first, second);
      now.add(pair);
      if (!touching.contains(pair)) {
        events.publish(SimEventType.COLLISION, first.getId(),
            "collided with " + second.getId());
        first.crash("collided with " + second.getId());
        second.crash("collided with " + first.getId());
      }
    }
    touching = now;
  }

  private void checkNoFlyZones() {
    if (noFlyZones.isEmpty()) {
      return;
    }
    for (Drone drone : droneList) {
      Vector3D position = drone.getPosition();
      for (NoFlyZone zone : noFlyZones) {
        DroneZone key = new DroneZone(drone, zone);
        if (zone.contains(position)) {
          if (inZone.add(key)) {
            events.publish(SimEventType.ZONE_ENTERED, drone.getId(), "entered " + zone.name());
          }
        } else if (inZone.remove(key)) {
          events.publish(SimEventType.ZONE_EXITED, drone.getId(), "left " + zone.name());
        }
      }
    }
  }

  /**
   * Returns the simulation time.
   *
   * @return the time in s
   */
  public double getTime() {
    return physics.getTime();
  }

  /**
   * Returns the drones in the order they were added.
   *
   * @return an unmodifiable list
   */
  public List<Drone> getDrones() {
    return List.copyOf(droneList);
  }

  /**
   * Returns the obstacles.
   *
   * @return an unmodifiable list
   */
  public List<Obstacle> getObstacles() {
    return obstacles.getObstacles();
  }

  /**
   * Returns the no-fly zones.
   *
   * @return an unmodifiable list
   */
  public List<NoFlyZone> getNoFlyZones() {
    return Collections.unmodifiableList(noFlyZones);
  }

  /**
   * Returns the weather.
   *
   * @return the wind
   */
  public WindField getWind() {
    return wind;
  }

  /**
   * Returns the event bus.
   *
   * @return the event bus
   */
  public EventBus getEvents() {
    return events;
  }

  /** Two drones in contact, in the order the detector reports them. */
  private record DronePair(Drone first, Drone second) {
  }

  /** A drone inside a no-fly zone. */
  private record DroneZone(Drone drone, NoFlyZone zone) {
  }
}
