package de.jeisfeld.lifx.os;

import de.jeisfeld.lifx.lan.LifxLan;

/**
 * A factory for a device registry.
 */
public final class DeviceRegistryFactory {
	/**
	 * Get a dummy device registry.
	 *
	 * @return null
	 */
	public static DeviceRegistryInterface getDeviceRegistry() {
		return LifxLan.getInstance();
	}

	/**
	 * Hide default constructor.
	 */
	private DeviceRegistryFactory() {
		// do nothing
	}
}
