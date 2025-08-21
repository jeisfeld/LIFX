package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.animation.MultizoneMoveDefinition;
import de.jeisfeld.lifx.lan.animation.TileChainWaveDefinition;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;

/**
 * Class holding data representing an animation.
 */
public abstract class AnimationData implements Serializable {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Key for the animation type within the intent.
	 */
	private static final String EXTRA_ANIMATION_TYPE = "de.jeisfeld.lifx.ANIMATION_TYPE";
	/**
	 * Key for the animation duration within the intent.
	 */
	protected static final String EXTRA_ANIMATION_DURATION = "de.jeisfeld.lifx.ANIMATION_DURATION";
	/**
	 * Key for the animation radius within the intent.
	 */
	protected static final String EXTRA_ANIMATION_RADIUS = "de.jeisfeld.lifx.ANIMATION_RADIUS";
	/**
	 * Key for the animation stretch within the intent.
	 */
	protected static final String EXTRA_ANIMATION_STRETCH = "de.jeisfeld.lifx.ANIMATION_STRETCH";
	/**
	 * Key for the animation direction within the intent.
	 */
	protected static final String EXTRA_ANIMATION_DIRECTION = "de.jeisfeld.lifx.ANIMATION_DIRECTION";
	/**
	 * Key for the animation form within the intent.
	 */
	protected static final String EXTRA_ANIMATION_FORM = "de.jeisfeld.lifx.ANIMATION_FORM";
	/**
	 * Key for the animation color regex within the intent.
	 */
	protected static final String EXTRA_ANIMATION_COLOR_REGEX = "de.jeisfeld.lifx.ANIMATION_COLOR_REGEX";
	/**
	 * Key for the animation brightness adjustment flag within the intent.
	 */
	protected static final String EXTRA_ANIMATION_ADJUST_BRIGHTNESS = "de.jeisfeld.lifx.ANIMATION_ADJUST_BRIGHTNESS";
	/**
	 * Key for the running flag within the intent.
	 */
	protected static final String EXTRA_ANIMATION_IS_RUNNING = "de.jeisfeld.lifx.ANIMATION_IS_RUNNING";
	/**
	 * Key for the animation cloud saturation within the intent.
	 */
	protected static final String EXTRA_ANIMATION_CLOUD_SATURATION = "de.jeisfeld.lifx.ANIMATION_CLOUD_SATURATION";
	/**
	 * Key for the multizone colors within the intent.
	 */
	protected static final String EXTRA_MULTIZONE_COLORS = "de.jeisfeld.lifx.MULTIZONE_COLORS";
	/**
	 * Key for a list of colors within the intent.
	 */
	protected static final String EXTRA_COLOR_LIST = "de.jeisfeld.lifx.COLOR_LIST";
	/**
	 * The default duration.
	 */
	private static final int DEFAULT_DURATION = 10000;

	/**
	 * Get the animation type.
	 *
	 * @return The animation type.
	 */
	protected abstract AnimationType getType();

	/**
	 * Get the animation definition.
	 *
	 * @param light The light.
	 * @return The animation definition.
	 */
	protected abstract AnimationDefinition getAnimationDefinition(Light light);

	/**
	 * Get the basic drawable displayed as stored animation button.
	 *
	 * @param context            the context.
	 * @param light              the light.
	 * @param relativeBrightness the relative brightness.
	 * @return The drawable.
	 */
	public abstract Drawable getBaseButtonDrawable(Context context, Light light, double relativeBrightness);

	/**
	 * Get information if the animation is native and is already running.
	 *
	 * @return true if the animation is native and is already running.
	 */
	// OVERRIDABLE
	protected boolean isRunning() {
		return false;
	}

	/**
	 * Check if this animation has native implementation on the light.
	 *
	 * @param light The light.
	 * @return true if there is native implementation.
	 */
	// OVERRIDABLE
	protected boolean hasNativeImplementation(final Light light) {
		return false;
	}

	/**
	 * Check if this animation is valid.
	 *
	 * @return false if invalid.
	 */
	// OVERRIDABLE
	public boolean isValid() {
		return true;
	}

	/**
	 * Get the definition of the native implementation of this animation.
	 *
	 * @param light The light.
	 * @return The native definition.
	 */
	// OVERRIDABLE
	protected NativeAnimationDefinition getNativeAnimationDefinition(final Light light) {
		return new NativeAnimationDefinition() {
			@Override
			public void startAnimation() {
				// do nothing
			}

			@Override
			public void stopAnimation() {
				// do nothing
			}
		};
	}

	/**
	 * Get the selected brightness for a light.
	 *
	 * @param light The light.
	 * @return the selected brightness.
	 */
	public static double getSelectedBrightness(final Light light) {
		Integer deviceId = (Integer) light.getParameter(DeviceRegistry.DEVICE_ID);
		if (deviceId != null) {
			return PreferenceUtil.getIndexedSharedPreferenceDouble(R.string.key_device_selected_brightness, deviceId, 1);
		}
		else {
			return 1;
		}
	}

	/**
	 * Add the data to an intent.
	 *
	 * @param serviceIntent The intent.
	 */
	// OVERRIDABLE
	public void addToIntent(final Intent serviceIntent) {
		serviceIntent.putExtra(EXTRA_ANIMATION_TYPE, getType());
	}

	/**
	 * Restore animation data from service intent.
	 *
	 * @param intent The service intent.
	 * @return The animation data.
	 */
	public static AnimationData fromIntent(final Intent intent) {
		AnimationType animationType = (AnimationType) intent.getSerializableExtra(EXTRA_ANIMATION_TYPE);
		if (animationType == null) {
			return null;
		}

		final int duration;
		switch (animationType) {
		case MULTIZONE_MOVE:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, DEFAULT_DURATION);
			double stretch = intent.getDoubleExtra(EXTRA_ANIMATION_STRETCH, 1);
			MultizoneMoveDefinition.Direction multizoneDirection =
					(MultizoneMoveDefinition.Direction) intent.getSerializableExtra(EXTRA_ANIMATION_DIRECTION);
			MultizoneColors colors = (MultizoneColors) intent.getSerializableExtra(EXTRA_MULTIZONE_COLORS);
			boolean isRunning = intent.getBooleanExtra(EXTRA_ANIMATION_IS_RUNNING, false);
			return new MultizoneMove(duration, stretch, multizoneDirection, colors, isRunning);
		case TILECHAIN_WAVE:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, DEFAULT_DURATION);
			double radius = intent.getDoubleExtra(EXTRA_ANIMATION_RADIUS, 10); // MAGIC_NUMBER
			TileChainWaveDefinition.Direction tilechainDirection =
					(TileChainWaveDefinition.Direction) intent.getSerializableExtra(EXTRA_ANIMATION_DIRECTION);
			TileChainWaveDefinition.Form tilechainForm = (TileChainWaveDefinition.Form) intent.getSerializableExtra(EXTRA_ANIMATION_FORM);
			@SuppressWarnings("unchecked") ArrayList<Color> tileColors = (ArrayList<Color>) intent.getSerializableExtra(EXTRA_COLOR_LIST);
			return new TileChainWave(duration, radius, tilechainDirection, tilechainForm, tileColors);
		case TILECHAIN_IMAGE_TRANSITION:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, DEFAULT_DURATION);
			String colorRegex = intent.getStringExtra(EXTRA_ANIMATION_COLOR_REGEX);
			boolean adjustBrightness = intent.getBooleanExtra(EXTRA_ANIMATION_ADJUST_BRIGHTNESS, true);
			return new TileChainImageTransition(duration, colorRegex, adjustBrightness);
		case TILECHAIN_FLAME:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, DEFAULT_DURATION);
			return new TileChainFlame(duration, false);
		case TILECHAIN_MORPH:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, DEFAULT_DURATION);
			@SuppressWarnings("unchecked") final ArrayList<Color> tileColors2 = (ArrayList<Color>) intent.getSerializableExtra(EXTRA_COLOR_LIST);
			return new TileChainMorph(duration, tileColors2, false);
		case TILECHAIN_CLOUDS:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, DEFAULT_DURATION);
			final int cloudSaturation = intent.getIntExtra(EXTRA_ANIMATION_CLOUD_SATURATION, (byte) 50);
			@SuppressWarnings("unchecked") final ArrayList<Color> tileColors3 = (ArrayList<Color>) intent.getSerializableExtra(EXTRA_COLOR_LIST);
			return new TileChainClouds(duration, cloudSaturation, tileColors3, false);
		default:
			return null;
		}
	}

	/**
	 * Restore animation data from stored animation.
	 *
	 * @param colorId The stored color id.
	 * @return The animation data.
	 */
	public static AnimationData fromStoredAnimation(final int colorId) {
		AnimationType animationType = AnimationType.fromOrdinal(
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_type, colorId, 0));
		if (animationType == null) {
			return null;
		}

		final int duration;
		switch (animationType) {
		case MULTIZONE_MOVE:
			duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, DEFAULT_DURATION);
			double stretch = PreferenceUtil.getIndexedSharedPreferenceDouble(R.string.key_animation_stretch, colorId, 1);
			MultizoneMoveDefinition.Direction multizoneDirection = MultizoneMoveDefinition.Direction.fromOrdinal(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_direction, colorId, 0));
			MultizoneColors colors = new StoredMultizoneColors(colorId).getColors();
			boolean isRunning = false;
			return new MultizoneMove(duration, stretch, multizoneDirection, colors, isRunning);
		case TILECHAIN_WAVE:
			duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, DEFAULT_DURATION);
			double radius = PreferenceUtil.getIndexedSharedPreferenceDouble(R.string.key_animation_radius, colorId, 10); // MAGIC_NUMBER
			TileChainWaveDefinition.Direction tilechainDirection = TileChainWaveDefinition.Direction.fromOrdinal(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_direction, colorId, 0));
			TileChainWaveDefinition.Form tilechainForm = TileChainWaveDefinition.Form.fromOrdinal(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_form, colorId, 0));
			ArrayList<Color> tileColors = PreferenceUtil.getIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId);
			return new TileChainWave(duration, radius, tilechainDirection, tilechainForm, tileColors);
		case TILECHAIN_IMAGE_TRANSITION:
			duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, DEFAULT_DURATION);
			String colorRegex = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_animation_color_regex, colorId);
			boolean adjustBrightness = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_animation_adjust_brightness, colorId, false);
			return new TileChainImageTransition(duration, colorRegex, adjustBrightness);
		case TILECHAIN_FLAME:
			duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, DEFAULT_DURATION);
			return new TileChainFlame(duration, false);
		case TILECHAIN_MORPH:
			duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, DEFAULT_DURATION);
			ArrayList<Color> tileColors2 = PreferenceUtil.getIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId);
			return new TileChainMorph(duration, tileColors2, false);
		case TILECHAIN_CLOUDS:
			duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, DEFAULT_DURATION);
			final int cloudSaturation = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_animation_cloud_saturation, colorId, 50);
			ArrayList<Color> tileColors3 = PreferenceUtil.getIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId);
			return new TileChainClouds(duration, cloudSaturation, tileColors3, false);
		default:
			return null;
		}
	}

	/**
	 * Store animation data within stored color.
	 *
	 * @param colorId The stored color id.
	 */
	// OVERRIDABLE
	public void store(final int colorId) {
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_type, colorId, getType().ordinal());
	}

	/**
	 * The type of the animation.
	 */
	public enum AnimationType {
		/**
		 * No animation.
		 */
		NONE,
		/**
		 * Multizone move.
		 */
		MULTIZONE_MOVE,
		/**
		 * Tilechain wave.
		 */
		TILECHAIN_WAVE,
		/**
		 * Tilechain image transition.
		 */
		TILECHAIN_IMAGE_TRANSITION,
		/**
		 * Tilechain flame.
		 */
		TILECHAIN_FLAME,
		/**
		 * Tilechain morph.
		 */
		TILECHAIN_MORPH,
		/**
		 * Tilechain clouds.
		 */
		TILECHAIN_CLOUDS;

		/**
		 * Get Animation Type from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The animationType.
		 */
		private static AnimationType fromOrdinal(final int ordinal) {
			for (AnimationType animationType : values()) {
				if (ordinal == animationType.ordinal()) {
					return animationType;
				}
			}
			return AnimationType.NONE;
		}
	}

	/**
	 * A definition of animation supported natively on the device.
	 */
	public interface NativeAnimationDefinition {
		/**
		 * Start the animation.
		 *
		 * @throws IOException Connectivity issues.
		 */
		void startAnimation() throws IOException;

		/**
		 * Stop the animation.
		 *
		 * @throws IOException Connectivity issues.
		 */
		void stopAnimation() throws IOException;
	}
}
