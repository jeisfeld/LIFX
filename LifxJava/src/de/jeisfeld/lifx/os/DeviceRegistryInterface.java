package de.jeisfeld.lifx.os;

import java.util.List;

import de.jeisfeld.lifx.lan.Device;

/**
 * An interface for a device registry.
 */
public interface DeviceRegistryInterface {
	/**
	 * Get the list of known devices.
	 *
	 * @param onlyFlagged Get only devices which are flagged.
	 * @return The list of known devices.
	 */
	List<Device> getDevices(boolean onlyFlagged);

	/**
	 * Get a known device by its storage id.
	 *
	 * @param id the storage id
	 * @return The device
	 */
	Device getDeviceById(int id);

	/**
	 * Get a known device by its mac.
	 *
	 * @param mac The mac.
	 * @return The device.
	 */
	Device getDeviceByMac(String mac);
}
