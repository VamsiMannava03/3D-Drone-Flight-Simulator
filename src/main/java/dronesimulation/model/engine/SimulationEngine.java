package dronesimulation.model.engine;

import dronesimulation.model.events.SimEventType;
import dronesimulation.model.world.World;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs the simulation in fixed time steps.
 *
 * <p>The controller hands in actions with {@link #submit(Consumer)} from any thread. They are
 * queued and applied at the start of the next step on the simulation thread, so the model is
 * only ever touched by one thread and results stay deterministic. Views receive a
 * {@link WorldSnapshot} through listeners (Observer pattern) and never see the live model.
 */
public class SimulationEngine {

  /** Default time step, in s. */
  public static final double DEFAULT_DT = 0.01;
  /** Smallest allowed time scale. */
  public static final double MIN_TIME_SCALE = 0.1;
  /** Largest allowed time scale. */
  public static final double MAX_TIME_SCALE = 20;

  private final Supplier<World> scenario;
  private final double dt;
  private final Queue<Consumer<SimulationEngine>> inbox = new ConcurrentLinkedQueue<>();
  private final List<Consumer<WorldSnapshot>> snapshotListeners = new CopyOnWriteArrayList<>();
  /** Listeners whose failure was already reported; only used on the simulation thread. */
  private final Set<Consumer<WorldSnapshot>> failedListeners =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private final List<Consumer<SimulationEngine>> script = new ArrayList<>();
  private boolean replaying;
  private World world;
  private volatile boolean running;
  private volatile boolean paused;
  private volatile double timeScale = 1;

  /**
   * Creates an engine for one world. It cannot be reset.
   *
   * @param world the world to simulate
   * @param dt the fixed time step, in s, must be positive
   * @throws IllegalArgumentException if dt is not positive
   */
  public SimulationEngine(World world, double dt) {
    this(fixed(world), dt, false);
  }

  /**
   * Creates an engine for a scenario. {@link #reset()} builds a fresh world from it and replays
   * the script, so the same scenario always runs the same way.
   *
   * @param scenario builds the starting world; called now and on every reset
   * @param dt the fixed time step, in s, must be positive
   * @throws IllegalArgumentException if dt is not positive
   */
  public SimulationEngine(Supplier<World> scenario, double dt) {
    this(scenario, dt, true);
  }

  private SimulationEngine(Supplier<World> scenario, double dt, boolean resettable) {
    this.scenario = resettable ? Objects.requireNonNull(scenario, "scenario") : null;
    if (!(dt > 0) || !Double.isFinite(dt)) {
      throw new IllegalArgumentException("dt must be positive, got " + dt);
    }
    this.dt = dt;
    this.world = Objects.requireNonNull(scenario.get(), "world");
  }

  private static Supplier<World> fixed(World world) {
    Objects.requireNonNull(world, "world");
    return () -> world;
  }

  /**
   * Sets the actions applied at the start and again after every reset, such as a mission file.
   * The actions run on the simulation thread before the next step.
   *
   * @param actions the actions, in order
   */
  public void setScript(List<Consumer<SimulationEngine>> actions) {
    submit(e -> {
      script.clear();
      script.addAll(actions);
      runScript();
    });
  }

  /**
   * Throws the current world away, builds a fresh one from the scenario, and replays the script.
   * Time goes back to zero; with the same scenario and script, the run repeats exactly. Call it
   * from the simulation thread, for example inside an action passed to {@link #submit}.
   *
   * @throws IllegalStateException if this engine was created for a single world
   */
  public void reset() {
    if (scenario == null) {
      throw new IllegalStateException("this simulation has no scenario to reset to");
    }
    world.detach();
    world = Objects.requireNonNull(scenario.get(), "world");
    // The script replays right away: ahead of any command already queued behind the reset,
    // which may need the script's drones, and before the next frame is published, so views
    // never see the bare scenario.
    runScript();
  }

  /**
   * Returns the simulated world. Only use it from the simulation thread, for example inside an
   * action passed to {@link #submit(Consumer)}.
   *
   * @return the world
   */
  public World world() {
    return world;
  }

  /**
   * Returns the fixed time step.
   *
   * @return dt in s
   */
  public double getDt() {
    return dt;
  }

  /**
   * Queues an action to run on the simulation thread before the next step. Safe to call from any
   * thread. If the action throws, the error is reported as a COMMAND_REJECTED event.
   *
   * @param action the action
   */
  public void submit(Consumer<SimulationEngine> action) {
    inbox.add(Objects.requireNonNull(action, "action"));
  }

  /**
   * Registers a listener that receives a snapshot after each published frame.
   *
   * @param listener the listener
   */
  public void addSnapshotListener(Consumer<WorldSnapshot> listener) {
    snapshotListeners.add(Objects.requireNonNull(listener, "listener"));
  }

  /** Applies queued actions, then advances the world by one step. */
  public void tick() {
    processInbox();
    world.step(dt);
  }

  /**
   * Runs a number of steps as fast as possible, then publishes one snapshot. For tests and
   * batch runs.
   *
   * @param steps how many steps, at least 0
   */
  public void runSteps(int steps) {
    for (int i = 0; i < steps; i++) {
      tick();
    }
    publishSnapshot();
  }

  /**
   * Runs in real time until {@link #stop()} is called, publishing a snapshot every frame. Each
   * frame advances simulated time by {@code timeScale / frameRate} in whole fixed steps, so speed
   * changes never change the physics.
   *
   * @param frameRate frames per second, must be positive
   * @throws IllegalArgumentException if frameRate is not positive
   */
  public void runRealtime(double frameRate) {
    if (!(frameRate > 0)) {
      throw new IllegalArgumentException("frameRate must be positive, got " + frameRate);
    }
    running = true;
    long frameNanos = (long) (1e9 / frameRate);
    long next = System.nanoTime();
    double owed = 0;
    while (running) {
      if (paused) {
        processInbox();
      } else {
        owed += timeScale / frameRate;
        while (owed >= dt) {
          tick();
          owed -= dt;
        }
      }
      publishSnapshot();
      next += frameNanos;
      long sleep = next - System.nanoTime();
      if (sleep > 0) {
        try {
          Thread.sleep(sleep / 1_000_000, (int) (sleep % 1_000_000));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          running = false;
        }
      } else {
        next = System.nanoTime(); // fell behind: do not try to catch up in a burst
      }
    }
  }

  /** Makes {@link #runRealtime(double)} return after the current frame. Safe from any thread. */
  public void stop() {
    running = false;
  }

  /**
   * Pauses or resumes the simulation. Safe from any thread.
   *
   * @param paused true to pause
   */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  /**
   * Tells whether the simulation is paused.
   *
   * @return true if paused
   */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Sets how many simulated seconds pass per real second. Safe from any thread.
   *
   * @param scale the time scale, from {@link #MIN_TIME_SCALE} to {@link #MAX_TIME_SCALE}
   * @throws IllegalArgumentException if the scale is out of range
   */
  public void setTimeScale(double scale) {
    if (!(scale >= MIN_TIME_SCALE && scale <= MAX_TIME_SCALE)) {
      throw new IllegalArgumentException(String.format(Locale.ROOT,
          "speed must be between %.1f and %.0f, got %s", MIN_TIME_SCALE, MAX_TIME_SCALE, scale));
    }
    this.timeScale = scale;
  }

  /**
   * Returns the time scale.
   *
   * @return simulated seconds per real second
   */
  public double getTimeScale() {
    return timeScale;
  }

  /**
   * Returns a snapshot of the current state. Call it from the simulation thread.
   *
   * @return the snapshot
   */
  public WorldSnapshot snapshot() {
    return WorldSnapshot.of(world, paused, timeScale);
  }

  /** Sends a snapshot of the current state to every snapshot listener. */
  public void publishSnapshot() {
    if (snapshotListeners.isEmpty()) {
      return;
    }
    WorldSnapshot snapshot = snapshot();
    for (Consumer<WorldSnapshot> listener : snapshotListeners) {
      // A broken view must not stop the simulation or the other views. In the 3D mode this
      // runs on its own thread, where an escaping exception would freeze everything silently.
      try {
        listener.accept(snapshot);
      } catch (RuntimeException e) {
        if (failedListeners.add(listener)) { // it will likely fail every frame: say so once
          System.err.println("snapshot listener failed: " + e);
        }
      }
    }
  }

  private void processInbox() {
    Consumer<SimulationEngine> action;
    while ((action = inbox.poll()) != null) {
      run(action);
    }
    world.getEvents().flush(); // report rejections even while paused
  }

  private void runScript() {
    if (replaying) {
      // A reset inside the script would replay the script, which resets again, forever.
      throw new IllegalStateException("the script cannot reset the simulation");
    }
    replaying = true;
    try {
      for (Consumer<SimulationEngine> step : List.copyOf(script)) { // a step may set a script
        run(step);
      }
    } finally {
      replaying = false;
    }
  }

  /** Runs one action; if it fails, the failure is reported and the simulation goes on. */
  private void run(Consumer<SimulationEngine> action) {
    try {
      action.accept(this);
    } catch (RuntimeException e) {
      world.getEvents().publish(SimEventType.COMMAND_REJECTED, "sim", e.getMessage());
    }
  }
}
