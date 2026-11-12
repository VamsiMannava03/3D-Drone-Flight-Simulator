package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import org.junit.jupiter.api.Test;

class SphereColliderTest {

  private static SolidPointMass ball(String id, double x, double y) {
    return new SolidPointMass(id, 1, 0.5, new Vector3D(x, y, 0), Vector3D.ZERO);
  }

  @Test
  void detectsOverlapAndDepth() {
    Collider collider = new SphereCollider();
    SolidPointMass a = ball("a", 0, 0);
    SolidPointMass b = ball("b", 0.8, 0);

    Collision hit = collider.check(a, b).orElseThrow();

    assertEquals(0.2, hit.depth(), 1e-9);
    assertSame(a, hit.first());
    assertSame(b, hit.second());
    assertEquals(0.5, collider.check(a, ball("d", 0.3, 0.4)).orElseThrow().depth(), 1e-9);
  }

  @Test
  void ignoresFarTouchingAndSelf() {
    Collider collider = new SphereCollider();
    SolidPointMass a = ball("a", 0, 0);
    assertTrue(collider.check(a, ball("c", 5, 0)).isEmpty());
    assertTrue(collider.check(a, ball("t", 1, 0)).isEmpty());
    assertTrue(collider.check(a, a).isEmpty());
  }
}
