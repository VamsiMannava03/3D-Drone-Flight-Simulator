package dronesimulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.world.Building;
import dronesimulation.view.SimulationView;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Main}.
 */
public class MainTest {

  @Test
  public void defaultsToTheCityWithTheDemoMission() {
    Main.Options options = Main.Options.parse(new String[0]);
    assertEquals(java.awt.GraphicsEnvironment.isHeadless() ? "text" : "gui", options.mode());
    assertEquals("city", options.scenario());
    assertNull(options.screenshot());
    assertEquals(42, options.seed());
    assertFalse(options.paused());
    assertEquals(Path.of("missions", "demo.txt"), options.mission());
  }

  @Test
  public void parsesEveryOption() {
    Main.Options options = Main.Options.parse(new String[] {"TEXT", "--scenario=Empty",
        "--seed=7", "--paused", "--mission=missions/usecase.txt", "--screenshot=shot.png",
        "--screenshot-at=3.5"});
    assertEquals("text", options.mode());
    assertEquals("empty", options.scenario());
    assertEquals(7, options.seed());
    assertTrue(options.paused());
    assertEquals(Path.of("missions/usecase.txt"), options.mission());
    assertEquals(Path.of("shot.png"), options.screenshot());
    assertEquals(3.5, options.screenshotAt());
    assertEquals("gui", Main.Options.parse(new String[] {"gui"}).mode());
    assertNull(Main.Options.parse(new String[] {"--scenario=empty"}).mission());
  }

  @Test
  public void rejectsUnknownModesAndScenarios() {
    assertThrows(IllegalArgumentException.class,
        () -> Main.Options.parse(new String[] {"vr"}));
    assertThrows(IllegalArgumentException.class,
        () -> Main.Options.parse(new String[] {"--scenario=moon"}));
    assertThrows(NumberFormatException.class,
        () -> Main.Options.parse(new String[] {"--seed=x"}));
  }

  @Test
  public void buildsTheChosenScenario() {
    SimulationEngine city = Main.createEngine(Main.Options.parse(new String[0]), new EventBus());
    assertTrue(city.world().getObstacles().stream().anyMatch(o -> o instanceof Building));
    SimulationEngine empty = Main.createEngine(
        Main.Options.parse(new String[] {"--scenario=empty"}), new EventBus());
    assertTrue(empty.world().getObstacles().isEmpty());
    assertTrue(empty.world().getDrones().isEmpty());
  }

  @Test
  public void connectsViewsToSnapshotsAndEvents() {
    EventBus events = new EventBus();
    SimulationEngine engine = Main.createEngine(
        Main.Options.parse(new String[] {"--scenario=empty"}), events);
    List<String> seen = new ArrayList<>();
    Main.connect(new SimulationView() {
      @Override
      public void render(dronesimulation.model.engine.WorldSnapshot snapshot) {
        seen.add("frame");
      }

      @Override
      public void showMessage(String message) {
        seen.add(message);
      }
    }, engine, events);

    engine.runSteps(1);
    events.publish(new SimEvent(SimEventType.LANDED, "q1", 1.5, "landed"));
    events.flush();

    assertEquals("frame", seen.get(0));
    assertEquals(Main.format(new SimEvent(SimEventType.LANDED, "q1", 1.5, "landed")),
        seen.get(1));
  }

  @Test
  public void formatsEventsAsLogLines() {
    assertEquals("[  2.50] q1   CRASH            hit the ground",
        Main.format(new SimEvent(SimEventType.CRASH, "q1", 2.5, "hit the ground")));
  }

  @Test
  public void reportsMissingOrBadMissionFiles() {
    HoverFacade hover = new HoverFacade(Main.createEngine(
        Main.Options.parse(new String[] {"--scenario=empty"}), new EventBus()));
    assertTrue(Main.loadMissions(Path.of("no-such-file.txt"), hover).startsWith("No mission"));
  }
}
