package de.jeisfeld.lifx.lan.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class to hold colors for one tile.
 */
public abstract class TileColors {
	/**
	 * The colors used for switching the tile off.
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
	 * Get the colors as list to be sent to the device.
	 *
	 * @param width the width of the tile.
	 * @param height the height of the tile.
	 * @return The list of colors.
	 */
	public List<Color> asList(final int width, final int height) {
		List<Color> result = new ArrayList<>();
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
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
	 * Get the max brightness of the tile.
	 *
	 * @param width the width of the tile.
	 * @param height the height of the tile.
	 * @return The max brightness.
	 */
	public int getMaxBrightness(final int width, final int height) {
		int maxBrightness = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				maxBrightness = Math.max(maxBrightness, TypeUtil.toUnsignedInt(getColor(x, y).getBrightness()));
			}
		}
		return maxBrightness;
	}

	/**
	 * Tile colors defined by a fixed color.
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

		@Override
		public final TileColors withRelativeBrightness(final double brightnessFactor) {
			return new TileColors.Fixed(mColor.withRelativeBrightness(brightnessFactor));
		}

		@Override
		public final String toString() {
			return "TileColors.Fixed[" + mColor + "]";
		}
	}

	/**
	 * Tile colors defined by a list of colors.
	 */
	public static class Exact extends TileColors {
		/**
		 * The colors.
		 */
		private final Color[][] mColors;
		/**
		 * The width of the tile.
		 */
		private final int mWidth;
		/**
		 * The height of the tile.
		 */
		private final int mHeight;

		/**
		 * Create Tile colors as 8x8 matrix.
		 *
		 * @param colors The defining colors. Must be array of size 8x8.
		 */
		public Exact(final Color[][] colors) {
			mColors = colors;
			mHeight = colors.length;
			mWidth = mHeight == 0 ? 0 : colors[0].length;
		}

		/**
		 * Create Tile colors from list as retrieved via getColors.
		 *
		 * @param colors The colors.
		 * @param width The width.
		 * @param height The height.
		 */
		public Exact(final List<Color> colors, final int width, final int height) {
			mWidth = width;
			mHeight = height;
			if (colors.size() < width * height) {
				int missingCount = width * height - colors.size();
				for (int i = 0; i < missingCount; i++) {
					colors.add(Color.OFF);
				}
			}
			mColors = new Color[height][width];
			Iterator<Color> iterator = colors.iterator();
			for (int y = height - 1; y >= 0; y--) {
				for (int x = 0; x < width; x++) {
					mColors[y][x] = iterator.next();
				}
			}
		}

		@Override
		public final Color getColor(final int x, final int y) {
			return mColors[y][x];
		}

		@Override
		public final TileColors withRelativeBrightness(final double brightnessFactor) {
			Color[][] colorsWithBrightness = new Color[mHeight][mWidth];
			for (int y = 0; y < mHeight; y++) {
				for (int x = 0; x < mWidth; x++) {
					colorsWithBrightness[y][x] = mColors[y][x].withRelativeBrightness(brightnessFactor);
				}
			}
			return new TileColors.Exact(colorsWithBrightness);
		}

		@Override
		public final String toString() {
			StringBuilder stringBuilder = new StringBuilder("TileColors.Exact[");
			for (int y = 0; y < mHeight; y++) {
				stringBuilder.append("[");
				for (int x = 0; x < mWidth; x++) {
					stringBuilder.append(mColors[y][x]).append(", ");
				}
				stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "], ");
			}
			stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "");
			return stringBuilder.toString();
		}
	}
}
