package de.jeisfeld.lifx.app.animation;

import android.content.Intent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.lan.type.TileEffectInfo.Morph;
import de.jeisfeld.lifx.lan.type.TileEffectType;

/**
 * Animation data for flames in a tile chain.
 */
public class TileChainMorph extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The duration of the flame.
	 */
	private final int mDuration;
	/**
	 * The colors to be used.
	 */
	private final ArrayList<Color> mColors;
	/**
	 * Flag indicating if the animation is native and is already running.
	 */
	private final boolean mIsRunning;

	/**
	 * Constructor.
	 *
	 * @param duration  the duration of the flane.
	 * @param colors    the colors to be used.
	 * @param isRunning the running flag.
	 */
	public TileChainMorph(final int duration, final List<Color> colors, final boolean isRunning) {
		mDuration = duration;
		mColors = new ArrayList<>(colors);
		mIsRunning = isRunning;
	}

	@Override
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_COLOR_LIST, mColors);
		serviceIntent.putExtra(EXTRA_ANIMATION_IS_RUNNING, mIsRunning);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_MORPH;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
		// Return dummy, as this is native effect.
		return new TileChain.AnimationDefinition() {
			@Override
			public TileChainColors getColors(final int n) {
				return null;
			}

			@Override
			public int getDuration(final int n) {
				return 0;
			}
		};
	}

	@Override
	protected final boolean hasNativeImplementation(final Light light) {
		return true;
	}

	@Override
	public final boolean isValid() {
		return mDuration > 0 && mColors.size() > 0;
	}

	/**
	 * Get the duration of the move.
	 *
	 * @return the duration
	 */
	public final int getDuration() {
		return mDuration;
	}

	@Override
	public final boolean isRunning() {
		return mIsRunning;
	}

	@Override
	protected final NativeAnimationDefinition getNativeAnimationDefinition(final Light light) {
		TileChain tileChain = (TileChain) light;
		return new NativeAnimationDefinition() {
			@Override
			public void startAnimation() throws IOException {
				tileChain.setEffect(new Morph(mDuration, mColors.toArray(new Color[0])));
			}

			@Override
			public void stopAnimation() throws IOException {
				TileEffectInfo effectInfo = tileChain.getEffectInfo();
				if (effectInfo == null || TileEffectType.MORPH == effectInfo.getType()) {
					tileChain.setEffect(TileEffectInfo.OFF);
				}
			}
		};
	}
}
