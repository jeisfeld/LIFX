package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.Product;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.lan.type.TileInfo;
import de.jeisfeld.lifx.lan.type.Vendor;
import de.jeisfeld.lifx.lan.type.Waveform;
import de.jeisfeld.lifx.lan.util.TypeUtil;
import de.jeisfeld.lifx.os.Logger;

/**
 * Class managing a LIFX Tile Chain.
 */
public class TileChain extends Light implements Serializable {
	/**
	 * The default serializable version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The number of tiles.
	 */
	private byte mTileCount = 0;
	/**
	 * The start index.
	 */
	private byte mStartIndex = 0;
	/**
	 * The total width.
	 */
	private int mTotalWidth = 0;
	/**
	 * The total height.
	 */
	private int mTotalHeight = 0;
	/**
	 * The tile information.
	 */
	private List<TileInfo> mTileInfo = null;

	/**
	 * Constructor.
	 *
	 * @param device The device which is a light.
	 */
	public TileChain(final Device device) {
		super(device);
		refreshTileInfo();
	}

	/**
	 * Constructor including version information.
	 *
	 * @param targetAddress The target address.
	 * @param inetAddress   The internet address.
	 * @param port          The port.
	 * @param sourceId      The sourceId.
	 * @param vendor        The vendor.
	 * @param product       The product.
	 * @param version       The version.
	 * @param label         The label.
	 * @param tileCount     The number of tiles.
	 * @param totalWidth    The total width of the tile chain.
	 * @param totalHeight   The total height of the tile chain.
	 */
	public TileChain(final String targetAddress, final InetAddress inetAddress, final int port, final int sourceId, // SUPPRESS_CHECKSTYLE
					 final Vendor vendor, final Product product, final int version, final String label, final byte tileCount, // SUPPRESS_CHECKSTYLE
					 final int totalWidth, final int totalHeight, final List<TileInfo> tileInfoList) { // SUPPRESS_CHECKSTYLE
		super(targetAddress, inetAddress, port, sourceId, vendor, product, version, label);
		mTileCount = tileCount;
		mTotalWidth = totalWidth;
		mTotalHeight = totalHeight;
		mTileInfo = tileInfoList;
	}

	/**
	 * Refresh the tile info.
	 */
	public void refreshTileInfo() {
		TileStateDeviceChain stateDeviceChain = getStateDeviceChain();
		if (stateDeviceChain != null) {
			mTileInfo = stateDeviceChain.getTileInfo();
			mStartIndex = stateDeviceChain.getStartIndex();
			mTileCount = (byte) (mTileInfo.size());
			int totalWidth = 0;
			int totalHeight = 0;
			for (TileInfo tileInfo : mTileInfo) {
				totalWidth = Math.max(totalWidth, tileInfo.getMinX() + tileInfo.getWidth());
				totalHeight = Math.max(totalHeight, tileInfo.getMinY() + tileInfo.getHeight());
			}
			mTotalWidth = totalWidth;
			mTotalHeight = totalHeight;
		}
	}

	@Override
	public final String getFullInformation(final String indent, final boolean includeVolatileInfo) {
		StringBuilder result = new StringBuilder(super.getFullInformation(indent, includeVolatileInfo));
		result.append(indent).append("TileCount: ").append(TypeUtil.toUnsignedString(mTileCount)).append("\n");
		result.append(indent).append("TotalSize: (").append(mTotalWidth).append(",").append(mTotalHeight).append(")\n");
		if (mTileInfo != null) {
			result.append("\n");
			for (int i = 0; i < mTileInfo.size(); i++) {
				result.append(indent).append("TileInfo[").append(i).append("]: ").append(mTileInfo.get(i)).append("\n");
			}
			if (includeVolatileInfo) {
				result.append(indent).append("Colors: ").append(getColors()).append("\n");
			}
		}
		if (includeVolatileInfo) {
			result.append(indent).append("Tile Effect: ").append(getEffectInfo());
		}
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
			Logger.connectionError(this, "ChainState", e);
			return null;
		}
	}

	/**
	 * Get the tile info.
	 *
	 * @return The tile info.
	 */
	public List<TileInfo> getTileInfo() {
		return mTileInfo;
	}

	/**
	 * Get the tile count.
	 *
	 * @return The tile count.
	 */
	public byte getTileCount() {
		return mTileCount;
	}

	/**
	 * Get the index of the first tile.
	 *
	 * @return the index of the first tile.
	 */
	public byte getStartIndex() {
		return mStartIndex;
	}

	/**
	 * Get the total width of the tile chain.
	 *
	 * @return the total width of the tile chain.
	 */
	public int getTotalWidth() {
		return mTotalWidth;
	}

	/**
	 * Get the total height of the tile chain.
	 *
	 * @return the total height of the tile chain.
	 */
	public int getTotalHeight() {
		return mTotalHeight;
	}

	/**
	 * Set the user position of one tile.
	 *
	 * @param tileIndex The tile index.
	 * @param userX     The x position of the tile.
	 * @param userY     The y position of the tile.
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
	public final TileColors getColors(final byte tileIndex) {
		try {
			return new TileColors.Exact(((TileStateTileState64) getConnection().requestWithResponse(
					new TileGetTileState64((byte) (mStartIndex + tileIndex), (byte) 1, (byte) 0, (byte) 0, mTileInfo.get(tileIndex).getWidth())))
					.getColors(),
					mTileInfo.get(tileIndex).getWidth(), mTileInfo.get(tileIndex).getHeight());
		}
		catch (IOException e) {
			Logger.connectionError(this, "Colors" + tileIndex, e);
			return null;
		}
	}

	/**
	 * Get the colors of all tiles.
	 *
	 * @return The colors of all tiles.
	 */
	public final TileChainColors getColors() {
		try {
			TileColors[] colors = new TileColors[mTileCount];
			for (byte tileIndex = 0; tileIndex < mTileCount; tileIndex++) {
				colors[tileIndex] = new TileColors.Exact(((TileStateTileState64) getConnection().requestWithResponse(
						new TileGetTileState64((byte) (mStartIndex + tileIndex), (byte) 1, (byte) 0, (byte) 0, mTileInfo.get(tileIndex).getWidth())))
						.getColors(),
						mTileInfo.get(tileIndex).getWidth(), mTileInfo.get(tileIndex).getHeight());
			}
			return new TileChainColors.PerTile(this, colors);
		}
		catch (IOException e) {
			Logger.connectionError(this, "Colors", e);
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
			Logger.connectionError(this, "EffectInfo", e);
			return null;
		}
	}

	/**
	 * Set the colors for one tile.
	 *
	 * @param tileIndex The tile index.
	 * @param duration  The duration of the color change.
	 * @param colors    the colors to be set.
	 * @throws IOException Connection issues
	 */
	private void setColors(final byte tileIndex, final int duration, final List<Color> colors) throws IOException {
		getConnection().requestWithResponse(
				new TileSetTileState64(tileIndex, (byte) 1, (byte) 0, (byte) 0, mTileInfo.get(tileIndex).getWidth(), duration, colors));
	}

	/**
	 * Set the colors for one tile.
	 *
	 * @param tileIndex The tile index.
	 * @param duration  The duration of the color change.
	 * @param colors    the colors to be set.
	 * @throws IOException Connection issues
	 */
	public final void setColors(final byte tileIndex, final int duration, final TileColors colors) throws IOException {
		setColors(tileIndex, duration, colors.asList(mTileInfo.get(tileIndex).getWidth(), mTileInfo.get(tileIndex).getHeight()));
	}

	/**
	 * Set the colors for all tiles.
	 *
	 * @param colors   the colors to be set.
	 * @param duration The duration of the color change.
	 * @param wait     flag indicating if the method should return only after the final color is reached.
	 * @throws IOException Connection issues
	 */
	public final void setColors(final TileChainColors colors, final int duration, final boolean wait) throws IOException {
		for (byte tileIndex = 0; tileIndex < mTileCount; tileIndex++) {
			TileInfo tileInfo = mTileInfo.get(tileIndex);
			setColors(tileIndex, duration,
					colors.getTileColors(tileInfo.getWidth(), tileInfo.getHeight(), tileInfo.getMinX(), tileInfo.getMinY(),
							tileInfo.getRotation(), mTotalWidth, mTotalHeight));
			if (wait) {
				try {
					Thread.sleep(duration);
				}
				catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Set the colors for a subset of tiles.
	 *
	 * @param colors   the colors to be set. May have null entries.
	 * @param duration The duration of the color change.
	 * @param wait     flag indicating if the method should return only after the final color is reached.
	 * @throws IOException Connection issues
	 */
	public final void setColorsOptional(final TileChainColors colors, final int duration, final boolean wait) throws IOException {
		final TileChainColors oldColors = getColors();
		if (oldColors != null) {
			setColors(new TileChainColors() {
				/**
				 * The default serializable version id.
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Color getColor(final int x, final int y, final int width, final int height) {
					Color newColor = colors.getColor(x, y, mTotalWidth, mTotalHeight);
					return newColor == null ? oldColors.getColor(x, y, mTotalWidth, mTotalHeight) : newColor;
				}
			}, duration, wait);
		}
	}

	/**
	 * Set the tile effect.
	 *
	 * @param effectInfo The effect info.
	 * @param duration   the duration of the effect in milliseconds
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
		setEffect(effectInfo, 0);
	}

	@Override
	public final TileChain.AnimationThread animation(final Light.AnimationDefinition definition) {
		return new TileChain.AnimationThread(this, (TileChain.AnimationDefinition) definition);
	}

	/**
	 * A thread animating the colors.
	 */
	public static class AnimationThread extends Light.AnimationThread { // SUPPRESS_CHECKSTYLE
		/**
		 * The animation definition.
		 */
		private final TileChain.AnimationDefinition mDefinition;
		/**
		 * The colors to be reached after ending the thread.
		 */
		private TileChainColors mEndColors = null;
		/**
		 * The transition time to the end color.
		 */
		private int mEndTransitionTime = 200; // MAGIC_NUMBER

		/**
		 * Create an animation thread.
		 *
		 * @param light      the tile chain light.
		 * @param definition The rules for the animation.
		 */
		private AnimationThread(final TileChain light, final TileChain.AnimationDefinition definition) {
			super(light, definition);
			mDefinition = definition;
		}

		@Override
		protected TileChain getLight() {
			return (TileChain) super.getLight();
		}

		/**
		 * Set the color that the lamp should get after finishing the cycle.
		 *
		 * @param endColors         The end colors. List of length 0 turns power off. Null keeps the current color.
		 * @param endTransitionTime The transition time to the end color.
		 * @return The updated animation thread.
		 */
		public TileChain.AnimationThread setEndColors(final TileChainColors endColors, final int endTransitionTime) {
			mEndColors = endColors;
			mEndTransitionTime = Math.max(endTransitionTime, 0);
			return this;
		}

		@Override
		public void run() {
			int count = 0;
			try {
				storeDeviceRegistry();
				if (mDefinition.waitForPreviousAnimationEnd()) {
					waitForPreviousAnimationEnd();
				}
				boolean isInterrupted = false;
				boolean isPowerChange = false;
				try {
					while (!isInterrupted() && mDefinition.getColors(count) != null) {
						Date givenStartTime = mDefinition.getStartTime(count);
						final long startTime = givenStartTime == null ? System.currentTimeMillis() : givenStartTime.getTime();
						if (givenStartTime != null) {
							long waitTime = givenStartTime.getTime() - System.currentTimeMillis();
							if (waitTime > 0) {
								Thread.sleep(waitTime);
							}
						}

						int errorCount = 0;
						boolean success = false;
						int duration = Math.max(mDefinition.getDuration(count), 0);
						Power power;
						boolean wasOff = count == 0
								? ((power = getLight().getPower()) != null && power.isOff()) // SUPPRESS_CHECKSTYLE
								: mDefinition.getColors(count - 1).isOff();
						while (!success) {
							try { // SUPPRESS_CHECKSTYLE
								TileChainColors colors = mDefinition.getColors(count).withRelativeBrightness(getRelativeBrightness());
								if (wasOff) {
									getLight().setColors(colors, 0, false);
									getLight().setPower(true, duration, false);
									isPowerChange = true;
								}
								else if (colors.isOff()) {
									getLight().setPower(false, duration, false);
									isPowerChange = true;
								}
								else {
									getLight().setColors(colors, duration, false);
									isPowerChange = false;
								}
								success = true;
							}
							catch (IOException e) {
								errorCount++;
								if (errorCount >= WAITING_TIMES_AFTER_ERROR.length) {
									throw e;
								}
								Thread.sleep(WAITING_TIMES_AFTER_ERROR[errorCount]);
							}
						}
						Thread.sleep(Math.max(0, duration + startTime - System.currentTimeMillis()));
						count++;
					}
				}
				catch (InterruptedException e) {
					isInterrupted = true;
				}

				if (mEndColors == null) {
					if (isInterrupted) {
						if (isPowerChange) {
							try {
								TileChainColors colors = Objects.requireNonNull(getLight().getColors())
										.withRelativeBrightness(TypeUtil.toDouble(getLight().getPower().getLevel()));
								getLight().setColors(colors, 200, false); // MAGIC_NUMBER
								getLight().setPower(true, 200, false); // MAGIC_NUMBER
							}
							catch (NullPointerException e) {
								// ignore
							}
						}
						else {
							// stop the previous color transition by sending setWaveform command with no change.
							getLight().setWaveform(false, null, null, null, null, 0, 0, 0, Waveform.PULSE, false);
						}
					}
				}
				else if (mEndColors == TileChainColors.OFF) {
					getLight().setPower(false, mEndTransitionTime, true);
				}
				else {
					getLight().setColors(mEndColors, mEndTransitionTime, true);
				}
				if (getAnimationCallback() != null) {
					getAnimationCallback().onAnimationEnd(isInterrupted);
				}
			}
			catch (IOException e) {
				Logger.connectionError(getLight(), "Animation", e);
				if (getAnimationCallback() != null) {
					getAnimationCallback().onException(e);
				}
			}
			cleanAnimationThread(this);
		}
	}

	/**
	 * Interface for defining an animation.
	 */
	public interface AnimationDefinition extends Light.AnimationDefinition {
		/**
		 * The n-th color list of the animation.
		 *
		 * @param n counter starting with 0
		 * @return The n-th color list. Null will end the animation.
		 */
		TileChainColors getColors(int n);

		@Override
		default Color getColor(final int n) {
			return null;
		}
	}

}
