package de.jeisfeld.lifx;

import java.util.Random;

import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;
import de.jeisfeld.lifx.os.Logger;

/**
 * Test class for testing LIFX API.
 */
public final class TestTile {
	// JAVADOC:OFF
	// SYSTEMOUT:OFF
	private static final String MAC_TILE_4 = "D0:73:D5:55:1B:DF";

	private static final TileChain TILE_4 = (TileChain) LifxLan.getInstance().getLightByMac(MAC_TILE_4);

	public static void main(final String[] args) throws Exception { // SUPPRESS_CHECKSTYLE
		Logger.setLogDetails(false);
		new TestTile().test3();
	}

	void test0() throws Exception {
		for (Device device : LifxLan.getInstance().getDevices()) {
			System.out.println(device.getFullInformation(TypeUtil.INDENT, true));
		}
	}

	void test1() throws Exception { // SUPPRESS_CHECKSTYLE
		double xCenter = (TILE_4.getTotalWidth() - 1) / 2.0;
		double yCenter = (TILE_4.getTotalHeight() - 1) / 2.0;
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			final int j = i;
			TILE_4.setColors(new TileChainColors() {
				/**
				 * The default serializable version id.
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Color getColor(final int x, final int y, final int width, final int height) {
					double distance = Math.sqrt((x - xCenter) * (x - xCenter) + (y - yCenter) * (y - yCenter));
					return new Color((int) (1024 * (5 * distance - j)), -1, 10000, 4000); // MAGIC_NUMBER
				}
			}, 0, false);
		}

	}

	void test2() throws Exception { // SUPPRESS_CHECKSTYLE
		TILE_4.setColors(new TileChainColors.InterpolatedCorners(Color.RED, Color.GREEN, Color.GREEN, Color.RED)
				.withRelativeBrightness(0.01), 0, false); // MAGIC_NUMBER

		// TILE_4.setEffect(new TileEffectInfo.Morph(10000, Color.RED, Color.WHITE));
	}

	void test3() throws Exception {
		TILE_4.animation(new TileChain.AnimationDefinition() {
			private final Color mCandleColor = Color.RED.withBrightness(0.15);
			private final int mCandleLeft = 6;
			private final int mCandleRight = 9;
			private final int mCandleBottom = 0;
			private final int mCandleTop = 11;
			private final int mFlameLeft = 7;
			private final int mFlameRight = 8;
			private final int mFlameTop = 15;
			private final int mMinTemp = 1000;
			private final int mMaxTemp = 3000;
			private final Random mRandom = new Random();

			@Override
			public int getDuration(final int n) {
				return 100 + mRandom.nextInt(2000); // MAGIC_NUMBER
			}

			@Override
			public TileChainColors getColors(final int n) {
				// Color baseColor = new Color(60., 0, mRandom.nextDouble() * 0.5 + 0.5, mRandom.nextInt(1500) + 2000);
				// Color baseColor = getRandomColor(mRandom, mMinTemp, mMaxTemp, 0.5, 1, 4);
				int baseTemperature = getRandomColorTemperature(mRandom, 2000, 3000); // MAGIC_NUMBER
				double baseBrightness = getRandomBrightness(mRandom, 0.6, 1, 3); // MAGIC_NUMBER

				int[] baseTemperatures = {baseTemperature - 200 + mRandom.nextInt(400), baseTemperature - 200 + mRandom.nextInt(400)}; // MAGIC_NUMBER
				double[] baseBrightnesses =
						{baseBrightness * (1 - 0.2 * mRandom.nextDouble()), baseBrightness * (1 - 0.2 * mRandom.nextDouble())}; // MAGIC_NUMBER

				return new TileChainColors() {
					@Override
					public Color getColor(final int x, final int y, final int width, final int height) {
						if (mCandleLeft <= x && x <= mCandleRight && mCandleBottom <= y && y <= mCandleTop) {
							return mCandleColor;
						}
						if (mFlameLeft <= x && x <= mFlameRight && mCandleTop < y && y <= mFlameTop) {
							int flameX = x - mFlameLeft;
							int flameY = y - mCandleTop - 1;
							return convertColorTemperature(baseTemperatures[flameX] - 300 * flameY, // MAGIC_NUMBER
									ensureInRange01(baseBrightnesses[flameX] - 0.15 * flameY)); // MAGIC_NUMBER
						}
						else {
							return Color.OFF;
						}
					}

				};
			}
		})
				.start();
	}

	/**
	 * Get a random color with saturation 0.
	 *
	 * @param random The random number generator.
	 * @param minTemperature The minimum color temperature.
	 * @param maxTemperature The maximum color temperature.
	 * @param minBrightness The minimum brightness.
	 * @param maxBrightness The maximum brightness.
	 * @param brightnessType The distribution type.
	 * @return the random color.
	 */
	private static Color getRandomColor(final Random random, final int minTemperature, final int maxTemperature, final double minBrightness,
			final double maxBrightness, final int brightnessType) {
//		return new Color(60., 0, getRandomBrightness(random, minBrightness, maxBrightness, brightnessType), // MAGIC_NUMBER
//				getRandomColorTemperature(random, minTemperature, maxTemperature));

		return convertColorTemperature(getRandomColorTemperature(random, minTemperature, maxTemperature),
				getRandomBrightness(random, minBrightness, maxBrightness, brightnessType));
	}

	/**
	 * Get a random value for the brightness.
	 *
	 * @param random The random number generator.
	 * @param minBrightness The minimum brightness.
	 * @param maxBrightness The maximum brightness.
	 * @param randomtype The distribution type.
	 * @return The random brightness.
	 */
	private static double getRandomBrightness(final Random random, final double minBrightness, final double maxBrightness, final int randomtype) {
		double randomvalue;
		switch (randomtype) {
		case 1: // centered
			randomvalue = Math.pow(random.nextDouble() - 0.5, 3) * 4 + 0.5; // MAGIC_NUMBER
			break;
		case 2: // lowered
			randomvalue = Math.pow(random.nextDouble(), 2);
			break;
		case 3: // slightly lowered // MAGIC_NUMBER
			randomvalue = (Math.pow(random.nextDouble() + 0.5, 2) - 0.25) / 2; // MAGIC_NUMBER
			break;
		case 4: // slightly centered // MAGIC_NUMBER
			randomvalue = Math.tan(2 * random.nextDouble() - 1) / (2 * Math.tan(1)) + 0.5; // MAGIC_NUMBER
			break;
		default:
			randomvalue = random.nextDouble();
			break;
		}
		return minBrightness + randomvalue * (maxBrightness - minBrightness);
	}

	/**
	 * Get a random color temperature.
	 *
	 * @param random The random number generator.
	 * @param minTemperature The minimum color temperature.
	 * @param maxTemperature The maximum color temperature.
	 * @return The random color temperature.
	 */
	private static int getRandomColorTemperature(final Random random, final int minTemperature, final int maxTemperature) {
		return (int) Math.exp(Math.log(minTemperature) + random.nextDouble() * (Math.log(maxTemperature) - Math.log(minTemperature)));
	}

	/**
	 * Convert color temperature into RGB color.
	 *
	 * @param temperature The color temperature in Kelvin.
	 * @param brightness The brighntess (between 0 and 1)
	 * @return The resulting RGB color.
	 */
	private static Color convertColorTemperature(final double temperature, final double brightness) {
		double red, green, blue;
		if (temperature < 6600) { // MAGIC_NUMBER
			red = 1;
			green = 0.39 * Math.log(temperature / 100) - 0.634; // MAGIC_NUMBER
			blue = 0.543 * Math.log(temperature / 100 - 10) - 1.186; // MAGIC_NUMBER
		}
		else {
			red = 1.269 * Math.pow(temperature / 100 - 60, -0.1332); // MAGIC_NUMBER
			green = 1.144 * Math.pow(temperature / 100 - 60, -0.0755); // MAGIC_NUMBER
			blue = 1;
		}
		red = ensureInRange01(red) * brightness;
		green = ensureInRange01(green) * brightness;
		blue = ensureInRange01(blue) * brightness;
		return new Color.RGBK(TypeUtil.toShort(red), TypeUtil.toShort(green), TypeUtil.toShort(blue), (short) 3000).toHsbk(); // MAGIC_NUMBER
	}

	/**
	 * Ensure that a double value is between 0 and 1.
	 *
	 * @param value The value
	 * @return The corresponding value between 0 and 1.
	 */
	private static double ensureInRange01(final double value) {
		if (value < 0) {
			return 0;
		}
		else if (value > 1) {
			return 1;
		}
		else {
			return value;
		}
	}

}
