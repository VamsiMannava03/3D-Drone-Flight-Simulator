package dronesimulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the design rules: the model uses only the standard JDK, and contains no display or
 * input code. Only the view and controller packages may depend on UI libraries.
 */
class ArchitectureTest {

  private static final Path SOURCE = Path.of("src", "main", "java", "dronesimulation");
  private static final List<String> MODEL_PACKAGES = List.of("model", "model/physics",
      "model/drones", "model/control", "model/states", "model/missions", "model/obstacles",
      "model/world", "model/events", "model/engine");
  private static final Pattern IMPORT = Pattern.compile("^import\\s+(static\\s+)?([\\w.]+)",
      Pattern.MULTILINE);

  private static List<String> violations(String pkg, List<String> forbiddenPrefixes) {
    List<String> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(SOURCE.resolve(pkg))) {
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        Matcher m = IMPORT.matcher(Files.readString(file));
        while (m.find()) {
          String imported = m.group(2);
          boolean jdkOrOwn = imported.startsWith("java.") || imported.startsWith("dronesimulation.");
          boolean forbidden = forbiddenPrefixes.stream().anyMatch(imported::startsWith);
          if (!jdkOrOwn || forbidden) {
            found.add(SOURCE.relativize(file) + " imports " + imported);
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return found;
  }

  @Test
  void modelUsesOnlyTheJdkAndNoDisplayCode() {
    List<String> found = new ArrayList<>();
    for (String pkg : MODEL_PACKAGES) {
      assertTrue(Files.isDirectory(SOURCE.resolve(pkg)), "missing package " + pkg);
      found.addAll(violations(pkg,
          List.of("java.awt", "javax.", "javafx.", "dronesimulation.view",
              "dronesimulation.controller")));
    }
    assertEquals(List.of(), found);
  }

  @Test
  void controllerDoesNotDependOnViews() {
    assertEquals(List.of(), violations("controller",
        List.of("java.awt", "javax.", "javafx.", "dronesimulation.view")));
  }
}
