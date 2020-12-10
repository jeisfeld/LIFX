package de.jeisfeld.lifx.lan.animation;

import static de.jeisfeld.lifx.lan.animation.CandleAnimationDefinition.Background.CRADLE;

import java.util.Random;

import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * An animation of a candle on a tile chain.
 */
public final class CandleAnimationDefinition implements TileChain.AnimationDefinition {
	/**
	 * The candle color.
	 */
	private static final Color CANDLE_COLOR = Color.RED.withBrightness(0.15);
	/**
	 * The random number generator.
	 */
	private static final Random RANDOM = new Random();
	/**
	 * The tile chain light.
	 */
	private final TileChain mLight;
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
	private final Background mBackground;
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
	 * Constructor for the animation definition.
	 *
	 * @param light The tile chain light.
	 * @param candleCount The number of candles.
	 * @param candleBurndown The burndown of the candles.
	 * @param brightnessFactor An overall brightness factor.
	 * @param background The background mode.
	 */
	public CandleAnimationDefinition(final TileChain light, final int candleCount, final int candleBurndown, final double brightnessFactor,
			final Background background) {
		mLight = light;
		mCandleCount = candleCount;
		mCandleBurndown = candleBurndown;
		mBrightnessFactor = brightnessFactor;
		mBackground = background;

		switch (candleCount) {
		case 2:
			mFlameWidth = 2;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 4; // MAGIC_NUMBER
			mCandleHeight = mBackground == CRADLE ? new int[] {9 - mCandleBurndown, 11 - mCandleBurndown} // MAGIC_NUMBER
					: new int[] {10 - mCandleBurndown, 12 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {2, 10}; // MAGIC_NUMBER
			mCandleBottom = mBackground == CRADLE ? new int[] {1, 1} : new int[] {0, 0}; // MAGIC_NUMBER
			break;
		case 3: // MAGIC_NUMBER
			mFlameWidth = 2;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 4; // MAGIC_NUMBER
			mCandleHeight = mBackground == CRADLE ? new int[] {7 - mCandleBurndown, 11 - mCandleBurndown, 9 - mCandleBurndown} // MAGIC_NUMBER
					: new int[] {8 - mCandleBurndown, 12 - mCandleBurndown, 10 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {0, 6, 12}; // MAGIC_NUMBER
			mCandleBottom = mBackground == CRADLE ? new int[] {2, 1, 2} : new int[] {0, 0, 0}; // MAGIC_NUMBER
			break;
		case 4: // MAGIC_NUMBER
			mFlameWidth = 1;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 3; // MAGIC_NUMBER
			mCandleHeight = mBackground == CRADLE // MAGIC_NUMBER
					? new int[] {5 - mCandleBurndown, 11 - mCandleBurndown, 7 - mCandleBurndown, 9 - mCandleBurndown} // MAGIC_NUMBER
					: new int[] {6 - mCandleBurndown, 12 - mCandleBurndown, 10 - mCandleBurndown, 8 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {0, 4, 9, 13}; // MAGIC_NUMBER
			mCandleBottom = mBackground == CRADLE ? new int[] {3, 1, 5, 3} : new int[] {0, 0, 0, 0}; // MAGIC_NUMBER
			break;
		case 1:
		default:
			mFlameWidth = 2;
			mFlameHeight = 4; // MAGIC_NUMBER
			mCandleWidth = 4; // MAGIC_NUMBER
			mCandleHeight = mBackground == CRADLE ? new int[] {11 - mCandleBurndown} : new int[] {12 - mCandleBurndown}; // MAGIC_NUMBER
			mCandleLeft = new int[] {6}; // MAGIC_NUMBER
			mCandleBottom = mBackground == CRADLE ? new int[] {1} : new int[] {0};
			break;
		}

		switch (mBackground) {
		case WHITE:
			mBackgroundColors = new TileChainColors.Fixed(Color.WHITE.withBrightness(0.01)); // MAGIC_NUMBER
			break;
		case KEEP:
			mBackgroundColors = mLight.getColors();
			break;
		case CRADLE:
			mBackgroundColors = new CrestColors();
			break;
		case BLACK:
		case LIGHT:
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
							mLight.getTotalWidth(), mLight.getTotalHeight());
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
		if (mBackground == CRADLE) {
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
		else if (mBackground == Background.LIGHT) { // MAGIC_NUMBER
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
		private static final Color[] CREST_COLORS = {
				new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600), new Color(23665, 49151, 2052, 3600), new Color(15365, 65535, 3852, 3600),
				new Color(23830, 65535, 3852, 3600), new Color(24365, 56797, 3852, 3600), new Color(15745, 65535, 4622, 3600),
				new Color(15029, 65535, 5393, 3600), new Color(23524, 42597, 5136, 3600), new Color(15891, 65535, 5907, 3600),
				new Color(23665, 65535, 4365, 3600), new Color(24029, 43690, 3852, 3600), new Color(15665, 65535, 2309, 3600),
				new Color(15485, 65535, 1310, 3600), new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600),
				new Color(15845, 65535, 1795, 3600), new Color(23301, 54612, 4622, 3600), new Color(22625, 53970, 4365, 3600),
				new Color(21845, 65535, 2566, 3600), new Color(21845, 65535, 1310, 3600), new Color(21845, 65535, 2052, 3600),
				new Color(21845, 65535, 2309, 3600), new Color(21845, 65535, 1795, 3600), new Color(21845, 65535, 2566, 3600),
				new Color(21845, 65535, 2052, 3600), new Color(21845, 65535, 2052, 3600), new Color(21845, 65535, 1310, 3600),
				new Color(21845, 65535, 1310, 3600), new Color(23058, 65535, 2309, 3600), new Color(23058, 42129, 3594, 3600),
				new Color(15845, 65535, 2566, 3600),
				new Color(23130, 46420, 6164, 3600), new Color(21845, 59577, 2823, 3600), new Color(21845, 65535, 1538, 3600),
				new Color(21845, 65535, 2052, 3600), new Color(21845, 59577, 2823, 3600), new Color(23405, 48288, 4879, 3600),
				new Color(23665, 56172, 3594, 3600), new Color(21845, 43690, 3852, 3600), new Color(21845, 60493, 3337, 3600),
				new Color(21845, 65535, 2052, 3600), new Color(21116, 54612, 4622, 3600), new Color(22451, 65535, 4622, 3600),
				new Color(21845, 65535, 2309, 3600), new Color(21845, 65535, 2052, 3600), new Color(21845, 65535, 1310, 3600),
				new Color(22625, 48288, 4879, 3600),
				new Color(15845, 65535, 5393, 3600), new Color(22391, 52428, 6424, 3600), new Color(21845, 65535, 3337, 3600),
				new Color(21202, 65535, 4365, 3600), new Color(24029, 54612, 3080, 3600), new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600),
				new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600),
				new Color(23130, 46420, 6164, 3600), new Color(21845, 65535, 4622, 3600), new Color(22573, 65535, 3852, 3600),
				new Color(21845, 65535, 3080, 3600), new Color(15625, 65535, 4622, 3600),
				new Color(21845, 65535, 1310, 3600), new Color(22685, 50115, 4365, 3600), new Color(22391, 56986, 5907, 3600),
				new Color(21845, 65535, 5136, 3600), new Color(21845, 65535, 3337, 3600), new Color(21845, 65535, 1310, 3600),
				new Color(21845, 65535, 1310, 3600), new Color(21845, 65535, 1310, 3600), new Color(21845, 65535, 1310, 3600),
				new Color(21845, 65535, 1310, 3600), new Color(21845, 65535, 2309, 3600), new Color(21845, 65535, 3594, 3600),
				new Color(22527, 61680, 4365, 3600), new Color(22885, 57343, 6164, 3600), new Color(21845, 48059, 3852, 3600),
				new Color(0, 0, 0, 3600),
				new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600), new Color(21845, 65535, 1310, 3600),
				new Color(22755, 65535, 3080, 3600), new Color(21845, 65535, 2566, 3600), new Color(21845, 65535, 1795, 3600),
				new Color(21845, 65535, 1538, 3600), new Color(21845, 65535, 1310, 3600), new Color(21845, 65535, 2052, 3600),
				new Color(21845, 65535, 1795, 3600), new Color(23058, 65535, 2309, 3600), new Color(23405, 50971, 2309, 3600),
				new Color(21845, 32767, 1310, 3600), new Color(0, 0, 0, 3600), new Color(0, 0, 0, 3600)
		};

		@Override
		public Color getColor(final int x, final int y, final int width, final int height) {
			if (y >= CREST_COLORS.length / 16) { // MAGIC_NUMBER
				return Color.OFF;
			}
			else {
				return CREST_COLORS[x + 16 * y]; // MAGIC_NUMBER
			}
		}
	}

	/**
	 * The background behind the candles.
	 */
	public enum Background {
		/**
		 * Black background.
		 */
		BLACK,
		/**
		 * A very light white background.
		 */
		WHITE,
		/**
		 * Keep the original tile light as background.
		 */
		KEEP,
		/**
		 * A cradle as background.
		 */
		CRADLE,
		/**
		 * With foreground light on bottom right.
		 */
		LIGHT;

		/**
		 * Get Direction from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The direction.
		 */
		public static Background fromOrdinal(final int ordinal) {
			for (Background background : values()) {
				if (ordinal == background.ordinal()) {
					return background;
				}
			}
			return BLACK;
		}
	}

}
