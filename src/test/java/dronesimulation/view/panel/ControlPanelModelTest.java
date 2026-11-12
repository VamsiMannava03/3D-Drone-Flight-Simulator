package dronesimulation.view.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.controller.HoverFacade;
import dronesimulation.model.Vector3D;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The SRS control panel, pressed as a user would. Built on the test thread; no window. */
class ControlPanelModelTest {

  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.create(42, new EventBus()), SimulationEngine.DEFAULT_DT);
  private final DroneSelection selection = new DroneSelection();
  private final ControlPanelModel panel = new ControlPanelModel(new HoverFacade(engine), selection);

  @Test
  void startPauseResetAndSpeed() {
    panel.press("Pause");
    engine.tick();
    assertTrue(engine.isPaused());
    assertEquals("paused", panel.feedback().message());

    panel.press("Start");
    panel.chooseSpeed(8);
    engine.tick();
    assertFalse(engine.isPaused());
    assertEquals(8, engine.getTimeScale());

    engine.runSteps(100);
    panel.press("Reset");
    engine.tick();
    assertEquals(SimulationEngine.DEFAULT_DT, engine.snapshot().time(), 1e-12);
  }

  @Test
  void speedChangedElsewhereIsShownWithoutBeingSentBack() {
    engine.setTimeScale(4);
    panel.update(engine.snapshot());
    engine.setTimeScale(2); // if update() had re-sent 4x, this would be overwritten
    engine.tick();
    assertEquals(2, engine.getTimeScale());
  }

  @Test
  void addsAndRemovesDrones() {
    panel.update(engine.snapshot());
    panel.addDrone("hexa", "", "40", "-10", "1.5", "80");
    engine.tick();
    assertEquals("added h2", panel.feedback().message(), "h1 exists, so h2 is suggested");
    assertEquals(new Vector3D(40, -10, 0), engine.world().drone("h2").getHome());
    assertEquals(1.5, engine.world().drone("h2").getPayloadMass(), 1e-12);
    assertEquals(0.8, engine.world().drone("h2").getBattery().getLevel(), 1e-12);

    selection.set("h2");
    panel.press("Remove selected");
    engine.tick();
    assertTrue(engine.world().findDrone("h2").isEmpty());
  }

  @Test
  void invalidInputShowsTheParserMessage() {
    panel.addDrone("quad", "z1", "ten", "0", "0", "100");
    assertTrue(panel.feedback().isError());
    assertEquals("Expected a number for x but got 'ten'", panel.feedback().message());

    panel.press("Remove selected");
    assertEquals("Select a drone in the status panel first.", panel.feedback().message());

    panel.setWind("-3", "w", "0");
    assertEquals("wind speed and gusts must not be negative", panel.feedback().message());
    assertThrows(IllegalArgumentException.class, () -> panel.press("Launch"));
  }

  @Test
  void setsWindFromABearingOrDegrees() {
    panel.setWind("5", "w", "0");
    engine.tick();
    assertEquals(5, engine.world().getWind().airVelocityAt(Vector3D.ZERO).x(), 1e-9);

    panel.setWind("4", "180", "0"); // typed degrees: from the south blows toward +y
    engine.tick();
    assertEquals(4, engine.world().getWind().airVelocityAt(Vector3D.ZERO).y(), 1e-9);
    assertFalse(panel.feedback().isError());
  }

  @Test
  void loadsAMissionFile() {
    panel.loadMission(Path.of("missions", "demo.txt"));
    assertFalse(panel.feedback().isError());
    assertTrue(panel.feedback().message().startsWith("loaded "), panel.feedback().message());

    panel.loadMission(Path.of("missions", "missing.txt"));
    assertTrue(panel.feedback().isError());
  }

  @Test
  void suggestsTheFirstFreeId() {
    assertEquals("q1", ControlPanelModel.suggestId("quad", Set.of()));
    assertEquals("q3", ControlPanelModel.suggestId("quad", Set.of("q1", "q2", "h3")));
    assertEquals("fw2", ControlPanelModel.suggestId("plane", Set.of("fw1")));
  }
}
