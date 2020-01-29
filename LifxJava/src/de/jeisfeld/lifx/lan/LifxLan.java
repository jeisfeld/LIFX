package de.jeisfeld.lifx.lan;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.message.GetService;
import de.jeisfeld.lifx.lan.message.ResponseMessage;
import de.jeisfeld.lifx.lan.message.StateService;
import de.jeisfeld.lifx.lan.util.Logger;

/**
 * Handler for managing LIFX via LAN API.
 */
public class LifxLan {
	/**
	 * The number of devices.
	 */
	private final Integer mNumDevices;
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
	private final List<Light> mLights = new ArrayList<>();

	/**
	 * Initialize the LIFX handler.
	 */
	public LifxLan() {
		mNumDevices = null;
		mSourceId = LanCheck.getPid();
	}

	/**
	 * Initialize the LIFX handler.
	 *
	 * @param numDevices The number of devices.
	 */
	public LifxLan(final int numDevices) {
		mNumDevices = numDevices;
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
	 * Get information about devices in the LAN.
	 *
	 * @throws SocketException Exception while getting information.
	 */
	private void retrieveDeviceInformation() throws SocketException {
		List<ResponseMessage> responses =
				new LifxLanConnection(mSourceId, (byte) 0, mNumDevices).broadcastWithResponse(new GetService());
		Logger.info("Found " + responses.size() + " devices.");
		mDevices = new ArrayList<>();
		for (ResponseMessage response : responses) {
			Device device = ((StateService) response).getDevice();
			device = device.getDeviceProduct();
			mDevices.add(device);
			if (device instanceof Light) {
				mLights.add((Light) device);
			}
		}
	}

}
