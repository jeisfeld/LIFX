package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.Light.AnimationThread;
import de.jeisfeld.lifx.lan.message.MultizoneGetColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneGetExtendedColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneSetColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneSetColorZones.Apply;
import de.jeisfeld.lifx.lan.message.MultizoneStateExtendedColorZones;
import de.jeisfeld.lifx.lan.message.MultizoneStateZone;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.Product;
import de.jeisfeld.lifx.lan.type.Vendor;
import de.jeisfeld.lifx.lan.type.Waveform;
import de.jeisfeld.lifx.lan.util.TypeUtil;
import de.jeisfeld.lifx.os.Logger;

/**
 * Class managing a LIFX multizone light.
 */
public class MultiZoneLight extends Light {
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
	 */
	public MultiZoneLight(final String targetAddress, final InetAddress inetAddress, final int port, final int sourceId, // SUPPRESS_CHECKSTYLE
			final Vendor vendor, final Product product, final int version, final String label, final byte zoneCount) { // SUPPRESS_CHECKSTYLE
		super(targetAddress, inetAddress, port, sourceId, vendor, product, version, label);
		mZoneCount = zoneCount;
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
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the light state via extended API. Works only with new devices and new Firmware.
	 *
	 * @return the light state.
	 */
	public MultizoneStateExtendedColorZones getMultizoneState() {
		try {
			return (MultizoneStateExtendedColorZones) getConnection().requestWithResponse(new MultizoneGetExtendedColorZones());
		}
		catch (IOException e) {
			Logger.error(e);
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
			if(multizoneState == null) {
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
		return getColors((byte) 0, (byte) (getZoneCount() - 1));
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
	 * @param duration the duration of power change in millis.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @param colors the target colors intermediate colors will be interpolated.
	 * @throws IOException Connection issues
	 */
	public void setColors(final int duration, final boolean wait, final MultizoneColors colors) throws IOException {
		for (int i = 0; i < mZoneCount; i++) {
			setColor((byte) i, (byte) i, colors.getColor(i, getZoneCount()), duration, false, i == mZoneCount - 1);
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
		result.append(TypeUtil.INDENT).append("Zone count: ").append(getZoneCount()).append("\n");
		result.append(TypeUtil.INDENT).append("Colors: ").append(getColors()).append("\n");
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

	@Override
	public final AnimationThread animation(final Light.AnimationDefinition definition) {
		return new AnimationThread((MultiZoneLight.AnimationDefinition) definition);
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
	public class AnimationThread extends Light.AnimationThread { // SUPPRESS_CHECKSTYLE
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
		 * @param definition The rules for the animation.
		 */
		private AnimationThread(final AnimationDefinition definition) {
			super(definition);
			mDefinition = definition;
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
				boolean isInterrupted = false;
				try {
					while (!isInterrupted() && mDefinition.getColors(count) != null) {
						final long startTime = System.currentTimeMillis();
						int errorCount = 0;
						boolean success = false;
						int duration = Math.max(mDefinition.getDuration(count), 0);
						while (!success) {
							try { // SUPPRESS_CHECKSTYLE
								MultizoneColors colors = mDefinition.getColors(count).withRelativeBrightness(getRelativeBrightness());
								Power power;
								if (count == 0 && (power = getPower()) != null && power.isOff()) {
									setColors(0, false, colors);
									setPower(true, duration, false);
								}
								else {
									setColors(duration, false, colors);
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
					// stop the previous color transition by sending setWaveform command with no change.
					setWaveform(false, null, null, null, null, 0, 0, 0, Waveform.PULSE, false);
				}
				else if (mEndColors == MultizoneColors.OFF) {
					setPower(false, mEndTransitionTime, true);
				}
				else {
					setColors(mEndTransitionTime, true, mEndColors);
				}
				if (getAnimationCallback() != null) {
					getAnimationCallback().onAnimationEnd(isInterrupted);
				}
			}
			catch (IOException e) {
				Logger.error(e);
				if (getAnimationCallback() != null) {
					getAnimationCallback().onException(e);
				}
			}
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
