package dronesimulation.view.fx;

import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.view.SimulationView;
import dronesimulation.view.panel.ControlPanelModel;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The control panel across the top of the window: start, pause, reset, speed and the clock;
 * adding and removing drones; the wind; and loading a mission file.
 */
public final class ControlPane extends VBox {

  private final ControlPanelModel model;
  private final Label clock = new Label("t = 0.0 s");
  private final ComboBox<String> speed = new ComboBox<>();
  private final ComboBox<String> type = new ComboBox<>();
  private final TextField id = Panes.field("", 4);
  private final TextField x = Panes.field("0", 3);
  private final TextField y = Panes.field("0", 3);
  private final TextField payload = Panes.field("0", 3);
  private final TextField battery = Panes.field("100", 3);
  private final TextField windSpeed = Panes.field("0", 3);
  private final ComboBox<String> windFrom = new ComboBox<>();
  private final TextField gust = Panes.field("0", 3);
  private boolean updatingSpeed;

  /**
   * Creates the control panel.
   *
   * @param model what the panel does
   */
  public ControlPane(ControlPanelModel model) {
    this.model = model;
    setPadding(new Insets(4, 8, 2, 8));
    setSpacing(4);
    SimulationView.SPEED_STEPS.forEach(s -> speed.getItems().add(ControlPanelModel.speedLabel(s)));
    speed.getSelectionModel().select(model.speedIndex());
    speed.setOnAction(e -> {
      int i = speed.getSelectionModel().getSelectedIndex();
      if (!updatingSpeed && i >= 0) {
        model.chooseSpeed(SimulationView.SPEED_STEPS.get(i));
      }
    });
    type.getItems().addAll(ControlPanelModel.TYPES);
    type.getSelectionModel().selectFirst();
    windFrom.getItems().addAll(ControlPanelModel.BEARINGS);
    windFrom.setEditable(true); // a bearing in degrees can be typed too
    windFrom.getSelectionModel().selectFirst();
    windFrom.setPrefWidth(80);
    clock.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 13px;");

    getChildren().addAll(
        row(Panes.button("Start", () -> model.press("Start")),
            Panes.button("Pause", () -> model.press("Pause")),
            Panes.button("Reset", () -> model.press("Reset")),
            new Label("  Speed"), speed, new Label("   "), clock),
        row(new Label("Drone"), type, new Label("id"), id, new Label("x"), x, new Label("y"), y,
            new Label("payload kg"), payload, new Label("battery %"), battery,
            Panes.button("Add", this::addDrone),
            Panes.button("Remove selected", () -> model.press("Remove selected"))),
        row(new Label("Wind m/s"), windSpeed, new Label("from"), windFrom,
            new Label("gusts m/s"), gust,
            Panes.button("Set wind", () -> model.setWind(windSpeed.getText(), windFromText(),
                gust.getText())),
            new Label("   "), Panes.button("Load mission file...", this::chooseMission)),
        row(Panes.feedbackLabel(model.feedback())));
  }

  /**
   * Shows the clock and the speed from a snapshot.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    model.update(snapshot);
    clock.setText(model.clockText());
    if (speed.getSelectionModel().getSelectedIndex() != model.speedIndex()) {
      updatingSpeed = true; // changed elsewhere, such as the terminal: just show it
      try {
        speed.getSelectionModel().select(model.speedIndex());
      } finally {
        updatingSpeed = false;
      }
    }
  }

  private static HBox row(javafx.scene.Node... parts) {
    HBox row = new HBox(6, parts);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private String windFromText() {
    String typed = windFrom.getEditor().getText();
    return typed == null || typed.isBlank() ? String.valueOf(windFrom.getValue()) : typed;
  }

  private void addDrone() {
    if (model.addDrone(type.getValue(), id.getText(), x.getText(), y.getText(),
        payload.getText(), battery.getText())) {
      id.clear();
    }
  }

  private void chooseMission() {
    File file = Panes.missionChooser("Load mission file").showOpenDialog(getScene().getWindow());
    if (file != null) {
      model.loadMission(file.toPath());
    }
  }
}
