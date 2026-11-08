package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PointMass} and the defaults of {@link PhysicsBody}.
 */
public class PointMassTest {

  @Test
  public void storesInitialState() {
    PointMass body = new PointMass(2.0, new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));
    assertEquals(2.0, body.getMass(), 0);
    assertEquals(new Vector3D(1, 2, 3), body.getPosition());
    assertEquals(new Vector3D(4, 5, 6), body.getVelocity());
  }

  @Test
  public void startsAtRestByDefault() {
    assertEquals(Vector3D.ZERO, new PointMass(1.0, Vector3D.UP).getVelocity());
  }

  @Test
  public void isActiveWithNoOwnForcesByDefault() {
    PointMass body = new PointMass(1.0, Vector3D.ZERO);
    assertTrue(body.isActive());
    assertEquals(Vector3D.ZERO, body.ownForces());
  }

  @Test
  public void settersUpdateState() {
    PointMass body = new PointMass(1.0, Vector3D.ZERO);
    body.setPosition(new Vector3D(7, 8, 9));
    body.setVelocity(new Vector3D(-1, 0, 1));
    body.setMass(1.5);

    assertEquals(new Vector3D(7, 8, 9), body.getPosition());
    assertEquals(new Vector3D(-1, 0, 1), body.getVelocity());
    assertEquals(1.5, body.getMass(), 0);
  }

  @Test
  public void rejectsInvalidMass() {
    assertThrows(IllegalArgumentException.class, () -> new PointMass(0, Vector3D.ZERO));
    assertThrows(IllegalArgumentException.class, () -> new PointMass(-1.5, Vector3D.ZERO));
    assertThrows(IllegalArgumentException.class, () -> new PointMass(Double.NaN, Vector3D.ZERO));
    PointMass body = new PointMass(1.0, Vector3D.ZERO);
    assertThrows(
        IllegalArgumentException.class, () -> body.setMass(Double.POSITIVE_INFINITY));
    assertEquals(1.0, body.getMass(), 0);
  }

  @Test
  public void rejectsNullVectors() {
    assertThrows(NullPointerException.class, () -> new PointMass(1.0, null));
    assertThrows(NullPointerException.class, () -> new PointMass(1.0, Vector3D.ZERO, null));
    PointMass body = new PointMass(1.0, Vector3D.ZERO);
    assertThrows(NullPointerException.class, () -> body.setPosition(null));
    assertThrows(NullPointerException.class, () -> body.setVelocity(null));
  }
}
