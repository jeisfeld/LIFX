package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import java.io.IOException;

import androidx.core.content.ContextCompat;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.lan.type.TileEffectInfo.Flame;
import de.jeisfeld.lifx.lan.type.TileEffectType;

/**
 * Animation data for flames in a tile chain.
 */
public class TileChainFlame extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The duration of the flame.
	 */
	private final int mDuration;
	/**
	 * Flag indicating if the animation is native and is already running.
	 */
	private final boolean mIsRunning;

	/**
	 * Constructor.
	 *
	 * @param duration  the duration of the flane.
	 * @param isRunning the running flag.
	 */
	public TileChainFlame(final int duration, final boolean isRunning) {
		mDuration = duration;
		mIsRunning = isRunning;
	}

	@Override
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_ANIMATION_IS_RUNNING, mIsRunning);
	}

	@Override
	public final void store(final int colorId) {
		super.store(colorId);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, mDuration);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_FLAME;
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
		return mDuration > 0;
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
				tileChain.setEffect(new Flame(mDuration));
			}

			@Override
			public void stopAnimation() throws IOException {
				TileEffectInfo effectInfo = tileChain.getEffectInfo();
				if (effectInfo == null || TileEffectType.FLAME == effectInfo.getType()) {
					tileChain.setEffect(TileEffectInfo.OFF);
				}
			}
		};
	}

        @Override
        public final Drawable getBaseButtonDrawable(final Context context, final Light light, final double relativeBrightness) {
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.flame).mutate();
                float brightness = (float) relativeBrightness;
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setScale(brightness, brightness, brightness, 1f);
                drawable.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
                return drawable;
        }
}
