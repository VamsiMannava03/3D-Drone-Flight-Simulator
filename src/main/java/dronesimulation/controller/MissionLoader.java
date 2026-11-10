package dronesimulation.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads missions from a text file: one command per line, in the console command language.
 * Blank lines and lines starting with {@code #} are skipped. {@code reset} is rejected, because a
 * reset replays the loaded file.
 *
 * <pre>
 * # Package run
 * q1 takeoff 10
 * q1 deliver 30 20 0
 * q1 home
 * q1 land
 * </pre>
 */
public class MissionLoader {

  private final CommandParser parser;

  /**
   * Creates a loader.
   *
   * @param parser the parser for each line
   */
  public MissionLoader(CommandParser parser) {
    this.parser = Objects.requireNonNull(parser, "parser");
  }

  /**
   * Loads a mission file.
   *
   * @param file the file
   * @return the commands in file order
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if a line is invalid; the message names the file and line
   */
  public List<ParsedCommand> load(Path file) throws IOException {
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return load(reader, file.toString());
    }
  }

  /**
   * Loads mission text.
   *
   * @param reader the text
   * @param sourceName a name for error messages, such as the file name
   * @return the commands in order
   * @throws IOException if the text cannot be read
   * @throws IllegalArgumentException if a line is invalid; the message names the line
   */
  public List<ParsedCommand> load(Reader reader, String sourceName) throws IOException {
    List<ParsedCommand> commands = new ArrayList<>();
    BufferedReader lines = new BufferedReader(reader);
    String line;
    int number = 0;
    while ((line = lines.readLine()) != null) {
      number++;
      if (number == 1 && line.startsWith("\uFEFF")) {
        line = line.substring(1); // byte order mark from editors such as Windows Notepad
      }
      String text = line.strip();
      if (text.isEmpty() || text.startsWith("#")) {
        continue;
      }
      try {
        ParsedCommand command = parser.parse(text);
        if (command.text().equals("reset")) {
          // Reset replays the mission file, so a reset inside it would replay itself forever.
          throw new IllegalArgumentException("'reset' cannot be used in a mission file");
        }
        commands.add(command);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            sourceName + " line " + number + ": " + e.getMessage(), e);
      }
    }
    return commands;
  }
}
