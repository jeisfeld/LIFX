package de.jeisfeld.lifx.lan.type;

import static de.jeisfeld.lifx.lan.util.TypeUtil.INDENT;

import de.jeisfeld.lifx.lan.TileChain;

/**
 * Class to hold tile chain colors.
 */
public abstract class TileChainColors {
	/**
	 * The colors used for switching the tile chain off.
	 */
	public static final TileChainColors OFF = new TileChainColors() {
		@Override
		public Color getColor(final int x, final int y, final int width, final int height) {
			return Color.OFF;
		}
	};

	/**
	 * Get the color at a certain position.
	 *
	 * @param x The x coordinate
	 * @param y The y coordinate
	 * @param width The width
	 * @param height The height
	 * @return The color at this position.
	 */
	public abstract Color getColor(int x, int y, int width, int height);

	/**
	 * Shift the colors by a certain amount of zones.
	 *
	 * @param shiftX The number of zones for the shift in x direction.
	 * @param shiftY The number of zones for the shift in y direction.
	 * @return The shifted colors.
	 */
	public TileChainColors shift(final int shiftX, final int shiftY) {
		TileChainColors base = this;
		return new TileChainColors() {
			@Override
			public Color getColor(final int x, final int y, final int width, final int height) {
				return base.getColor(x - shiftX, y - shiftY, width, height);
			}
		};
	}

	/**
	 * Multiply the colors by a certain brightness factor.
	 *
	 * @param brightnessFactor The brightness factor (1 meaning unchanged).
	 * @return The changed colors.
	 */
	public TileChainColors withRelativeBrightness(final double brightnessFactor) {
		TileChainColors base = this;
		return new TileChainColors() {
			@Override
			public Color getColor(final int x, final int y, final int width, final int height) {
				return base.getColor(x, y, width, height).withRelativeBrightness(brightnessFactor);
			}
		};
	}

	/**
	 * Return the colors as String output.
	 *
	 * @param width The width
	 * @param height The height
	 * @return The String output.
	 */
	public String getColorString(final int width, final int height) {
		StringBuilder result = new StringBuilder("Tile Chain Colors: \n");
		for (int y = 0; y < height; y++) {
			result.append(INDENT).append("[");
			for (int x = 0; x < width; x++) {
				result.append(getColor(x, y, width, height)).append(", ");
			}
			result.replace(result.length() - 2, result.length(), "]\n");
		}
		return result.toString();
	}

	/**
	 * Mix with another set of colors.
	 *
	 * @param other The other colors.
	 * @param quota The quota of the other colors (between 0 and 1)
	 * @return The mixed colors.
	 */
	public TileChainColors add(final TileChainColors other, final double quota) {
		TileChainColors base = this;
		return new TileChainColors() {
			@Override
			public Color getColor(final int x, final int y, final int width, final int height) {
				return base.getColor(x, y, width, height).add(other.getColor(x, y, width, height), quota);
			}
		};
	}

	/**
	 * Get the tile colors for a certain tile.
	 *
	 * @param minX The min x coordinate of the tile.
	 * @param minY The min y coordinate of the tile.
	 * @param totalWidth the total width of the tile chain.
	 * @param totalHeight the total height of the tile chain.
	 * @return The tile colors of the tile.
	 */
	public TileColors getTileColors(final int minX, final int minY, final int totalWidth, final int totalHeight) {
		return new TileColors() {
			@Override
			public Color getColor(final int x, final int y) {
				return TileChainColors.this.getColor(minX + x, minY + y, totalWidth, totalHeight);
			}

		};
	}

	/**
	 * Get the max brightness of the tile chain having these colors.
	 *
	 * @param tileChain The tile chain.
	 * @return The max brightness.
	 */
	public int getMaxBrightness(final TileChain tileChain) {
		int maxBrightness = 0;
		for (TileInfo tileInfo : tileChain.getTileInfo()) {
			maxBrightness = Math.max(maxBrightness,
					getTileColors(tileInfo.getMinX(), tileInfo.getMinY(), tileChain.getTotalWidth(), tileChain.getTotalHeight()).getMaxBrightness());
		}
		return maxBrightness;
	}

	/**
	 * Tile chain colors defined by a fixed color.
	 */
	public static class Fixed extends TileChainColors {
		/**
		 * The color.
		 */
		private final Color mColor;

		/**
		 * Create one-color tile chain colors.
		 *
		 * @param color The defining color.
		 */
		public Fixed(final Color color) {
			mColor = color;
		}

		@Override
		public final Color getColor(final int x, final int y, final int width, final int height) {
			return mColor;
		}

		@Override
		public final TileChainColors withRelativeBrightness(final double brightnessFactor) {
			return new TileChainColors.Fixed(mColor.withRelativeBrightness(brightnessFactor));
		}

		@Override
		public final String toString() {
			return "TileChainColors.Fixed[" + mColor + "]";
		}
	}

	/**
	 * Tile chain colors defined by the exact colors of a list of tiles.
	 */
	public static class PerTile extends TileChainColors {
		/**
		 * The tile chain.
		 */
		private final TileChain mTileChain;
		/**
		 * The colors.
		 */
		private final TileColors[] mColors;

		/**
		 * Define the colors from the existing tile colors.
		 *
		 * @param tileChain the tile chain
		 * @param colors colors of the individual tiles.
		 */
		public PerTile(final TileChain tileChain, final TileColors[] colors) {
			mTileChain = tileChain;
			mColors = colors;
		}

		@Override
		public final Color getColor(final int x, final int y, final int width, final int height) {
			for (int i = 0; i < mTileChain.getTileCount(); i++) {
				TileInfo tileInfo = mTileChain.getTileInfo().get(i);
				if (x >= tileInfo.getMinX() && x < tileInfo.getMinX() + tileInfo.getWidth()
						&& y >= tileInfo.getMinY() && y < tileInfo.getMinY() + tileInfo.getHeight()) {
					return mColors[i].getColor(x - tileInfo.getMinX(), y - tileInfo.getMinY());
				}
			}

			return Color.OFF;
		}

		@Override
		public final TileChainColors withRelativeBrightness(final double brightnessFactor) {
			TileColors[] colorsWithBrightness = new TileColors[mColors.length];
			for (int i = 0; i < mColors.length; i++) {
				colorsWithBrightness[i] = mColors[i].withRelativeBrightness(brightnessFactor);
			}
			return new TileChainColors.PerTile(mTileChain, colorsWithBrightness);
		}

		@Override
		public final String toString() {
			StringBuilder stringBuilder = new StringBuilder("TileChainColors.PerTile[");
			for (TileColors colors : mColors) {
				stringBuilder.append(colors).append(", ");
			}
			stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "]");
			return stringBuilder.toString();
		}
	}

	/**
	 * Tile colors defined by colors in the corners that are linearly interpolated.
	 */
	public static class InterpolatedCorners extends TileChainColors {
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
		public final Color getColor(final int x, final int y, final int width, final int height) {
			double xquota = (double) x / (width - 1);
			double yquota = (double) y / (height - 1);

			Color colorTop = mColorTopLeft.add(mColorTopRight, xquota);
			Color colorBottom = mColorBottomLeft.add(mColorBottomRight, xquota);

			return colorBottom.add(colorTop, yquota);
		}

		@Override
		public final TileChainColors withRelativeBrightness(final double brightnessFactor) {
			return new TileChainColors.InterpolatedCorners(mColorTopLeft.withRelativeBrightness(brightnessFactor),
					mColorTopRight.withRelativeBrightness(brightnessFactor), mColorBottomLeft.withRelativeBrightness(brightnessFactor),
					mColorBottomRight.withRelativeBrightness(brightnessFactor));
		}

		@Override
		public final String toString() {
			return "TileChainColors.InterpolatedCorners[" + mColorTopLeft + "," + mColorTopRight + ","
					+ mColorBottomLeft + "," + mColorBottomRight + "," + "]";
		}
	}

}
