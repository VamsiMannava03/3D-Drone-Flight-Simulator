package dronesimulation.view.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.model.events.EventBus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The SRS drone status panel and event log. Built on the test thread; no window is opened. */
class StatusAndLogModelTest {

  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.create(42, new EventBus()), SimulationEngine.DEFAULT_DT);
  private final HoverFacade hover = new HoverFacade(engine);
  private final DroneSelection selection = new DroneSelection();
  private final DroneStatusModel status = new DroneStatusModel(selection);

  @Test
  void selectsTheFirstDroneAndShowsItsStatus() {
    hover.command("q1 takeoff 10");
    engine.runSteps(300);
    status.update(engine.snapshot());

    assertEquals(List.of("q1", "q2", "fw1", "h1"), status.listedDrones());
    assertEquals("q1", selection.get());
    assertEquals("Quadcopter", status.valueOf("Type"));
    assertTrue(status.valueOf("Altitude").endsWith(" m"));
    assertTrue(status.valueOf("Battery").matches("\\d{1,3}%"), status.valueOf("Battery"));
    assertTrue(status.valueOf("Throttle").matches("\\d{1,3}%"), status.valueOf("Throttle"));
    assertEquals("TakingOff", status.valueOf("State"));
    assertEquals("takeoff 10", status.valueOf("Step"));
  }

  @Test
  void showsPayloadDropImmediately() {
    hover.command("h1 takeoff 5");
    hover.command("h1 drop");
    engine.runSteps(1);
    selection.set("h1");
    status.update(engine.snapshot());
    String before = status.valueOf("Mass");
    assertEquals("1.50 kg", status.valueOf("Payload"));

    engine.runSteps(600);
    status.update(engine.snapshot());
    assertEquals("none", status.valueOf("Payload"));
    assertTrue(Double.parseDouble(status.valueOf("Mass").split(" ")[0])
        < Double.parseDouble(before.split(" ")[0]), "lighter after the drop (SRS step 7)");
  }

  @Test
  void followsDronesAddedAndRemovedWhileRunning() {
    status.update(engine.snapshot());
    List<String> seen = new ArrayList<>();
    selection.onChange(seen::add);

    status.choose("q2");
    assertEquals("q2", selection.get());

    hover.removeDrone("q2");
    hover.command("add quad z9 0 0");
    engine.runSteps(1);
    status.update(engine.snapshot());
    assertEquals(List.of("q1", "fw1", "h1", "z9"), status.listedDrones());
    assertEquals("q1", selection.get(), "a removed drone hands the selection on");
    assertEquals(List.of("q2", "q1"), seen);
  }

  @Test
  void emptyWorldShowsDashes() {
    SimulationEngine empty = new SimulationEngine(() -> DemoScenario.empty(new EventBus()),
        SimulationEngine.DEFAULT_DT);
    WorldSnapshot snapshot = empty.snapshot();
    status.update(snapshot);
    assertNull(selection.get());
    assertEquals("-", status.valueOf("Altitude"));
    assertEquals("-", status.valueOf("Battery"));
  }

  @Test
  void eventLogKeepsTheNewestLines() {
    EventLog log = new EventLog();
    log.append("[  1.00] q1   TOOK_OFF         reached 10.0 m");
    log.append("[  2.00] q1   LANDED           landed");
    assertEquals(2, log.lineCount());
    assertTrue(log.contents().endsWith("landed"));

    for (int i = 0; i < EventLog.MAX_LINES + 5; i++) {
      log.append("line " + i);
    }
    assertEquals(EventLog.MAX_LINES, log.lineCount());
    assertTrue(log.contents().startsWith("line 5\n"));
    assertEquals(EventLog.MAX_LINES, log.contents().split("\n").length);

    log.clear();
    assertEquals(0, log.lineCount());
    assertEquals("", log.contents());
  }
}
