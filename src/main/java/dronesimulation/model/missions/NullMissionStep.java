package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;

/**
 * A step that does nothing and is always complete (Null Object pattern).
 */
public final class NullMissionStep implements MissionStep {

  /** The single shared instance. */
  public static final NullMissionStep INSTANCE = new NullMissionStep();

  private NullMissionStep() {
    // use INSTANCE
  }

  @Override
  public boolean isValidFor(Drone drone) {
    return true;
  }

  @Override
  public void execute(Drone drone) {
    // does nothing
  }

  @Override
  public boolean isComplete(Drone drone) {
    return true;
  }
}
