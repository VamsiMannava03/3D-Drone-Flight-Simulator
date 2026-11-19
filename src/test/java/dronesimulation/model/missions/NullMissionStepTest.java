package dronesimulation.model.missions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.drones.NullDrone;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NullMissionStep}.
 */
public class NullMissionStepTest {

  private final MissionStep step = NullMissionStep.INSTANCE;

  @Test
  public void isValidAndCompleteForAnyDrone() {
    assertTrue(step.isValidFor(NullDrone.INSTANCE));
    assertTrue(step.isComplete(NullDrone.INSTANCE));
  }

  @Test
  public void executeDoesNothing() {
    assertDoesNotThrow(() -> step.execute(NullDrone.INSTANCE));
  }
}
