package de.jeisfeld.lifx.lan.type;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * A light color.
 */
public class Color {
	// JAVADOC:OFF
	public static final Color WHITE = new Color(50., 0, 1, Color.WHITE_TEMPERATURE);
	public static final Color WARM_WHITE = new Color(50., 0, 1, 3000);
	public static final Color COLD_WHITE = new Color(50., 0, 1, 8000);
	public static final Color RED = new Color(0., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color ORANGE = new Color(35., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color YELLOW = new Color(60., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color GRASS_GREEN = new Color(80., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color GREEN = new Color(120., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color BLUE_GREEN = new Color(155., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color CYAN = new Color(180., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color LIGHT_BLUE = new Color(210., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color BLUE = new Color(240., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color PURPLE = new Color(270., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color MAGENTA = new Color(300., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color PINK = new Color(325., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color GOLD = new Color(50., .5, 1, 2500);
	public static final Color SILVER = new Color(180., .1, 1, 9000);
	// JAVADOC:ON

	/**
	 * The color temperature used for basic white.
	 */
	private static final short WHITE_TEMPERATURE = 5000;
	/**
	 * The difference below which colors are considered similar.
	 */
	private static final short MIN_DIFFERENCE = 3;

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
	public Color(final int hue, final int saturation, final int brightness, final int colorTemperature) {
		mHue = (short) hue;
		mSaturation = (short) saturation;
		mBrightness = (short) brightness;
		mColorTemperature = (short) colorTemperature;
	}

	/**
	 * Create color from HSBK.
	 *
	 * @param hue The hue in range 0 - 360
	 * @param saturation The saturation in range 0 - 1
	 * @param brightness The brightness in range 0 - 1
	 * @param colorTemperature The color temperature.
	 */
	public Color(final double hue, final double saturation, final double brightness, final int colorTemperature) {
		this(Color.toShort(hue / 360), Color.toShort(saturation), Color.toShort(brightness), (short) colorTemperature); // MAGIC_NUMBER
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

	/**
	 * Convert double value 0 - 1 to short value.
	 *
	 * @param value the double value
	 * @return The short value
	 */
	public static short toShort(final double value) {
		return (short) (65535.99999 * Math.min(1, Math.max(0, value))); // MAGIC_NUMBER
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mBrightness;
		if (mBrightness != 0) {
			result = prime * result + mColorTemperature;
			result = prime * result + mSaturation;
			if (mSaturation != 0) {
				result = prime * result + mHue;
			}
		}
		return result;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof Color)) {
			return false;
		}
		Color other = (Color) obj;
		if (this == obj || (getBrightness() == 0 && other.getBrightness() == 0)) {
			return true;
		}
		return (getHue() == other.getHue() || getSaturation() == 0) && getSaturation() == other.getSaturation()
				&& getBrightness() == other.getBrightness() && getColorTemperature() == other.getColorTemperature();
	}

	/**
	 * Compare with other color.
	 *
	 * @param other the other color
	 * @return true if colors are basically the same.
	 */
	public final boolean isSimilar(final Color other) {
		if (getBrightness() == 0 && other.getBrightness() == 0) {
			return true;
		}
		return (Color.areSame(getHue(), other.getHue(), true) || Color.areSame(getSaturation(), (short) 0, false))
				&& Color.areSame(getSaturation(), other.getSaturation(), false) && isSimilarBlackWhite(other);
	}

	/**
	 * Compare with other color ignoring hue and saturation.
	 *
	 * @param other The other color.
	 * @return true if brightness and color temperature match.
	 */
	public final boolean isSimilarBlackWhite(final Color other) {
		return Color.areSame(getBrightness(), other.getBrightness(), false)
				&& Color.areSame(getColorTemperature(), other.getColorTemperature(), false);
	}

	/**
	 * Check if two color parameters should be considered as the same.
	 *
	 * @param a one parameter
	 * @param b the other parameter
	 * @param isHue indicator if this is hue
	 * @return true if the parameters should be considered as different
	 */
	private static boolean areSame(final short a, final short b, final boolean isHue) {
		int a1 = a - Short.MIN_VALUE;
		int b1 = b - Short.MIN_VALUE;
		boolean areSame = Math.abs(a1 - b1) < Color.MIN_DIFFERENCE || Math.abs(a1 - b1) > 65536 - Color.MIN_DIFFERENCE; // MAGIC_NUMBER
		if (isHue) {
			return areSame;
		}
		else {
			return areSame && !(a < 0 && b >= 0) && !(b < 0 && a >= 0); // BOOLEAN_EXPRESSION_COMPLEXITY
		}
	}

}
