package de.jeisfeld.lifx.app.animation;

import java.io.IOException;

import android.content.Intent;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;
import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo.Move;

/**
 * Animation data for moving colors on a multizone device.
 */
public class MultizoneMove extends AnimationData {
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
	 * The stretch factor.
	 */
	private final double mStretch;
	/**
	 * The initial colors.
	 */
	private final MultizoneColors mColors;

	/**
	 * Constructor.
	 *
	 * @param duration the duration of the move.
	 * @param stretch the stretch factor.
	 * @param direction the direction of the move.
	 * @param colors the initial colors.
	 */
	public MultizoneMove(final int duration, final double stretch, final Direction direction, final MultizoneColors colors) {
		mDuration = duration;
		mStretch = stretch;
		mDirection = direction;
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
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_ANIMATION_STRETCH, mStretch);
		serviceIntent.putExtra(EXTRA_ANIMATION_DIRECTION, mDirection);
		serviceIntent.putExtra(EXTRA_MULTIZONE_COLORS, mColors);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.MULTIZONE_MOVE;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
		final MultiZoneLight mMultiZoneLight = (MultiZoneLight) light;
		final MultizoneColors stretchColors = mColors.stretch(mStretch);
		switch (getDirection()) {
		case INWARD:
		case OUTWARD:
			final int sgn1 = getDirection() == Direction.INWARD ? -1 : 1;
			return new MultiZoneLight.AnimationDefinition() {
				@Override
				public int getDuration(final int n) {
					return Math.abs(mDuration) / mMultiZoneLight.getZoneCount() * 2;
				}

				@Override
				public MultizoneColors getColors(final int n) {
					return stretchColors.shift(sgn1 * n).mirror().withRelativeBrightness(getSelectedBrightness(mMultiZoneLight));
				}
			};
		case FORWARD:
		case BACKWARD:
		default:
			final int sgn2 = getDirection() == Direction.BACKWARD ? -1 : 1;
			return new MultiZoneLight.AnimationDefinition() {
				@Override
				public int getDuration(final int n) {
					return Math.abs(mDuration) / mMultiZoneLight.getZoneCount();
				}

				@Override
				public MultizoneColors getColors(final int n) {
					return stretchColors.shift(sgn2 * n).withRelativeBrightness(getSelectedBrightness(mMultiZoneLight));
				}
			};
		}
	}

	@Override
	protected final boolean hasNativeImplementation(final Light light) {
		return ((MultiZoneLight) light).hasExtendedApi() && mStretch == 1
				&& (getDirection() == Direction.FORWARD || getDirection() == Direction.BACKWARD);
	}

	@Override
	protected final NativeAnimationDefinition getNativeAnimationDefinition(final Light light) {
		MultiZoneLight multiZoneLight = (MultiZoneLight) light;
		return new NativeAnimationDefinition() {
			@Override
			public void startAnimation() throws IOException {
				multiZoneLight.setColors(mColors.withRelativeBrightness(getSelectedBrightness(light)), 0, false);
				multiZoneLight.setEffect(new Move(Math.abs(getDuration()), getDirection() == Direction.BACKWARD));
			}

			@Override
			public void stopAnimation() throws IOException {
				multiZoneLight.setEffect(MultizoneEffectInfo.OFF);
			}
		};
	}

	@Override
	public final boolean isValid() {
		return mColors != null;
	}

	/**
	 * The direction of the animation.
	 */
	public enum Direction {
		/**
		 * Forward movement.
		 */
		FORWARD,
		/**
		 * Backward movement.
		 */
		BACKWARD,
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
			return FORWARD;
		}
	}
}
