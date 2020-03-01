package de.jeisfeld.lifx.app.util;

import java.util.List;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Utility methods for handling colors.
 */
public final class ColorUtil {
	/**
	 * Divisor for transform from short to byte.
	 */
	private static final int SHORT_TO_BYTE_FACTOR = 256;

	/**
	 * Hide default constructor.
	 */
	private ColorUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the Android color to displayed on the button for a certain LIFX color.
	 *
	 * @param color The LIFX color.
	 * @return The color to be displayed.
	 */
	public static int toAndroidDisplayColor(final Color color) {
		Color displayColor = color;
		if (color.getSaturation() != -1) {
			Color temperatureColor = convertColorTemperature(color);
			displayColor = temperatureColor.add(displayColor, TypeUtil.toDouble(color.getSaturation()));
		}
		if (color.getBrightness() != 0) {
			displayColor = displayColor.withBrightness(TypeUtil.toDouble(color.getBrightness()) * 0.7 + 0.3); // MAGIC_NUMBER
		}
		return getAndroidColor(displayColor);
	}

	/**
	 * Get the Android colors to be displayed on a button for a certain list of LIFX colors.
	 *
	 * @param colors The LIFX colors.
	 * @return The colors to be displayed.
	 */
	public static int[] toAndroidDisplayColors(final List<Color> colors) {
		int[] result = new int[colors.size()];
		for (int i = 0; i < colors.size(); i++) {
			result[i] = toAndroidDisplayColor(colors.get(i));
		}
		return result;
	}

	/**
	 * Convert color temperature into RGB color.
	 *
	 * @param color The input color.
	 * @return The corresponding color as RGB, without relateion to color temperature.
	 */
	private static Color convertColorTemperature(final Color color) {
		double red, green, blue;
		int temperature = TypeUtil.toUnsignedInt(color.getColorTemperature());
		if (temperature < 6600) { // MAGIC_NUMBER
			red = 1;
			green = 0.39 * Math.log(temperature / 100.0) - 0.634; // MAGIC_NUMBER
			blue = 0.543 * Math.log(temperature / 100.0 - 10) - 1.186; // MAGIC_NUMBER
		}
		else {
			red = 1.269 * Math.pow(temperature / 100.0 - 60, -0.1332); // MAGIC_NUMBER
			green = 1.144 * Math.pow(temperature / 100.0 - 60, -0.0755); // MAGIC_NUMBER
			blue = 1;
		}
		return convertAndroidColorToColor(
				((int) (red * 255.0f + 0.5f) << 16) | ((int) (green * 255.0f + 0.5f) << 8) | (int) (blue * 255.0f + 0.5f), // MAGIC_NUMBER
				(short) 4000, true).withBrightness(color.getBrightness()); // MAGIC_NUMBER
	}

	/**
	 * Convert a color to Android format.
	 *
	 * @param color The Android color.
	 * @return The color as int.
	 */
	public static Integer getAndroidColor(final Color color) {
		Color.RGBK rgbk = color.toRgbk();
		return android.graphics.Color.rgb(
				TypeUtil.toUnsignedInt(rgbk.getRed()) / SHORT_TO_BYTE_FACTOR,
				TypeUtil.toUnsignedInt(rgbk.getGreen()) / SHORT_TO_BYTE_FACTOR,
				TypeUtil.toUnsignedInt(rgbk.getBlue()) / SHORT_TO_BYTE_FACTOR);
	}

	/**
	 * Convert Android color to custom color.
	 *
	 * @param color The Android color.
	 * @param colorTemperature The color temperature.
	 * @param allowZeroBrightness Flag indicating if zero brightness is allowed.
	 * @return The custom color.
	 */
	public static Color convertAndroidColorToColor(final int color, final short colorTemperature, final boolean allowZeroBrightness) {
		float[] hsv = new float[3]; // MAGIC_NUMBER
		android.graphics.Color.colorToHSV(color, hsv);
		// Use alpha as color temperature
		double brightness = !allowZeroBrightness && hsv[2] == 0 ? 1 / 65535.0 : hsv[2]; // MAGIC_NUMBER
		return new Color(hsv[0], hsv[1], brightness, colorTemperature);
	}
}
