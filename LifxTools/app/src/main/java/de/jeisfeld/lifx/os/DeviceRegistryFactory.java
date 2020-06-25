package de.jeisfeld.lifx.os;

import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;

/**
 * A factory for a device registry.
 */
public final class DeviceRegistryFactory {
	/**
	 * Get a device registry instance.
	 *
	 * @return null
	 */
	public static DeviceRegistryInterface getDeviceRegistry() {
		return DeviceRegistry.getInstance();
	}

	/**
	 * Hide default constructor.
	 */
	private DeviceRegistryFactory() {
		// do nothing
	}
}
