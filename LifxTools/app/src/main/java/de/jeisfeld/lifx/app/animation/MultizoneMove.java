package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.io.IOException;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.animation.MultizoneMoveDefinition;
import de.jeisfeld.lifx.lan.animation.MultizoneMoveDefinition.Direction;
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
	 * Flag indicating if the animation is native and is already running.
	 */
	private final boolean mIsRunning;

	/**
	 * Constructor.
	 *
	 * @param duration the duration of the move.
	 * @param stretch the stretch factor.
	 * @param direction the direction of the move.
	 * @param colors the initial colors.
	 * @param isRunning the running flag.
	 */
	public MultizoneMove(final int duration, final double stretch, final Direction direction, final MultizoneColors colors, final boolean isRunning) {
		mDuration = duration;
		mStretch = stretch;
		mDirection = direction;
		mColors = colors;
		mIsRunning = isRunning;
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
		serviceIntent.putExtra(EXTRA_ANIMATION_IS_RUNNING, mIsRunning);
	}

	@Override
	public final void store(final int colorId) {
		super.store(colorId);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, mDuration);
		PreferenceUtil.setIndexedSharedPreferenceDouble(R.string.key_animation_stretch, colorId, mStretch);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_direction, colorId, mDirection.ordinal());
		StoredMultizoneColors.storeMultizoneColors(colorId, mColors);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.MULTIZONE_MOVE;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
		return new MultizoneMoveDefinition((MultiZoneLight) light, mDuration, mStretch, mDirection, mColors) {
			@Override
			protected double getSelectedBrightness() {
				return AnimationData.getSelectedBrightness(light);
			}
		};
	}

	@Override
	public final Drawable getBaseButtonDrawable(final Context context, final Light light, final double relativeBrightness) {
		return StoredMultizoneColors.getButtonDrawable(context, mColors.withRelativeBrightness(relativeBrightness),
				(int) light.getParameter(DeviceRegistry.DEVICE_ID));
	}

	@Override
	protected final boolean hasNativeImplementation(final Light light) {
		return ((MultiZoneLight) light).hasExtendedApi() && mStretch == 1
				&& (getDirection() == Direction.FORWARD || getDirection() == Direction.BACKWARD);
	}

	@Override
	public final boolean isRunning() {
		return mIsRunning;
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
}
