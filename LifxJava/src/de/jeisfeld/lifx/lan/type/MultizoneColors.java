package de.jeisfeld.lifx.lan.type;

/**
 * Class to hold multizone colors.
 */
public abstract class MultizoneColors {
	/**
	 * The colors used for switching the multizone device off.
	 */
	public static final MultizoneColors OFF = new MultizoneColors() {
		@Override
		protected Color getBaseColor(final int zoneIndex, final int zoneCount) {
			return Color.OFF;
		}

	};

	/**
	 * Base constructor.
	 */
	protected MultizoneColors() {
	}

	/**
	 * Base constructor, cloning existing MultizoneColors.
	 *
	 * @param other the base of the clone.
	 */
	protected MultizoneColors(final MultizoneColors other) {
		this();
		mRelativeBrightness = other.mRelativeBrightness;
	}

	/**
	 * The relative brightness.
	 */
	private double mRelativeBrightness = 1;

	/**
	 * Set the relative brightness.
	 *
	 * @param relativeBrightness The relative brightness.
	 * @return the updated MultizoneColors.
	 */
	public MultizoneColors setRelativeBrightness(final double relativeBrightness) {
		mRelativeBrightness = relativeBrightness;
		return this;
	}

	/**
	 * Get the base color at a certain zone index (not considering brightness factor).
	 *
	 * @param zoneIndex The zone index
	 * @param zoneCount The number of zones
	 * @return the color at this index.
	 */
	protected abstract Color getBaseColor(int zoneIndex, int zoneCount);

	/**
	 * Get the color at a certain zone index.
	 *
	 * @param zoneIndex The zone index
	 * @param zoneCount The number of zones
	 * @return the color at this index.
	 */
	public Color getColor(final int zoneIndex, final int zoneCount) {
		return getBaseColor(zoneIndex, zoneCount).withRelativeBrightness(mRelativeBrightness);
	}

	/**
	 * Shift the colors by a certain amount of zones.
	 *
	 * @param shiftCount The number of zones for the shift.
	 * @return The shifted colors.
	 */
	public MultizoneColors shift(final int shiftCount) {
		MultizoneColors base = this;
		return new MultizoneColors(this) {
			@Override
			protected Color getBaseColor(final int zoneIndex, final int zoneCount) {
				return base.getBaseColor(zoneIndex - shiftCount, zoneCount);
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
		private final Color[] mColors;
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
			mColors = colors;
		}

		@Override
		protected final Color getBaseColor(final int zoneIndex, final int zoneCount) {
			int index = (zoneIndex % zoneCount + zoneCount) % zoneCount;
			if (mColors == null || mColors.length <= 1) {
				return mColors == null || mColors.length == 0 ? Color.OFF : mColors[0];
			}
			else if (mColors.length >= zoneCount) {
				return mColors[index];
			}
			else {
				if (mCyclic) {
					double relativeIndex = (double) index * mColors.length / zoneCount;
					return relativeIndex >= mColors.length - 1 ? mColors[mColors.length - 1].add(mColors[0], relativeIndex % 1)
							: mColors[(int) Math.floor(relativeIndex)].add(mColors[(int) Math.floor(relativeIndex) + 1], relativeIndex % 1);
				}
				else {
					double relativeIndex = (double) index * (mColors.length - 1) / (zoneCount - 1);
					return relativeIndex >= mColors.length - 1 ? mColors[mColors.length - 1]
							: mColors[(int) Math.floor(relativeIndex)].add(mColors[(int) Math.floor(relativeIndex) + 1], relativeIndex % 1);
				}
			}
		}
	}

}
