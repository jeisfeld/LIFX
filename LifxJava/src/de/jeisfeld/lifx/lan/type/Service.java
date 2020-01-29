package de.jeisfeld.lifx.lan.type;

/**
 * Possible service values.
 */
public enum Service {
	/**
	 * UDP Service.
	 */
	UDP,
	/**
	 * Unknown Service.
	 */
	UNKNOWN;

	/**
	 * Get the service from its integer value.
	 *
	 * @param service the service integer value.
	 * @return The service.
	 */
	public static Service fromByte(final byte service) {
		switch (service) {
		case 1:
			return UDP;
		default:
			return UNKNOWN;
		}
	}
}
