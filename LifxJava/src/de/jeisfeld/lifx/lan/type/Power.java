package de.jeisfeld.lifx.lan.type;

/**
 * Possible power values.
 */
public enum Power {
	/**
	 * Power is on.
	 */
	ON,
	/**
	 * Power is off.
	 */
	OFF,
	/**
	 * Power is unknown.
	 */
	UNKNOWN;

	/**
	 * Get the power value from level.
	 *
	 * @param level The power level.
	 * @return The power value.
	 */
	public static Power fromLevel(final short level) {
		switch (level) {
		case -1:
			return ON;
		case 0:
			return OFF;
		default:
			return UNKNOWN;
		}
	}
}
