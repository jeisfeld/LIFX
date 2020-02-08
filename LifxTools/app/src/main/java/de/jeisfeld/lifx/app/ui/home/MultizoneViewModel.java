package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;

import de.jeisfeld.lifx.lan.Light.ExceptionCallback;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;

/**
 * Class holding data for the display view of a multizone light.
 */
public class MultizoneViewModel extends LightViewModel {
	/**
	 * Constructor.
	 *
	 * @param multiZoneLight The multiZone light.
	 */
	public MultizoneViewModel(final MultiZoneLight multiZoneLight) {
		super(multiZoneLight);
	}

	/**
	 * Get the light.
	 *
	 * @return The light.
	 */
	private MultiZoneLight getLight() {
		return (MultiZoneLight) getDevice();
	}

	/**
	 * Switch the animation on or off.
	 *
	 * @param status true to switch on, false to switch off.
	 */
	protected void updateAnimation(final boolean status) {
		mAnimationStatus.setValue(status);
		if (status) {
			getLight().rollingAnimation(10000, // MAGIC_NUMBER
					new MultizoneColors.Interpolated(true, Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE))
					.setBrightness(0.3) // MAGIC_NUMBER
					.setExceptionCallback(new ExceptionCallback() {
						@Override
						public void onException(final IOException e) {
							getLight().endAnimation();
							mAnimationStatus.postValue(false);
						}
					})
					.start();
		}
		else {
			getLight().endAnimation();
		}
	}

}
