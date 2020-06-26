package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.LifxLanConnection.DeviceFilter;
import de.jeisfeld.lifx.lan.LifxLanConnection.RetryPolicy;
import de.jeisfeld.lifx.lan.message.GetService;
import de.jeisfeld.lifx.lan.message.ResponseMessage;
import de.jeisfeld.lifx.lan.message.StateService;
import de.jeisfeld.lifx.os.DeviceRegistryInterface;
import de.jeisfeld.lifx.os.Logger;
import de.jeisfeld.lifx.os.OsTools;

/**
 * Handler for managing LIFX via LAN API.
 */
public final class LifxLan implements DeviceRegistryInterface {
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
		mSourceId = OsTools.getPid();
	}

	@Override
	public List<Device> getDevices() {
		try {
			return getDevices(mDevices == null || mDevices.isEmpty());
		}
		catch (IOException e) {
			return new ArrayList<>();
		}
	}

	@Override
	public Device getDeviceByMac(final String mac) {
		for (Device device : getDevices()) {
			if (device.getTargetAddress().equals(mac)) {
				return device;
			}
		}
		return null;
	}

	/**
	 * Get all devices.
	 *
	 * @param search flag indicating if devices should be newly searched or only already found devices should be shown.
	 * @return The list of found devices.
	 * @throws IOException Exception while getting information
	 */
	public List<Device> getDevices(final boolean search) throws IOException {
		if (search) {
			retrieveDeviceInformation();
		}
		return mDevices;
	}

	/**
	 * Get all lights in the LAN.
	 *
	 * @return The list of found lights.
	 * @throws IOException Exception while getting information
	 */
	public List<Light> getLights() throws IOException {
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
		DeviceFilter lightFilter = device -> device.getProduct().isLight() && filter.matches(device);
		try {
			List<Device> devices = retrieveDeviceInformation(new RetryPolicy() {
				@Override
				public int getTimeout(final int attempt) {
					return 5000; // MAGIC_NUMBER
				}
			}, lightFilter);
			return devices.size() > 0 ? (Light) devices.get(0) : null;
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Get all devices by a filter.
	 *
	 * @param filter the filter.
	 * @return the devices matching this filter.
	 */
	public List<Device> getDevicesByFilter(final DeviceFilter filter) {
		List<Device> resultList = new ArrayList<>();
		for (Device device : mDevices) {
			if (filter.matches(device)) {
				resultList.add(device);
			}
		}
		if (resultList.size() > 0) {
			return resultList;
		}
		else {
			try {
				retrieveDeviceInformation();
			}
			catch (IOException e) {
				Logger.error(e);
			}
			for (Device device : mDevices) {
				if (filter.matches(device)) {
					resultList.add(device);
				}
			}
			return resultList;
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
	 * @throws IOException Exception while getting information.
	 */
	public void retrieveDeviceInformation(final Integer numDevices) throws IOException {
		retrieveDeviceInformation(new RetryPolicy() {
			@Override
			public int getTimeout(final int attempt) {
				return 2500; // MAGIC_NUMBER
			}

			@Override
			public int getExpectedResponses() {
				return numDevices == null ? Integer.MAX_VALUE : numDevices;
			}

		}, null);
	}

	/**
	 * Get information about devices in the LAN.
	 *
	 * @throws IOException Exception while getting information.
	 */
	public void retrieveDeviceInformation() throws IOException {
		retrieveDeviceInformation(null);
	}

	/**
	 * Get information about devices in the LAN.
	 *
	 * @param retryPolicy the retry policy
	 * @param filter a filter for responses
	 * @return the found devices.
	 * @throws IOException Exception while getting information.
	 */
	public List<Device> retrieveDeviceInformation(final RetryPolicy retryPolicy, final DeviceFilter filter)
			throws IOException {
		List<ResponseMessage> responses = new LifxLanConnection(mSourceId, filter).broadcastWithResponse(new GetService(), retryPolicy);
		Logger.info("Found " + responses.size() + " devices.");
		if (filter == null) {
			mDevices = new ArrayList<>();
			mLights = new ArrayList<>();
		}
		else {
			mDevices.removeIf(filter::matches);
			mLights.removeIf(filter::matches);
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
