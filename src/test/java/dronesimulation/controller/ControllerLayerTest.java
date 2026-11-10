package dronesimulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.StubDrone;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.events.SimEvent;
import dronesimulation.model.events.SimEventType;
import dronesimulation.model.missions.DroneCommand;
import dronesimulation.model.missions.GoTo;
import dronesimulation.model.world.World;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ControllerLayerTest {

  private final CommandParser parser = new CommandParser();
  private final SimulationEngine engine;
  private final List<SimEvent> events = new ArrayList<>();
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();
  private final SimulationController controller;

  ControllerLayerTest() {
    World world = new World(new EventBus());
    StubDrone q1 = new StubDrone("q1", Vector3D.ZERO);
    q1.loadPayload(0.5);
    world.addDrone(q1);
    world.getEvents().subscribe(events::add);
    engine = new SimulationEngine(world, 0.01);
    controller = new SimulationController(engine, parser,
        new PrintStream(output, true, StandardCharsets.UTF_8));
  }

  private List<String> queued() {
    engine.tick();
    List<String> list = new ArrayList<>();
    DroneCommand current = engine.world().mission("q1").getCurrent();
    if (current != null) {
      list.add(current.describe());
    }
    engine.world().mission("q1").getQueued().forEach(c -> list.add(c.describe()));
    return list;
  }

  private String printed() {
    return output.toString(StandardCharsets.UTF_8);
  }

  @Test
  void parsesEveryDroneCommand() {
    for (String line : List.of("q1 takeoff 10", "q1 goto 1 2 3", "q1 hover 4",
        "q1 deliver 5 6 0", "q1 home", "q1 land", "q1 charge")) {
      engine.submit(parser.parse(line).action());
    }
    assertEquals(List.of("takeoff 10", "goto 1 2 3", "hover 4", "deliver 5 6 0", "home",
        "land", "charge"), queued());
  }

  @Test
  void parsingIsCaseInsensitiveAndTrimsSpaces() {
    ParsedCommand c = parser.parse("  q1   GOTO 1  2 3 ");
    assertEquals("q1 goto 1 2 3", c.text());
  }

  @Test
  void clearCancelsTheMission() {
    engine.submit(parser.parse("q1 takeoff 10").action());
    engine.submit(parser.parse("q1 hover 5").action());
    engine.submit(parser.parse("q1 clear").action());
    assertEquals(List.of(), queued());
  }

  @Test
  void simulationCommandsChangeTheEngine() {
    engine.submit(parser.parse("speed 4").action());
    engine.submit(parser.parse("pause").action());
    engine.tick();
    assertEquals(4, engine.getTimeScale());
    assertTrue(engine.isPaused());
    engine.submit(parser.parse("resume").action());
    engine.tick();
    assertFalse(engine.isPaused());
  }

  @Test
  void badInputGivesClearErrors() {
    assertEquals("Unknown command 'fly'. Expected takeoff, goto, hover, deliver, drop, home, "
        + "land, charge or clear.", assertThrows(IllegalArgumentException.class,
            () -> parser.parse("q1 fly 3")).getMessage());
    // A mission step alone cannot be "clear", so the editor's message does not offer it.
    assertEquals("Unknown command 'fly'. Expected takeoff, goto, hover, deliver, drop, home, "
        + "land or charge.", assertThrows(IllegalArgumentException.class,
            () -> parser.parseStep("fly 3")).getMessage());
    assertEquals("Expected a number for altitude but got 'high'",
        assertThrows(IllegalArgumentException.class,
            () -> parser.parse("q1 takeoff high")).getMessage());
    assertEquals("Usage: q1 goto <x> <y> <z>", assertThrows(IllegalArgumentException.class,
        () -> parser.parse("q1 goto 1 2")).getMessage());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    assertThrows(IllegalArgumentException.class, () -> parser.parse("q1"));
    assertThrows(IllegalArgumentException.class, () -> parser.parse("q1 goto 1 1 0"));
  }

  @Test
  void unknownDroneIsReportedWhenTheActionRuns() {
    engine.submit(parser.parse("zz land").action());
    engine.tick();
    assertEquals(SimEventType.COMMAND_REJECTED, events.get(0).type());
  }

  @Test
  void controllerPrintsRepliesAndErrors() {
    assertTrue(controller.execute("q1 takeoff 5"));
    assertFalse(controller.execute("q1 takeoff"));
    assertTrue(controller.execute("help"));
    String text = printed();
    assertTrue(text.contains("> q1 takeoff 5"));
    assertTrue(text.contains("! Usage: q1 takeoff <altitude>"));
    assertTrue(text.contains("Drone commands:"));
  }

  @Test
  void quitStopsTheEngineAndRunsTheHook() {
    boolean[] quit = {false};
    controller.setOnQuit(() -> quit[0] = true);
    controller.execute("quit");
    assertTrue(quit[0]);
  }

  @Test
  void consoleThreadReadsLines() throws InterruptedException {
    Thread t = controller.startConsole(new ByteArrayInputStream(
        "q1 takeoff 7\nq1 hover 2\n".getBytes(StandardCharsets.UTF_8)));
    t.join(2000);
    assertEquals(List.of("takeoff 7", "hover 2"), queued());
  }

  @Test
  void savedMissionLoadsBackTheSamePlan(@TempDir Path dir) throws IOException {
    for (String line : List.of("q1 takeoff 12", "q1 goto 4 5 12", "q1 drop", "q1 hover 3",
        "q1 home", "q1 land")) {
      engine.submit(parser.parse(line).action());
    }
    Path file = dir.resolve("saved.txt");
    engine.submit(parser.parse("save " + file).action());
    engine.tick();

    List<String> saved = new MissionLoader(parser).load(file).stream()
        .map(ParsedCommand::text).toList();
    assertEquals(List.of("q1 takeoff 12", "q1 goto 4 5 12", "q1 drop", "q1 hover 3",
        "q1 home", "q1 land"), saved);
    assertTrue(Files.readString(file).contains("# q1 (Stub)"));
  }

  @Test
  void missionSavedInFlightRunsAgainFromTheGround(@TempDir Path dir) throws IOException {
    engine.submit(parser.parse("q1 takeoff 10").action());
    engine.submit(parser.parse("q1 goto 40 0 10").action());
    for (int i = 0; i < 3000 && !(engine.world().mission("q1").getCurrent() instanceof GoTo);
        i++) {
      engine.tick(); // until the take-off step is done and only the goto is left
    }
    Path file = dir.resolve("in-flight.txt");
    engine.submit(parser.parse("save " + file).action());
    engine.tick();

    List<ParsedCommand> loaded = new MissionLoader(parser).load(file);
    assertEquals(List.of("q1 takeoff 10", "q1 goto 40 0 10"),
        loaded.stream().map(ParsedCommand::text).toList());
    // A fresh world, as after --mission, accepts the whole file.
    World fresh = new World(new EventBus());
    fresh.addDrone(new StubDrone("q1", Vector3D.ZERO));
    List<SimEvent> freshEvents = new ArrayList<>();
    fresh.getEvents().subscribe(freshEvents::add);
    SimulationEngine replay = new SimulationEngine(fresh, 0.01);
    loaded.forEach(c -> replay.submit(c.action()));
    replay.runSteps(10);
    assertTrue(freshEvents.stream().noneMatch(e -> e.type() == SimEventType.COMMAND_REJECTED),
        freshEvents.toString());
  }

  @Test
  void saveAcceptsAPathWithSpaces(@TempDir Path dir) throws IOException {
    engine.submit(parser.parse("q1 takeoff 12").action());
    Path file = Files.createDirectories(dir.resolve("My Missions")).resolve("run 1.txt");
    engine.submit(parser.parse("save " + file).action());
    engine.tick();

    assertEquals(List.of("q1 takeoff 12"), new MissionLoader(parser).load(file).stream()
        .map(ParsedCommand::text).toList());
  }

  @Test
  void invalidStepIsRejectedWithTheDronesReason() {
    engine.submit(parser.parse("q1 hover 5").action()); // still on the ground
    engine.tick();
    SimEvent rejected = events.get(0);
    assertEquals(SimEventType.COMMAND_REJECTED, rejected.type());
    assertEquals("q1", rejected.droneId());
    assertEquals("'hover 5' rejected: q1 must take off before it can hover",
        rejected.message());
  }

  @Test
  void missionFileSkipsCommentsAndReportsLineNumbers(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("demo.txt");
    Files.writeString(file, "# demo\n\nq1 takeoff 10\n  q1 hover 3  \n");
    MissionLoader loader = new MissionLoader(parser);

    List<ParsedCommand> commands = loader.load(file);

    assertEquals(List.of("q1 takeoff 10", "q1 hover 3"),
        commands.stream().map(ParsedCommand::text).toList());
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> loader.load(new StringReader("q1 takeoff 5\nq1 jump\n"), "bad.txt"));
    assertTrue(e.getMessage().startsWith("bad.txt line 2: Unknown command 'jump'"));
  }

  @Test
  void missionFileCannotReset() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new MissionLoader(parser).load(new StringReader("q1 takeoff 5\n RESET \n"), "r.txt"));
    assertEquals("r.txt line 2: 'reset' cannot be used in a mission file", e.getMessage());
  }

  @Test
  void missionFileMayStartWithAByteOrderMark() throws IOException {
    MissionLoader loader = new MissionLoader(parser);

    assertEquals(List.of("q1 takeoff 10"), loader.load(
        new StringReader("\uFEFF# saved by Notepad\nq1 takeoff 10\n"), "bom.txt").stream()
        .map(ParsedCommand::text).toList());
    assertEquals(List.of("q1 takeoff 10"), loader.load(
        new StringReader("\uFEFFq1 takeoff 10\n"), "bom.txt").stream()
        .map(ParsedCommand::text).toList());
  }
}
