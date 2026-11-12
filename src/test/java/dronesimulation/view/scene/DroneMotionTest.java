package dronesimulation.view.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dronesimulation.model.Vector3D;
import dronesimulation.model.engine.DroneSnapshot;
import org.junit.jupiter.api.Test;

class DroneMotionTest {

  private static DroneSnapshot drone(String type, String state, Vector3D position,
      Vector3D velocity, Vector3D thrust) {
    return new DroneSnapshot("d", type, state, position, velocity, thrust, 0.5, 0.9, 1.5, 0,
        "", 0, null, Vector3D.ZERO, 0.3);
  }

  @Test
  void tiltsWithTheThrustAndTurnsToTheVelocity() {
    DroneMotion motion = new DroneMotion(0x3B82F6);
    DroneSnapshot d = drone("Quadcopter", "Flying", new Vector3D(0, 0, 10),
        new Vector3D(0, 5, 0), new Vector3D(0, 5, 15));
    for (int i = 0; i < 200; i++) {
      motion.update(d, 1.0 / 60);
    }
    assertTrue(motion.up()[1] > 0.2, "leans toward +y");
    assertEquals(Math.PI / 2, motion.yaw(), 0.05);
    assertTrue(motion.rotorRate() > 28);
    assertTrue(motion.rotorAngle() > 0);
    assertEquals(0x3B82F6, motion.colour());
    double[] p = Mat4.transform(motion.model(d), 0, 0, 0, 1);
    assertEquals(10 + 0.08 * DroneMotion.QUAD_SCALE, p[2], 1e-4);
  }

  @Test
  void leavesATrailOfSpacedPoints() {
    DroneMotion motion = new DroneMotion(0);
    for (int i = 0; i < 10; i++) {
      motion.update(drone("Quadcopter", "Flying", new Vector3D(i * 0.3, 0, 5), Vector3D.ZERO,
          Vector3D.ZERO), 0.1);
    }
    assertEquals(5, motion.trail().size());
    for (int i = 0; i < DroneMotion.TRAIL_POINTS + 20; i++) {
      motion.update(drone("Quadcopter", "Flying", new Vector3D(i, 0, 5), Vector3D.ZERO,
          Vector3D.ZERO), 0.1);
    }
    assertEquals(DroneMotion.TRAIL_POINTS, motion.trail().size());
  }

  @Test
  void crashedDronesTipOverAndStopSpinning() {
    DroneMotion motion = new DroneMotion(0);
    DroneSnapshot d = drone("Hexacopter", "Crashed", Vector3D.ZERO, Vector3D.ZERO,
        new Vector3D(0, 0, 20));
    for (int i = 0; i < 300; i++) {
      motion.update(d, 1.0 / 60);
    }
    assertTrue(motion.up()[2] < 0.5);
    assertEquals(0, motion.rotorRate(), 1e-3);
    assertSame(DroneModels.HEX_MOTORS, DroneMotion.motorsOf(d));
    assertTrue(DroneMotion.isCrashed(d));
    assertFalse(DroneMotion.isActive(d));
  }

  @Test
  void planesBankAndPitch() {
    DroneMotion motion = new DroneMotion(0);
    DroneSnapshot d = drone("FixedWing", "Flying", new Vector3D(0, 0, 30),
        new Vector3D(15, 0, 3), new Vector3D(10, 0, 20));
    for (int i = 0; i < 120; i++) {
      motion.update(d, 1.0 / 60);
    }
    assertTrue(DroneMotion.isPlane(d));
    assertTrue(DroneMotion.isActive(d));
    assertEquals(30 + 0.14 * DroneMotion.PLANE_SCALE, DroneMotion.visualCentre(d)[2], 1e-9);
    assertSame(DroneModels.QUAD_MOTORS, DroneMotion.motorsOf(
        drone("Quadcopter", "Idle", Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO)));
    double[] nose = Mat4.transform(motion.model(d), 1, 0, 0, 0);
    assertTrue(nose[2] > 0, "nose up while climbing");
  }

  @Test
  void wrapsAnglesIntoMinusPiToPi() {
    assertEquals(0.5, DroneMotion.wrapAngle(0.5 + 4 * Math.PI), 1e-9);
    assertEquals(-3, DroneMotion.wrapAngle(-3), 1e-9);
    assertEquals(Math.PI - 0.1, DroneMotion.wrapAngle(-Math.PI - 0.1), 1e-9);
  }
}
