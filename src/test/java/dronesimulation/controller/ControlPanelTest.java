package dronesimulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.DroneType;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.physics.GustyWind;
import dronesimulation.model.physics.Winds;
import dronesimulation.model.world.World;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SRS FR18 and the control panel: start, pause, reset, speed, drones and wind. */
class ControlPanelTest {

  private final EventBus events = new EventBus();
  private final List<SimEvent> received = new ArrayList<>();
  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.create(42, events), SimulationEngine.DEFAULT_DT);
  private final HoverFacade hover = new HoverFacade(engine);

  ControlPanelTest() {
    events.subscribe(received::add);
  }

  @Test
  void resetRerunsTheScenarioIdentically() throws IOException {
    hover.loadMission(Path.of("missions", "demo.txt"));
    engine.runSteps(4000);
    WorldSnapshot first = engine.snapshot();
    int firstEvents = received.size();

    hover.reset();
    engine.tick(); // applies the reset and replays the mission file
    assertEquals(SimulationEngine.DEFAULT_DT, engine.snapshot().time(), 1e-12);
    engine.runSteps(3999);

    assertEquals(first, engine.snapshot());
    assertEquals(firstEvents * 2, received.size(), "every event happens again, once");
  }

  @Test
  void resetRebuildsTheWorldFromScratch() {
    World before = engine.world();
    hover.command("q1 takeoff 10");
    engine.runSteps(300);
    hover.reset();
    engine.tick();
    assertNotEquals(before, engine.world());
    assertEquals("Idle", engine.snapshot().drones().get(0).state());
  }

  @Test
  void singleWorldEngineCannotReset() {
    SimulationEngine plain = new SimulationEngine(DemoScenario.create(1, new EventBus()), 0.01);
    assertThrows(IllegalStateException.class, plain::reset);
  }

  @Test
  void startPauseAndSpeed() {
    hover.pause();
    engine.tick();
    assertTrue(engine.isPaused());
    hover.start();
    hover.setSpeed(4);
    engine.tick();
    assertFalse(engine.isPaused());
    assertEquals(4, engine.getTimeScale());
  }

  @Test
  void addAndRemoveDronesWhileRunning() {
    hover.addDrone(DroneType.HEXACOPTER, "h9", 40, 40, 1.0);
    engine.tick();
    assertTrue(engine.world().findDrone("h9").isPresent());
    assertEquals(1.0, engine.world().drone("h9").getPayloadMass(), 1e-12);
    assertEquals(new Vector3D(40, 40, 0), engine.world().drone("h9").getHome());

    hover.command("h9 takeoff 5");
    engine.runSteps(100);
    hover.removeDrone("h9");
    engine.tick();
    assertTrue(engine.world().findDrone("h9").isEmpty());
    assertTrue(engine.snapshot().drones().stream().noneMatch(d -> d.id().equals("h9")));
  }

  @Test
  void windIsSetFromABearing() {
    hover.setWind(5, 270, 0); // 5 m/s from the west blows toward +x
    engine.tick();
    Vector3D air = engine.world().getWind().airVelocityAt(Vector3D.ZERO);
    assertEquals(5, air.x(), 1e-9);
    assertEquals(0, air.y(), 1e-9);

    hover.command("wind 3 north 1");
    engine.tick();
    GustyWind gusty = (GustyWind) engine.world().getWind();
    double meanY = 0;
    for (int t = 0; t < 60; t++) {
      meanY += gusty.velocityAt(t).y() / 60;
    }
    assertTrue(meanY < -1.5, "from the north blows toward -y, with gusts");
    assertEquals(new Vector3D(0, -3, 0).x(), Winds.velocity(3, 0).x(), 1e-9);
  }

  @Test
  void controlPanelInputIsValidated() {
    CommandParser parser = new CommandParser();
    assertThrows(IllegalArgumentException.class, () -> parser.parse("add tank t1 0 0"));
    // A drone called "speed" could never be commanded: "speed takeoff 10" is the speed command.
    assertEquals("'Speed' is a command word and cannot be a drone id",
        assertThrows(IllegalArgumentException.class, () -> parser.parse("add quad Speed 0 0"))
            .getMessage());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("wind -2 west"));
    assertThrows(IllegalArgumentException.class, () -> parser.parse("wind 2 upwards"));
    assertThrows(IllegalArgumentException.class, () -> parser.parse("remove"));

    hover.command("remove ghost");
    engine.tick();
    assertTrue(received.stream().anyMatch(e -> e.type() == SimEventType.COMMAND_REJECTED
        && e.message().startsWith("No drone called 'ghost'")));
  }
}
