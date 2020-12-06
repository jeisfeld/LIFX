package de.jeisfeld.lifx;

import java.util.Random;

import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * An animation of a candle on a tile chain.
 */
public final class CandleAnimation implements TileChain.AnimationDefinition {
	/**
	 * The candle color.
	 */
	private static final Color CANDLE_COLOR = Color.RED.withBrightness(0.15);
	/**
	 * The random number generator.
	 */
	private static final Random RANDOM = new Random();
	/**
	 * The MAC of the tile chain used.
	 */
	private static final String MAC_TILE_4 = "D0:73:D5:55:1B:DF";
	/**
	 * The tile chain used.
	 */
	private static final TileChain TILE_4 = (TileChain) LifxLan.getInstance().getLightByMac(MAC_TILE_4);
	/**
	 * The number of candles.
	 */
	private final int mCandleCount;
	/**
	 * The burndown of the candles.
	 */
	private final int mCandleBurndown;
	/**
	 * A global brightness factor.
	 */
	private final double mBrightnessFactor;
	/**
	 * A flag indicating the background.
	 */
	private final int mBackground;
	/**
	 * The background colors.
	 */
	private final TileChainColors mBackgroundColors;
	/**
	 * The flame color generators.
	 */
	private final FlameColorGenerator[] mFlameColorGenerators;
	/**
	 * The flame width.
	 */
	private final int mFlameWidth;
	/**
	 * The flame height.
	 */
	private final int mFlameHeight;
	/**
	 * The left positions of the flames.
	 */
	private final int[] mFlameLeft;
	/**
	 * The bottom positions of the flames.
	 */
	private final int[] mFlameBottom;
	/**
	 * The candle width.
	 */
	private final int mCandleWidth;
	/**
	 * The candle heights.
	 */
	private final int[] mCandleHeight;
	/**
	 * The left positions of the candles.
	 */
	private final int[] mCandleLeft;
	/**
	 * The bottom positions of the candles.
	 */
	private final int[] mCandleBottom;
	/**
	 * The current step.
	 */
	private int mCurrentStep = 0;
	/**
	 * Flag indicating if this is the first step.
	 */
	private boolean mIsFirstStep = true;
	/**
	 * The current duration.
	 */
	private int mCurrentDuration = 0;

	/**
	 * The main class to run the animation.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(final String[] args) throws Exception { // SUPPRESS_CHECKSTYLE
		int candleCount = 1;
		int burndown = 1;
		double brightnessFactor = 1;
		int background = 0;

		if (args.length > 0) {
			candleCount = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			burndown = Integer.parseInt(args[1]);
		}
		if (args.length > 2) {
			brightnessFactor = Double.parseDouble(args[2]);
		}
		if (args.length > 3) { // MAGIC_NUMBER
			background = Integer.parseInt(args[3]); // MAGIC_NUMBER
		}

		TILE_4.animation(new CandleAnimation(candleCount, burndown, brightnessFactor, background)).start();
	}

	/**
	 * Constructor for the animation.
	 *
	 * @param candleCount The number of candles.
	 * @param candleBurndown The burndown of the candles.
	 * @param brightnessFactor An overall brightness factor.
	 * @param background The background mode.
	 */
	private CandleAnimation(final int candleCount, final int candleBurndown, final double brightnessFactor, final int background) {
		mCandleCount = candleCount;
		mCandleBurndown = candleBurndown;
		mBrightnessFactor = brightnessFactor;
		mBackground = background;

		switch (candleCount) {
		case 2:
			mFlameWidth = 2;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 4; // MAGIC_NUMBER
			mCandleHeight = mBackground == 3 ? new int[] {9 - mCandleBurndown, 11 - mCandleBurndown} // MAGIC_NUMBER
					: new int[] {10 - mCandleBurndown, 12 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {2, 10}; // MAGIC_NUMBER
			mCandleBottom = mBackground == 3 ? new int[] {1, 1} : new int[] {0, 0}; // MAGIC_NUMBER
			break;
		case 3: // MAGIC_NUMBER
			mFlameWidth = 2;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 4; // MAGIC_NUMBER
			mCandleHeight = mBackground == 3 ? new int[] {7 - mCandleBurndown, 11 - mCandleBurndown, 9 - mCandleBurndown} // MAGIC_NUMBER
					: new int[] {8 - mCandleBurndown, 12 - mCandleBurndown, 10 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {0, 6, 12}; // MAGIC_NUMBER
			mCandleBottom = mBackground == 3 ? new int[] {2, 1, 2} : new int[] {0, 0, 0}; // MAGIC_NUMBER
			break;
		case 4: // MAGIC_NUMBER
			mFlameWidth = 1;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 3; // MAGIC_NUMBER
			mCandleHeight = mBackground == 3 // MAGIC_NUMBER
					? new int[] {5 - mCandleBurndown, 11 - mCandleBurndown, 7 - mCandleBurndown, 9 - mCandleBurndown} // MAGIC_NUMBER
					: new int[] {6 - mCandleBurndown, 12 - mCandleBurndown, 10 - mCandleBurndown, 8 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {0, 4, 9, 13}; // MAGIC_NUMBER
			mCandleBottom = mBackground == 3 ? new int[] {2, 1, 4, 2} : new int[] {0, 0, 0, 0}; // MAGIC_NUMBER
			break;
		case 1:
		default:
			mFlameWidth = 2;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 4; // MAGIC_NUMBER
			mCandleHeight = mBackground == 3 ? new int[] {11 - mCandleBurndown} : new int[] {12 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {6}; // MAGIC_NUMBER
			mCandleBottom = mBackground == 3 ? new int[] {1} : new int[] {0}; // MAGIC_NUMBER
			break;
		}

		switch (mBackground) {
		case 1:
			mBackgroundColors = new TileChainColors.Fixed(Color.WHITE.withBrightness(0.01)); // MAGIC_NUMBER
			break;
		case 2:
			mBackgroundColors = TILE_4.getColors();
			break;
		case 3: // MAGIC_NUMBER
			mBackgroundColors = new CrestColors();
			break;
		case 0:
		case 4: // MAGIC_NUMBER
		default:
			mBackgroundColors = TileChainColors.OFF;
		}

		int flameOffset = (mCandleWidth - mFlameWidth) / 2;
		mFlameLeft = new int[mCandleCount];
		mFlameBottom = new int[mCandleCount];
		mFlameColorGenerators = new FlameColorGenerator[mCandleCount];
		for (int i = 0; i < mCandleCount; i++) {
			mFlameLeft[i] = mCandleLeft[i] + flameOffset;
			mFlameBottom[i] = mCandleBottom[i] + mCandleHeight[i];

			Color[][] backgroundColors = new Color[mFlameWidth][mFlameHeight];
			for (int x = 0; x < mFlameWidth; x++) {
				for (int y = 0; y < mFlameHeight; y++) {
					backgroundColors[x][y] = getBackgroundColor(mFlameLeft[i] + x, mFlameBottom[i] + y,
							TILE_4.getTotalWidth(), TILE_4.getTotalHeight());
				}
			}
			mFlameColorGenerators[i] = new FlameColorGenerator(mFlameWidth, mFlameHeight, backgroundColors);
		}
	}

	@Override
	public int getDuration(final int n) {
		if (mIsFirstStep) {
			mCurrentStep = n;
			mIsFirstStep = false;
			mCurrentDuration = 0;
		}
		if (n > mCurrentStep) {
			int previousDuration = getDuration(mCurrentStep);
			for (FlameColorGenerator flameColorGenerator : mFlameColorGenerators) {
				flameColorGenerator.addTime(previousDuration);
			}
			mCurrentStep = n;
			mCurrentDuration = 0;
		}
		if (mCurrentDuration == 0) {
			int newDuration = Integer.MAX_VALUE;
			for (FlameColorGenerator flameColorGenerator : mFlameColorGenerators) {
				newDuration = Math.min(newDuration, flameColorGenerator.getCurrentDuration());
			}
			newDuration = Math.max(newDuration, 20); // MAGIC_NUMBER minimum duration
			mCurrentDuration = newDuration;
		}
		return mCurrentDuration;
	}

	@Override
	public TileChainColors getColors(final int n) {
		if (mIsFirstStep || n > mCurrentStep) {
			getDuration(n);
		}

		return new TileChainColors() {
			/**
			 * The default serial version UID.
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Color getColor(final int x, final int y, final int width, final int height) {
				Color foregroundColor = getForegroundColor(x, y, width, height);
				if (foregroundColor != null) {
					return foregroundColor;
				}
				for (int i = 0; i < mCandleCount; i++) {
					if (mCandleLeft[i] <= x && x < mCandleLeft[i] + mCandleWidth
							&& mCandleBottom[i] <= y && y < mCandleBottom[i] + mCandleHeight[i]) {
						return CANDLE_COLOR.withRelativeBrightness(mBrightnessFactor);
					}
					if (mFlameLeft[i] <= x && x < mFlameLeft[i] + mFlameWidth
							&& mFlameBottom[i] <= y && y < mFlameBottom[i] + mFlameHeight) {
						return mFlameColorGenerators[i].getNextColor(mCurrentDuration, x - mFlameLeft[i], y - mFlameBottom[i])
								.withRelativeBrightness(mBrightnessFactor);
					}
				}
				return getBackgroundColor(x, y, width, height);
			}

		};
	}

	/**
	 * A foreground color (in front of the candles). Null in case that candles are in foreground.
	 *
	 * @param x The x coordinate
	 * @param y The y coordinate
	 * @param width The width
	 * @param height The height
	 * @return The foreground color.
	 */
	private Color getForegroundColor(final int x, final int y, final int width, final int height) {
		if (mBackground == 3) { // MAGIC_NUMBER
			if (y == 0) {
				return getBackgroundColor(x, y, width, height);
			}
			else if (y == 1 && mCandleCount == 2 && (x == 2 || x == 13)) { // MAGIC_NUMBER
				return getBackgroundColor(x, y, width, height).add(CANDLE_COLOR, 0.5); // MAGIC_NUMBER
			}
			else if (y == 2 && mCandleCount == 3 && (x == 0 || x == 2 || x == 13 || x == 15)) { // SUPPRESS_CHECKSTYLE
				return getBackgroundColor(x, y, width, height).add(CANDLE_COLOR, 0.5); // MAGIC_NUMBER
			}
			else {
				return null;
			}
		}
		else if (mBackground == 4) { // MAGIC_NUMBER
			return x >= width / 2 && y < height / 2 ? Color.WHITE : null;
		}
		else {
			return null;
		}
	}

	/**
	 * A background color (behind the candles).
	 *
	 * @param x The x coordinate
	 * @param y The y coordinate
	 * @param width The width
	 * @param height The height
	 * @return The background color.
	 */
	private Color getBackgroundColor(final int x, final int y, final int width, final int height) {
		return mBackgroundColors.getColor(x, y, width, height);
	}

	/**
	 * Class for generating flame colors.
	 */
	private static final class FlameColorGenerator {
		/**
		 * The width of the flame.
		 */
		private final int mWidth;
		/**
		 * The height of the flame.
		 */
		private final int mHeight;
		/**
		 * The current duration.
		 */
		private int mCurrentDuration;
		/**
		 * The current target colors.
		 */
		private final Color[][] mTargetColors;
		/**
		 * The current colors.
		 */
		private final Color[][] mCurrentColors;
		/**
		 * The background colors.
		 */
		private final Color[][] mBackground;

		/**
		 * Create a flame color generator.
		 *
		 * @param width The width of the flame.
		 * @param height The height of the flame.
		 * @param backgroundColors The background colors behind the flame.
		 */
		private FlameColorGenerator(final int width, final int height, final Color[][] backgroundColors) {
			mWidth = width;
			mHeight = height;
			mCurrentColors = new Color[width][height];
			mTargetColors = new Color[width][height];
			mBackground = backgroundColors;
			setNewTarget();
			addTime(mCurrentDuration); // move target to current and set new target
		}

		/**
		 * Get the duration of the current color transition.
		 *
		 * @return The duration of the current color transition.
		 */
		private int getCurrentDuration() {
			return mCurrentDuration;
		}

		/**
		 * Get the next color at a certain time at a certain position.
		 *
		 * @param time the target time.
		 * @param x the x position.
		 * @param y the y position.
		 * @return The colors at this time.
		 */
		private Color getNextColor(final int time, final int x, final int y) {
			if (time >= mCurrentDuration) {
				return mTargetColors[x][y];
			}
			return mCurrentColors[x][y].add(mTargetColors[x][y], (double) time / mCurrentDuration);
		}

		/**
		 * Set new target colors and target duration.
		 */
		private void setNewTarget() {
			int baseTemperature = getRandomColorTemperature(RANDOM, 2500, 3500); // MAGIC_NUMBER
			double baseBrightness = getRandomBrightness(RANDOM, 0.7, 1, 3); // MAGIC_NUMBER

			for (int x = 0; x < mWidth; x++) {
				int baseTemperatureX = baseTemperature - 200 + RANDOM.nextInt(400); // MAGIC_NUMBER
				double baseBrightnessX = baseBrightness * (1 - 0.2 * RANDOM.nextDouble()); // MAGIC_NUMBER

				for (int y = 0; y < mHeight; y++) {
					double brightnessFactor = ensureInRange01(baseBrightnessX - 0.15 * y) / baseBrightnessX; // MAGIC_NUMBER
					mTargetColors[x][y] = convertColorTemperature(baseTemperatureX - 400 * y, baseBrightnessX) // MAGIC_NUMBER
							.add(Color.YELLOW, 0.3 * (mHeight - y - 1) / mHeight) // MAGIC_NUMBER
							.add(mBackground[x][y], 1 - brightnessFactor);
				}
			}

			mCurrentDuration = 100 + RANDOM.nextInt(2000); // MAGIC_NUMBER
		}

		/**
		 * Increase the base time.
		 *
		 * @param addedMillis The number of milliseconds to be added.
		 */
		private void addTime(final int addedMillis) {
			if (addedMillis > mCurrentDuration - 20) { // MAGIC_NUMBER Do not leave less than 20 ms remaining.
				for (int x = 0; x < mWidth; x++) {
					for (int y = 0; y < mHeight; y++) {
						mCurrentColors[x][y] = mTargetColors[x][y];
					}
				}
				setNewTarget();
			}
			else {
				double quota = (double) addedMillis / mCurrentDuration;
				for (int x = 0; x < mWidth; x++) {
					for (int y = 0; y < mHeight; y++) {
						mCurrentColors[x][y] = mCurrentColors[x][y].add(mTargetColors[x][y], quota);
					}
				}
				mCurrentDuration -= addedMillis;
			}
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

	/**
	 * Colors defining a crest for candles.
	 */
	private static class CrestColors extends TileChainColors {
		/**
		 * The default serial version UID.
		 */
		private static final long serialVersionUID = 1L;
		/**
		 * The colors used for the crest.
		 */
		private static final Color[] CREST_COLORS = {Color.OFF, Color.OFF, color(19, 30, 17), color(35, 43, 30), color(10, 13, 8), // MAGIC_NUMBER
				color(15, 20, 10), color(20, 26, 15), color(28, 35, 25), color(25, 31, 19), color(22, 28, 17), color(46, 59, 36), // MAGIC_NUMBER
				color(43, 56, 35), color(35, 45, 30), color(15, 25, 20), Color.OFF, Color.OFF, // MAGIC_NUMBER

				Color.OFF, color(13, 27, 11), color(27, 34, 21), color(25, 32, 21), color(42, 53, 36), color(50, 60, 44), // MAGIC_NUMBER
				color(57, 70, 50), color(56, 65, 51), color(45, 54, 37), color(52, 63, 43), color(72, 89, 63), color(41, 52, 34), // MAGIC_NUMBER
				color(32, 43, 24), color(51, 66, 41), color(54, 71, 42), color(22, 37, 20), // MAGIC_NUMBER

				color(34, 48, 40), color(47, 57, 41), color(38, 48, 32), color(57, 70, 52), color(52, 66, 46), color(58, 73, 50), // MAGIC_NUMBER
				color(60, 71, 52), color(62, 69, 59), color(54, 62, 49), color(47, 53, 43), color(68, 82, 58), color(58, 72, 51), // MAGIC_NUMBER
				color(44, 58, 33), color(30, 41, 23), color(55, 76, 40), color(79, 92, 65), // MAGIC_NUMBER

				color(14, 24, 13), color(52, 57, 48), color(68, 79, 59), color(51, 62, 45), color(60, 76, 53), color(47, 59, 38), // MAGIC_NUMBER
				color(33, 42, 27), color(24, 31, 19), color(7, 20, 5), color(22, 28, 17), color(31, 41, 24), color(40, 50, 32), // MAGIC_NUMBER
				color(43, 56, 33), color(52, 67, 41), color(67, 81, 58), color(34, 38, 30), // MAGIC_NUMBER

				Color.OFF, Color.OFF, color(11, 11, 11), color(28, 30, 25), color(30, 34, 27), color(39, 43, 36), // MAGIC_NUMBER
				color(53, 61, 48), color(44, 53, 36), color(53, 61, 48), color(51, 59, 43), color(51, 57, 43), color(59, 66, 55), // MAGIC_NUMBER
				color(40, 45, 35), color(10, 12, 9), Color.OFF, Color.OFF // MAGIC_NUMBER
		};

		@Override
		public Color getColor(final int x, final int y, final int width, final int height) {
			if (y > 4) { // MAGIC_NUMBER
				return Color.OFF;
			}
			else {
				return CREST_COLORS[x + 16 * y]; // MAGIC_NUMBER
			}
		}

		/**
		 * Get a color from its RGB values.
		 *
		 * @param red the red byte
		 * @param blue the blue byte
		 * @param green the green byte
		 * @return The color
		 */
		private static Color color(final int red, final int green, final int blue) {
			Color baseColor = new Color.RGBK((short) (red * 256), (short) (green * 256), (short) (blue * 256), (short) 4500).toHsbk(); // MAGIC_NUMBER
			return new Color(baseColor.getHue(),
					TypeUtil.toShort(TypeUtil.toDouble(baseColor.getSaturation()) * 3), baseColor.getBrightness(), 4500); // MAGIC_NUMBER
		}

	}

}
