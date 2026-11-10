package dronesimulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.missions.GoTo;
import dronesimulation.model.missions.Hover;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

/** SRS mission editor support: parse, check and run a drone's mission through the facade. */
class MissionEditorFacadeTest {

  private final EventBus events = new EventBus();
  private final List<SimEvent> received = new ArrayList<>();
  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.create(42, events), SimulationEngine.DEFAULT_DT);
  private final HoverFacade hover = new HoverFacade(engine);

  MissionEditorFacadeTest() {
    events.subscribe(received::add);
  }

  private Map<Integer, String> check(String drone, String... steps) throws Exception {
    CompletableFuture<Map<Integer, String>> result = hover.checkMission(drone, List.of(steps));
    assertFalse(result.isDone(), "the check runs on the simulation thread");
    engine.tick();
    return result.get();
  }

  @Test
  void parsesStepsWithoutADroneId() {
    CommandParser parser = new CommandParser();
    assertInstanceOf(GoTo.class, parser.parseStep("goto 10 0 5"));
    assertInstanceOf(Hover.class, parser.parseStep("  HOVER\t3 "));
    assertEquals("Usage: <drone> takeoff <altitude>",
        assertThrows(IllegalArgumentException.class, () -> parser.parseStep("takeoff"))
            .getMessage());
    assertThrows(IllegalArgumentException.class, () -> parser.parseStep("clear"));
    assertThrows(IllegalArgumentException.class, () -> parser.parseStep(" "));
    assertThrows(IllegalArgumentException.class, () -> parser.parseStep(null));
  }

  @Test
  void validMissionHasNoProblems() throws Exception {
    assertEquals(Map.of(), check("q1", "takeoff 15", "goto 40 0 15", "hover 10", "home",
        "land"));
  }

  @Test
  void flagsTheExactStepsThatCannotRun() throws Exception {
    Map<Integer, String> problems = check("fw1", "takeoff 40", "hover 5", "goto 1 2", "land");
    assertEquals(List.of(1, 2), List.copyOf(problems.keySet()), "sorted by step");
    assertEquals("fw1 is a FixedWing drone and cannot hover", problems.get(1));
    assertEquals("Usage: <drone> goto <x> <y> <z>", problems.get(2));
  }

  @Test
  void unknownDroneFailsTheCheck() {
    CompletableFuture<Map<Integer, String>> result = hover.checkMission("ghost",
        List.of("takeoff 5"));
    engine.tick();
    ExecutionException e = assertThrows(ExecutionException.class, result::get);
    assertInstanceOf(IllegalArgumentException.class, e.getCause());
    assertTrue(e.getCause().getMessage().startsWith("No drone called 'ghost'"));
    assertTrue(received.isEmpty(), "a check is not a command, so nothing is reported");
  }

  @Test
  void runMissionReplacesTheQueue() {
    hover.command("q1 takeoff 30");
    hover.command("q1 goto 100 100 30");
    hover.runMission("q1", List.of("takeoff 10", "hover 2", "land"));
    engine.runSteps(1);

    assertEquals("takeoff 10", engine.snapshot().drones().get(0).command());
    assertEquals(2, engine.snapshot().drones().get(0).queuedCommands());
    engine.runSteps(3000);
    assertTrue(received.stream().anyMatch(e -> e.type() == SimEventType.LANDED
        && e.droneId().equals("q1")));
  }

  @Test
  void runMissionChangesNothingIfAStepIsMalformed() {
    hover.command("q1 takeoff 30");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> hover.runMission("q1", List.of("takeoff 10", "fly away")));
    assertTrue(e.getMessage().startsWith("step 2: Unknown command 'fly'"), e.getMessage());
    engine.runSteps(1);
    assertEquals("takeoff 30", engine.snapshot().drones().get(0).command());
  }

  @Test
  void runMissionIsOneActionOnTheSimulationThread() {
    hover.command("q1 takeoff 30");
    hover.runMission("q1", List.of("takeoff 10", "hover 2", "land"));
    // Queued from another thread after runMission: it must land after the whole mission,
    // not between the clear and the new steps.
    hover.command("q1 charge");
    engine.runSteps(1);

    assertTrue(received.stream().noneMatch(e -> e.type() == SimEventType.COMMAND_REJECTED),
        () -> received.toString());
    assertEquals("takeoff 10", engine.snapshot().drones().get(0).command());
    assertEquals(3, engine.snapshot().drones().get(0).queuedCommands()); // hover, land, charge
  }

  @Test
  void runMissionForAnUnknownDroneIsRejectedOnce() {
    hover.runMission("nobody", List.of("takeoff 10", "hover 2", "land"));
    engine.runSteps(1);
    assertEquals(1, received.stream().filter(e -> e.type() == SimEventType.COMMAND_REJECTED)
        .count());
  }
}
