package de.jeisfeld.lifx.app.animation;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import android.content.Intent;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
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
	 * Key for the animation color regex within the intent.
	 */
	protected static final String EXTRA_ANIMATION_COLOR_REGEX = "de.jeisfeld.lifx.ANIMATION_COLOR_REGEX";
	/**
	 * Key for the animation color regex within the intent.
	 */
	protected static final String EXTRA_ANIMATION_ADJUST_BRIGHTNESS = "de.jeisfeld.lifx.ANIMATION_ADJUST_BRIGHTNESS";
	/**
	 * Key for the multizone colors within the intent.
	 */
	protected static final String EXTRA_MULTIZONE_COLORS = "de.jeisfeld.lifx.MULTIZONE_COLORS";
	/**
	 * Key for a list of colors within the intent.
	 */
	protected static final String EXTRA_COLOR_LIST = "de.jeisfeld.lifx.MULTIZONE_COLOR_LIST";

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
	 * @return false if invalie.
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
	protected static double getSelectedBrightness(final Light light) {
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
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, 10000); // MAGIC_NUMBER
			double stretch = intent.getDoubleExtra(EXTRA_ANIMATION_STRETCH, 1);
			final MultizoneMove.Direction multizoneDirection = (MultizoneMove.Direction) intent.getSerializableExtra(EXTRA_ANIMATION_DIRECTION);
			final MultizoneColors colors = (MultizoneColors) intent.getSerializableExtra(EXTRA_MULTIZONE_COLORS);
			return new MultizoneMove(duration, stretch, multizoneDirection, colors);
		case TILECHAIN_MOVE:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, 10000); // MAGIC_NUMBER
			double radius = intent.getDoubleExtra(EXTRA_ANIMATION_RADIUS, 10); // MAGIC_NUMBER
			final TileChainMove.Direction tilechainDirection = (TileChainMove.Direction) intent.getSerializableExtra(EXTRA_ANIMATION_DIRECTION);
			@SuppressWarnings("unchecked")
			final ArrayList<Color> tileColors = (ArrayList<Color>) intent.getSerializableExtra(EXTRA_COLOR_LIST);
			return new TileChainMove(duration, radius, tilechainDirection, tileColors);
		case TILECHAIN_IMAGE_TRANSITION:
			duration = intent.getIntExtra(EXTRA_ANIMATION_DURATION, 10000); // MAGIC_NUMBER
			String colorRegex = intent.getStringExtra(EXTRA_ANIMATION_COLOR_REGEX);
			boolean adjustBrightness = intent.getBooleanExtra(EXTRA_ANIMATION_ADJUST_BRIGHTNESS, true);
			return new TileChainImageTransition(duration, colorRegex, adjustBrightness);
		default:
			return null;
		}
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
		 * Tilechain move.
		 */
		TILECHAIN_MOVE,
		/**
		 * Tilechain image transition.
		 */
		TILECHAIN_IMAGE_TRANSITION;

		/**
		 * Get Direction from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The direction.
		 */
		protected static AnimationType fromOrdinal(final int ordinal) {
			for (AnimationType direction : values()) {
				if (ordinal == direction.ordinal()) {
					return direction;
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
