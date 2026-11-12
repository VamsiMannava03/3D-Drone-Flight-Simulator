package dronesimulation.model.missions;

import dronesimulation.model.drones.Drone;
import dronesimulation.model.states.Charging;
import dronesimulation.model.states.Crashed;
import dronesimulation.model.states.Idle;
import dronesimulation.model.states.Landed;
import java.util.Locale;

/**
 * What a drone's situation will be after the commands checked so far: in the air or not, whether
 * it must charge before flying again, and how much payload it still carries. Each command checks
 * itself against the plan and then updates it, so a whole mission can be validated before it
 * runs.
 */
public final class MissionPlan {

  /** Highest altitude any step may fly to, in m (a common drone flight ceiling). */
  public static final double MAX_ALTITUDE = 120;

  private final Drone drone;
  private boolean airborne;
  private boolean mustCharge;
  private boolean crashed;
  private double payload;

  private MissionPlan(Drone drone) {
    this.drone = drone;
  }

  /**
   * Starts a plan from a drone's current state.
   *
   * @param drone the drone
   * @return the plan
   */
  public static MissionPlan from(Drone drone) {
    MissionPlan plan = new MissionPlan(drone);
    var state = drone.getState();
    plan.crashed = state == Crashed.INSTANCE;
    plan.airborne = !plan.crashed && state != Idle.INSTANCE && state != Landed.INSTANCE
        && state != Charging.INSTANCE;
    plan.mustCharge = state == Landed.INSTANCE;
    plan.payload = drone.getPayloadMass();
    return plan;
  }

  /**
   * Returns the drone being planned for.
   *
   * @return the drone
   */
  public Drone drone() {
    return drone;
  }

  /**
   * Fails unless the drone will be in the air.
   *
   * @param action what the step wants to do
   * @throws IllegalStateException if the drone will be on the ground
   */
  public void requireAirborne(String action) {
    requireNotCrashed();
    if (!airborne) {
      throw new IllegalStateException(drone.getId() + " must take off before it can " + action);
    }
  }

  /**
   * Fails unless the drone will be on the ground and able to take off.
   *
   * @throws IllegalStateException if it will be flying or must charge first
   */
  public void requireReadyToTakeOff() {
    requireNotCrashed();
    if (airborne) {
      throw new IllegalStateException(drone.getId() + " is already flying");
    }
    if (mustCharge) {
      throw new IllegalStateException(drone.getId() + " must charge after landing before it "
          + "can take off again");
    }
  }

  /**
   * Fails if an altitude is above the flight ceiling. (Steps below the ground are already
   * refused when they are created, by the TakeOff and GoTo constructors.)
   *
   * @param altitude the altitude, in m
   * @throws IllegalArgumentException if it is out of range
   */
  public void requireAltitude(double altitude) {
    if (altitude > MAX_ALTITUDE) {
      throw new IllegalArgumentException(String.format(Locale.ROOT,
          "%.0f m is above the %.0f m flight ceiling", altitude, MAX_ALTITUDE));
    }
  }

  /**
   * Fails unless the drone will carry a payload.
   *
   * @throws IllegalStateException if it will carry none
   */
  public void requirePayload() {
    if (payload <= 0) {
      throw new IllegalStateException(drone.getId() + " has no payload to drop");
    }
  }

  private void requireNotCrashed() {
    if (crashed) {
      throw new IllegalStateException(drone.getId() + " has crashed");
    }
  }

  /** Records a take-off. */
  public void tookOff() {
    airborne = true;
  }

  /** Records a landing: the drone must charge before its next take-off. */
  public void landed() {
    airborne = false;
    mustCharge = true;
  }

  /** Records a full charge. */
  public void charged() {
    mustCharge = false;
  }

  /**
   * Fails unless the drone will be landed and needing a charge.
   *
   * @throws IllegalStateException if it will be flying or has nothing to charge after
   */
  public void requireLandedForCharge() {
    requireNotCrashed();
    if (airborne || !mustCharge) {
      throw new IllegalStateException(drone.getId() + " can only charge after landing");
    }
  }

  /** Records that the payload is gone. */
  public void droppedPayload() {
    payload = 0;
  }
}
