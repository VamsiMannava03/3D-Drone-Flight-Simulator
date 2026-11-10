package dronesimulation.model.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.drones.Battery;
import dronesimulation.model.drones.Drone;
import dronesimulation.model.drones.StubDrone;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Every legal and illegal state change from the lifecycle diagram. */
class DroneStateTest {

  private final Drone ground = new StubDrone("d1", Vector3D.ZERO);
  private final Drone air = new StubDrone("d2", new Vector3D(0, 0, 10));

  private static void assertIllegal(DroneState state, Function<DroneState, DroneState> action) {
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> action.apply(state));
    assertTrue(e.getMessage().endsWith("while " + state.name()), e.getMessage());
  }

  @Test
  void idle() {
    DroneState s = Idle.INSTANCE;
    assertSame(TakingOff.INSTANCE, s.takeOff(ground));
    assertIllegal(s, x -> x.land(ground));
    assertIllegal(s, x -> x.charge(ground));
    assertIllegal(s, x -> x.crash(ground));
    assertFalse(s.canFly());
    assertFalse(s.canCrash());
  }

  @Test
  void takingOffBecomesFlyingAtTargetAltitude() {
    DroneState s = TakingOff.INSTANCE;
    Drone d = new StubDrone("d", Vector3D.ZERO);
    d.takeOff(10);
    assertSame(s, s.update(d, 0.02));
    d.setPosition(new Vector3D(0, 0, 9.6));
    assertSame(Flying.INSTANCE, s.update(d, 0.02));
    assertIllegal(s, x -> x.takeOff(ground));
    assertIllegal(s, x -> x.land(ground));
    assertIllegal(s, x -> x.charge(ground));
    assertSame(Crashed.INSTANCE, s.crash(ground)); // a collision during the climb is a crash
    assertTrue(s.canFly());
    assertTrue(s.canCrash());
    assertSame(s, s.touchdown(ground, 1)); // bumping the ground during a run-up is ignored
    // Falling back down mid-climb, for example on an empty battery, is a crash.
    assertSame(Crashed.INSTANCE, s.touchdown(ground, 5));
  }

  @Test
  void flying() {
    DroneState s = Flying.INSTANCE;
    assertSame(Landing.INSTANCE, s.land(air));
    assertSame(Crashed.INSTANCE, s.crash(air));
    assertSame(Landing.INSTANCE, s.touchdown(air, 1.0));
    assertSame(Crashed.INSTANCE, s.touchdown(air, 3.0));
    assertIllegal(s, x -> x.takeOff(air));
    assertIllegal(s, x -> x.charge(air));
    assertTrue(s.canFly());
    assertTrue(s.canCrash());
  }

  @Test
  void landing() {
    DroneState s = Landing.INSTANCE;
    assertSame(Landed.INSTANCE, s.touchdown(air, 1.9));
    assertSame(Crashed.INSTANCE, s.touchdown(air, 2.1));
    assertSame(Crashed.INSTANCE, s.crash(air));
    assertSame(s, s.update(air, 0.02));
    assertSame(Landed.INSTANCE, s.update(ground, 0.02));
    assertIllegal(s, x -> x.takeOff(air));
    assertIllegal(s, x -> x.land(air));
    assertIllegal(s, x -> x.charge(air));
  }

  @Test
  void landed() {
    DroneState s = Landed.INSTANCE;
    assertSame(Charging.INSTANCE, s.charge(ground));
    assertIllegal(s, x -> x.takeOff(ground));
    assertIllegal(s, x -> x.land(ground));
    assertIllegal(s, x -> x.crash(ground));
    assertFalse(s.canFly());
  }

  @Test
  void chargingRefillsThenBecomesIdle() {
    Battery battery = new Battery(1, 0.2);
    battery.drain(2000);
    Drone d = new StubDrone("d", Vector3D.ZERO, battery);
    DroneState s = Charging.INSTANCE;

    assertSame(s, s.update(d, 1.0)); // 1000 W for 1 s: 1600 J of 3600 J
    assertEquals(2600, battery.getCharge(), 1e-9);
    assertSame(Idle.INSTANCE, s.update(d, 1.0));
    assertIllegal(s, x -> x.takeOff(d));
    assertIllegal(s, x -> x.land(d));
    assertIllegal(s, x -> x.charge(d));
    assertIllegal(s, x -> x.crash(d));
  }

  @Test
  void crashedIsTerminal() {
    DroneState s = Crashed.INSTANCE;
    assertIllegal(s, x -> x.takeOff(ground));
    assertIllegal(s, x -> x.land(ground));
    assertIllegal(s, x -> x.charge(ground));
    assertIllegal(s, x -> x.crash(ground));
    assertSame(s, s.update(ground, 1));
    assertSame(s, s.touchdown(ground, 50));
    assertFalse(s.canFly());
  }

  @Test
  void illegalMessageIsClear() {
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> Crashed.INSTANCE.takeOff(ground));
    assertEquals("Cannot take off while Crashed", e.getMessage());
  }
}
