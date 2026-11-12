package dronesimulation.view.fx;

import dronesimulation.model.Vector3D;
import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.model.obstacles.BoxObstacle;
import dronesimulation.model.obstacles.Obstacle;
import dronesimulation.model.obstacles.SphereObstacle;
import dronesimulation.model.world.Building;
import dronesimulation.model.world.NoFlyZone;
import dronesimulation.view.Bounds2D;
import dronesimulation.view.panel.DronePalette;
import dronesimulation.view.panel.DroneSelection;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A 2D view of the world from above or from the south side, with a grid in metres. It fits
 * itself to everything it has seen, and draws buildings, no-fly zones, trails and drones.
 */
public final class PlanView extends Pane {

  /** Which way the view looks. */
  public enum Projection {
    /** From above: x across, y up. */
    TOP("Top view", "x (m)", "y (m)"),
    /** From the south: x across, height up. */
    SIDE("Side view", "x (m)", "z (m)");

    private final String title;
    private final String hLabel;
    private final String vLabel;

    Projection(String title, String hLabel, String vLabel) {
      this.title = title;
      this.hLabel = hLabel;
      this.vLabel = vLabel;
    }

    double h(Vector3D p) {
      return p.x();
    }

    double v(Vector3D p) {
      return this == TOP ? p.y() : p.z();
    }
  }

  private static final int TRAIL_LENGTH = 800;
  private static final double MIN_SPAN = 10;
  private static final double PADDING = 0.08;
  private static final double LEFT = 52;
  private static final double RIGHT = 16;
  private static final double TOP = 34;
  private static final double BOTTOM = 40;
  private static final Color BACKGROUND = Color.web("#F8FAFC");
  private static final Color GRID = Color.web("#E2E8F0");
  private static final Color AXIS_TEXT = Color.web("#64748B");
  private static final Color GROUND = Color.web("#D9CBB0");
  private static final Color GROUND_LINE = Color.web("#8B6F47");
  private static final Color BUILDING = Color.web("#94A3B8");
  private static final Color HOUSE_WALL = Color.web("#E4D6B8");
  private static final Color HOUSE_ROOF = Color.web("#9C5A44");
  private static final Color ZONE_FILL = Color.rgb(220, 38, 38, 0.16);
  private static final Color ZONE_EDGE = Color.rgb(220, 38, 38, 0.63);
  private static final Color SHADOW = Color.rgb(0, 0, 0, 0.16);

  private final Projection projection;
  private final DroneSelection selection;
  private final DronePalette palette;
  private final Canvas canvas = new Canvas();
  private final Map<String, Deque<Vector3D>> trails = new LinkedHashMap<>();
  private Bounds2D seen;
  private WorldSnapshot latest;

  /**
   * Creates a 2D view.
   *
   * @param projection which way it looks
   * @param selection the drone picked in the window, drawn with a ring
   * @param palette gives each drone its colour
   */
  public PlanView(Projection projection, DroneSelection selection, DronePalette palette) {
    this.projection = projection;
    this.selection = selection;
    this.palette = palette;
    getChildren().add(canvas);
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
    canvas.widthProperty().addListener(o -> paint());
    canvas.heightProperty().addListener(o -> paint());
    setMinSize(200, 150);
  }

  /**
   * Shows a new snapshot: extends the trails and the fitted area, then repaints.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    boolean reset = latest != null && snapshot.time() < latest.time();
    boolean advanced = latest == null || reset || snapshot.time() > latest.time();
    if (reset) {
      trails.clear();
      seen = null;
    }
    trails.keySet().removeIf(id -> snapshot.drones().stream().noneMatch(d -> d.id().equals(id)));
    if (latest == null || reset) {
      for (Obstacle o : snapshot.obstacles()) {
        double[] box = bounds(o);
        include(new Vector3D(box[0], box[1], box[2]));
        include(new Vector3D(box[3], box[4], box[5]));
      }
      for (NoFlyZone z : snapshot.noFlyZones()) {
        include(new Vector3D(z.centreX() - z.radius(), z.centreY() - z.radius(), 0));
        include(new Vector3D(z.centreX() + z.radius(), z.centreY() + z.radius(), z.ceiling()));
      }
    }
    latest = snapshot;
    for (DroneSnapshot drone : snapshot.drones()) {
      Deque<Vector3D> trail = trails.computeIfAbsent(drone.id(), id -> new ArrayDeque<>());
      if (advanced) {
        trail.addLast(drone.position());
        if (trail.size() > TRAIL_LENGTH) {
          trail.removeFirst();
        }
      }
      include(drone.position());
    }
    paint();
  }

  private void include(Vector3D p) {
    double h = projection.h(p);
    double v = projection.v(p);
    seen = seen == null ? Bounds2D.around(h, v) : seen.include(h, v);
    if (projection == Projection.SIDE) {
      seen = seen.include(h, 0);
    }
  }

  private static double[] bounds(Obstacle o) {
    if (o instanceof Building b) {
      return new double[] {b.min().x(), b.min().y(), b.min().z(), b.max().x(), b.max().y(),
          b.max().z()};
    }
    if (o instanceof BoxObstacle b) {
      return new double[] {b.getMin().x(), b.getMin().y(), b.getMin().z(), b.getMax().x(),
          b.getMax().y(), b.getMax().z()};
    }
    if (o instanceof SphereObstacle s) {
      Vector3D c = s.getCenter();
      double r = s.getRadius();
      return new double[] {c.x() - r, c.y() - r, c.z() - r, c.x() + r, c.y() + r, c.z() + r};
    }
    return new double[] {0, 0, 0, 0, 0, 0};
  }

  private void paint() {
    GraphicsContext g = canvas.getGraphicsContext2D();
    double w = canvas.getWidth();
    double h = canvas.getHeight();
    g.setFill(Color.WHITE);
    g.fillRect(0, 0, w, h);
    double plotW = w - LEFT - RIGHT;
    double plotH = h - TOP - BOTTOM;
    g.setFill(Color.web("#334155"));
    g.setFont(Font.font("System", FontWeight.BOLD, 14));
    g.fillText(projection.title, LEFT, 22);
    if (seen == null || latest == null || plotW <= 0 || plotH <= 0) {
      return;
    }
    Bounds2D fit = seen.padded(MIN_SPAN, PADDING);
    if (projection == Projection.SIDE) {
      fit = fit.withFloor(-MIN_SPAN * PADDING);
    }
    double scale = Math.min(plotW / (fit.maxH() - fit.minH()),
        plotH / (fit.maxV() - fit.minV()));
    Mapper m = new Mapper(LEFT + plotW / 2.0, TOP + plotH / 2.0,
        (fit.minH() + fit.maxH()) / 2, (fit.minV() + fit.maxV()) / 2, scale);
    double minH = m.toH(LEFT);
    double maxH = m.toH(LEFT + plotW);
    double minV = m.toV(TOP + plotH);
    double maxV = m.toV(TOP);

    g.setFill(BACKGROUND);
    g.fillRect(LEFT, TOP, plotW, plotH);
    g.save();
    g.beginPath();
    g.rect(LEFT, TOP, plotW, plotH);
    g.clip();
    if (projection == Projection.SIDE) {
      double groundY = m.toY(0);
      g.setFill(GROUND);
      g.fillRect(LEFT, groundY, plotW, TOP + plotH - groundY);
    }
    double step = Bounds2D.niceStep(Math.max(maxH - minH, maxV - minV), 10);
    g.setStroke(GRID);
    g.setLineWidth(1);
    for (double v = Math.ceil(minH / step) * step; v <= maxH; v += step) {
      double x = Math.round(m.toX(v)) + 0.5;
      g.strokeLine(x, TOP, x, TOP + plotH);
    }
    for (double v = Math.ceil(minV / step) * step; v <= maxV; v += step) {
      double y = Math.round(m.toY(v)) + 0.5;
      g.strokeLine(LEFT, y, LEFT + plotW, y);
    }
    paintZones(g, m);
    paintObstacles(g, m);
    if (projection == Projection.SIDE) {
      g.setStroke(GROUND_LINE);
      g.setLineWidth(2);
      g.strokeLine(LEFT, m.toY(0), LEFT + plotW, m.toY(0));
    }
    paintTrails(g, m);
    paintDrones(g, m);
    g.restore();

    g.setFill(AXIS_TEXT);
    g.setFont(Font.font(11));
    for (double v = Math.ceil(minH / step) * step; v <= maxH; v += step) {
      g.fillText(format(v), m.toX(v) - 8, TOP + plotH + 16);
    }
    for (double v = Math.ceil(minV / step) * step; v <= maxV; v += step) {
      g.fillText(format(v), 8, m.toY(v) + 4);
    }
    g.setStroke(GRID.darker());
    g.setLineWidth(1);
    g.strokeRect(LEFT + 0.5, TOP + 0.5, plotW, plotH);
    g.setFont(Font.font(12));
    g.fillText(projection.hLabel, LEFT + plotW - 40, TOP + plotH + 34);
    g.fillText(projection.vLabel, 6, TOP - 8);
  }

  private void paintObstacles(GraphicsContext g, Mapper m) {
    g.setLineWidth(1);
    for (Obstacle o : latest.obstacles()) {
      if (o instanceof Building b && b.style() == Building.Style.HOUSE) {
        paintHouse(g, m, b);
        continue;
      }
      double[] box = bounds(o);
      Vector3D min = new Vector3D(box[0], box[1], box[2]);
      Vector3D max = new Vector3D(box[3], box[4], box[5]);
      double[] r = rect(m, projection.h(min), projection.v(min), projection.h(max),
          projection.v(max));
      g.setFill(BUILDING);
      g.setStroke(BUILDING.darker());
      if (o instanceof SphereObstacle) {
        g.fillOval(r[0], r[1], r[2], r[3]);
        g.strokeOval(r[0], r[1], r[2], r[3]);
      } else {
        g.fillRect(r[0], r[1], r[2], r[3]);
        g.strokeRect(r[0], r[1], r[2], r[3]);
      }
    }
  }

  private void paintHouse(GraphicsContext g, Mapper m, Building o) {
    Vector3D min = o.min();
    Vector3D max = o.max();
    boolean ridgeAlongX = max.x() - min.x() >= max.y() - min.y();
    double cx = (min.x() + max.x()) / 2;
    if (projection == Projection.TOP) {
      double[] roof = rect(m, min.x(), min.y(), max.x(), max.y());
      g.setFill(HOUSE_ROOF);
      g.fillRect(roof[0], roof[1], roof[2], roof[3]);
      g.setStroke(HOUSE_ROOF.darker());
      g.strokeRect(roof[0], roof[1], roof[2], roof[3]);
      double cy = (min.y() + max.y()) / 2;
      if (ridgeAlongX) {
        g.strokeLine(m.toX(min.x()), m.toY(cy), m.toX(max.x()), m.toY(cy));
      } else {
        g.strokeLine(m.toX(cx), m.toY(min.y()), m.toX(cx), m.toY(max.y()));
      }
      return;
    }
    double eaves = o.eavesHeight();
    double[] walls = rect(m, min.x(), 0, max.x(), eaves);
    g.setFill(HOUSE_WALL);
    g.fillRect(walls[0], walls[1], walls[2], walls[3]);
    g.setStroke(HOUSE_WALL.darker());
    g.strokeRect(walls[0], walls[1], walls[2], walls[3]);
    g.setFill(HOUSE_ROOF);
    g.setStroke(HOUSE_ROOF.darker());
    if (ridgeAlongX) {
      double[] roof = rect(m, min.x(), eaves, max.x(), max.z());
      g.fillRect(roof[0], roof[1], roof[2], roof[3]);
      g.strokeRect(roof[0], roof[1], roof[2], roof[3]);
    } else {
      double[] xs = {m.toX(min.x()), m.toX(max.x()), m.toX(cx)};
      double[] ys = {m.toY(eaves), m.toY(eaves), m.toY(max.z())};
      g.fillPolygon(xs, ys, 3);
      g.strokePolygon(xs, ys, 3);
    }
  }

  private void paintZones(GraphicsContext g, Mapper m) {
    g.setLineWidth(1.5);
    for (NoFlyZone z : latest.noFlyZones()) {
      double[] r = projection == Projection.TOP
          ? rect(m, z.centreX() - z.radius(), z.centreY() - z.radius(),
              z.centreX() + z.radius(), z.centreY() + z.radius())
          : rect(m, z.centreX() - z.radius(), 0, z.centreX() + z.radius(), z.ceiling());
      g.setFill(ZONE_FILL);
      g.setStroke(ZONE_EDGE);
      if (projection == Projection.TOP) {
        g.fillOval(r[0], r[1], r[2], r[3]);
        g.strokeOval(r[0], r[1], r[2], r[3]);
      } else {
        g.fillRect(r[0], r[1], r[2], r[3]);
        g.strokeRect(r[0], r[1], r[2], r[3]);
      }
    }
  }

  private void paintTrails(GraphicsContext g, Mapper m) {
    g.setLineWidth(2);
    g.setLineCap(StrokeLineCap.ROUND);
    g.setLineJoin(StrokeLineJoin.ROUND);
    for (Map.Entry<String, Deque<Vector3D>> entry : trails.entrySet()) {
      Iterator<Vector3D> it = entry.getValue().iterator();
      if (!it.hasNext()) {
        continue;
      }
      g.beginPath();
      Vector3D first = it.next();
      g.moveTo(m.toX(projection.h(first)), m.toY(projection.v(first)));
      while (it.hasNext()) {
        Vector3D p = it.next();
        g.lineTo(m.toX(projection.h(p)), m.toY(projection.v(p)));
      }
      g.setStroke(colour(entry.getKey()).deriveColor(0, 1, 1, 0.43));
      g.stroke();
    }
  }

  private void paintDrones(GraphicsContext g, Mapper m) {
    g.setFont(Font.font("System", FontWeight.BOLD, 12));
    for (DroneSnapshot body : latest.drones()) {
      double x = m.toX(projection.h(body.position()));
      double y = m.toY(projection.v(body.position()));
      if (projection == Projection.SIDE && body.position().z() > 0) {
        g.setFill(SHADOW);
        g.fillOval(x - 7, m.toY(0) - 2.5, 14, 5);
      }
      Color c = colour(body.id());
      g.setFill(c);
      g.fillOval(x - 7, y - 7, 14, 14);
      g.setStroke(Color.WHITE);
      g.setLineWidth(2);
      g.strokeOval(x - 7, y - 7, 14, 14);
      if (body.id().equals(selection.get())) {
        g.setStroke(c);
        g.strokeOval(x - 12, y - 12, 24, 24);
      }
      g.setFill(c.darker());
      g.fillText(body.id(), x + 10, y - 8);
    }
  }

  private Color colour(String droneId) {
    int rgb = palette.colourOf(droneId);
    return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
  }

  private static double[] rect(Mapper m, double h1, double v1, double h2, double v2) {
    double x1 = m.toX(h1);
    double x2 = m.toX(h2);
    double y1 = m.toY(v1);
    double y2 = m.toY(v2);
    return new double[] {Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1),
        Math.abs(y2 - y1)};
  }

  private static String format(double value) {
    double rounded = Math.abs(value) < 1e-9 ? 0 : value;
    return rounded == Math.rint(rounded) ? String.valueOf((long) rounded)
        : String.format(Locale.ROOT, "%.1f", rounded);
  }

  private record Mapper(double centreX, double centreY, double midH, double midV,
      double scale) {

    double toX(double h) {
      return centreX + (h - midH) * scale;
    }

    double toY(double v) {
      return centreY - (v - midV) * scale;
    }

    double toH(double x) {
      return midH + (x - centreX) / scale;
    }

    double toV(double y) {
      return midV - (y - centreY) / scale;
    }
  }
}
