package de.jeisfeld.lifx.app.animation;

import android.content.Intent;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Animation data for moving colors on a multizone device.
 */
public class TileChainMove extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The min difference between two calls. Set to 250 ms, meaning that there are at most 20 calls per second in case of 5 tiles.
	 */
	private static final int MIN_DURATION = 250;

	/**
	 * The duration of the move.
	 */
	private final int mDuration;
	/**
	 * The radius of the move.
	 */
	private final double mRadius;
	/**
	 * The direction of the move.
	 */
	private final Direction mDirection;

	/**
	 * Constructor.
	 *
	 * @param duration the duration of the move.
	 * @param radius the radius of the move.
	 * @param direction the direction of the move.
	 */
	public TileChainMove(final int duration, final double radius, final Direction direction) {
		mDuration = duration;
		mRadius = radius;
		mDirection = direction;
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
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_ANIMATION_RADIUS, mRadius);
		serviceIntent.putExtra(EXTRA_ANIMATION_DIRECTION, mDirection);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_MOVE;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
		final TileChain tileChainLight = (TileChain) light;

		final double xCenter = (tileChainLight.getTotalWidth() - 1) / 2.0;
		final double yCenter = (tileChainLight.getTotalHeight() - 1) / 2.0;

		return new TileChain.AnimationDefinition() {
			private final int mStepDuration = Math.max(MIN_DURATION, (int) (mDuration / (2 * mRadius)));
			private final double mRadiusFactor = mStepDuration * mRadius / mDuration * (mDirection == Direction.INWARD ? 1 : -1);

			@Override
			public TileChainColors getColors(final int n) {
				return new TileChainMoveColors(xCenter, yCenter, mRadius, mRadiusFactor * n, getSelectedBrightness(light));
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
		private final short mBrightness;
		/**
		 * The radius.
		 */
		private final double mRadius;
		/**
		 * The offset.
		 */
		private final double mOffset;

		/**
		 * Constructor.
		 *
		 * @param xCenter The x center.
		 * @param yCenter The y center.
		 * @param radius The radius.
		 * @param offset The offset.
		 * @param brightness The brightness.
		 */
		private TileChainMoveColors(final double xCenter, final double yCenter, final double radius,
				final double offset, final double brightness) {
			mXCenter = xCenter;
			mYCenter = yCenter;
			mBrightness = TypeUtil.toShort(brightness);
			mRadius = radius;
			mOffset = offset;
		}

		/**
		 * The default serializable version id.
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Color getColor(final int x, final int y, final int width, final int height) {
			double distance = Math.sqrt((x - mXCenter) * (x - mXCenter) + (y - mYCenter) * (y - mYCenter));
			return new Color((int) (65536 * (distance + mOffset) / mRadius), -1, mBrightness, 4000); // MAGIC_NUMBER
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
		INWARD;

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
}
