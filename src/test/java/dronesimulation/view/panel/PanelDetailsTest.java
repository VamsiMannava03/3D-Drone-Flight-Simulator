package dronesimulation.view.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.view.SimulationView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class PanelDetailsTest {

  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.create(42, new EventBus()), SimulationEngine.DEFAULT_DT);
  private final HoverFacade hover = new HoverFacade(engine);
  private final DroneSelection selection = new DroneSelection();

  @Test
  void selectionTellsListenersOnlyAboutChanges() {
    List<String> seen = new ArrayList<>();
    selection.onChange(seen::add);
    selection.set("q1");
    selection.set("q1");
    selection.set(null);
    assertEquals(Arrays.asList("q1", null), seen);
  }

  @Test
  void controlPanelShowsClockAndSpeed() {
    ControlPanelModel panel = new ControlPanelModel(hover, selection);
    List<String> messages = new ArrayList<>();
    panel.feedback().onChange(() -> messages.add(panel.feedback().message()));
    assertEquals("2x", ControlPanelModel.speedLabel(2));
    assertEquals("0.25x", ControlPanelModel.speedLabel(0.25));
    panel.chooseSpeed(3);
    assertTrue(panel.feedback().isError());
    panel.chooseSpeed(4);
    assertEquals(SimulationView.SPEED_STEPS.indexOf(4.0), panel.speedIndex());
    hover.pause();
    engine.runSteps(1);
    panel.update(engine.snapshot());
    assertTrue(panel.clockText().endsWith("(paused)"), panel.clockText());
    assertEquals(List.of("Unknown speed 3.0", "speed set"), messages);
  }

  @Test
  void statusShowsBatteryLookAndRejectsUnknownRows() {
    DroneStatusModel status = new DroneStatusModel(selection);
    assertEquals(DroneStatusModel.BatteryLook.NONE, status.batteryLook());
    assertEquals(-1, status.throttle());
    status.update(engine.snapshot());
    assertEquals(DroneStatusModel.BatteryLook.OK, status.batteryLook());
    status.choose("q2");
    status.update(engine.snapshot());
    assertTrue(status.battery() > 0 && status.battery() < 1);
    assertThrows(IllegalArgumentException.class, () -> status.valueOf("Colour"));
  }

  @Test
  void editorListsRowsWithProblemsAndClears() throws Exception {
    Queue<Runnable> ui = new ArrayDeque<>();
    MissionEditorModel editor = new MissionEditorModel(hover, selection, ui::add);
    List<String> changes = new ArrayList<>();
    editor.onChange(() -> changes.add("changed"));
    assertEquals("Mission editor (no drone)", editor.title());
    selection.set("q1");
    assertEquals("Mission editor for q1", editor.title());
    editor.addStep("land", null);
    engine.tick();
    ui.forEach(Runnable::run);
    assertTrue(editor.rowText(0).startsWith(" 1  land    <- "), editor.rowText(0));
    assertEquals(0, editor.selected());
    editor.select(9);
    assertEquals(-1, editor.selected());
    editor.press(0, "Clear");
    assertTrue(editor.steps().isEmpty());
    assertEquals("", editor.feedback().message());
    assertThrows(IllegalArgumentException.class, () -> editor.press(0, "Fly"));
    assertTrue(changes.size() >= 3);
  }

  @Test
  void eventLogTitleCountsLines() {
    EventLog log = new EventLog();
    assertEquals("Event log", log.title());
    log.append("a");
    log.append("b");
    assertEquals("Event log (2)", log.title());
    assertEquals(List.of("a", "b"), log.lines());
  }
}
