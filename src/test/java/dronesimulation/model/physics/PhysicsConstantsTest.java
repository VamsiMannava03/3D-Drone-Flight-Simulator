package dronesimulation.model.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PhysicsConstants}.
 */
public class PhysicsConstantsTest {

  @Test
  public void matchesTheSpecification() {
    assertEquals(9.81, PhysicsConstants.GRAVITY, 0);
    assertEquals(0.01, PhysicsConstants.TIME_STEP, 0);
  }
}
