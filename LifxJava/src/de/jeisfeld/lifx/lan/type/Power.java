package de.jeisfeld.lifx.lan.type;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Possible power values.
 */
public class Power {
	/**
	 * The Power for OFF.
	 */
	public static final Power OFF = new Power((short) 0);
	/**
	 * The Power for ON.
	 */
	public static final Power ON = new Power((short) -1);
	/**
	 * The Power for ON.
	 */
	public static final Power UNDEFINED = new Power(Short.MAX_VALUE);

	/**
	 * The power level.
	 */
	private final short mLevel;

	/**
	 * Constructor.
	 *
	 * @param level The power level.
	 */
	public Power(final short level) {
		mLevel = level;
	}

	/**
	 * Get the power level.
	 *
	 * @return the power level.
	 */
	public short getLevel() {
		return mLevel;
	}

	/**
	 * Determine if power is on.
	 *
	 * @return true if power is on.
	 */
	public boolean isOn() {
		return equals(Power.ON);
	}

	/**
	 * Determine if power is off.
	 *
	 * @return true if power is off.
	 */
	public boolean isOff() {
		return equals(Power.OFF);
	}

	@Override
	public final String toString() {
		return (isOn() ? "ON" : isOff() ? "OFF" : "?") + " (" + TypeUtil.toUnsignedString(mLevel) + ")";
	}

	@Override
	public final int hashCode() {
		return mLevel;
	}

	@Override
	public final boolean equals(final Object other) {
		return other instanceof Power && ((Power) other).getLevel() == getLevel();
	}

}
