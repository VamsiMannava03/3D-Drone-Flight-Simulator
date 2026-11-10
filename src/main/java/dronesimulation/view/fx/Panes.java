package dronesimulation.view.fx;

import dronesimulation.view.panel.Feedback;
import java.io.File;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

/** Small helpers shared by the panels. */
final class Panes {

  static final Color OK = Color.web("#15803D");
  static final Color ERROR = Color.web("#B91C1C");

  private Panes() {
    // static helpers only
  }

  static Button button(String text, Runnable action) {
    Button button = new Button(text);
    button.setOnAction(e -> action.run());
    return button;
  }

  static TextField field(String text, int columns) {
    TextField field = new TextField(text);
    field.setPrefColumnCount(columns);
    return field;
  }

  static Label title(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold;");
    return label;
  }

  /** Keeps a label showing a feedback's message, green for success and red for errors. */
  static Label feedbackLabel(Feedback feedback) {
    Label label = new Label(" ");
    Runnable show = () -> {
      label.setText(feedback.message().isEmpty() ? " " : feedback.message());
      label.setTextFill(feedback.isError() ? ERROR : OK);
    };
    feedback.onChange(show);
    show.run();
    return label;
  }

  static FileChooser missionChooser(String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    File missions = new File("missions");
    if (missions.isDirectory()) {
      chooser.setInitialDirectory(missions);
    }
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mission files", "*.txt"));
    return chooser;
  }
}
