package dronesimulation.view.fx;

import dronesimulation.view.panel.EventLog;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;

/** The time-stamped event log. */
public final class EventLogPane extends BorderPane {

  private final EventLog log;
  private final TextArea text = new TextArea();
  private final Label title = Panes.title("Event log");

  /**
   * Creates the event log panel.
   *
   * @param log the lines it shows
   */
  public EventLogPane(EventLog log) {
    this.log = log;
    setPadding(new Insets(6, 8, 6, 8));
    text.setEditable(false);
    text.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
    text.setPrefRowCount(8);
    Region gap = new Region();
    HBox.setHgrow(gap, Priority.ALWAYS);
    HBox header = new HBox(6, title, gap, Panes.button("Clear", this::clear));
    setTop(header);
    setCenter(text);
    BorderPane.setMargin(text, new Insets(4, 0, 0, 0));
  }

  /**
   * Adds lines and scrolls to the newest.
   *
   * @param lines the new lines
   */
  public void append(Iterable<String> lines) {
    for (String line : lines) {
      log.append(line);
    }
    text.setText(log.contents());
    text.positionCaret(text.getLength());
    text.setScrollTop(Double.MAX_VALUE);
    title.setText(log.title());
  }

  private void clear() {
    log.clear();
    text.clear();
    title.setText(log.title());
  }
}
