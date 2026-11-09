package dronesimulation.model.events;

/** The kinds of events the simulation reports. */
public enum SimEventType {
  /** A drone finished taking off and is now flying. */
  TOOK_OFF,
  /** A drone touched down at or below its safe landing speed. */
  LANDED,
  /** A drone hit the ground too fast, or collided with something. */
  CRASH,
  /** A drone touched another drone or an obstacle. */
  COLLISION,
  /** A drone's battery fell below its warning level. */
  LOW_BATTERY,
  /** A drone's battery ran out and its motors stopped. */
  BATTERY_DEPLETED,
  /** A drone entered a no-fly zone. */
  ZONE_ENTERED,
  /** A drone left a no-fly zone. */
  ZONE_EXITED,
  /** A drone released its payload. */
  PAYLOAD_DROPPED,
  /** A policy sent a drone home, abandoning the rest of its mission. */
  RETURN_HOME,
  /** A drone finished one step of its mission. */
  MISSION_STEP_DONE,
  /** A drone finished every step of its mission. */
  MISSION_COMPLETE,
  /** A command could not run, for example taking off after a crash. */
  COMMAND_REJECTED
}
