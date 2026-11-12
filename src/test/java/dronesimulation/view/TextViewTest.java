package dronesimulation.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.StubDrone;
import dronesimulation.model.engine.SimulationEngine;
import dronesimulation.model.events.EventBus;
import dronesimulation.model.missions.TakeOff;
import dronesimulation.model.world.World;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TextViewTest {

  @Test
  void printsEveryDroneAtMostOncePerInterval() {
    World world = new World(new EventBus());
    world.addDrone(new StubDrone("d1", Vector3D.ZERO));
    world.addDrone(new StubDrone("d2", new Vector3D(5, 0, 0)));
    world.mission("d1").add(new TakeOff(5));
    SimulationEngine engine = new SimulationEngine(world, 0.01);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    TextView view = new TextView(new PrintStream(buffer, true, StandardCharsets.UTF_8), 1.0);
    engine.addSnapshotListener(view::render);

    for (int i = 0; i < 25; i++) {
      engine.runSteps(10); // a snapshot every 0.1 s for 2.5 s
    }

    String text = buffer.toString(StandardCharsets.UTF_8);
    assertEquals(3, text.split("--- t = ", -1).length - 1, text); // t = 0.1, 1.1, 2.1
    assertTrue(text.contains("d1    Stub       TakingOff") || text.contains("d1    Stub       Flying"));
    assertTrue(text.contains("[takeoff 5]"));
    assertTrue(text.contains("d2    Stub       Idle"));
  }

  @Test
  void printsAgainRightAfterAReset() {
    SimulationEngine engine = new SimulationEngine(() -> {
      World world = new World(new EventBus());
      world.addDrone(new StubDrone("d1", Vector3D.ZERO));
      return world;
    }, 0.01);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    TextView view = new TextView(new PrintStream(buffer, true, StandardCharsets.UTF_8), 1.0);
    engine.addSnapshotListener(view::render);
    for (int i = 0; i < 25; i++) {
      engine.runSteps(10);
    }

    engine.reset(); // time goes back to 0
    engine.runSteps(10);

    String text = buffer.toString(StandardCharsets.UTF_8);
    assertEquals(4, text.split("--- t = ", -1).length - 1, text); // 0.1, 1.1, 2.1, then 0.x
    assertTrue(text.substring(text.lastIndexOf("--- t = ")).startsWith("--- t = 0."), text);
  }

  @Test
  void showMessagePrintsALine() {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    new TextView(new PrintStream(buffer, true, StandardCharsets.UTF_8), 1).showMessage("hello");
    assertEquals("hello" + System.lineSeparator(), buffer.toString(StandardCharsets.UTF_8));
  }
}
