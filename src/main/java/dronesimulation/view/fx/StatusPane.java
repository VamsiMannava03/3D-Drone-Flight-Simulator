package dronesimulation.view.fx;

import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.view.panel.DroneSelection;
import dronesimulation.view.panel.DroneStatusModel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The drone status panel: pick a drone and see its live state, altitude, speed, throttle,
 * battery, mass, payload and mission step.
 */
public final class StatusPane extends VBox {

  private final DroneStatusModel model;
  private final ComboBox<String> chooser = new ComboBox<>();
  private final Map<String, Label> values = new LinkedHashMap<>();
  private final ProgressBar throttle = new ProgressBar(0);
  private final ProgressBar battery = new ProgressBar(0);
  private final Label throttleText = new Label("-");
  private final Label batteryText = new Label("-");
  private boolean updatingList;

  /**
   * Creates the status panel.
   *
   * @param model what the panel shows
   * @param selection the drone picked in the window
   */
  public StatusPane(DroneStatusModel model, DroneSelection selection) {
    this.model = model;
    setPadding(new Insets(8));
    setSpacing(6);
    setPrefWidth(260);
    setMinWidth(220);
    chooser.setMaxWidth(Double.MAX_VALUE);
    chooser.setPromptText("no drone");
    chooser.setOnAction(e -> {
      if (!updatingList && chooser.getValue() != null) {
        model.choose(chooser.getValue());
      }
    });
    selection.onChange(id -> {
      if (!Objects.equals(id, chooser.getValue())) {
        updatingList = true;
        try {
          chooser.setValue(id);
        } finally {
          updatingList = false;
        }
      }
    });
    GridPane grid = new GridPane();
    grid.setHgap(8);
    grid.setVgap(4);
    int row = 0;
    for (String name : DroneStatusModel.ROWS) {
      grid.add(new Label(name), 0, row);
      if (name.equals("Throttle")) {
        grid.add(bar(throttle, throttleText), 1, row);
      } else if (name.equals("Battery")) {
        grid.add(bar(battery, batteryText), 1, row);
      } else {
        Label value = new Label("-");
        value.setStyle("-fx-font-family: monospace;");
        values.put(name, value);
        grid.add(value, 1, row);
      }
      row++;
    }
    getChildren().addAll(Panes.title("Drone status"), chooser, grid);
  }

  private static StackPane bar(ProgressBar bar, Label text) {
    bar.setPrefWidth(140);
    text.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
    return new StackPane(bar, text);
  }

  /**
   * Shows a snapshot.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    model.update(snapshot);
    if (!chooser.getItems().equals(model.listedDrones())) {
      updatingList = true;
      try {
        chooser.getItems().setAll(model.listedDrones());
      } finally {
        updatingList = false;
      }
    }
    values.forEach((name, label) -> label.setText(model.valueOf(name)));
    throttle.setProgress(Math.max(0, model.throttle()));
    throttleText.setText(model.valueOf("Throttle"));
    battery.setProgress(Math.max(0, model.battery()));
    batteryText.setText(model.valueOf("Battery"));
    String colour = switch (model.batteryLook()) {
      case LOW -> "#D97706";
      case EMPTY -> "#DC2626";
      default -> "#16A34A";
    };
    battery.setStyle("-fx-accent: " + colour + ";");
  }
}
