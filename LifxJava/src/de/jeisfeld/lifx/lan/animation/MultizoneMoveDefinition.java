package de.jeisfeld.lifx.lan.animation;

import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.MultiZoneLight.AnimationDefinition;
import de.jeisfeld.lifx.lan.type.MultizoneColors;

/**
 * Animation definition for moving colors on a multizone device.
 */
public class MultizoneMoveDefinition implements AnimationDefinition {
	/**
	 * The light.
	 */
	private final MultiZoneLight mLight;
	/**
	 * The duration of the move.
	 */
	private final int mDuration;
	/**
	 * The direction of the move.
	 */
	private final Direction mDirection;
	/**
	 * The initial colors.
	 */
	private final MultizoneColors mColors;
	/**
	 * A signum used within the animation.
	 */
	private final int mSgn;
	/**
	 * The selected brightness.
	 */
	private double mSelectedBrightness = 1;

	/**
	 * Constructor.
	 *
	 * @param light the multizone lignt.
	 * @param duration the duration of the move.
	 * @param stretch the stretch factor.
	 * @param direction the direction of the move.
	 * @param colors the initial colors.
	 */
	public MultizoneMoveDefinition(final MultiZoneLight light, final int duration, final double stretch, final Direction direction,
			final MultizoneColors colors) {
		mLight = light;
		mDuration = duration;
		mDirection = direction;
		mColors = colors.stretch(stretch);
		if (direction == Direction.INWARD || direction == Direction.BACKWARD) {
			mSgn = -1;
		}
		else {
			mSgn = 1;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param light the multizone lignt.
	 * @param duration the duration of the move.
	 * @param stretch the stretch factor.
	 * @param direction the direction of the move.
	 */
	public MultizoneMoveDefinition(final MultiZoneLight light, final int duration, final double stretch, final Direction direction) {
		this(light, duration, stretch, direction, new MultizoneColors.Exact(light.getColors()));
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
	public final int getDuration(final int n) {
		switch (mDirection) {
		case INWARD:
		case OUTWARD:
			return Math.abs(mDuration) / mLight.getZoneCount() * 2;
		case FORWARD:
		case BACKWARD:
		default:
			return Math.abs(mDuration) / mLight.getZoneCount();
		}
	}

	@Override
	public final MultizoneColors getColors(final int n) {
		switch (mDirection) {
		case INWARD:
		case OUTWARD:
			return mColors.shift(mSgn * n).mirror().withRelativeBrightness(getSelectedBrightness());
		case FORWARD:
		case BACKWARD:
		default:
			return mColors.shift(mSgn * n).withRelativeBrightness(getSelectedBrightness());
		}
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
		public static Direction fromOrdinal(final int ordinal) {
			for (Direction direction : values()) {
				if (ordinal == direction.ordinal()) {
					return direction;
				}
			}
			return FORWARD;
		}
	}
}
