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
	 * The duration of the move.
	 */
	private final int mDuration;
	/**
	 * The direction of the move.
	 */
	private final Direction mDirection;

	/**
	 * Constructor.
	 *
	 * @param duration the duration of the move.
	 * @param direction the direction of the move.
	 */
	public TileChainMove(final int duration, final Direction direction) {
		mDuration = duration;
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
			@Override
			public TileChainColors getColors(final int n) {
				return new TileChainMoveColors(xCenter, yCenter, getSelectedBrightness(light), n);
			}

			@Override
			public int getDuration(final int n) {
				return 200; // MAGIC_NUMBER
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
		 * The offset.
		 */
		private final int mOffset;

		/**
		 * Constructor.
		 *
		 * @param xCenter The x center.
		 * @param yCenter The y center.
		 * @param brightness The brightness.
		 * @param offset the offset.
		 */
		private TileChainMoveColors(final double xCenter, final double yCenter, final double brightness, final int offset) {
			mXCenter = xCenter;
			mYCenter = yCenter;
			mBrightness = TypeUtil.toShort(brightness);
			mOffset = offset;
		}

		/**
		 * The default serializable version id.
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Color getColor(final int x, final int y, final int width, final int height) {
			double distance = Math.sqrt((x - mXCenter) * (x - mXCenter) + (y - mYCenter) * (y - mYCenter));
			return new Color((int) (1024 * (5 * distance - mOffset)), -1, mBrightness, 4000); // MAGIC_NUMBER
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
