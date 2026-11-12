package dronesimulation.view.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.DemoScenario;
import dronesimulation.controller.HoverFacade;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The SRS mission editor: build, check, save, load and run a mission. Checks run on the
 * simulation thread and come back on the Swing thread, so {@link #settle()} runs one tick and
 * then waits for the Swing thread.
 */
class MissionEditorModelTest {

  private final SimulationEngine engine = new SimulationEngine(
      () -> DemoScenario.create(42, new EventBus()), SimulationEngine.DEFAULT_DT);
  private final DroneSelection selection = new DroneSelection();
  private final Queue<Runnable> uiQueue = new ArrayDeque<>();
  private final MissionEditorModel editor =
      new MissionEditorModel(new HoverFacade(engine), selection, uiQueue::add);

  @TempDir
  Path dir;

  private void drainUi() {
    Runnable task;
    while ((task = uiQueue.poll()) != null) {
      task.run();
    }
  }

  private void settle() throws Exception {
    engine.tick();
    drainUi();
  }

  @Test
  void flagsInvalidStepsForTheSelectedDrone() throws Exception {
    selection.set("fw1");
    editor.addStep("takeoff", "40");
    editor.addStep("hover", "5");
    editor.addStep("goto", "1 2");
    settle();

    assertEquals(List.of("takeoff 40", "hover 5", "goto 1 2"), editor.steps());
    assertEquals(List.of(1, 2), List.copyOf(editor.problems().keySet()));
    assertTrue(editor.feedback().message().startsWith("2 invalid steps. Step 2: fw1 is a FixedWing"),
        editor.feedback().message());

    selection.set("q1"); // a quadcopter can hover, but "goto 1 2" is still malformed
    settle();
    assertEquals(List.of(2), List.copyOf(editor.problems().keySet()));

    editor.press(2, "Remove");
    settle();
    assertEquals(Map.of(), editor.problems());
    assertEquals("All 2 steps can run.", editor.feedback().message());
  }

  @Test
  void reordersSteps() throws Exception {
    selection.set("q1");
    editor.addStep("land", "");
    editor.addStep("takeoff", "10");
    settle();
    assertEquals(0, editor.problems().keySet().iterator().next(), "land before takeoff");

    editor.press(1, "Up");
    settle();
    assertEquals(List.of("takeoff 10", "land"), editor.steps());
    assertEquals(Map.of(), editor.problems());
    editor.press(1, "Down"); // already last: nothing happens
    assertEquals(List.of("takeoff 10", "land"), editor.steps());
  }

  @Test
  void rechecksWhenTheDroneStateChanges() throws Exception {
    selection.set("q1");
    editor.addStep("takeoff", "10");
    settle();
    editor.update(engine.snapshot());
    settle();
    assertEquals(Map.of(), editor.problems());

    new HoverFacade(engine).command("q1 takeoff 10");
    engine.runSteps(1000);
    editor.update(engine.snapshot()); // now Flying: taking off again is not possible
    settle();
    assertEquals(List.of(0), List.copyOf(editor.problems().keySet()));
  }

  @Test
  void runsTheMissionOnlyWhenEveryStepIsValid() throws Exception {
    selection.set("q1");
    editor.addStep("goto", "5 5 5");
    settle();
    editor.press(0, "Run on drone");
    assertEquals("Fix the steps marked in red first.", editor.feedback().message());

    editor.press(0, "Remove");
    editor.addStep("takeoff", "8");
    editor.addStep("land", "");
    settle();
    editor.press(0, "Run on drone");
    assertEquals("Sent 2 steps to q1.", editor.feedback().message());
    engine.runSteps(1);
    assertEquals("takeoff 8", engine.snapshot().drones().get(0).command());
  }

  @Test
  void doesNotRunStepsWhoseCheckHasNotAnsweredYet() throws Exception {
    selection.set("q1");
    editor.addStep("takeoff", "8");
    settle();
    assertEquals(Map.of(), editor.problems());

    editor.addStep("takeoff", "8"); // invalid: already flying by then; its check is pending
    editor.press(0, "Run on drone");
    assertEquals("Still checking the steps; try again in a moment.", editor.feedback().message());
    engine.runSteps(1);
    assertEquals("", engine.snapshot().drones().get(0).command(), "nothing was sent");

    drainUi(); // the check's answer arrives
    editor.press(0, "Run on drone");
    assertEquals("Fix the steps marked in red first.", editor.feedback().message());
  }

  @Test
  void savesAndLoadsMissionFiles() throws Exception {
    selection.set("q1");
    editor.addStep("takeoff", "15");
    editor.addStep("goto", "40 0 15");
    editor.addStep("land", "");
    Path file = dir.resolve("q1.txt");
    editor.save(file);
    assertEquals(List.of("# Mission for q1, written by the Hover mission editor.",
        "q1 takeoff 15", "q1 goto 40 0 15", "q1 land"), Files.readAllLines(file));

    selection.set("q2"); // a mission saved for one drone loads onto another
    editor.load(file);
    settle();
    assertEquals(List.of("takeoff 15", "goto 40 0 15", "land"), editor.steps());
    assertEquals("All 3 steps can run.", editor.feedback().message());

    editor.load(dir.resolve("missing.txt"));
    assertTrue(editor.feedback().message().startsWith("Could not load missing.txt"));
  }

  @Test
  void readsOnlyTheDronesStepsFromAMissionFile() {
    List<String> lines = List.of("# demo", "add quad u1 0 0", "wind 5 west", "",
        "u1 takeoff 15", "q2 takeoff 8", "u1 goto 40 0 15", "speed 2");
    assertEquals(List.of("takeoff 15", "goto 40 0 15"),
        MissionEditorModel.fromFile("u1", lines));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> MissionEditorModel.fromFile("h1", lines));
    assertEquals("no steps for h1; the file has steps for u1, q2", e.getMessage());
    assertThrows(IllegalArgumentException.class,
        () -> MissionEditorModel.fromFile("h1", List.of("wind 5 west")));
  }

  @Test
  void needsADrone() {
    editor.addStep("takeoff", "10");
    editor.press(0, "Run on drone");
    assertEquals("Select a drone in the status panel first.", editor.feedback().message());
    editor.save(dir.resolve("x.txt"));
    assertEquals("Select a drone in the status panel first.", editor.feedback().message());
  }

  @Test
  void unknownDroneIsReported() throws Exception {
    selection.set("ghost");
    editor.addStep("takeoff", "10");
    settle();
    assertTrue(editor.feedback().message().startsWith("No drone called 'ghost'"), editor.feedback().message());

    editor.press(0, "Run on drone"); // a failed check does not let the steps through
    editor.press(0, "Run on drone");
    assertTrue(editor.feedback().message().startsWith("The steps could not be checked: No drone called"),
        editor.feedback().message());
  }
}
