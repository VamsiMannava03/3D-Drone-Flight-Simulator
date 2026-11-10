package dronesimulation.view.panel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The time-stamped event log, without any widgets. It keeps the newest {@link #MAX_LINES}
 * lines.
 */
public final class EventLog {

  /** The most lines kept. */
  public static final int MAX_LINES = 1000;

  private final Deque<String> lines = new ArrayDeque<>();

  /** Creates an empty log. */
  public EventLog() {
    // nothing logged yet
  }

  /**
   * Adds a line, dropping the oldest if the log is full.
   *
   * @param line the line
   */
  public void append(String line) {
    lines.addLast(line);
    while (lines.size() > MAX_LINES) {
      lines.removeFirst();
    }
  }

  /** Removes every line. */
  public void clear() {
    lines.clear();
  }

  /**
   * Returns the whole log.
   *
   * @return the lines joined with newlines
   */
  public String contents() {
    return String.join("\n", lines);
  }

  /**
   * Returns the lines, oldest first.
   *
   * @return the lines
   */
  public List<String> lines() {
    return List.copyOf(lines);
  }

  /**
   * Returns how many lines the log holds.
   *
   * @return the line count
   */
  public int lineCount() {
    return lines.size();
  }

  /**
   * Returns the panel title, with the line count once there are lines.
   *
   * @return for example {@code Event log (12)}
   */
  public String title() {
    return lines.isEmpty() ? "Event log" : "Event log (" + lines.size() + ")";
  }
}
