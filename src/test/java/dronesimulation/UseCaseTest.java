package dronesimulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.DroneSnapshot;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.engine.WorldSnapshot;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** End-to-end acceptance test: the SRS primary use case, from missions/usecase.txt. */
class UseCaseTest {

  private final EventBus events = new EventBus();
  private final List<SimEvent> log = new ArrayList<>();
  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.empty(events), SimulationEngine.DEFAULT_DT);
  private final HoverFacade hover = new HoverFacade(engine);

  UseCaseTest() {
    events.subscribe(log::add);
  }

  private List<SimEventType> keyEvents() {
    return log.stream().map(SimEvent::type)
        .filter(t -> t != SimEventType.MISSION_STEP_DONE).toList();
  }

  @Test
  void primaryUseCasePlaysOutAsTheSrsDescribes() throws IOException {
    // Steps 1-4: empty world, quadcopter with 0.5 kg payload, 5 m/s wind from the west,
    // a validated mission.
    assertEquals(9, hover.loadMission(Path.of("missions", "usecase.txt")));
    engine.tick();
    WorldSnapshot start = engine.snapshot();
    assertEquals(1, start.drones().size());
    assertEquals(2.0, start.drones().get(0).mass(), 1e-9);
    assertEquals(5, start.wind().x(), 1.5 + 1e-9, "from the west, light gusts");
    assertTrue(log.stream().noneMatch(e -> e.type() == SimEventType.COMMAND_REJECTED));

    // Steps 5-8: fly, drop, low battery, automatic return, safe landing.
    engine.runSteps(6000);
    assertEquals(List.of(SimEventType.TOOK_OFF, SimEventType.PAYLOAD_DROPPED,
        SimEventType.LOW_BATTERY, SimEventType.RETURN_HOME, SimEventType.LANDED,
        SimEventType.MISSION_COMPLETE), keyEvents());
    DroneSnapshot end = engine.snapshot().drones().get(0);
    assertEquals("Landed", end.state());
    assertEquals(1.5, end.mass(), 1e-9);
    assertTrue(Math.hypot(end.position().x(), end.position().y()) < 5, "back home");
    assertTrue(end.batteryLevel() > 0.05, "landed with charge to spare");

    // Step 9: reset and rerun, identical results.
    WorldSnapshot first = engine.snapshot();
    List<SimEvent> firstRun = List.copyOf(log);
    log.clear();
    hover.reset();
    engine.tick(); // the reset plus the first step, as in the first run
    engine.runSteps(6000);
    assertEquals(first, engine.snapshot());
    assertEquals(firstRun, log);
  }
}
