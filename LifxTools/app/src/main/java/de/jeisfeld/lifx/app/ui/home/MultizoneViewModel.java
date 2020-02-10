package de.jeisfeld.lifx.app.ui.home;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.service.LifxAnimationService;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.os.Logger;

/**
 * Class holding data for the display view of a multizone light.
 */
public class MultizoneViewModel extends LightViewModel {
	/**
	 * Constructor.
	 *
	 * @param context        the context.
	 * @param multiZoneLight The multiZone light.
	 */
	public MultizoneViewModel(final Context context, final MultiZoneLight multiZoneLight) {
		super(context, multiZoneLight);
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
			Intent serviceIntent = new Intent(getContext(), LifxAnimationService.class);
			serviceIntent.putExtra(LifxAnimationService.EXTRA_NOTIFICATION_TEXT,
					getContext().getString(R.string.notification_text_multizone_animation_running));
			serviceIntent.putExtra(LifxAnimationService.EXTRA_DEVICE_MAC, getLight().getTargetAddress());
			ContextCompat.startForegroundService(getContext(), serviceIntent);
		}
		else {
			LifxAnimationService.stopAnimationForMac(getContext(), getLight().getTargetAddress());
		}
	}
}