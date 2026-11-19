package dronesimulation.model.drones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.missions.NullMissionStep;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NullDrone}.
 */
public class NullDroneTest {

  private final Drone drone = NullDrone.INSTANCE;

  @Test
  public void reportsEmptyValues() {
    assertEquals("none", drone.getId());
    assertEquals(0, drone.getBatteryLevel(), 0);
    assertEquals(0, drone.getMaxThrust(), 0);
    assertEquals(0, drone.getThrottle(), 0);
    assertEquals(Vector3D.ZERO, drone.getPosition());
    assertEquals(Vector3D.ZERO, drone.getVelocity());
  }

  @Test
  public void hasPositiveMassAndIsNeverActive() {
    assertTrue(drone.getMass() > 0);
    assertFalse(drone.isActive());
  }

  @Test
  public void usesTheNullStateAndNullStep() {
    assertSame(NullDroneState.INSTANCE, drone.getState());
    assertSame(NullMissionStep.INSTANCE, drone.getCurrentStep());
  }

  @Test
  public void ignoresSettersAndLifecycleActions() {
    drone.setThrottle(0.5);
    drone.setPosition(new Vector3D(1, 2, 3));
    drone.setVelocity(new Vector3D(4, 5, 6));
    drone.takeOff();
    drone.land();
    drone.crash();
    assertEquals(0, drone.getThrottle(), 0);
    assertEquals(Vector3D.ZERO, drone.getPosition());
    assertEquals(Vector3D.ZERO, drone.getVelocity());
    assertSame(NullDroneState.INSTANCE, drone.getState());
  }
}
