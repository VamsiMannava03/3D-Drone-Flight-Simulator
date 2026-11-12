package dronesimulation.model.drones;

/** The kinds of drone {@link DroneFactory} can make. */
public enum DroneType {
  /** A hovering four-rotor multicopter: {@link QuadcopterDrone}. */
  QUADCOPTER,
  /** A heavier six-rotor multicopter for cargo: {@link HexacopterDrone}. */
  HEXACOPTER,
  /** A plane-like drone that must keep moving: {@link FixedWingDrone}. */
  FIXED_WING
}
