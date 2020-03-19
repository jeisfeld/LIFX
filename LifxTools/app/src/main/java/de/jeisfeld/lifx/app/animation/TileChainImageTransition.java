package de.jeisfeld.lifx.app.animation;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import android.content.Intent;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Animation data for moving colors on a multizone device.
 */
public class TileChainImageTransition extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The duration of the move.
	 */
	private final int mDuration;
	/**
	 * The regex telling which colors to use.
	 */
	private final String mColorRegex;
	/**
	 * flag telling if brightness should be adjusted.
	 */
	private final boolean mAdjustBrightness;

	/**
	 * Constructor.
	 *
	 * @param duration the duration of the move.
	 * @param colorRegex the regex telling which colors to use.
	 * @param adjustBrightness flag telling if brightness should be adjusted.
	 */
	public TileChainImageTransition(final int duration, final String colorRegex, final boolean adjustBrightness) {
		mDuration = duration;
		mColorRegex = colorRegex;
		mAdjustBrightness = adjustBrightness;
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
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mDuration);
		serviceIntent.putExtra(EXTRA_ANIMATION_COLOR_REGEX, mColorRegex);
		serviceIntent.putExtra(EXTRA_ANIMATION_ADJUST_BRIGHTNESS, mAdjustBrightness);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.TILECHAIN_IMAGE_TRANSITION;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
		final TileChain tileChain = (TileChain) light;
		final Integer deviceId = (Integer) light.getParameter(DeviceRegistry.DEVICE_ID);
		final Random random = new Random();
		final List<StoredColor> storedColorsList = ColorRegistry.getInstance().getStoredColors(deviceId).stream()
				.filter(storedColor -> storedColor.getName().matches(mColorRegex)).collect(Collectors.toList());

		return new TileChain.AnimationDefinition() {
			@Override
			public TileChainColors getColors(final int n) {
				if (storedColorsList.size() == 0) {
					return null;
				}
				else {
					StoredColor storedColor = storedColorsList.get(random.nextInt(storedColorsList.size()));
					TileChainColors colors = ((StoredTileColors) storedColor).getColors();
					if (mAdjustBrightness) {
						double maxBrightness = TypeUtil.toDouble((short) colors.getMaxBrightness(tileChain));
						if (maxBrightness == 0) {
							return TileChainColors.OFF;
						}
						return colors.withRelativeBrightness(getSelectedBrightness(tileChain) / maxBrightness).withMinBrightness((short) 1);
					}
					else {
						return colors.withRelativeBrightness(getSelectedBrightness(tileChain)).withMinBrightness((short) 1);
					}
				}
			}

			@Override
			public int getDuration(final int n) {
				return n == 0 ? 0 : mDuration;
			}
		};
	}
}
