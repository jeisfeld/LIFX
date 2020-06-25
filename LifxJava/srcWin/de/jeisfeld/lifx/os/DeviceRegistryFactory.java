package de.jeisfeld.lifx.os;

import java.util.List;

import de.jeisfeld.lifx.lan.Device;

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
		return new DeviceRegistryInterface() {

			@Override
			public List<Device> getDevices(final boolean onlyFlagged) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Device getDeviceByMac(final String mac) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Device getDeviceById(final int id) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	/**
	 * Hide default constructor.
	 */
	private DeviceRegistryFactory() {
		// do nothing
	}
}
