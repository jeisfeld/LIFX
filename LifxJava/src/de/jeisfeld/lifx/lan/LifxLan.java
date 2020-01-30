package de.jeisfeld.lifx.lan;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.LifxLanConnection.DeviceFilter;
import de.jeisfeld.lifx.lan.message.GetService;
import de.jeisfeld.lifx.lan.message.ResponseMessage;
import de.jeisfeld.lifx.lan.message.StateService;
import de.jeisfeld.lifx.lan.util.Logger;

/**
 * Handler for managing LIFX via LAN API.
 */
public final class LifxLan {
	/**
	 * The singleton instance.
	 */
	private static LifxLan mInstance = null;
	/**
	 * The sourceId.
	 */
	private final int mSourceId;
	/**
	 * The list of devices.
	 */
	private List<Device> mDevices = new ArrayList<>();
	/**
	 * The list of lights.
	 */
	private List<Light> mLights = new ArrayList<>();

	/**
	 * Get a LifxLan instance as singleton.
	 *
	 * @return A LifxLan instance.
	 */
	public static synchronized LifxLan getInstance() {
		if (LifxLan.mInstance == null) {
			LifxLan.mInstance = new LifxLan();
		}
		return LifxLan.mInstance;
	}

	/**
	 * Initialize the LIFX handler.
	 */
	private LifxLan() {
		mSourceId = LanCheck.getPid();
	}

	/**
	 * Get all devices in the LAN.
	 *
	 * @return The list of found devices.
	 * @throws SocketException Exception while getting information
	 */
	public List<Device> getDevices() throws SocketException {
		retrieveDeviceInformation();
		return mDevices;
	}

	/**
	 * Get all lights in the LAN.
	 *
	 * @return The list of found lights.
	 * @throws SocketException Exception while getting information
	 */
	public List<Light> getLights() throws SocketException {
		retrieveDeviceInformation();
		return mLights;
	}

	/**
	 * Get a light by a filter.
	 *
	 * @param filter the filter.
	 * @return the Light (if found)
	 */
	public Light getLightByFilter(final DeviceFilter filter) {
		for (Light light : mLights) {
			if (filter.matches(light)) {
				return light;
			}
		}
		DeviceFilter lightFilter = new DeviceFilter() {
			@Override
			public boolean matches(final Device device) {
				return device.getProduct().isLight() && filter.matches(device);
			}
		};
		try {
			List<Device> devices = retrieveDeviceInformation(5000, 2, 1, lightFilter); // MAGIC_NUMBER
			return devices.size() > 0 ? (Light) devices.get(0) : null;
		}
		catch (SocketException e) {
			return null;
		}
	}

	/**
	 * Get a light by its MAC.
	 *
	 * @param mac The mac.
	 * @return The light (if found).
	 */
	public Light getLightByMac(final String mac) {
		return getLightByFilter(device -> mac.equalsIgnoreCase(device.getTargetAddress()));
	}

	/**
	 * Get a light by its label.
	 *
	 * @param regex A regex for the label.
	 * @return The light (if found)
	 */
	public Light getLightByLabel(final String regex) {
		return getLightByFilter(device -> device.getLabel().matches(regex));
	}

	/**
	 * Get information about devices in the LAN.
	 *
	 * @param numDevices the number of devices after which the search is stopped.
	 * @throws SocketException Exception while getting information.
	 */
	private void retrieveDeviceInformation(final Integer numDevices) throws SocketException {
		retrieveDeviceInformation(2000, 1, numDevices, null); // MAGIC_NUMBER
	}

	/**
	 * Get information about devices in the LAN.
	 *
	 * @throws SocketException Exception while getting information.
	 */
	private void retrieveDeviceInformation() throws SocketException {
		retrieveDeviceInformation(null);
	}

	/**
	 * Get information about devices in the LAN.
	 *
	 * @param timeout the timeout
	 * @param attempts the number of attempts
	 * @param numDevices the number of devices after which the search is stopped
	 * @param filter a filter for responses
	 * @return the found devices.
	 * @throws SocketException Exception while getting information.
	 */
	private List<Device> retrieveDeviceInformation(final int timeout, final int attempts, final Integer numDevices, final DeviceFilter filter)
			throws SocketException {
		List<ResponseMessage> responses =
				new LifxLanConnection(mSourceId, timeout, attempts, filter).broadcastWithResponse(new GetService(), numDevices);
		Logger.info("Found " + responses.size() + " devices.");
		if (filter == null) {
			mDevices = new ArrayList<>();
			mLights = new ArrayList<>();
		}
		else {
			mDevices.removeIf(device -> filter.matches(device));
			mLights.removeIf(device -> filter.matches(device));
		}
		List<Device> foundDevices = new ArrayList<>();
		for (ResponseMessage response : responses) {
			Device device = ((StateService) response).getDevice().getDeviceProduct();
			foundDevices.add(device);
			mDevices.add(device);
			if (device instanceof Light) {
				mLights.add((Light) device);
			}
		}
		return foundDevices;
	}

}
