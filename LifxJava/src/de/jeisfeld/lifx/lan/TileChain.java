package de.jeisfeld.lifx.lan;

import static de.jeisfeld.lifx.lan.util.TypeUtil.INDENT;

import java.io.IOException;
import java.util.List;

import de.jeisfeld.lifx.lan.message.TileGetDeviceChain;
import de.jeisfeld.lifx.lan.message.TileGetTileEffect;
import de.jeisfeld.lifx.lan.message.TileGetTileState64;
import de.jeisfeld.lifx.lan.message.TileSetTileEffect;
import de.jeisfeld.lifx.lan.message.TileSetTileState64;
import de.jeisfeld.lifx.lan.message.TileSetUserPosition;
import de.jeisfeld.lifx.lan.message.TileStateDeviceChain;
import de.jeisfeld.lifx.lan.message.TileStateTileEffect;
import de.jeisfeld.lifx.lan.message.TileStateTileState64;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.lan.type.TileInfo;
import de.jeisfeld.lifx.os.Logger;

/**
 * Class managing a LIFX Tile Chain.
 */
public class TileChain extends Light {
	/**
	 * The tile information.
	 */
	private final List<TileInfo> mTileInfo;

	/**
	 * Constructor.
	 *
	 * @param device The device which is a light.
	 */
	public TileChain(final Device device) {
		super(device);
		TileStateDeviceChain stateDeviceChain = getStateDeviceChain();
		mTileInfo = stateDeviceChain == null ? null : stateDeviceChain.getTileInfo();
	}

	@Override
	public final String getFullInformation() {
		StringBuilder result = new StringBuilder(super.getFullInformation());
		if (mTileInfo != null) {
			for (int i = 0; i < mTileInfo.size(); i++) {
				result.append(INDENT).append("TileInfo[").append(i).append("]: ").append(mTileInfo.get(i)).append("\n");
			}
			for (int i = 0; i < mTileInfo.size(); i++) {
				result.append(INDENT).append("Colors[").append(i).append("]: ").append(getColors((byte) i)).append("\n");
			}
		}
		result.append(INDENT).append("Tile Effect: ").append(getEffectInfo());
		return result.toString();
	}

	/**
	 * Get the tile state.
	 *
	 * @return the tile state.
	 */
	private TileStateDeviceChain getStateDeviceChain() {
		try {
			return (TileStateDeviceChain) getConnection().requestWithResponse(new TileGetDeviceChain());
		}
		catch (IOException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Set the user position of one tile.
	 *
	 * @param tileIndex The tile index.
	 * @param userX The x position of the tile.
	 * @param userY The y position of the tile.
	 * @throws IOException Connection issues
	 */
	public final void setUserPosition(final byte tileIndex, final float userX, final float userY) throws IOException {
		getConnection().requestWithResponse(new TileSetUserPosition(tileIndex, userX, userY));
	}

	/**
	 * Get the colors for one tile.
	 *
	 * @param tileIndex The tile index.
	 * @return the colors of that tile.
	 */
	public final List<Color> getColors(final byte tileIndex) {
		try {
			return ((TileStateTileState64) getConnection().requestWithResponse(
					new TileGetTileState64(tileIndex, (byte) 1, (byte) 0, (byte) 0, mTileInfo.get(tileIndex).getWidth()))).getColors();
		}
		catch (IOException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the effect info.
	 *
	 * @return The effect info.
	 */
	public final TileEffectInfo getEffectInfo() {
		try {
			return ((TileStateTileEffect) getConnection().requestWithResponse(new TileGetTileEffect())).getEffectInfo();
		}
		catch (IOException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Set the colors for one tile.
	 *
	 * @param tileIndex The tile index.
	 * @param duration The duration of the color change.
	 * @param colors the colors to be set.
	 * @throws IOException Connection issues
	 */
	public final void setColors(final byte tileIndex, final int duration, final List<Color> colors) throws IOException {
		getConnection().requestWithResponse(
				new TileSetTileState64(tileIndex, (byte) 1, (byte) 0, (byte) 0, mTileInfo.get(tileIndex).getWidth(), duration, colors));
	}

	/**
	 * Set the colors for one tile.
	 *
	 * @param tileIndex The tile index.
	 * @param duration The duration of the color change.
	 * @param colors the colors to be set.
	 * @throws IOException Connection issues
	 */
	public final void setColors(final byte tileIndex, final int duration, final TileColors colors) throws IOException {
		setColors(tileIndex, duration, colors.asList());
	}

	/**
	 * Set the tile effect.
	 *
	 * @param effectInfo The effect info.
	 * @param duration the duration of the effect in milliseconds
	 * @throws IOException Connection issues
	 */
	public final void setEffect(final TileEffectInfo effectInfo, final long duration) throws IOException {
		getConnection().requestWithResponse(new TileSetTileEffect(effectInfo, 1000000 * duration)); // MAGIC_NUMBER
	}

	/**
	 * Set the tile effect.
	 *
	 * @param effectInfo The effect info.
	 * @throws IOException Connection issues
	 */
	public final void setEffect(final TileEffectInfo effectInfo) throws IOException {
		getConnection().requestWithResponse(new TileSetTileEffect(effectInfo, 0));
	}

}
