package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.jeisfeld.lifx.lan.message.MultizoneGetColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneGetExtendedColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneGetMultizoneEffect;
import de.jeisfeld.lifx.lan.message.MultizoneSetColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneSetColorZones.Apply;
import de.jeisfeld.lifx.lan.message.MultizoneSetExtendedColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneSetMultizoneEffect;
import de.jeisfeld.lifx.lan.message.MultizoneStateExtendedColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneStateMultizoneEffect;
import de.jeisfeld.lifx.lan.message.MultizoneStateZone;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.Product;
import de.jeisfeld.lifx.lan.type.Vendor;
import de.jeisfeld.lifx.lan.type.Waveform;
import de.jeisfeld.lifx.lan.util.TypeUtil;
import de.jeisfeld.lifx.os.Logger;

import static de.jeisfeld.lifx.lan.util.TypeUtil.INDENT;

/**
 * Class managing a LIFX multizone light.
 */
public class MultiZoneLight extends Light {
	/**
	 * The default serializable version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The size of a bulk of colors.
	 */
	private static final int BULK_SIZE = 8;

	/**
	 * The count of colors zones.
	 */
	private final byte mZoneCount;

	/**
	 * Constructor.
	 *
	 * @param device The device which is a light.
	 */
	public MultiZoneLight(final Device device) {
		super(device);
		MultizoneStateZone stateZone = getMultizoneState((byte) 0, (byte) 0);
		mZoneCount = stateZone == null ? 0 : stateZone.getCount();
		getFirmwareBuildTime();
	}

	/**
	 * Constructor including version information.
	 *
	 * @param targetAddress The target address.
	 * @param inetAddress The internet address.
	 * @param port The port.
	 * @param sourceId The sourceId.
	 * @param vendor The vendor.
	 * @param product The product.
	 * @param version The version.
	 * @param label The label.
	 * @param zoneCount The number of zones.
	 * @param firmwareBuildTimeStamp The firmware build timestamp
	 */
	public MultiZoneLight(final String targetAddress, final InetAddress inetAddress, final int port, final int sourceId, // SUPPRESS_CHECKSTYLE
			final Vendor vendor, final Product product, final int version, final String label, final byte zoneCount,
			final long firmwareBuildTimeStamp) {
		super(targetAddress, inetAddress, port, sourceId, vendor, product, version, label);
		mZoneCount = zoneCount;
		setFirmwareBuildTime(firmwareBuildTimeStamp);
	}

	/**
	 * Check if the device has extended API.
	 *
	 * @return true if it has extended API.
	 */
	public boolean hasExtendedApi() {
		return getProduct().hasExtendedApi(getFirmwareBuildTime());
	}

	/**
	 * Get the light state.
	 *
	 * @param startIndex the start index.
	 * @param endIndex the end index.
	 * @return the light state.
	 */
	private MultizoneStateZone getMultizoneState(final byte startIndex, final byte endIndex) {
		try {
			return (MultizoneStateZone) getConnection().requestWithResponse(new MultizoneGetColorZones(startIndex, endIndex));
		}
		catch (IOException e) {
			Logger.connectionError(this, "State", e);
			return null;
		}
	}

	/**
	 * Get the light state via extended API. Works only with new devices and new Firmware.
	 *
	 * @return the light state.
	 */
	public List<Color> getMultizoneExtendedColors() {
		try {
			MultizoneStateExtendedColorZones multizoneExtendedColorZones =
					(MultizoneStateExtendedColorZones) getConnection().requestWithResponse(new MultizoneGetExtendedColorZones());
			if (multizoneExtendedColorZones == null) {
				return null;
			}
			else {
				return multizoneExtendedColorZones.getColors();
			}
		}
		catch (IOException e) {
			Logger.connectionError(this, "ExtendedColors", e);
			return null;
		}
	}

	/**
	 * Get the colors of a range of zones.
	 *
	 * @param startIndex The index of the start zone.
	 * @param endIndex The index of the end zone.
	 * @return The colors of these zones.
	 */
	public final List<Color> getColors(final byte startIndex, final byte endIndex) {
		int start = TypeUtil.toUnsignedInt(startIndex);
		int end = TypeUtil.toUnsignedInt(endIndex);
		List<Color> result = new ArrayList<>();
		for (int blockIndex = start / 8; blockIndex <= end / 8; blockIndex++) { // MAGIC_NUMBER
			MultizoneStateZone multizoneState =
					getMultizoneState((byte) Math.max(start, blockIndex * 8), (byte) Math.min(end, blockIndex * 8 + 7)); // MAGIC_NUMBER
			if (multizoneState == null) {
				return null;
			}
			else {
				result.addAll(multizoneState.getColors());
			}
		}
		return result;
	}

	/**
	 * Get the colors of all zones.
	 *
	 * @return The colors of all zones.
	 */
	public final List<Color> getColors() {
		if (hasExtendedApi()) {
			return getMultizoneExtendedColors();
		}
		else {
			return getColors((byte) 0, (byte) (getZoneCount() - 1));
		}
	}

	/**
	 * Get the effect info.
	 *
	 * @return The effect info.
	 */
	public final MultizoneEffectInfo getEffectInfo() {
		if (!hasExtendedApi()) {
			return null;
		}
		try {
			return ((MultizoneStateMultizoneEffect) getConnection().requestWithResponse(new MultizoneGetMultizoneEffect())).getEffectInfo();
		}
		catch (IOException e) {
			Logger.connectionError(this, "EffectInfo", e);
			return null;
		}
	}

	/**
	 * Set the color of a certain range.
	 *
	 * @param startIndex the start index.
	 * @param endIndex the end index.
	 * @param color the target color.
	 * @param duration the duration of power change in millis.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @param apply flag indicating if the change should apply.
	 * @throws IOException Connection issues
	 */
	public void setColor(final byte startIndex, final byte endIndex, final Color color, final int duration, final boolean wait, final boolean apply)
			throws IOException {
		getConnection().requestWithResponse(new MultizoneSetColorZones(startIndex, endIndex, color, duration, apply ? Apply.APPLY : Apply.NO_APPLY));
	}

	/**
	 * Set the colors of the multizone light.
	 *
	 * @param colors the target colors intermediate colors will be interpolated.
	 * @param duration the duration of power change in millis.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @throws IOException Connection issues
	 */
	public void setColors(final MultizoneColors colors, final int duration, final boolean wait) throws IOException {
		if (hasExtendedApi()) {
			getConnection().requestWithResponse(
					new MultizoneSetExtendedColorZones((byte) 0, duration, Apply.APPLY, colors.getColors(mZoneCount)));
		}
		else {
			for (int i = 0; i < mZoneCount; i++) {
				setColor((byte) i, (byte) i, colors.getColor(i, getZoneCount()), duration, false, i == mZoneCount - 1);
			}
		}
		if (wait) {
			try {
				Thread.sleep(duration);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

	@Override
	public final String getFullInformation() {
		StringBuilder result = new StringBuilder(super.getFullInformation());
		result.append(INDENT).append("Zone count: ").append(getZoneCount()).append("\n");
		if (hasExtendedApi()) {
			result.append(INDENT).append("Effect type: ").append(getEffectInfo()).append("\n");
		}
		List<Color> colors = getColors();
		for (int bulk = 0; bulk < colors.size() / BULK_SIZE; bulk++) {
			result.append(INDENT).append("Colors[").append(bulk).append("]: [");
			for (int i = 0; i < BULK_SIZE && bulk * BULK_SIZE + i < colors.size(); i++) {
				result.append(colors.get(bulk * BULK_SIZE + i).toString()).append(", ");
			}
			result.replace(result.length() - 2, result.length(), "\n");
		}
		return result.toString();
	}

	/**
	 * Get the number of zones.
	 *
	 * @return The number of zones.
	 */
	public byte getZoneCount() {
		return mZoneCount;
	}

	/**
	 * Set the multizone effect.
	 *
	 * @param effectInfo The effect info.
	 * @param duration the duration of the effect in milliseconds
	 * @throws IOException Connection issues
	 */
	public final void setEffect(final MultizoneEffectInfo effectInfo, final long duration) throws IOException {
		getConnection().requestWithResponse(new MultizoneSetMultizoneEffect(effectInfo, duration));
	}

	/**
	 * Set the multizone effect.
	 *
	 * @param effectInfo The effect info.
	 * @throws IOException Connection issues
	 */
	public final void setEffect(final MultizoneEffectInfo effectInfo) throws IOException {
		setEffect(effectInfo, 0);
	}

	@Override
	public final AnimationThread animation(final Light.AnimationDefinition definition) {
		return new AnimationThread(this, (MultiZoneLight.AnimationDefinition) definition);
	}

	/**
	 * Create an animation rolling colors cyclically along the stripe.
	 *
	 * @param duration The duration of one rolling cycle. Negative values roll backwards.
	 * @param colors The colors.
	 * @return The animation thread.
	 */
	public final AnimationThread rollingAnimation(final int duration, final MultizoneColors colors) {
		return animation(new AnimationDefinition() {
			/**
			 * The signum defining the rolling direction.
			 */
			private final int mSgn = (int) Math.signum(duration);

			@Override
			public int getDuration(final int n) {
				return Math.abs(duration) / getZoneCount();
			}

			@Override
			public MultizoneColors getColors(final int n) {
				return colors.shift(mSgn * n);
			}

		});
	}

	/**
	 * A thread animating the colors.
	 */
	public static class AnimationThread extends Light.AnimationThread { // SUPPRESS_CHECKSTYLE
		/**
		 * The animation definition.
		 */
		private final AnimationDefinition mDefinition;
		/**
		 * The colors to be reached after ending the thread.
		 */
		private MultizoneColors mEndColors = null;
		/**
		 * The transition time to the end color.
		 */
		private int mEndTransitionTime = 200; // MAGIC_NUMBER

		/**
		 * Create an animation thread.
		 *
		 * @param light the multizone light.
		 * @param definition The rules for the animation.
		 */
		private AnimationThread(final MultiZoneLight light, final AnimationDefinition definition) {
			super(light, definition);
			mDefinition = definition;
		}

		@Override
		protected MultiZoneLight getLight() {
			return (MultiZoneLight) super.getLight();
		}

		/**
		 * Set the color that the lamp should get after finishing the cycle.
		 *
		 * @param endColors The end colors. List of length 0 turns power off. Null keeps the current color.
		 * @param endTransitionTime The transition time to the end color.
		 * @return The updated animation thread.
		 */
		public AnimationThread setEndColors(final MultizoneColors endColors, final int endTransitionTime) {
			mEndColors = endColors;
			mEndTransitionTime = Math.max(endTransitionTime, 0);
			return this;
		}

		@Override
		public void run() {
			int count = 0;
			try {
				storeDeviceRegistry();
				boolean isInterrupted = false;
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
								MultizoneColors colors = mDefinition.getColors(count).withRelativeBrightness(getRelativeBrightness());
								if (wasOff) {
									getLight().setColors(colors, 0, false);
									getLight().setPower(true, duration, false);
								}
								else if (colors.isOff()) {
									getLight().setPower(false, duration, false);
								}
								else {
									getLight().setColors(colors, duration, false);
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
						// stop the previous color transition by sending setWaveform command with no change.
						getLight().setWaveform(false, null, null, null, null, 0, 0, 0, Waveform.PULSE, false);
					}
				}
				else if (mEndColors == MultizoneColors.OFF) {
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
			cleanAnimationThread();
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
		MultizoneColors getColors(int n);

		@Override
		default Color getColor(final int n) {
			return null;
		}
	}

}
