package de.jeisfeld.lifx.lan;

import java.net.SocketException;

import de.jeisfeld.lifx.lan.message.LightGetPower;
import de.jeisfeld.lifx.lan.message.LightStatePower;
import de.jeisfeld.lifx.lan.util.Logger;
import de.jeisfeld.lifx.lan.util.TypeUtil;

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
	public String getFullInformation() {
		StringBuilder result = new StringBuilder(super.getFullInformation());
		result.append(TypeUtil.INDENT).append("PowerLevel: ").append(TypeUtil.toUnsignedString(getPowerLevel())).append("\n");
		return result.toString();
	}

	@Override
	public final String toString() {
		return "Light: " + getTargetAddress() + ", " + getInetAddress().getHostAddress() + ":" + getPort();
	}

	/**
	 * Get the power level.
	 *
	 * @return the power level.
	 */
	public final Short getPowerLevel() {
		LightStatePower lightStatePower;
		try {
			lightStatePower = (LightStatePower) new LifxLanConnection(getSourceId(), (byte) 0, getInetAddress(), getPort())
					.requestWithResponse(new LightGetPower(getTargetAddress()));
			return lightStatePower.getLevel();
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}
}
