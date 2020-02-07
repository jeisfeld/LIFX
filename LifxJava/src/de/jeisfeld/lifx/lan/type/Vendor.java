package de.jeisfeld.lifx.lan.type;

/**
 * Possible Vendor values.
 */
public enum Vendor {
	/**
	 * LIFX.
	 */
	LIFX,
	/**
	 * Unknown vendor.
	 */
	UNKNOWN;

	/**
	 * Get the service from its integer value.
	 *
	 * @param vendor the vendor integer value.
	 * @return The vendor.
	 */
	public static Vendor fromInt(final int vendor) {
		switch (vendor) {
		case 1:
			return LIFX;
		default:
			return UNKNOWN;
		}
	}

	/**
	 * Get the value of this vendor.
	 * @return The value.
	 */
	public int value() {
		return this == LIFX ? 1 : 0;
	}
}
