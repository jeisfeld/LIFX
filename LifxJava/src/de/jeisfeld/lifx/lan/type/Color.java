package de.jeisfeld.lifx.lan.type;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * A light color.
 */
public class Color {
	/**
	 * The hue.
	 */
	private final short mHue;
	/**
	 * The saturation.
	 */
	private final short mSaturation;
	/**
	 * The brightness.
	 */
	private final short mBrightness;
	/**
	 * The color temperature.
	 */
	private final short mColorTemperature;

	/**
	 * Create color from HSBK.
	 *
	 * @param hue The hue.
	 * @param saturation The saturation.
	 * @param brightness The brightness.
	 * @param colorTemperature The color temperature.
	 */
	public Color(final short hue, final short saturation, final short brightness, final short colorTemperature) {
		mHue = hue;
		mSaturation = saturation;
		mBrightness = brightness;
		mColorTemperature = colorTemperature;
	}

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder("Color(");
		result.append(TypeUtil.toUnsignedString(mHue))
				.append(",")
				.append(TypeUtil.toUnsignedString(mSaturation))
				.append(",")
				.append(TypeUtil.toUnsignedString(mBrightness))
				.append(",")
				.append(TypeUtil.toUnsignedString(mColorTemperature))
				.append(")");
		return result.toString();
	}

	/**
	 * Get the hue.
	 *
	 * @return the hue
	 */
	public final short getHue() {
		return mHue;
	}

	/**
	 * Get the saturation.
	 *
	 * @return the saturation
	 */
	public final short getSaturation() {
		return mSaturation;
	}

	/**
	 * Get the brightness.
	 *
	 * @return the brightness
	 */
	public final short getBrightness() {
		return mBrightness;
	}

	/**
	 * Get the color temperature in Kelvin.
	 *
	 * @return the color temperature in Kelvin
	 */
	public final short getColorTemperature() {
		return mColorTemperature;
	}

}
