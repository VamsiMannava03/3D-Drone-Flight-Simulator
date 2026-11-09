package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dronesimulation.model.Vector3D;
import java.util.List;
import org.junit.jupiter.api.Test;

class AtmosphereTest {

  private static final double G = PhysicsConstants.GRAVITY;

  @Test
  void standardDensityFallsExponentially() {
    Atmosphere air = Atmosphere.standard();
    assertEquals(1.225, air.densityAt(0), 1e-12);
    assertEquals(1.225 / Math.E, air.densityAt(Atmosphere.SCALE_HEIGHT), 1e-12);
    assertEquals(1.225, air.densityAt(-5), 1e-12);
  }

  @Test
  void uniformDensityIsTheSameEverywhere() {
    assertEquals(2.0, Atmosphere.uniform(2.0).densityAt(1000), 1e-12);
    assertThrows(IllegalArgumentException.class, () -> Atmosphere.uniform(0));
    assertThrows(IllegalArgumentException.class, () -> Atmosphere.uniform(Double.NaN));
  }

  @Test
  void dragUsesBodyAreaAndLocalDensity() {
    SolidPointMass low = new SolidPointMass("low", 1, Vector3D.ZERO, new Vector3D(10, 0, 0));
    SolidPointMass high = new SolidPointMass("high", 1,
        new Vector3D(0, 0, Atmosphere.SCALE_HEIGHT), new Vector3D(10, 0, 0));
    low.setDragArea(0.04);
    high.setDragArea(0.04);
    Force drag = new AtmosphericDrag(Atmosphere.standard());

    double atGround = 0.5 * 1.225 * 0.04 * 100;
    assertEquals(-atGround, drag.compute(low).x(), 1e-9);
    assertEquals(-atGround / Math.E, drag.compute(high).x(), 1e-9);
  }

  @Test
  void dragIsRelativeToTheWind() {
    SolidPointMass still = new SolidPointMass("b", 1, Vector3D.ZERO, Vector3D.ZERO);
    WindField wind = position -> new Vector3D(5, 0, 0);
    AtmosphericDrag drag = new AtmosphericDrag(Atmosphere.uniform(1.0), wind);
    assertEquals(0.5 * SolidBody.DEFAULT_DRAG_AREA * 25, drag.compute(still).x(), 1e-9);
    assertSame(wind, drag.getWind());
  }

  @Test
  void plainBodiesFeelNoDrag() {
    PointMass plain = new PointMass(1, Vector3D.ZERO, new Vector3D(10, 0, 0));
    assertEquals(Vector3D.ZERO, new AtmosphericDrag(Atmosphere.standard()).compute(plain));
  }

  @Test
  void terminalVelocityWithinPointOnePercent() {
    double rho = 1.225;
    double area = 0.05;
    OwnPhysicsEngine engine = new OwnPhysicsEngine(
        List.of(new Gravity(), new AtmosphericDrag(Atmosphere.uniform(rho))));
    SolidPointMass body = new SolidPointMass("ball", 1, new Vector3D(0, 0, 50_000),
        Vector3D.ZERO);
    body.setDragArea(area);
    engine.addBody(body);

    for (int i = 0; i < 6000; i++) {
      engine.step(PhysicsConstants.TIME_STEP);
    }

    double terminal = Math.sqrt(2 * G / (rho * area));
    assertEquals(terminal, -body.getVelocity().z(), terminal * 0.001);
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> new AtmosphericDrag(null));
    assertThrows(NullPointerException.class,
        () -> new AtmosphericDrag(Atmosphere.standard(), null));
  }
}
