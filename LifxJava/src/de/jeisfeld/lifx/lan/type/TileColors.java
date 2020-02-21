package de.jeisfeld.lifx.lan.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold colors for one tile.
 */
public abstract class TileColors {
	/**
	 * The tile width. Assumed to be fixed value 8.
	 */
	private static final int WIDTH = 8;

	/**
	 * The colors used for switching the multizone device off.
	 */
	public static final TileColors OFF = new TileColors() {
		@Override
		public Color getColor(final int zoneIndex, final int zoneCount) {
			return Color.OFF;
		}
	};

	/**
	 * Get the color at a certain point.
	 *
	 * @param x The x coordinate (0-7).
	 * @param y The y coordinate (0-7).
	 * @return the color at this position.
	 */
	public abstract Color getColor(int x, int y);

	/**
	 * Shift the colors by a certain amount.
	 *
	 * @param shiftX The shift in X direction.
	 * @param shiftY The shift in Y direction.
	 * @return The shifted colors.
	 */
	public TileColors shift(final int shiftX, final int shiftY) {
		TileColors base = this;
		return new TileColors() {
			@Override
			public Color getColor(final int x, final int y) {
				return base.getColor(x - shiftX, y - shiftY);
			}
		};
	}

	/**
	 * Multiply the colors by a certain brightness factor.
	 *
	 * @param brightnessFactor The brightness factor (1 meaning unchanged).
	 * @return The changed colors.
	 */
	public TileColors withRelativeBrightness(final double brightnessFactor) {
		TileColors base = this;
		return new TileColors() {
			@Override
			public Color getColor(final int x, final int y) {
				return base.getColor(x, y).withRelativeBrightness(brightnessFactor);
			}
		};
	}

	/**
	 * Return the colors as String output.
	 *
	 * @return The String output.
	 */
	public String getColorString() {
		StringBuilder result = new StringBuilder("Multizone Colors: \n");
		for (int y = 0; y < WIDTH; y++) {
			for (int x = 0; x < WIDTH; x++) {
				result.append("      ").append(getColor(x, y)).append(",");
			}
			result.replace(result.length() - 1, result.length(), "\n");
		}
		return result.toString();
	}

	/**
	 * Get the colors as list to be sent to the device.
	 *
	 * @return The list.
	 */
	public List<Color> asList() {
		List<Color> result = new ArrayList<>();
		for (int y = 0; y < WIDTH; y++) {
			for (int x = 0; x < WIDTH; x++) {
				result.add(getColor(x, y));
			}
		}
		return result;
	}

	/**
	 * Mix with another set of colors.
	 *
	 * @param other The other colors.
	 * @param quota The quota of the other colors (between 0 and 1)
	 * @return The mixed colors.
	 */
	public final TileColors add(final TileColors other, final double quota) {
		TileColors base = this;
		return new TileColors() {
			@Override
			public Color getColor(final int x, final int y) {
				return base.getColor(x, y).add(other.getColor(x, y), quota);
			}
		};
	}

	/**
	 * Multizone colors defined by colors in the corners that are linearly interpolated.
	 */
	public static class InterpolatedCorners extends TileColors {
		/**
		 * The color on top left.
		 */
		private final Color mColorTopLeft;
		/**
		 * The color on top right.
		 */
		private final Color mColorTopRight;
		/**
		 * The color on bottom left.
		 */
		private final Color mColorBottomLeft;
		/**
		 * The color on bottom right.
		 */
		private final Color mColorBottomRight;

		/**
		 * Create interpolated colors.
		 *
		 * @param colorTopLeft The color on top left
		 * @param colorTopRight The color on top right
		 * @param colorBottomLeft The color on bottom left
		 * @param colorBottomRight The color on bottom right
		 */
		public InterpolatedCorners(final Color colorTopLeft, final Color colorTopRight, final Color colorBottomLeft, final Color colorBottomRight) {
			mColorTopLeft = colorTopLeft;
			mColorTopRight = colorTopRight;
			mColorBottomLeft = colorBottomLeft;
			mColorBottomRight = colorBottomRight;
		}

		@Override
		public final Color getColor(final int x, final int y) {
			double xquota = (double) x / (WIDTH - 1);
			double yquota = (double) y / (WIDTH - 1);

			Color colorTop = mColorTopLeft.add(mColorTopRight, xquota);
			Color colorBottom = mColorBottomLeft.add(mColorBottomRight, xquota);

			return colorTop.add(colorBottom, yquota);
		}
	}

	/**
	 * Multizone colors defined by a fixed color.
	 */
	public static class Fixed extends TileColors {
		/**
		 * The color.
		 */
		private final Color mColor;

		/**
		 * Create one-color multizone colors.
		 *
		 * @param color The defining color.
		 */
		public Fixed(final Color color) {
			mColor = color;
		}

		@Override
		public final Color getColor(final int x, final int y) {
			return mColor;
		}
	}

	/**
	 * Multizone colors defined by a list of colors.
	 */
	public static class Exact extends TileColors {
		/**
		 * The colors.
		 */
		private final Color[][] mColors;

		/**
		 * Create Tile colors as 8x8 matrix.
		 *
		 * @param colors The defining colors. Must be array of size 8x8.
		 */
		public Exact(final Color[][] colors) {
			mColors = colors;
		}

		/**
		 * Create Tile colors from list as retrieved via getColors.
		 *
		 * @param colors The colors.
		 */
		public Exact(final List<Color> colors) {
			if (colors.size() < WIDTH * WIDTH) {
				int missingCount = WIDTH * WIDTH - colors.size();
				for (int i = 0; i < missingCount; i++) {
					colors.add(Color.OFF);
				}
			}
			mColors = new Color[WIDTH][WIDTH];
			Iterator<Color> iterator = colors.iterator();
			for (int y = 0; y < WIDTH; y++) {
				for (int x = 0; x < WIDTH; x++) {
					mColors[y][x] = iterator.next();
				}
			}
		}

		@Override
		public final Color getColor(final int x, final int y) {
			return mColors[y][x];
		}
	}
}
