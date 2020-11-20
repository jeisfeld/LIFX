package de.jeisfeld.lifx.app.animation;

import android.content.Intent;

import java.util.ArrayList;

import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;

/**
 * Animation data for moving colors on a multizone device.
 */
public class TileChainWave extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;
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
	 * The duration of the wave.
	 */
	private final int mDuration;
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
	 * Constructor.
	 *
	 * @param duration  the duration of the wave.
	 * @param radius    the radius of the wave.
	 * @param direction the direction of the wave.
	 * @param form      the form of the wave.
	 * @param colors    The colors to be used (interpolated cyclically).
	 */
	public TileChainWave(final int duration, final double radius, final Direction direction, final Form form, final ArrayList<Color> colors) {
		mDuration = duration;
		mRadius = radius;
		mDirection = direction;
		mForm = form;
		mColors = colors;
	}

	/**
	 * Get the duration of the move.
	 *
	 * @return the duration
	 */
	public final int getDuration() {
		return mDuration;
	}

	/**
	 * Get the direction of the move.
	 *
	 * @return the direction
	 */
	public final Direction getDirection() {
		return mDirection;
	}

	@Override
	public final boolean isValid() {
		return mDuration > 0 && mColors.size() > 0;
	}

	@Override
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_ANIMATION_RADIUS, mRadius);
		serviceIntent.putExtra(EXTRA_ANIMATION_DIRECTION, mDirection);
		serviceIntent.putExtra(EXTRA_ANIMATION_FORM, mForm);
		serviceIntent.putExtra(EXTRA_COLOR_LIST, mColors);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_WAVE;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
		final TileChain tileChainLight = (TileChain) light;

		final double xCenter;
		switch (mDirection) {
		case FROM_LEFT:
			xCenter = -ONE_HALF; // MAGIC_
			break;
		case FROM_RIGHT:
			xCenter = tileChainLight.getTotalWidth() - ONE_HALF;
			break;
		default:
			xCenter = (tileChainLight.getTotalWidth() - 1) * ONE_HALF;
		}

		final double yCenter;
		switch (mDirection) {
		case FROM_BOTTOM:
			yCenter = -ONE_HALF;
			break;
		case FROM_TOP:
			yCenter = tileChainLight.getTotalHeight() - ONE_HALF;
			break;
		default:
			yCenter = (tileChainLight.getTotalHeight() - 1) * (mForm == Form.HEART ? HEART_CENTER : ONE_HALF);
		}

		return new TileChain.AnimationDefinition() {
			private final int mStepDuration = Math.max(MIN_DURATION, (int) (mDuration / (2 * mRadius)));
			private final double mRadiusFactor = mStepDuration * mRadius / mDuration * (mDirection == Direction.INWARD ? 1 : -1);

			@Override
			public TileChainColors getColors(final int n) {
				return new TileChainMoveColors(xCenter, yCenter, mRadius, mRadiusFactor * n, mColors, mForm, getSelectedBrightness(light));
			}

			@Override
			public int getDuration(final int n) {
				return n == 0 ? 0 : mStepDuration;
			}
		};
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
		protected static Direction fromOrdinal(final int ordinal) {
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
		 * Get Direction from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The direction.
		 */
		protected static Form fromOrdinal(final int ordinal) {
			for (Form form : values()) {
				if (ordinal == form.ordinal()) {
					return form;
				}
			}
			return CIRCLE;
		}
	}
}
