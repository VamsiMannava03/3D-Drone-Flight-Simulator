package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

class SolidPointMassTest {

  @Test
  void usesDefaultsUnlessGiven() {
    SolidPointMass body = new SolidPointMass("b", 2, Vector3D.ZERO, Vector3D.ZERO);
    assertEquals("b", body.getId());
    assertEquals(2, body.getMass());
    assertEquals(SolidBody.DEFAULT_RADIUS, body.getRadius());
    assertEquals(SolidBody.DEFAULT_DRAG_AREA, body.getDragArea());
    assertEquals(SolidBody.DEFAULT_GROUND_FRICTION, body.getGroundFriction());
    assertDoesNotThrow(() -> body.onGroundImpact(5));
  }

  @Test
  void storesRadiusAndDragArea() {
    SolidPointMass body = new SolidPointMass("b", 1, 0.5, Vector3D.ZERO, Vector3D.ZERO);
    body.setDragArea(0.04);
    assertEquals(0.5, body.getRadius());
    assertEquals(0.04, body.getDragArea());
  }

  @Test
  void rejectsBadValues() {
    assertThrows(IllegalArgumentException.class,
        () -> new SolidPointMass(" ", 1, Vector3D.ZERO, Vector3D.ZERO));
    assertThrows(NullPointerException.class,
        () -> new SolidPointMass(null, 1, Vector3D.ZERO, Vector3D.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> new SolidPointMass("b", 1, 0, Vector3D.ZERO, Vector3D.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> new SolidPointMass("b", 1, Double.NaN, Vector3D.ZERO, Vector3D.ZERO));
    SolidPointMass body = new SolidPointMass("b", 1, Vector3D.ZERO, Vector3D.ZERO);
    assertThrows(IllegalArgumentException.class, () -> body.setDragArea(-0.1));
    assertThrows(IllegalArgumentException.class, () -> body.setDragArea(Double.NaN));
  }
}
