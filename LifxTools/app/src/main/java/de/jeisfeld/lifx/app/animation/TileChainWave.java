package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.animation.TileChainWaveDefinition;
import de.jeisfeld.lifx.lan.animation.TileChainWaveDefinition.Direction;
import de.jeisfeld.lifx.lan.animation.TileChainWaveDefinition.Form;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Animation data for moving colors on a multizone device.
 */
public class TileChainWave extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

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
	 * @param duration the duration of the wave.
	 * @param radius the radius of the wave.
	 * @param direction the direction of the wave.
	 * @param form the form of the wave.
	 * @param colors The colors to be used (interpolated cyclically).
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
	public final void store(final int colorId) {
		super.store(colorId);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, mDuration);
		PreferenceUtil.setIndexedSharedPreferenceDouble(R.string.key_animation_radius, colorId, mRadius);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_direction, colorId, mDirection.ordinal());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_form, colorId, mForm.ordinal());
		PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId, mColors);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_WAVE;
	}

	@Override
	public final AnimationDefinition getAnimationDefinition(final Light light) {
		return new TileChainWaveDefinition((TileChain) light, mDuration, mRadius, mDirection, mForm, mColors) {
			@Override
			protected double getSelectedBrightness() {
				return AnimationData.getSelectedBrightness(light);
			}
		};
	}

	@Override
	public final Drawable getBaseButtonDrawable(final Context context, final Light light, final double relativeBrightness) {
		TileChainWaveDefinition animationDefinition = (TileChainWaveDefinition) getAnimationDefinition(light);
		return StoredTileColors.getTileChainDrawable((TileChain) light,
				animationDefinition.getColors(0).withMinBrightness((short) -1).withRelativeBrightness(relativeBrightness));
	}
}
