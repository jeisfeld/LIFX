package de.jeisfeld.lifx.lan;

import java.net.SocketException;

/**
 * Class managing a LIFX light.
 */
public class Light extends Device {
	/**
	 * Constructor.
	 *
	 * @param device The device which is a light.
	 */
	public Light(final Device device) {
		super(device.getTargetAddress(), device.getInetAddress(), device.getPort(), device.getSourceId());
		setVersionInformation(device.getVendor(), device.getProduct(), device.getVersion());
	}

	// OVERRIDABLE
	@Override
	public void retrieveInformation() throws SocketException {
		super.retrieveInformation();
		// LightGetPower
		// LightGet
	}

	// OVERRIDABLE
	@Override
	public String getFullInformation() {
		StringBuilder result = new StringBuilder(super.getFullInformation());
		return result.toString();
	}

	@Override
	public final String toString() {
		return "Light: " + getTargetAddress() + ", " + getInetAddress().getHostAddress() + ":" + getPort();
	}
}
