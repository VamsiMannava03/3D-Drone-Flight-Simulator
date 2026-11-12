package dronesimulation.view.fx;

import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.view.panel.MissionEditorModel;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The mission editor: build a mission step by step for the selected drone. Steps the drone
 * cannot run are shown in red with the reason. Missions can be run on the drone, saved and
 * loaded.
 */
public final class MissionEditorPane extends BorderPane {

  private final MissionEditorModel model;
  private final ListView<Integer> list = new ListView<>();
  private final ComboBox<String> verb = new ComboBox<>();
  private final TextField args = Panes.field("", 12);
  private final Label hint = new Label();
  private final Label title = Panes.title("");
  private boolean refreshing;

  /**
   * Creates the mission editor.
   *
   * @param model what the editor does
   */
  public MissionEditorPane(MissionEditorModel model) {
    this.model = model;
    setPadding(new Insets(6, 8, 6, 8));
    verb.getItems().addAll(MissionEditorModel.STEPS.keySet());
    verb.setOnAction(e -> hint.setText(MissionEditorModel.STEPS.get(verb.getValue())));
    verb.setValue("takeoff");
    hint.setText(MissionEditorModel.STEPS.get("takeoff"));
    hint.setStyle("-fx-text-fill: #64748B;");
    args.setOnAction(e -> addStep()); // Enter adds the step
    HBox builder = row(title, new Label("   Step"), verb, args, hint,
        Panes.button("Add step", this::addStep));

    list.setPrefHeight(140);
    list.setStyle("-fx-font-family: monospace;");
    list.setCellFactory(v -> new ListCell<>() {
      @Override
      protected void updateItem(Integer row, boolean empty) {
        super.updateItem(row, empty);
        if (empty || row == null || row >= model.steps().size()) {
          setText(null);
          setStyle("");
          return;
        }
        setText(model.rowText(row));
        setStyle(model.problems().containsKey(row) ? "-fx-text-fill: #B91C1C;" : "");
      }
    });
    list.getSelectionModel().selectedIndexProperty().addListener((o, was, now) -> {
      if (!refreshing) {
        model.select(now.intValue());
      }
    });

    HBox buttons = row(press("Up"), press("Down"), press("Remove"), press("Clear"),
        new Label("  "), Panes.button("Save...", this::chooseSave),
        Panes.button("Load...", this::chooseLoad), new Label("  "), press("Run on drone"));
    VBox south = new VBox(4, buttons, Panes.feedbackLabel(model.feedback()));
    setTop(builder);
    setCenter(list);
    setBottom(south);
    BorderPane.setMargin(list, new Insets(4, 0, 4, 0));
    model.onChange(this::refresh);
    refresh();
  }

  /**
   * Rechecks the steps when the selected drone's state changes.
   *
   * @param snapshot the latest snapshot
   */
  public void update(WorldSnapshot snapshot) {
    model.update(snapshot);
    title.setText(model.title());
  }

  private static HBox row(javafx.scene.Node... parts) {
    HBox row = new HBox(6, parts);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private javafx.scene.control.Button press(String action) {
    return Panes.button(action, () -> model.press(model.selected(), action));
  }

  private void addStep() {
    model.addStep(verb.getValue(), args.getText());
    args.clear();
  }

  private void refresh() {
    refreshing = true;
    try {
      title.setText(model.title());
      list.getItems().clear();
      for (int i = 0; i < model.steps().size(); i++) {
        list.getItems().add(i);
      }
      if (model.selected() >= 0) {
        list.getSelectionModel().select(model.selected());
      }
    } finally {
      refreshing = false;
    }
  }

  private void chooseSave() {
    File file = Panes.missionChooser("Save mission").showSaveDialog(getScene().getWindow());
    if (file != null) {
      model.save(file.toPath());
    }
  }

  private void chooseLoad() {
    File file = Panes.missionChooser("Load mission").showOpenDialog(getScene().getWindow());
    if (file != null) {
      model.load(file.toPath());
    }
  }
}
