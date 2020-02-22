package de.jeisfeld.lifx.app.ui.home;

import android.content.Context;
import de.jeisfeld.lifx.lan.TileChain;

/**
 * Class holding data for the display view of a tile chain.
 */
public class TileViewModel extends LightViewModel {
	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param light The light.
	 */
	public TileViewModel(final Context context, final TileChain light) {
		super(context, light);
	}

	/**
	 * Get the light.
	 *
	 * @return The light.
	 */
	@Override
	protected TileChain getLight() {
		return (TileChain) getDevice();
	}
}
