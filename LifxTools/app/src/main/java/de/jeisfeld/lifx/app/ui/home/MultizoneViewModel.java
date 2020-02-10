package de.jeisfeld.lifx.app.ui.home;

import android.content.Context;
import de.jeisfeld.lifx.lan.MultiZoneLight;

/**
 * Class holding data for the display view of a multizone light.
 */
public class MultizoneViewModel extends LightViewModel {
	/**
	 * Constructor.
	 *
	 * @param context the context.
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
}
