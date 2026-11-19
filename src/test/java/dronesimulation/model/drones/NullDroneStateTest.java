package dronesimulation.model.drones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NullDroneState}.
 */
public class NullDroneStateTest {

  private final DroneState state = NullDroneState.INSTANCE;

  @Test
  public void hasAName() {
    assertEquals("NONE", state.name());
  }

  @Test
  public void everyTransitionStaysInTheSameState() {
    assertSame(state, state.takeOff());
    assertSame(state, state.land());
    assertSame(state, state.crash());
  }
}
