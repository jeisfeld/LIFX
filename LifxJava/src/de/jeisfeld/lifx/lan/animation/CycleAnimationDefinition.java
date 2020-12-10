package de.jeisfeld.lifx.lan.animation;

import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * A thread running a cycle of colors.
 */
public final class CycleAnimationDefinition implements AnimationDefinition {
	/**
	 * The duration of a cycle step in millis.
	 */
	private final int mStepDuration;
	/**
	 * The colors of the cycle.
	 */
	private final Color[] mColors;
	/**
	 * The number of cycles. Value 0 runs eternally.
	 */
	private int mCycleCount = 0;

	/**
	 * Create a cycle thread.
	 *
	 * @param stepDuration The duration of an animation step.
	 * @param cycleCount The number of cycles. Value 0 runs eternally.
	 * @param colors The colors of the cycle.
	 */
	public CycleAnimationDefinition(final int stepDuration, final int cycleCount, final Color... colors) {
		mStepDuration = stepDuration;
		mCycleCount = cycleCount;
		mColors = colors;
	}

	@Override
	public int getDuration(final int n) {
		return mStepDuration;
	}

	@Override
	public Color getColor(final int n) {
		if (mColors.length == 0 || (mCycleCount > 0 && n > (mCycleCount == 1 ? mColors.length - 1 : mCycleCount * mColors.length))) {
			return null;
		}
		else {
			return mColors[n % mColors.length];
		}
	}

}
