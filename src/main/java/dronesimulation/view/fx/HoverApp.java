package dronesimulation.view.fx;

import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.view.SimulationView;
import dronesimulation.view.panel.ControlPanelModel;
import dronesimulation.view.panel.DronePalette;
import dronesimulation.view.panel.DroneSelection;
import dronesimulation.view.panel.DroneStatusModel;
import dronesimulation.view.panel.EventLog;
import dronesimulation.view.panel.MissionEditorModel;
import dronesimulation.view.scene.World3D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;

/**
 * The Hover window: the control panel on top, the simulation in the middle (a 3D view, or the
 * top-down and side views), the drone status on the right, and the mission editor and event
 * log below. It is a {@link SimulationView}: it receives read-only snapshots and events, and
 * changes the simulation only through the {@link HoverFacade}.
 */
public final class HoverApp extends Application implements SimulationView {

  /** The window title. */
  public static final String TITLE = "Hover drone flight simulator";

  /**
   * What the window needs from the program that opens it.
   *
   * @param engine the simulation, run on its own thread at the given frame rate
   * @param frameRate snapshots per second
   * @param connect hooks a view up to the simulation's snapshots and events
   * @param screenshot if not null, save pictures of the views here and close
   * @param screenshotAt how many simulated seconds to wait before the pictures
   */
  public record Setup(SimulationEngine engine, double frameRate,
      Consumer<SimulationView> connect, Path screenshot, double screenshotAt) {

    /**
     * Checks the setup.
     *
     * @throws NullPointerException if the engine or the connector is missing
     */
    public Setup {
      Objects.requireNonNull(engine, "engine");
      Objects.requireNonNull(connect, "connect");
    }
  }

  private static Setup setup;

  private final AtomicReference<WorldSnapshot> pending = new AtomicReference<>();
  private final Queue<String> messages = new ConcurrentLinkedQueue<>();
  private final DroneSelection selection = new DroneSelection();
  private final DronePalette palette = new DronePalette();
  private ControlPane controls;
  private StatusPane status;
  private MissionEditorPane editor;
  private EventLogPane log;
  private World3D world3d;
  private PlanView top;
  private PlanView side;
  private TabPane views;
  private Stage stage;
  private WorldSnapshot latest;
  private boolean shooting;

  /** Creates the window; JavaFX calls this when the application starts. */
  public HoverApp() {
    // built in start()
  }

  /**
   * Opens the window and runs the simulation until the window closes.
   *
   * @param how what the window needs
   */
  public static void open(Setup how) {
    setup = Objects.requireNonNull(how, "setup");
    Application.launch(HoverApp.class);
  }

  @Override
  public void start(Stage primary) {
    stage = primary;
    HoverFacade hover = new HoverFacade(setup.engine());
    controls = new ControlPane(new ControlPanelModel(hover, selection));
    status = new StatusPane(new DroneStatusModel(selection), selection);
    editor = new MissionEditorPane(new MissionEditorModel(hover, selection, Platform::runLater));
    log = new EventLogPane(new EventLog());
    world3d = new World3D(selection, palette);
    top = new PlanView(PlanView.Projection.TOP, selection, palette);
    side = new PlanView(PlanView.Projection.SIDE, selection, palette);

    CheckBox follow = new CheckBox("Follow selected drone");
    follow.setOnAction(e -> world3d.setFollowing(follow.isSelected()));
    Button overview = Panes.button("Overview", () -> {
      world3d.overview();
      follow.setSelected(false);
    });
    Label help = new Label("Drag to turn, right-drag to move, scroll to zoom");
    help.setStyle("-fx-text-fill: #64748B;");
    HBox bar3d = new HBox(10, follow, overview, help);
    bar3d.setAlignment(Pos.CENTER_LEFT);
    bar3d.setPadding(new Insets(4, 8, 4, 8));
    BorderPane pane3d = new BorderPane(world3d, bar3d, null, null, null);
    SplitPane plans = new SplitPane(top, side);
    views = new TabPane(new Tab("3D view", pane3d), new Tab("Top and side views", plans));
    views.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    BorderPane centre = new BorderPane(views, null, status, null, null);
    SplitPane bottom = new SplitPane(editor, log);
    bottom.setDividerPositions(0.5);
    SplitPane main = new SplitPane(centre, bottom);
    main.setOrientation(Orientation.VERTICAL);
    main.setDividerPositions(0.7);
    BorderPane root = new BorderPane(main, controls, null, null, null);

    stage.setTitle(TITLE);
    stage.setScene(new Scene(root, 1440, 920));
    stage.setMinWidth(1000);
    stage.setMinHeight(640);
    stage.setOnCloseRequest(e -> {
      world3d.stop();
      setup.engine().stop();
      Platform.exit();
    });
    stage.show();

    setup.connect().accept(this);
    Thread simulation = new Thread(() -> setup.engine().runRealtime(setup.frameRate()),
        "simulation");
    simulation.setDaemon(true);
    simulation.start();
  }

  @Override
  public void render(WorldSnapshot snapshot) {
    if (pending.getAndSet(snapshot) == null) {
      Platform.runLater(() -> apply(pending.getAndSet(null)));
    }
  }

  @Override
  public void showMessage(String message) {
    messages.add(message);
    Platform.runLater(this::drainMessages);
  }

  private void drainMessages() {
    List<String> lines = new ArrayList<>();
    String line;
    while ((line = messages.poll()) != null) {
      lines.add(line);
    }
    if (!lines.isEmpty()) {
      log.append(lines);
    }
  }

  private void apply(WorldSnapshot snapshot) {
    if (snapshot == null) {
      return;
    }
    if (latest != null && snapshot.time() < latest.time()) {
      palette.clear();
    }
    latest = snapshot;
    controls.update(snapshot);
    status.update(snapshot);
    editor.update(snapshot);
    world3d.update(snapshot);
    top.update(snapshot);
    side.update(snapshot);
    if (setup.screenshot() != null && !shooting && snapshot.time() >= setup.screenshotAt()) {
      shooting = true;
      takeScreenshots();
    }
  }

  private void takeScreenshots() {
    Path base = setup.screenshot();
    String name = base.getFileName().toString().replaceFirst("\\.png$", "");
    Path dir = base.toAbsolutePath().getParent();
    after(0.8, () -> {
      save(dir.resolve(name + "-3d.png"));
      views.getSelectionModel().select(1);
      after(0.8, () -> {
        save(dir.resolve(name + "-2d.png"));
        world3d.setFollowing(true);
        views.getSelectionModel().select(0);
        after(2.5, () -> {
          save(dir.resolve(name + "-follow.png"));
          stage.getOnCloseRequest().handle(null);
        });
      });
    });
  }

  private static void after(double seconds, Runnable action) {
    PauseTransition wait = new PauseTransition(Duration.seconds(seconds));
    wait.setOnFinished(e -> action.run());
    wait.play();
  }

  private void save(Path file) {
    WritableImage image = stage.getScene().snapshot(null);
    int w = (int) image.getWidth();
    int h = (int) image.getHeight();
    int[] pixels = new int[w * h];
    image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0,
        w);
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    out.setRGB(0, 0, w, h, pixels, 0, w);
    try {
      ImageIO.write(out, "png", file.toFile());
      System.out.println("Saved " + file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
