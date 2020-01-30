package de.jeisfeld.lifx.lan;

import java.net.SocketException;

import de.jeisfeld.lifx.lan.message.LightGet;
import de.jeisfeld.lifx.lan.message.LightGetInfrared;
import de.jeisfeld.lifx.lan.message.LightGetPower;
import de.jeisfeld.lifx.lan.message.LightSetPower;
import de.jeisfeld.lifx.lan.message.LightState;
import de.jeisfeld.lifx.lan.message.LightStateInfrared;
import de.jeisfeld.lifx.lan.message.LightStatePower;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
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
		if (getProduct().hasInfrared()) {
			result.append(TypeUtil.INDENT).append("Infrared Brightness: ").append(TypeUtil.toUnsignedString(getInfraredBrightness())).append("\n");
		}
		result.append(TypeUtil.INDENT).append("Color: ").append(getColor()).append("\n");
		return result.toString();
	}

	@Override
	public final String toString() {
		return "Light: " + getTargetAddress() + ", " + getInetAddress().getHostAddress() + ":" + getPort();
	}

	@Override
	public final Power getPower() {
		LightStatePower lightStatePower;
		try {
			lightStatePower = (LightStatePower) getConnection().requestWithResponse(new LightGetPower());
			return new Power(lightStatePower.getLevel());
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the light state.
	 *
	 * @return the light state.
	 */
	public final LightState getState() {
		LightState lightState;
		try {
			lightState = (LightState) getConnection().requestWithResponse(new LightGet());
			return lightState;
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the infrared brightness.
	 *
	 * @return the infrared brightness.
	 */
	public final Short getInfraredBrightness() {
		LightStateInfrared lightStateInfrared;
		try {
			lightStateInfrared = (LightStateInfrared) getConnection().requestWithResponse(new LightGetInfrared());
			return lightStateInfrared.getBrightness();
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the color.
	 *
	 * @return the color.
	 */
	public final Color getColor() {
		LightState lightState = getState();
		return lightState == null ? null : lightState.getColor();
	}

	/**
	 * Set the power.
	 *
	 * @param status true for switching on, false for switching off
	 * @param duration the duration of power change in millis.
	 * @throws SocketException Connection issues
	 */
	public void setPower(final boolean status, final int duration) throws SocketException {
		getConnection().requestWithResponse(new LightSetPower(status, duration));
	}
}
