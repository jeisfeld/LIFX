package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileEffectCloudsType;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.lan.type.TileEffectInfo.Clouds;
import de.jeisfeld.lifx.lan.type.TileEffectType;

/**
 * Animation data for clouds in a tile chain.
 */
public class TileChainClouds extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The duration of the effect.
	 */
	private final int mDuration;
	/**
	 * The cloud saturation.
	 */
	private final int mCloudSaturation;
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
	 * @param duration        the duration of the effect.
	 * @param cloudSaturation the cloud saturation.
	 * @param colors          the colors to be used.
	 * @param isRunning       the running flag.
	 */
	public TileChainClouds(final int duration, final int cloudSaturation, final List<Color> colors, final boolean isRunning) {
		mDuration = duration;
		mCloudSaturation = cloudSaturation;
		mColors = new ArrayList<>(colors);
		mIsRunning = isRunning;
	}

	@Override
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_ANIMATION_CLOUD_SATURATION, mCloudSaturation);
		serviceIntent.putExtra(EXTRA_COLOR_LIST, mColors);
		serviceIntent.putExtra(EXTRA_ANIMATION_IS_RUNNING, mIsRunning);
	}

	@Override
	public final void store(final int colorId) {
		super.store(colorId);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, mDuration);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_cloud_saturation, colorId, mCloudSaturation);
		PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId, mColors);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_CLOUDS;
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
		return mDuration > 0 && !mColors.isEmpty();
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
				tileChain.setEffect(new Clouds(mDuration, TileEffectCloudsType.CLOUDS, (byte) mCloudSaturation, mColors.toArray(new Color[0])));
			}

			@Override
			public void stopAnimation() throws IOException {
				TileEffectInfo effectInfo = tileChain.getEffectInfo();
				if (effectInfo == null || TileEffectType.CLOUDS == effectInfo.getType()) {
					tileChain.setEffect(TileEffectInfo.OFF);
				}
			}
		};
	}

	@Override
	public final Drawable getBaseButtonDrawable(final Context context, final Light light, final double relativeBrightness) {
		int size = (int) context.getResources().getDimension(R.dimen.power_button_size);
		int strokeSize = (int) context.getResources().getDimension(R.dimen.power_button_stroke_size);

		Color baseColor = mColors.get(0).withRelativeBrightness(relativeBrightness);

		GradientDrawable base = new GradientDrawable();
		base.setShape(GradientDrawable.RECTANGLE);
		base.setColor(ColorUtil.toAndroidDisplayColor(baseColor));
		base.setStroke(strokeSize, android.graphics.Color.BLACK);
		base.setSize(size, size);

		Color cloudColor = baseColor.add(Color.COLD_WHITE, relativeBrightness / 2 + 0.2);
		GradientDrawable cloud1 = new GradientDrawable();
		cloud1.setShape(GradientDrawable.OVAL);
		cloud1.setColor(ColorUtil.toAndroidDisplayColor(cloudColor));

		GradientDrawable cloud2 = new GradientDrawable();
		cloud2.setShape(GradientDrawable.OVAL);
		cloud2.setColor(ColorUtil.toAndroidDisplayColor(cloudColor));

		LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{base, cloud1, cloud2});
		layerDrawable.setLayerInset(1, size / 8, size / 4, size / 2, size / 2);
		layerDrawable.setLayerInset(2, size / 2, size / 2, size / 6, size / 6);

		return layerDrawable;
	}
}
