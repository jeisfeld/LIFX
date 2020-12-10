package de.jeisfeld.lifx.lan.animation;

import java.util.ArrayList;

import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.TileChain.AnimationDefinition;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;

/**
 * Animation definition for moving colors on a multizone device.
 */
public class TileChainWaveDefinition implements AnimationDefinition {
	/**
	 * The min difference between two calls. Set to 250 ms, meaning that there are at most 20 calls per second in case of 5 tiles.
	 */
	private static final int MIN_DURATION = 250;
	/**
	 * Factor for the center.
	 */
	private static final double ONE_HALF = 0.5;
	/**
	 * Factor for the vertical center of the heart.
	 */
	private static final double HEART_CENTER = 0.6;

	/**
	 * The radius of the wave.
	 */
	private final double mRadius;
	/**
	 * The direction of the wave.
	 */
	private final Direction mDirection;
	/**
	 * The form of the wave.
	 */
	private final Form mForm;
	/**
	 * The colors to be used (interpolated cyclically).
	 */
	private final ArrayList<Color> mColors;
	/**
	 * The horizontal center of the tile chain.
	 */
	private final double mXCenter;
	/**
	 * The vertical center of the tile chain.
	 */
	private final double mYCenter;
	/**
	 * The duration of a step.
	 */
	private final int mStepDuration;
	/**
	 * A factor for the radius.
	 */
	private final double mRadiusFactor;
	/**
	 * The selected brightness.
	 */
	private double mSelectedBrightness = 1;

	/**
	 * Constructor.
	 *
	 * @param light     the tile chain light.
	 * @param duration  the duration of the wave.
	 * @param radius    the radius of the wave.
	 * @param direction the direction of the wave.
	 * @param form      the form of the wave.
	 * @param colors    The colors to be used (interpolated cyclically).
	 */
	public TileChainWaveDefinition(final TileChain light, final int duration, final double radius, final Direction direction, final Form form,
								   final ArrayList<Color> colors) {
		mRadius = radius;
		mDirection = direction;
		mForm = form;
		mColors = colors;

		switch (mDirection) {
		case FROM_LEFT:
			mXCenter = -ONE_HALF; // MAGIC_
			break;
		case FROM_RIGHT:
			mXCenter = light.getTotalWidth() - ONE_HALF;
			break;
		default:
			mXCenter = (light.getTotalWidth() - 1) * ONE_HALF;
		}

		switch (mDirection) {
		case FROM_BOTTOM:
			mYCenter = -ONE_HALF;
			break;
		case FROM_TOP:
			mYCenter = light.getTotalHeight() - ONE_HALF;
			break;
		default:
			mYCenter = (light.getTotalHeight() - 1) * (form == Form.HEART ? HEART_CENTER : ONE_HALF);
		}

		mStepDuration = Math.max(MIN_DURATION, (int) (duration / (2 * radius)));
		mRadiusFactor = mStepDuration * radius / duration * (direction == Direction.INWARD ? 1 : -1);
	}

	/**
	 * Get the selected brightness.
	 *
	 * @return the selected brightness.
	 */
	// OVERRIDABLE
	protected double getSelectedBrightness() {
		return mSelectedBrightness;
	}

	/**
	 * Set the selected brightness.
	 *
	 * @param selectedBrightness The new selected brightness.
	 */
	public void setSelectedBrightness(final double selectedBrightness) {
		mSelectedBrightness = selectedBrightness;
	}

	@Override
	public final TileChainColors getColors(final int n) {
		return new TileChainMoveColors(mXCenter, mYCenter, mRadius, mRadiusFactor * n, mColors, mForm, getSelectedBrightness());
	}

	@Override
	public final int getDuration(final int n) {
		return n == 0 ? 0 : mStepDuration;
	}

	/**
	 * Colors for displaying animated circles.
	 */
	private static final class TileChainMoveColors extends TileChainColors {
		/**
		 * The x center.
		 */
		private final double mXCenter;
		/**
		 * The y center.
		 */
		private final double mYCenter;
		/**
		 * The brightness.
		 */
		private final double mBrightness;
		/**
		 * The radius.
		 */
		private final double mRadius;
		/**
		 * The offset.
		 */
		private final double mOffset;
		/**
		 * The colors to be used (interpolated cyclically).
		 */
		private final ArrayList<Color> mColors;
		/**
		 * The form of the wave.
		 */
		private final Form mForm;

		/**
		 * Constructor.
		 *
		 * @param xCenter    The x center.
		 * @param yCenter    The y center.
		 * @param radius     The radius.
		 * @param offset     The offset.
		 * @param colors     The colors used for the animation.
		 * @param form       The from of the wave.
		 * @param brightness The brightness.
		 */
		private TileChainMoveColors(final double xCenter, final double yCenter, final double radius,
									final double offset, final ArrayList<Color> colors, final Form form, final double brightness) {
			mXCenter = xCenter;
			mYCenter = yCenter;
			mRadius = radius;
			mOffset = offset;
			mColors = colors;
			mForm = form;
			mBrightness = brightness;
		}

		/**
		 * The default serializable version id.
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Color getColor(final int x, final int y, final int width, final int height) {
			double distance = getDistance(x - mXCenter, y - mYCenter);
			double index = (((distance + mOffset) / mRadius * mColors.size()) % mColors.size() + mColors.size()) % mColors.size();
			Color colorBefore = mColors.get((int) Math.floor(index));
			Color colorAfter = mColors.get(((int) Math.ceil(index)) % mColors.size());
			double percentage = index % 1;
			return colorBefore.add(colorAfter, percentage).withRelativeBrightness(mBrightness);
		}

		/**
		 * Get the color distance of a point from the center.
		 *
		 * @param x The x coordinate relative to the center.
		 * @param y The y coordinate relative to the center.
		 * @return The color distance.
		 */
		private double getDistance(final double x, final double y) {
			switch (mForm) {
			case SQUARE:
				return Math.max(Math.abs(x), Math.abs(y));
			case DIAMOND:
				return Math.max(Math.abs(x + y), Math.abs(x - y));
			case HEART:
				double theta = Math.atan2(-y, Math.abs(x)) + Math.PI / 2;
				double r = Math.sqrt(x * x + y * y);
				double factor = Math.pow(2 * theta / Math.PI - 1, 5) + 1.2 + Math.cos(theta / 2) / 3; // MAGIC_NUMBER
				return r / factor;
			case VERTICAL:
				return Math.abs(x);
			case HORIZONTAL:
				return Math.abs(y);
			case CIRCLE:
			default:
				return Math.sqrt(x * x + y * y);
			}
		}
	}

	/**
	 * The direction of the animation.
	 */
	public enum Direction {
		/**
		 * Outward movement.
		 */
		OUTWARD,
		/**
		 * Inward movement.
		 */
		INWARD,
		/**
		 * Movement from the left.
		 */
		FROM_LEFT,
		/**
		 * Movement from the right.
		 */
		FROM_RIGHT,
		/**
		 * Movement from the bottom.
		 */
		FROM_BOTTOM,
		/**
		 * Movement from the top.
		 */
		FROM_TOP;

		/**
		 * Get Direction from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The direction.
		 */
		public static Direction fromOrdinal(final int ordinal) {
			for (Direction direction : values()) {
				if (ordinal == direction.ordinal()) {
					return direction;
				}
			}
			return OUTWARD;
		}
	}

	/**
	 * The form of the wave animation.
	 */
	public enum Form {
		/**
		 * Circle.
		 */
		CIRCLE,
		/**
		 * Square.
		 */
		SQUARE,
		/**
		 * Diamond.
		 */
		DIAMOND,
		/**
		 * Heart.
		 */
		HEART,
		/**
		 * Vertikal stripes.
		 */
		VERTICAL,
		/**
		 * Horizontal stripes.
		 */
		HORIZONTAL;

		/**
		 * Get Form from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The form.
		 */
		public static Form fromOrdinal(final int ordinal) {
			for (Form form : values()) {
				if (ordinal == form.ordinal()) {
					return form;
				}
			}
			return CIRCLE;
		}
	}
}
