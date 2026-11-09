package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WindTest {

  @Test
  void bodyInConstantWindDriftsToWindSpeed() {
    ConstantWind wind = new ConstantWind(new Vector3D(3, 0, 0));
    OwnPhysicsEngine engine = new OwnPhysicsEngine(List.of(new QuadraticDrag(5, wind)));
    PointMass body = new PointMass(1, new Vector3D(0, 0, 10));
    engine.addBody(body);

    for (int i = 0; i < 20_000; i++) {
      engine.step(0.001);
    }

    assertEquals(3, body.getVelocity().x(), 0.02);
    assertEquals(new Vector3D(3, 0, 0), wind.getVelocity());
  }

  @Test
  void gustyWindIsRepeatableWithSameSeed() {
    GustyWind a = new GustyWind(new Vector3D(1, 0, 0), 4, 2, new Random(42), () -> 0);
    GustyWind b = new GustyWind(new Vector3D(1, 0, 0), 4, 2, new Random(42), () -> 0);
    Vector3D late = b.velocityAt(9.3);
    Vector3D early = b.velocityAt(1.7);
    assertEquals(a.velocityAt(1.7), early);
    assertEquals(a.velocityAt(9.3), late);
  }

  @Test
  void gustyWindReadsItsClock() {
    double[] now = {0.5};
    GustyWind wind = new GustyWind(new Vector3D(2, 0, 0), 3, 1, new Random(7), () -> now[0]);
    Vector3D early = wind.airVelocityAt(Vector3D.ZERO);
    now[0] = 5.5;
    assertNotEquals(early, wind.airVelocityAt(Vector3D.ZERO));
    assertEquals(wind.velocityAt(5.5), wind.airVelocityAt(new Vector3D(10, 10, 10)));
  }

  @Test
  void gustyWindStaysWithinStrength() {
    Vector3D base = new Vector3D(2, 0, 0);
    GustyWind wind = new GustyWind(base, 3, 1, new Random(7), () -> 0);
    for (double t = 0; t < 30; t += 0.1) {
      Vector3D gust = wind.velocityAt(t).subtract(base);
      assertTrue(gust.magnitude() <= 3 + 1e-9);
      assertEquals(0, gust.z(), 1e-12);
    }
    assertEquals(base, wind.getBase());
    assertEquals(3, wind.getGustStrength());
  }

  @Test
  void gustyWindRejectsBadParameters() {
    assertThrows(IllegalArgumentException.class,
        () -> new GustyWind(Vector3D.ZERO, -1, 1, new Random(), () -> 0));
    assertThrows(IllegalArgumentException.class,
        () -> new GustyWind(Vector3D.ZERO, 1, 0, new Random(), () -> 0));
    assertThrows(NullPointerException.class,
        () -> new GustyWind(Vector3D.ZERO, 1, 1, new Random(), null));
  }

  @Test
  void compassDirectionIsWhereTheWindComesFrom() {
    Vector3D fromWest = Winds.velocity(5, 270);
    assertEquals(5, fromWest.x(), 1e-9);
    assertEquals(0, fromWest.y(), 1e-9);
    Vector3D fromNorth = Winds.velocity(2, 0);
    assertEquals(-2, fromNorth.y(), 1e-9);
  }

  @Test
  void windsPicksConstantOrGusty() {
    assertInstanceOf(ConstantWind.class, Winds.from(5, 270, 0, () -> 0));
    WindField gusty = Winds.from(5, 270, 2, () -> 1);
    assertInstanceOf(GustyWind.class, gusty);
    assertEquals(gusty.airVelocityAt(Vector3D.ZERO),
        Winds.from(5, 270, 2, () -> 1).airVelocityAt(Vector3D.ZERO));
    assertThrows(IllegalArgumentException.class, () -> Winds.from(-1, 0, 0, () -> 0));
    assertThrows(IllegalArgumentException.class, () -> Winds.from(1, 0, -1, () -> 0));
  }
}
