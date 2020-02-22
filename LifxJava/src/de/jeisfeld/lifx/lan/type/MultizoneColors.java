package de.jeisfeld.lifx.lan.type;

import static de.jeisfeld.lifx.lan.util.TypeUtil.INDENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to hold multizone colors.
 */
public abstract class MultizoneColors {
	/**
	 * The colors used for switching the multizone device off.
	 */
	public static final MultizoneColors OFF = new MultizoneColors() {
		@Override
		public Color getColor(final int zoneIndex, final int zoneCount) {
			return Color.OFF;
		}
	};

	/**
	 * Get the color at a certain zone index.
	 *
	 * @param zoneIndex The zone index
	 * @param zoneCount The number of zones
	 * @return the color at this index.
	 */
	public abstract Color getColor(int zoneIndex, int zoneCount);

	/**
	 * Shift the colors by a certain amount of zones.
	 *
	 * @param shiftCount The number of zones for the shift.
	 * @return The shifted colors.
	 */
	public MultizoneColors shift(final int shiftCount) {
		MultizoneColors base = this;
		return new MultizoneColors() {
			@Override
			public Color getColor(final int zoneIndex, final int zoneCount) {
				return base.getColor(zoneIndex - shiftCount, zoneCount);
			}
		};
	}

	/**
	 * Multiply the colors by a certain brightness factor.
	 *
	 * @param brightnessFactor The brightness factor (1 meaning unchanged).
	 * @return The changed colors.
	 */
	public MultizoneColors withRelativeBrightness(final double brightnessFactor) {
		MultizoneColors base = this;
		return new MultizoneColors() {
			@Override
			public Color getColor(final int zoneIndex, final int zoneCount) {
				return base.getColor(zoneIndex, zoneCount).withRelativeBrightness(brightnessFactor);
			}
		};
	}

	/**
	 * Return the colors as String output.
	 *
	 * @param zoneCount The number of zones.
	 * @return The String output.
	 */
	public String getColorString(final int zoneCount) {
		StringBuilder result = new StringBuilder("Multizone Colors: \n");
		for (int i = 0; i < zoneCount; i++) {
			result.append(INDENT).append(getColor(i, zoneCount)).append("\n");
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
	public MultizoneColors add(final MultizoneColors other, final double quota) {
		MultizoneColors base = this;
		return new MultizoneColors() {
			@Override
			public Color getColor(final int zoneIndex, final int zoneCount) {
				return base.getColor(zoneIndex, zoneCount).add(other.getColor(zoneIndex, zoneCount), quota);
			}
		};
	}

	/**
	 * Multizone colors defined by a base list of colors that are linearly interpolated.
	 */
	public static class Interpolated extends MultizoneColors {
		/**
		 * The colors used for interpolation.
		 */
		private final List<Color> mColors;
		/**
		 * Flag indicating if interpolation should be cyclically.
		 */
		private final boolean mCyclic;

		/**
		 * Create interpolated multizone colors.
		 *
		 * @param cyclic flag indicating if interpolation should be cyclically.
		 * @param colors The defining colors.
		 */
		public Interpolated(final boolean cyclic, final Color... colors) {
			mCyclic = cyclic;
			mColors = Arrays.asList(colors);
		}

		/**
		 * Create interpolated multizone colors.
		 *
		 * @param cyclic flag indicating if interpolation should be cyclically.
		 * @param colors The defining colors.
		 */
		public Interpolated(final boolean cyclic, final List<Color> colors) {
			mCyclic = cyclic;
			mColors = colors;
		}

		@Override
		public final Color getColor(final int zoneIndex, final int zoneCount) {
			int index = (zoneIndex % zoneCount + zoneCount) % zoneCount;
			if (mColors == null || mColors.size() <= 1) {
				return mColors == null || mColors.size() == 0 ? Color.OFF : mColors.get(0);
			}
			else if (mColors.size() >= zoneCount) {
				return mColors.get(index);
			}
			else {
				if (mCyclic) {
					double relativeIndex = (double) index * mColors.size() / zoneCount;
					return relativeIndex >= mColors.size() - 1 ? mColors.get(mColors.size() - 1).add(mColors.get(0), relativeIndex % 1)
							: mColors.get((int) Math.floor(relativeIndex)).add(mColors.get((int) Math.floor(relativeIndex) + 1), relativeIndex % 1);
				}
				else {
					double relativeIndex = (double) index * (mColors.size() - 1) / (zoneCount - 1);
					return relativeIndex >= mColors.size() - 1 ? mColors.get(mColors.size() - 1)
							: mColors.get((int) Math.floor(relativeIndex)).add(mColors.get((int) Math.floor(relativeIndex) + 1), relativeIndex % 1);
				}
			}
		}

		@Override
		public final MultizoneColors withRelativeBrightness(final double brightnessFactor) {
			List<Color> newColors = new ArrayList<>();
			for (Color color : mColors) {
				newColors.add(color.withRelativeBrightness(brightnessFactor));
			}
			return new MultizoneColors.Interpolated(mCyclic, newColors);
		}

		/**
		 * Redetect interpolated multizone colors from the device colors.
		 *
		 * @param count The number of colors to be interpolated. At least 1.
		 * @param colors The colors from the device.
		 * @return The redetected interpolated colors.
		 */
		public static MultizoneColors.Interpolated fromColors(final int count, final List<Color> colors) {
			int zoneCount = colors.size();
			Color[] newColors = new Color[count];

			newColors[count - 1] = colors.get(zoneCount - 1);
			newColors[0] = colors.get(0);

			double lastZone = 0;
			Color lastColor = colors.get(0);

			for (int i = 1; i < count - 1; i++) {
				double nextZone = (double) i * (zoneCount - 1) / (count - 1);
				int realZone = (int) Math.floor(nextZone + 0.000001); // MAGIC_NUMBER - ensure that floor is fine even with rounding issues
				Color realColor = colors.get(realZone);
				Color nextColor = lastColor.extrapolate(realColor, (nextZone - lastZone) / (realZone - lastZone));
				newColors[i] = nextColor;
				lastZone = nextZone;
				lastColor = nextColor;
			}

			return new MultizoneColors.Interpolated(false, newColors);
		}

		/**
		 * Get the list of colors to be interpolated.
		 *
		 * @return The colors.
		 */
		public List<Color> getColors() {
			return mColors;
		}

		@Override
		public final String toString() {
			return "MultizoneColors.Interpolated" + (mCyclic ? "°" : "") + mColors;
		}

	}

	/**
	 * Multizone colors defined by a fixed color.
	 */
	public static class Fixed extends MultizoneColors {
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
		public final Color getColor(final int zoneIndex, final int zoneCount) {
			return mColor;
		}

		@Override
		public final MultizoneColors withRelativeBrightness(final double brightnessFactor) {
			return new MultizoneColors.Fixed(mColor.withRelativeBrightness(brightnessFactor));
		}

		@Override
		public final String toString() {
			return "MultizoneColors.Fixed[" + mColor + "]";
		}
	}

	/**
	 * Multizone colors defined by a list of colors.
	 */
	public static class Exact extends MultizoneColors {
		/**
		 * The color list.
		 */
		private final List<Color> mColors;

		/**
		 * Create one-color multizone colors.
		 *
		 * @param colors The defining colors.
		 */
		public Exact(final List<Color> colors) {
			mColors = colors;
		}

		@Override
		public final Color getColor(final int zoneIndex, final int zoneCount) {
			return zoneIndex >= mColors.size() ? Color.OFF : mColors.get(zoneIndex);
		}

		@Override
		public final MultizoneColors withRelativeBrightness(final double brightnessFactor) {
			List<Color> newColors = new ArrayList<>();
			for (Color color : mColors) {
				newColors.add(color.withRelativeBrightness(brightnessFactor));
			}
			return new MultizoneColors.Exact(newColors);
		}

		@Override
		public final String toString() {
			return "MultizoneColors.Exact" + mColors;
		}
	}
}
