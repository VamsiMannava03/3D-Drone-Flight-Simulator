package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;

/** A six-rotor multicopter: heavier and stronger than a quadcopter, for carrying cargo. */
public class HexacopterDrone extends MultirotorDrone {

  /**
   * Creates a hexacopter. Use {@link DroneBuilder} or {@link DroneFactory}.
   *
   * @param id a unique, non-blank name
   * @param mass the empty mass in kg
   * @param radius the collision radius in m
   * @param home the start position
   * @param spec the performance limits
   * @param battery the battery
   * @param sensor the position sensor
   */
  HexacopterDrone(String id, double mass, double radius, Vector3D home, DroneSpec spec,
      Battery battery, Sensor sensor) {
    super(id, mass, radius, home, spec, battery, sensor);
  }

  @Override
  public int getRotorCount() {
    return 6;
  }

  @Override
  public String getTypeName() {
    return "Hexacopter";
  }
}
