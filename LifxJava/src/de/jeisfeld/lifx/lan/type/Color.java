package de.jeisfeld.lifx.lan.type;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * A light color.
 */
public class Color {
	// JAVADOC:OFF
	public static final Color WHITE = new Color(Color.WHITE_HUE_D, 0, 1, Color.WHITE_TEMPERATURE);
	public static final Color WARM_WHITE = new Color(Color.WHITE_HUE_D, 0, 1, 3000);
	public static final Color COLD_WHITE = new Color(Color.WHITE_HUE_D, 0, 1, 8000);
	public static final Color RED = new Color(0., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color ORANGE = new Color(35., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color YELLOW = new Color(60., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color GRASS_GREEN = new Color(80., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color GREEN = new Color(120., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color BLUE_GREEN = new Color(155., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color CYAN = new Color(180., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color DEEP_SKY_BLUE = new Color(210., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color BLUE = new Color(240., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color PURPLE = new Color(270., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color MAGENTA = new Color(300., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color PINK = new Color(325., 1, 1, Color.WHITE_TEMPERATURE);
	public static final Color GOLD = new Color(Color.WHITE_HUE_D, .5, 1, 2500);
	public static final Color SILVER = new Color(180., .1, 1, 9000);
	public static final Color LIGHT_BLUE = new Color(240., .5, 1, 9000);
	public static final Color DARKEST_WHITE = new Color(60., 1, 0.002, 2500);
	/**
	 * The color used to switch an animation finally off.
	 */
	public static final Color OFF = new Color(Color.WHITE_HUE_D, 0, 0, Color.WHITE_TEMPERATURE);

	/**
	 * Cycle through the colors, compensating the rather dark appearance of blue by darkening the other colors.
	 */
	public static final Color[] CYCLE_RAINBOW_LOW =
			{Color.RED.withBrightness(0.4), Color.YELLOW.withBrightness(0.3), Color.GREEN.withBrightness(0.4),
					Color.CYAN.withBrightness(0.4), Color.BLUE, Color.MAGENTA.withBrightness(0.3)};
	/**
	 * Cycle through the colors, compensating the rather dark appearance of blue by whitening the blue.
	 */
	public static final Color[] CYCLE_RAINBOW_HIGH =
			{Color.RED, Color.YELLOW.withBrightness(0.8), Color.GREEN, Color.CYAN, Color.LIGHT_BLUE, Color.MAGENTA.withBrightness(0.8)};

	public static final Color[] CYCLE_WAKEUP =
			{new Color(50., 1, 0.002, 2000), new Color(50., 0.5, 0.03, 2500), new Color(50., 0.3, 0.1, 3000), new Color(50., 0.1, 0.3, 4000),
					new Color(50., 0.01, 1, 6000)};

	// JAVADOC:ON

	/**
	 * The color temperature used for neutral white.
	 */
	public static final short WHITE_TEMPERATURE = 4000;
	/**
	 * The hue used for white as short.
	 */
	private static final short WHITE_HUE_S = 9100;
	/**
	 * The hue used for white as double.
	 */
	private static final double WHITE_HUE_D = Color.WHITE_HUE_S / 65536.0 * 360.0;

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
		this(TypeUtil.toShort(hue / 360), TypeUtil.toShort(saturation), TypeUtil.toShort(brightness), (short) colorTemperature); // MAGIC_NUMBER
	}

	@Override
	public final String toString() {
		return "Color(" + TypeUtil.toUnsignedString(mHue)
				+ ","
				+ TypeUtil.toUnsignedString(mSaturation)
				+ ","
				+ TypeUtil.toUnsignedString(mBrightness)
				+ ","
				+ TypeUtil.toUnsignedString(mColorTemperature)
				+ ")";
	}

	/**
	 * Update the color with given brightness.
	 *
	 * @param brightness The brightness.
	 * @return The updated color with this brightness.
	 */
	public final Color withBrightness(final double brightness) {
		return new Color(getHue(), getSaturation(), TypeUtil.toShort(brightness), getColorTemperature());
	}

	/**
	 * Update the color with given relative brightness.
	 *
	 * @param brightnessFactor The brightness factor (1 meaning unchanged)
	 * @return The updated color with updated brightness.
	 */
	public final Color withRelativeBrightness(final double brightnessFactor) {
		return new Color(getHue(), getSaturation(), TypeUtil.toShort(brightnessFactor * TypeUtil.toDouble(getBrightness())), getColorTemperature());
	}

	/**
	 * Update the color with given saturation.
	 *
	 * @param saturation The saturation.
	 * @return The updated color with this saturation.
	 */
	public final Color withSaturation(final double saturation) {
		return new Color(getHue(), TypeUtil.toShort(saturation), getBrightness(), getColorTemperature());
	}

	/**
	 * Mix with another color.
	 *
	 * @param other The other color.
	 * @param quota The quota of the other color (between 0 and 1)
	 * @return The mixed color.
	 */
	public final Color add(final Color other, final double quota) {
		double q = Math.min(1, Math.max(0, quota));
		int h1 = TypeUtil.toUnsignedInt(getHue());
		int h2 = TypeUtil.toUnsignedInt(other.getHue());
		if (Math.abs(h1 - h2) > 32768) { // MAGIC_NUMBER
			if (h2 > h1) {
				h2 -= 65536; // MAGIC_NUMBER
			}
			else {
				h2 += 65536; // MAGIC_NUMBER
			}
		}

		return new Color((short) (h2 * q + h1 * (1 - q)),
				(short) (TypeUtil.toUnsignedInt(other.getSaturation()) * q + TypeUtil.toUnsignedInt(getSaturation()) * (1 - q)),
				(short) (TypeUtil.toUnsignedInt(other.getBrightness()) * q + TypeUtil.toUnsignedInt(getBrightness()) * (1 - q)),
				(short) (TypeUtil.toUnsignedInt(other.getColorTemperature()) * q + TypeUtil.toUnsignedInt(getColorTemperature()) * (1 - q)));
	}

	/**
	 * Mix with another color in RGB mode.
	 *
	 * @param other The other color.
	 * @param quota The quota of the other color (between 0 and 1)
	 * @return The mixed color.
	 */
	public final Color addRgb(final Color other, final double quota) {
		return toRgbk().add(other.toRgbk(), quota).toHsbk();
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
		if (!(obj instanceof Color)) {
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

	/**
	 * Convert to RGBK color.
	 *
	 * @return The color as RGBK.
	 */
	public RGBK toRgbk() {
		double h = 6.0 * TypeUtil.toUnsignedInt(mHue) / 65536; // MAGIC_NUMBER hue from 0 to 6
		double s = TypeUtil.toDouble(mSaturation);
		double v = TypeUtil.toDouble(mBrightness);

		double c = v * s;
		double x = c * (1 - Math.abs(h % 2 - 1));
		double m = v - c;

		double r;
		double g;
		double b;

		switch ((int) h) {
		case 0:
			r = c;
			g = x;
			b = 0;
			break;
		case 1:
			r = x;
			g = c;
			b = 0;
			break;
		case 2:
			r = 0;
			g = c;
			b = x;
			break;
		case 3: // MAGIC_NUMBER
			r = 0;
			g = x;
			b = c;
			break;
		case 4: // MAGIC_NUMBER
			r = x;
			g = 0;
			b = c;
			break;
		case 5: // MAGIC_NUMBER
			r = c;
			g = 0;
			b = x;
			break;
		default:
			r = 0;
			g = 0;
			b = 0;
		}

		return new RGBK(TypeUtil.toShort(r + m), TypeUtil.toShort(g + m), TypeUtil.toShort(b + m), mColorTemperature);
	}

	/**
	 * A color in RGB plus Kelvin format.
	 */
	public static class RGBK { // SUPPRESS_CHECKSTYLE
		/**
		 * The red part.
		 */
		private final short mRed;
		/**
		 * The green part.
		 */
		private final short mGreen;
		/**
		 * The blue part.
		 */
		private final short mBlue;
		/**
		 * The color temperature.
		 */
		private final short mColorTemperature;

		/**
		 * Create the RGBK color.
		 *
		 * @param red the red part
		 * @param green the green part
		 * @param blue ghe blue part
		 * @param colorTemperature the color temperature in Kelvin.
		 */
		public RGBK(final short red, final short green, final short blue, final short colorTemperature) {
			mRed = red;
			mGreen = green;
			mBlue = blue;
			mColorTemperature = colorTemperature;
		}

		/**
		 * Convert a color from RGBK to HSBK.
		 *
		 * @return The Color as HSBK.
		 */
		public Color toHsbk() {
			int r = TypeUtil.toUnsignedInt(mRed);
			int g = TypeUtil.toUnsignedInt(mGreen);
			int b = TypeUtil.toUnsignedInt(mBlue);
			int maxrgb = Math.max(r, Math.max(g, b));
			int minrgb = Math.min(r, Math.min(g, b));
			double span = maxrgb - minrgb;

			if (span == 0) {
				return new Color(Color.WHITE_HUE_S, 0, (short) maxrgb, mColorTemperature);
			}
			else {
				int saturation = (int) (span * 65535 / maxrgb); // MAGIC_NUMBER

				double hue;
				if (maxrgb == r) {
					hue = ((g - b) / span) % 6; // MAGIC_NUMBER
				}
				else if (maxrgb == g) {
					hue = (b - r) / span + 2;
				}
				else {
					hue = (r - g) / span + 4; // MAGIC_NUMBER
				}

				hue = hue / 6 * 65536; // MAGIC_NUMBER
				return new Color((short) hue, (short) saturation, (short) maxrgb, mColorTemperature);
			}
		}

		/**
		 * Get the red part.
		 *
		 * @return the red part
		 */
		public final short getRed() {
			return mRed;
		}

		/**
		 * Get the green part.
		 *
		 * @return the green part
		 */
		public final short getGreen() {
			return mGreen;
		}

		/**
		 * Get the blue part.
		 *
		 * @return the blue part
		 */
		public final short getBlue() {
			return mBlue;
		}

		/**
		 * Get the color temperature in Kelvin.
		 *
		 * @return the color temperature in Kelvin
		 */
		public final short getColorTemperature() {
			return mColorTemperature;
		}

		@Override
		public final String toString() {
			return "Color(" + TypeUtil.toUnsignedString(mRed)
					+ ","
					+ TypeUtil.toUnsignedString(mGreen)
					+ ","
					+ TypeUtil.toUnsignedString(mBlue)
					+ ","
					+ TypeUtil.toUnsignedString(mColorTemperature)
					+ ")";
		}

		/**
		 * Mix with another color.
		 *
		 * @param other The other color.
		 * @param quota The quota of the other color (between 0 and 1)
		 * @return The mixed color.
		 */
		public final RGBK add(final RGBK other, final double quota) {
			double q = Math.min(1, Math.max(0, quota));
			return new RGBK((short) (TypeUtil.toUnsignedInt(other.getRed()) * q + TypeUtil.toUnsignedInt(getRed()) * (1 - q)),
					(short) (TypeUtil.toUnsignedInt(other.getGreen()) * q + TypeUtil.toUnsignedInt(getGreen()) * (1 - q)),
					(short) (TypeUtil.toUnsignedInt(other.getBlue()) * q + TypeUtil.toUnsignedInt(getBlue()) * (1 - q)),
					(short) (TypeUtil.toUnsignedInt(other.getColorTemperature()) * q + TypeUtil.toUnsignedInt(getColorTemperature()) * (1 - q)));
		}
	}

}
