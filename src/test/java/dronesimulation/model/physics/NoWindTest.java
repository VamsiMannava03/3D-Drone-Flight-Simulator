package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NoWind}.
 */
public class NoWindTest {

  @Test
  public void airIsStillEverywhere() {
    assertEquals(Vector3D.ZERO, NoWind.INSTANCE.airVelocityAt(Vector3D.ZERO));
    assertEquals(Vector3D.ZERO, NoWind.INSTANCE.airVelocityAt(new Vector3D(100, -50, 3000)));
  }
}
