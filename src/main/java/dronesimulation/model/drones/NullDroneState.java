package dronesimulation.model.drones;

/**
 * A state that allows every action and never changes (Null Object pattern).
 */
public final class NullDroneState implements DroneState {

  /** The single shared instance. */
  public static final NullDroneState INSTANCE = new NullDroneState();

  private NullDroneState() {
    // use INSTANCE
  }

  @Override
  public String name() {
    return "NONE";
  }

  @Override
  public DroneState takeOff() {
    return this;
  }

  @Override
  public DroneState land() {
    return this;
  }

  @Override
  public DroneState crash() {
    return this;
  }
}
