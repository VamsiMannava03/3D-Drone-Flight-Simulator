package dronesimulation.model.states;

/** Wrecked. The end of the lifecycle: every action is rejected. */
public final class Crashed implements DroneState {

  /** The single instance; states hold no per-drone data. */
  public static final Crashed INSTANCE = new Crashed();

  private Crashed() {
  }

  @Override
  public String name() {
    return "Crashed";
  }
}
