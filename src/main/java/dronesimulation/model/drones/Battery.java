package dronesimulation.model.drones;

/**
 * A drone battery that stores energy. Flying drains it in proportion to the power the motors
 * use, and charging refills it.
 */
public class Battery {

  private static final double JOULES_PER_WATT_HOUR = 3600;

  private final double capacity;
  private final double lowLevel;
  private double charge;

  /**
   * Creates a fully charged battery.
   *
   * @param capacityWattHours the capacity in Wh, must be positive
   * @param lowLevel the fraction (0 to 1) below which the battery counts as low
   * @throws IllegalArgumentException if the capacity is not positive or lowLevel is outside 0..1
   */
  public Battery(double capacityWattHours, double lowLevel) {
    this(capacityWattHours, lowLevel, 1.0);
  }

  /**
   * Creates a battery with a starting charge.
   *
   * @param capacityWattHours the capacity in Wh, must be positive
   * @param lowLevel the fraction (0 to 1) below which the battery counts as low
   * @param startLevel the starting charge as a fraction (0 to 1)
   * @throws IllegalArgumentException if the capacity is not positive, or a fraction is outside
   *     0..1
   */
  public Battery(double capacityWattHours, double lowLevel, double startLevel) {
    if (!(capacityWattHours > 0) || !Double.isFinite(capacityWattHours)) {
      throw new IllegalArgumentException("capacity must be positive, got " + capacityWattHours);
    }
    if (!(lowLevel >= 0 && lowLevel <= 1)) {
      throw new IllegalArgumentException("lowLevel must be between 0 and 1, got " + lowLevel);
    }
    if (!(startLevel >= 0 && startLevel <= 1)) {
      throw new IllegalArgumentException(
          "battery level must be between 0% and 100%, got " + startLevel * 100 + "%");
    }
    this.capacity = capacityWattHours * JOULES_PER_WATT_HOUR;
    this.lowLevel = lowLevel;
    this.charge = capacity * startLevel;
  }

  /**
   * Returns the warning level.
   *
   * @return the fraction below which the battery counts as low
   */
  public double getLowLevel() {
    return lowLevel;
  }

  /**
   * Returns the capacity.
   *
   * @return the capacity in J
   */
  public double getCapacity() {
    return capacity;
  }

  /**
   * Returns the stored energy.
   *
   * @return the charge in J
   */
  public double getCharge() {
    return charge;
  }

  /**
   * Returns how full the battery is.
   *
   * @return the charge as a fraction from 0 to 1
   */
  public double getLevel() {
    return charge / capacity;
  }

  /**
   * Tells whether the level is below the warning level.
   *
   * @return true if low
   */
  public boolean isLow() {
    return getLevel() < lowLevel;
  }

  /**
   * Tells whether the battery has no energy left.
   *
   * @return true if empty
   */
  public boolean isEmpty() {
    return charge <= 0;
  }

  /**
   * Tells whether the battery is full.
   *
   * @return true if full
   */
  public boolean isFull() {
    return charge >= capacity;
  }

  /**
   * Removes energy. The charge never goes below zero.
   *
   * @param joules the energy used, must be non-negative
   * @throws IllegalArgumentException if joules is negative
   */
  public void drain(double joules) {
    requireNonNegative(joules);
    charge = Math.max(0, charge - joules);
  }

  /**
   * Adds energy. The charge never goes above the capacity.
   *
   * @param joules the energy added, must be non-negative
   * @throws IllegalArgumentException if joules is negative
   */
  public void recharge(double joules) {
    requireNonNegative(joules);
    charge = Math.min(capacity, charge + joules);
  }

  private static void requireNonNegative(double joules) {
    if (!(joules >= 0) || !Double.isFinite(joules)) {
      throw new IllegalArgumentException("energy must be non-negative, got " + joules);
    }
  }
}
