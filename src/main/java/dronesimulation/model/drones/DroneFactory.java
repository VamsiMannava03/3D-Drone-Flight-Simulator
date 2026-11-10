package dronesimulation.model.drones;

import dronesimulation.model.Vector3D;
import java.util.Objects;
import java.util.Random;

/**
 * Makes ready-to-fly drones from presets (Factory pattern). Use {@link #builder} to start from
 * a preset and change a few parts.
 */
public final class DroneFactory {

  /** Sensor noise used by the presets, in m. */
  public static final double PRESET_SENSOR_NOISE = 0.02;

  private DroneFactory() {
  }

  /**
   * Returns a builder filled in with the preset for a drone type.
   *
   * @param type the kind of drone
   * @param id a unique, non-blank name
   * @param home the start position
   * @param random the seeded source of sensor noise
   * @return a builder ready to {@link DroneBuilder#build()} or adjust
   */
  public static DroneBuilder builder(DroneType type, String id, Vector3D home, Random random) {
    Objects.requireNonNull(random, "random");
    DroneBuilder builder = new DroneBuilder(type, id).at(home)
        .sensorNoise(PRESET_SENSOR_NOISE, random);
    return switch (type) {
      // Hover is about 46% throttle: 8 + 450 * 0.46^1.5 = about 150 W.
      case QUADCOPTER -> builder.mass(1.5).radius(0.45).maxThrust(32).cruiseSpeed(6)
          .battery(10, 0.2).maxPower(450).chargePower(150).idlePower(8).dragArea(0.035);
      // Hover is about 40% throttle (2.6 kg on 64 N): 12 + 950 * 0.40^1.5 = about 250 W.
      case HEXACOPTER -> builder.mass(2.6).radius(0.6).maxThrust(64).cruiseSpeed(7)
          .battery(18, 0.2).maxPower(950).chargePower(250).idlePower(12).dragArea(0.05);
      case FIXED_WING -> builder.mass(2.0).radius(0.9).maxThrust(40).cruiseSpeed(16)
          .stallSpeed(9).battery(15, 0.2).maxPower(300).chargePower(200).idlePower(6)
          .dragArea(0.025);
    };
  }

  /**
   * Makes a drone from the preset for its type.
   *
   * @param type the kind of drone
   * @param id a unique, non-blank name
   * @param home the start position
   * @param random the seeded source of sensor noise
   * @return the new drone
   */
  public static Drone create(DroneType type, String id, Vector3D home, Random random) {
    return builder(type, id, home, random).build();
  }
}
