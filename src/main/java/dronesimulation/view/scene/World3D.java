package dronesimulation.view.scene;

import dronesimulation.model.Vector3D;
import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.obstacles.SphereObstacle;
import dronesimulation.model.world.NoFlyZone;
import dronesimulation.view.panel.DronePalette;
import dronesimulation.view.panel.DroneSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Affine;

/**
 * The 3D view: the landscape and town, no-fly zones, and every drone with its trail and
 * target, seen through an orbit camera. Drag to turn the camera, drag with the right button
 * to move it, and scroll to zoom. With follow on, the camera keeps the selected drone in the
 * middle.
 *
 * <p>The view only reads snapshots; the model is z-up, so the whole world hangs under one
 * group that turns it into JavaFX's y-down coordinates.
 */
public final class World3D extends StackPane {

  /** The vertical field of view in degrees. */
  static final double FIELD_OF_VIEW = 50;

  private static final double[] OVERVIEW_TARGET = {20, 0, 10};
  private static final double OVERVIEW_DISTANCE = 125;
  private static final double[] SUN = {0.55, -0.42, 0.72};
  private static final int DARK = 0x24272C;
  private static final int ROTOR = 0x15171A;
  private static final int THRUST = 0xFFDB40;
  private static final int ZONE = 0xEF4444;

  private static final float[] QUAD_FRAME = DroneModels.quadFrame().toArray();
  private static final float[] QUAD_BODY = DroneModels.quadBody().toArray();
  private static final float[] HEX_FRAME = DroneModels.hexFrame().toArray();
  private static final float[] HEX_BODY = DroneModels.hexBody().toArray();
  private static final float[] ROTOR_BLADES = DroneModels.rotorBlades().toArray();
  private static final float[] PLANE_BODY = DroneModels.planeBody().toArray();
  private static final float[] PLANE_DETAILS = DroneModels.planeDetails().toArray();
  private static final float[] PROPELLER = DroneModels.propeller().toArray();
  private static final float[] PAD =
      new MeshBuilder().disc(Mat4.identity(), 1, 0, 40).toArray();
  private static final float[] ZONE_WALL =
      new MeshBuilder().cylinder(Mat4.identity(), 1, 0, 1, 64, false).toArray();

  private final DroneSelection selection;
  private final DronePalette palette;
  private final Group world = new Group();
  private final Group town = new Group();
  private final Group zones = new Group();
  private final Group drones = new Group();
  private final Group lines = new Group();
  private final Pane labels = new Pane();
  private final PerspectiveCamera camera = new PerspectiveCamera(true);
  private final Affine cameraPlace = new Affine();
  private final OrbitCamera orbit = new OrbitCamera(OVERVIEW_TARGET, -2.3, 0.5,
      OVERVIEW_DISTANCE);
  private final Map<String, DroneNode> nodes = new HashMap<>();
  private final Map<Integer, TriangleMesh> lineMeshes = new HashMap<>();
  private final SubScene subScene;
  private final AnimationTimer timer;
  private List<Obstacle> builtFor;
  private List<NoFlyZone> zonesFor;
  private WorldSnapshot latest;
  private double lastTime = -1;
  private boolean following;
  private double dragX;
  private double dragY;
  private long lastFrame;

  /**
   * Creates the 3D view.
   *
   * @param selection the drone picked in the window, followed by the camera
   * @param palette gives each drone its colour
   */
  public World3D(DroneSelection selection, DronePalette palette) {
    this.selection = Objects.requireNonNull(selection, "selection");
    this.palette = Objects.requireNonNull(palette, "palette");
    world.getTransforms().add(new Affine(1, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0, 0));
    world.getChildren().addAll(landscape(), town, drones, lines, zones, lights(), camera);
    camera.setFieldOfView(FIELD_OF_VIEW);
    camera.setNearClip(0.3);
    camera.setFarClip(9000);
    camera.getTransforms().add(cameraPlace);
    subScene = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
    subScene.setFill(Color.rgb(0xBD, 0xD1, 0xE4));
    subScene.setCamera(camera);
    subScene.widthProperty().bind(widthProperty());
    subScene.heightProperty().bind(heightProperty());
    labels.setMouseTransparent(true);
    getChildren().addAll(subScene, labels);
    setMinSize(200, 150);
    installMouse();
    placeCamera();
    timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        double dt = lastFrame == 0 ? 1.0 / 60 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        frame(dt);
      }
    };
    timer.start();
  }

  /**
   * Shows a new snapshot from the next frame on.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    latest = snapshot;
  }

  /**
   * Turns following the selected drone on or off.
   *
   * @param follow true to keep the selected drone in the middle
   */
  public void setFollowing(boolean follow) {
    following = follow;
    if (!follow) {
      return;
    }
    DroneSnapshot d = selected();
    if (d != null) {
      orbit.moveTo(DroneMotion.visualCentre(d), Math.min(orbit.getDistance(), 40));
    }
  }

  /**
   * Tells whether the camera follows the selected drone.
   *
   * @return true when following
   */
  public boolean isFollowing() {
    return following;
  }

  /** Puts the camera back over the whole town. */
  public void overview() {
    following = false;
    orbit.moveTo(OVERVIEW_TARGET, OVERVIEW_DISTANCE);
    placeCamera();
  }

  /** Stops the frame timer, when the window closes. */
  public void stop() {
    timer.stop();
  }

  private static Group landscape() {
    Group land = new Group();
    land.getChildren().addAll(FxMeshes.views(TownBuilder.landscape(200)));
    MeshView water = FxMeshes.view(TownBuilder.water().toArray(),
        FxMeshes.rgb(TownBuilder.WATER, 0.85));
    land.getChildren().add(water);
    return land;
  }

  private static Group lights() {
    AmbientLight ambient = new AmbientLight(Color.gray(0.58));
    DirectionalLight sun = new DirectionalLight(Color.gray(0.62));
    sun.setDirection(new Point3D(-SUN[0], -SUN[1], -SUN[2]));
    return new Group(ambient, sun);
  }

  private void installMouse() {
    subScene.setOnMousePressed(e -> {
      dragX = e.getX();
      dragY = e.getY();
    });
    subScene.setOnMouseDragged(e -> {
      double dx = e.getX() - dragX;
      double dy = e.getY() - dragY;
      dragX = e.getX();
      dragY = e.getY();
      if (e.getButton() == MouseButton.PRIMARY) {
        orbit.orbit(-dx * 0.006, dy * 0.006);
      } else {
        following = false;
        orbit.pan(dx, dy);
      }
      placeCamera();
    });
    subScene.setOnScroll(e -> {
      orbit.zoom(e.getDeltaY() / 40);
      placeCamera();
    });
  }

  private void placeCamera() {
    FxMeshes.set(cameraPlace, new CameraFrame(orbit.eye(), orbit.target()).matrix());
  }

  private DroneSnapshot selected() {
    if (latest == null) {
      return null;
    }
    String id = selection.get();
    return latest.drones().stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
  }

  private void frame(double dt) {
    WorldSnapshot s = latest;
    if (s == null) {
      return;
    }
    if (s.time() < lastTime) {
      nodes.values().forEach(n -> drones.getChildren().remove(n.group));
      nodes.clear(); // the simulation was reset: fresh trails
    }
    lastTime = s.time();
    if (!s.obstacles().equals(builtFor)) {
      builtFor = s.obstacles();
      rebuildTown(s.obstacles());
    }
    if (!s.noFlyZones().equals(zonesFor)) {
      zonesFor = s.noFlyZones();
      rebuildZones(s.noFlyZones());
    }
    nodes.keySet().removeIf(id -> {
      boolean gone = s.drones().stream().noneMatch(d -> d.id().equals(id));
      if (gone) {
        drones.getChildren().remove(nodes.get(id).group);
      }
      return gone;
    });
    for (DroneSnapshot d : s.drones()) {
      DroneNode node = nodes.computeIfAbsent(d.id(), id -> {
        DroneNode n = new DroneNode(d, palette.colourOf(id));
        drones.getChildren().add(n.group);
        return n;
      });
      node.update(d, dt);
    }
    DroneSnapshot followed = selected();
    if (following && followed != null) {
      orbit.follow(DroneMotion.visualCentre(followed), dt);
    }
    placeCamera();
    drawLines(s);
    drawLabels(s);
  }

  private void rebuildTown(List<Obstacle> obstacles) {
    town.getChildren().setAll(FxMeshes.views(TownBuilder.town(obstacles)));
    for (Obstacle o : obstacles) {
      if (o instanceof SphereObstacle ball) {
        Sphere sphere = new Sphere(ball.getRadius(), 32);
        sphere.setMaterial(FxMeshes.material(FxMeshes.rgb(TownBuilder.BUILDING_COLOURS[2])));
        Vector3D c = ball.getCenter();
        sphere.getTransforms().add(new Affine(1, 0, 0, c.x(), 0, 1, 0, c.y(), 0, 0, 1, c.z()));
        town.getChildren().add(sphere);
      }
    }
  }

  private void rebuildZones(List<NoFlyZone> noFlyZones) {
    zones.getChildren().clear();
    for (NoFlyZone z : noFlyZones) {
      MeshView wall = FxMeshes.view(new MeshBuilder().append(ZONE_WALL,
          Mat4.mul(Mat4.translation(z.centreX(), z.centreY(), 0),
              Mat4.scale(z.radius(), z.radius(), z.ceiling()))).toArray(),
          FxMeshes.rgb(ZONE, 0.13));
      zones.getChildren().add(wall);
    }
  }

  private void drawLines(WorldSnapshot s) {
    Map<Integer, MeshBuilder> byColour = new HashMap<>();
    double width = Math.max(0.12, orbit.getDistance() * 0.0022);
    for (DroneSnapshot d : s.drones()) {
      DroneNode node = nodes.get(d.id());
      int colour = node.motion.colour();
      MeshBuilder own = byColour.computeIfAbsent(colour, c -> new MeshBuilder());
      Lines.ring(own, d.home().x(), d.home().y(), 0.08, 1.8, 32, width);
      Lines.polyline(own, node.motion.trail(), width);
      double[] centre = DroneMotion.visualCentre(d);
      Vector3D target = d.target();
      if (target != null && DroneMotion.isActive(d)) {
        double[] t = {target.x(), target.y(), target.z()};
        double r = 1.2;
        Lines.segment(own, new double[] {t[0] - r, t[1], t[2]}, new double[] {t[0] + r, t[1],
            t[2]}, width);
        Lines.segment(own, new double[] {t[0], t[1] - r, t[2]}, new double[] {t[0], t[1] + r,
            t[2]}, width);
        Lines.segment(own, new double[] {t[0], t[1], t[2] - r}, new double[] {t[0], t[1],
            t[2] + r}, width);
        for (double z = 0; z < t[2]; z += 2) {
          Lines.segment(own, new double[] {t[0], t[1], z},
              new double[] {t[0], t[1], Math.min(z + 1, t[2])}, width * 0.6);
        }
      }
      if (d.thrust().magnitude() > 0.1 && !DroneMotion.isCrashed(d)) {
        Vector3D th = d.thrust();
        MeshBuilder thrust = byColour.computeIfAbsent(THRUST, c -> new MeshBuilder());
        Lines.segment(thrust, centre, new double[] {centre[0] + th.x() * 0.12,
            centre[1] + th.y() * 0.12, centre[2] + th.z() * 0.12}, width);
      }
      if (d.id().equals(selection.get())) {
        Lines.ring(own, centre[0], centre[1], centre[2], 2.2, 32, width);
      }
    }
    MeshBuilder zoneRings = byColour.computeIfAbsent(ZONE, c -> new MeshBuilder());
    for (NoFlyZone z : s.noFlyZones()) {
      Lines.ring(zoneRings, z.centreX(), z.centreY(), 0.1, z.radius(), 64, width * 1.5);
      Lines.ring(zoneRings, z.centreX(), z.centreY(), z.ceiling(), z.radius(), 64, width);
    }
    for (Map.Entry<Integer, MeshBuilder> e : byColour.entrySet()) {
      TriangleMesh mesh = lineMeshes.get(e.getKey());
      if (mesh == null) {
        mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        MeshView view = new MeshView(mesh);
        view.setMaterial(FxMeshes.glow(FxMeshes.rgb(e.getKey(), 0.9)));
        view.setCullFace(CullFace.NONE);
        lineMeshes.put(e.getKey(), mesh);
        lines.getChildren().add(view);
      }
      FxMeshes.update(mesh, e.getValue().toArray());
    }
    for (Map.Entry<Integer, TriangleMesh> e : lineMeshes.entrySet()) {
      if (!byColour.containsKey(e.getKey())) {
        FxMeshes.update(e.getValue(), new float[0]);
      }
    }
  }

  private void drawLabels(WorldSnapshot s) {
    CameraFrame frame = new CameraFrame(orbit.eye(), orbit.target());
    labels.getChildren().clear();
    for (DroneSnapshot d : s.drones()) {
      double[] c = DroneMotion.visualCentre(d);
      double[] at = frame.project(new double[] {c[0], c[1], c[2] + 1.6}, FIELD_OF_VIEW,
          getWidth(), getHeight());
      if (at == null || at[0] < 0 || at[1] < 0 || at[0] > getWidth() || at[1] > getHeight()) {
        continue;
      }
      Label label = new Label(d.id() + "  " + d.state());
      int colour = nodes.get(d.id()).motion.colour();
      label.setStyle(String.format("-fx-background-color: rgba(15,20,28,0.62);"
          + " -fx-text-fill: white; -fx-padding: 1 6 1 6; -fx-background-radius: 4;"
          + " -fx-font-size: 11px; -fx-border-color: #%06X; -fx-border-radius: 4;"
          + " -fx-border-width: 0 0 0 3;", colour));
      label.relocate(at[0] + 8, at[1] - 20);
      labels.getChildren().add(label);
    }
  }

  /** The shapes of one drone, moved every frame. */
  private static final class DroneNode {
    private final Group group = new Group();
    private final DroneMotion motion;
    private final Affine model = new Affine();
    private final Affine[] spinners;
    private final MeshView body;
    private final boolean plane;
    private final double[][] motors;
    private final Color colour;

    DroneNode(DroneSnapshot d, int rgb) {
      motion = new DroneMotion(rgb);
      colour = FxMeshes.rgb(rgb);
      plane = DroneMotion.isPlane(d);
      boolean hex = DroneMotion.isHex(d);
      Vector3D home = d.home();
      MeshView pad = FxMeshes.view(new MeshBuilder().append(PAD,
          Mat4.mul(Mat4.translation(home.x(), home.y(), 0.04), Mat4.scale(1.8, 1.8, 1)))
          .toArray(), FxMeshes.rgb(0x3A3F47));
      Group craft = new Group();
      craft.getTransforms().add(model);
      if (plane) {
        body = FxMeshes.view(PLANE_BODY, colour);
        motors = new double[0][];
        Affine spin = new Affine();
        MeshView prop = FxMeshes.view(PROPELLER, FxMeshes.rgb(DARK));
        prop.getTransforms().add(spin);
        spinners = new Affine[] {spin};
        craft.getChildren().addAll(body, FxMeshes.view(PLANE_DETAILS, FxMeshes.rgb(DARK)),
            prop);
      } else {
        body = FxMeshes.view(hex ? HEX_BODY : QUAD_BODY, colour);
        craft.getChildren().addAll(FxMeshes.view(hex ? HEX_FRAME : QUAD_FRAME,
            FxMeshes.rgb(DARK)), body);
        motors = DroneMotion.motorsOf(d);
        spinners = new Affine[motors.length];
        for (int m = 0; m < motors.length; m++) {
          spinners[m] = new Affine();
          MeshView rotor = FxMeshes.view(ROTOR_BLADES, FxMeshes.rgb(ROTOR));
          rotor.getTransforms().add(spinners[m]);
          craft.getChildren().add(rotor);
        }
      }
      group.getChildren().addAll(pad, craft);
    }

    void update(DroneSnapshot d, double dt) {
      motion.update(d, dt);
      FxMeshes.set(model, motion.model(d));
      double angle = motion.rotorAngle();
      if (plane) {
        double[] at = DroneModels.PLANE_PROPELLER;
        FxMeshes.set(spinners[0], Mat4.mul(Mat4.translation(at[0], at[1], at[2]),
            Mat4.rotationX(angle)));
      } else {
        for (int m = 0; m < motors.length; m++) {
          double[] at = motors[m];
          double spin = (m % 2 == 0 ? 1 : -1) * angle;
          FxMeshes.set(spinners[m], Mat4.mul(Mat4.translation(at[0], at[1],
              at[2] + DroneModels.ROTOR_HEIGHT + 0.004), Mat4.rotationZ(spin + m)));
        }
      }
      Color shown = DroneMotion.isCrashed(d) ? colour.deriveColor(0, 1, 0.45, 1) : colour;
      ((PhongMaterial) body.getMaterial()).setDiffuseColor(shown);
    }
  }
}
