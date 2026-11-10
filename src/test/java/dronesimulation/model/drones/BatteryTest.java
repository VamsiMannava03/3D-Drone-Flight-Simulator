package dronesimulation.model.drones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BatteryTest {

  @Test
  void startsFullAndDrainsToLowThenEmpty() {
    Battery battery = new Battery(1, 0.2); // 3600 J
    assertTrue(battery.isFull());
    assertEquals(3600, battery.getCapacity(), 1e-9);

    battery.drain(3000);
    assertEquals(600.0 / 3600, battery.getLevel(), 1e-9);
    assertTrue(battery.isLow());

    battery.drain(10_000);
    assertTrue(battery.isEmpty());
    assertEquals(0, battery.getCharge());
  }

  @Test
  void rechargeIsCappedAtCapacity() {
    Battery battery = new Battery(1, 0.2);
    battery.drain(1000);
    battery.recharge(5000);
    assertTrue(battery.isFull());
    assertFalse(battery.isLow());
  }

  @Test
  void canStartPartlyCharged() {
    Battery battery = new Battery(1, 0.2, 0.25);
    assertEquals(900, battery.getCharge(), 1e-9);
    assertEquals(0.2, battery.getLowLevel(), 1e-12);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new Battery(1, 0.2, 1.5));
    assertEquals("battery level must be between 0% and 100%, got 150.0%", e.getMessage());
  }

  @Test
  void specRejectsInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new DroneSpec(0, 5, 3, 100, 5, 100));
    assertThrows(IllegalArgumentException.class, () -> new DroneSpec(30, 5, 3, 100, -1, 100));
    assertThrows(IllegalArgumentException.class, () -> new DroneSpec(30, 5, 3, 100, 5, 0));
    assertEquals(3.0, DroneSpec.DEFAULT_SAFE_LANDING_SPEED);
  }

  @Test
  void rejectsInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new Battery(0, 0.2));
    assertThrows(IllegalArgumentException.class, () -> new Battery(1, 1.5));
    assertThrows(IllegalArgumentException.class, () -> new Battery(1, 0.2).drain(-1));
    assertThrows(IllegalArgumentException.class, () -> new Battery(1, 0.2).recharge(-1));
  }

  @Test
  void perfectSensorReadsExactly() {
    Vector3D p = new Vector3D(1, 2, 3);
    assertEquals(p, Sensor.perfect().read(p));
  }

  @Test
  void noisySensorIsRepeatableAndClose() {
    Sensor a = new NoisySensor(Sensor.perfect(), 0.1, new Random(5));
    Sensor b = new NoisySensor(Sensor.perfect(), 0.1, new Random(5));
    Vector3D p = new Vector3D(10, 10, 10);
    for (int i = 0; i < 100; i++) {
      Vector3D reading = a.read(p);
      assertEquals(reading, b.read(p));
      assertTrue(reading.distanceTo(p) < 1.0);
    }
    assertEquals(0.1, new NoisySensor(Sensor.perfect(), 0.1, new Random()).getNoise());
    assertThrows(IllegalArgumentException.class,
        () -> new NoisySensor(Sensor.perfect(), -1, new Random()));
  }

  @Test
  void decoratorsStack() {
    Vector3D offset = new Vector3D(2, 0, -1);
    Sensor biased = new BiasedSensor(Sensor.perfect(), offset);
    assertEquals(new Vector3D(3, 2, 2), biased.read(new Vector3D(1, 2, 3)));

    Sensor noisyBiased = new NoisySensor(biased, 0.05, new Random(9));
    double meanX = 0;
    for (int i = 0; i < 2000; i++) {
      meanX += noisyBiased.read(Vector3D.ZERO).x() / 2000;
    }
    assertEquals(2, meanX, 0.01, "the noise averages out, the bias does not");
    assertEquals(Vector3D.ZERO, new NoisySensor(Sensor.perfect(), 0, new Random())
        .read(Vector3D.ZERO));
  }

  @Test
  void biasedGpsMakesTheDroneFlyToTheWrongPlace() {
    Drone d = new StubDrone("d1", new Vector3D(0, 0, 5), new Battery(1, 0.2),
        new BiasedSensor(Sensor.perfect(), new Vector3D(1, 0, 0)));
    d.takeOff(4);
    d.update(0.01);
    d.setController(d.navigateTo(new Vector3D(0, 0, 5), 0.1));
    dronesimulation.model.physics.TestWorld w = new dronesimulation.model.physics.TestWorld();
    w.addForce(new dronesimulation.model.physics.Gravity());
    w.addBody(d);
    for (int i = 0; i < 2000; i++) {
      d.update(0.01);
      w.step(0.01);
    }
    // It believes it is 1 m further east than it is, so it settles 1 m west of the target.
    assertEquals(-1, d.getPosition().x(), 0.01);
  }
}
