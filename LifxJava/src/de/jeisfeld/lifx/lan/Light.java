package de.jeisfeld.lifx.lan;

import java.net.SocketException;

import de.jeisfeld.lifx.lan.message.LightGet;
import de.jeisfeld.lifx.lan.message.LightGetInfrared;
import de.jeisfeld.lifx.lan.message.LightGetPower;
import de.jeisfeld.lifx.lan.message.LightSetColor;
import de.jeisfeld.lifx.lan.message.LightSetInfrared;
import de.jeisfeld.lifx.lan.message.LightSetPower;
import de.jeisfeld.lifx.lan.message.LightSetWaveform;
import de.jeisfeld.lifx.lan.message.LightSetWaveformOptional;
import de.jeisfeld.lifx.lan.message.LightState;
import de.jeisfeld.lifx.lan.message.LightStateInfrared;
import de.jeisfeld.lifx.lan.message.LightStatePower;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.Waveform;
import de.jeisfeld.lifx.lan.util.Logger;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class managing a LIFX light.
 */
public class Light extends Device {
	/**
	 * The cycle thread.
	 */
	private Cycle mCycle = null;

	/**
	 * Constructor.
	 *
	 * @param device The device which is a light.
	 */
	public Light(final Device device) {
		super(device.getTargetAddress(), device.getInetAddress(), device.getPort(), device.getSourceId());
		setVersionInformation(device.getVendor(), device.getProduct(), device.getVersion());
	}

	// OVERRIDABLE
	@Override
	public String getFullInformation() {
		StringBuilder result = new StringBuilder(super.getFullInformation());
		if (getProduct().hasInfrared()) {
			result.append(TypeUtil.INDENT).append("Infrared Brightness: ").append(TypeUtil.toUnsignedString(getInfraredBrightness())).append("\n");
		}
		result.append(TypeUtil.INDENT).append("Color: ").append(getColor()).append("\n");
		return result.toString();
	}

	@Override
	public final Power getPower() {
		LightStatePower lightStatePower;
		try {
			lightStatePower = (LightStatePower) getConnection().requestWithResponse(new LightGetPower());
			return new Power(lightStatePower.getLevel());
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the light state.
	 *
	 * @return the light state.
	 */
	public final LightState getState() {
		LightState lightState;
		try {
			lightState = (LightState) getConnection().requestWithResponse(new LightGet());
			return lightState;
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the infrared brightness.
	 *
	 * @return the infrared brightness.
	 */
	public final Short getInfraredBrightness() {
		LightStateInfrared lightStateInfrared;
		try {
			lightStateInfrared = (LightStateInfrared) getConnection().requestWithResponse(new LightGetInfrared());
			return lightStateInfrared.getBrightness();
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the color.
	 *
	 * @return the color.
	 */
	public final Color getColor() {
		LightState lightState = getState();
		return lightState == null ? null : lightState.getColor();
	}

	/**
	 * Set the power.
	 *
	 * @param status true for switching on, false for switching off
	 * @param duration the duration of power change in millis.
	 * @throws SocketException Connection issues
	 */
	public void setPower(final boolean status, final int duration) throws SocketException {
		getConnection().requestWithResponse(new LightSetPower(status, duration));
	}

	/**
	 * Set the color.
	 *
	 * @param color the target color.
	 * @param duration the duration of power change in millis.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @throws SocketException Connection issues
	 */
	public void setColor(final Color color, final int duration, final boolean wait) throws SocketException {
		getConnection().requestWithResponse(new LightSetColor(color, duration));
		if (wait) {
			try {
				Thread.sleep(duration);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * Set the color.
	 *
	 * @param color the target color.
	 * @throws SocketException Connection issues
	 */
	public void setColor(final Color color) throws SocketException {
		setColor(color, 0, false);
	}

	/**
	 * Set a waveform.
	 *
	 * @param isTransient the transient flag indicating if the color should finally return to prior value.
	 * @param color The target color.
	 * @param period the cycle period.
	 * @param cycles the number of cycles.
	 * @param waveform the waveform.
	 * @param skewRatio the skew ratio between 0 and 1. For Pulse, this is the time in the period when the pulse goes on. For Sine and Triangle this
	 *            is the time in the period where the target color is reached. For Saw and Half-sine this has no effect.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @throws SocketException Connection issues
	 */
	public void setWaveform(final boolean isTransient, final Color color, final int period, final double cycles,
			final Waveform waveform, final double skewRatio, final boolean wait) throws SocketException {
		float floatCycles = (float) Math.max(0, Math.min(Float.MAX_VALUE, cycles));
		getConnection()
				.requestWithResponse(
						new LightSetWaveform(isTransient, color, period, floatCycles,
								(short) (TypeUtil.toShort(skewRatio) + Short.MIN_VALUE), waveform));
		if (wait) {
			try {
				Thread.sleep((long) (period * floatCycles));
			}
			catch (InterruptedException e) {
				// ignore
			}

		}
	}

	/**
	 * Set a waveform.
	 *
	 * @param color The target color.
	 * @param period the cycle period.
	 * @param cycles the number of cycles. 0 for eternal run without waiting. Positive number for limited run with waiting.
	 * @param waveform the waveform.
	 * @throws SocketException Connection issues
	 */
	public void setWaveform(final Color color, final int period, final int cycles,
			final Waveform waveform) throws SocketException {
		setWaveform(true, color, period, cycles <= 0 ? Float.MAX_VALUE : cycles, waveform, 0.5, cycles > 0); // MAGIC_NUMBER
	}

	/**
	 * Set a waveform, transitioning once.
	 *
	 * @param color The target color.
	 * @param period the cycle period.
	 * @param waveform the waveform.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @throws SocketException Connection issues
	 */
	public void setWaveform(final Color color, final int period, final Waveform waveform, final boolean wait) throws SocketException {
		setWaveform(false, color, period, 1, waveform, 0.5, wait); // MAGIC_NUMBER
	}

	/**
	 * Set a waveform for change of only some of the parameters hue, saturation, brightness, color temperature.
	 *
	 * @param isTransient the transient flag indicating if the color should finally return to prior value.
	 * @param hue The hue value from 0 to 360. May be null.
	 * @param saturation The saturation value from 0 to 1. May be null.
	 * @param brightness The brightness value from 0 to 1. May be null.
	 * @param colorTemperature The color temperature value in Kelvin. May be null.
	 * @param period the cycle period.
	 * @param cycles the number of cycles.
	 * @param skewRatio the skew ratio between 0 and 1. For Pulse, this is the time in the period when the pulse goes on. For Sine and Triangle this
	 *            is the time in the period where the target color is reached. For Saw and Half-sine this has no effect.
	 * @param wait flag indicating if the method should return only after the final color is reached.
	 * @param waveform the waveform.
	 * @throws SocketException Connection issues
	 */
	public void setWaveform(final boolean isTransient, final Double hue, final Double saturation, final Double brightness, // SUPPRESS_CHECKSTYLE
			final Integer colorTemperature, final int period, final double cycles, final double skewRatio,
			final Waveform waveform, final boolean wait) throws SocketException {
		float floatCycles = (float) Math.max(0, Math.min(Float.MAX_VALUE, cycles));
		short hueValue = TypeUtil.toShort((hue == null ? 180 : hue) / 360); // MAGIC_NUMBER
		short saturationValue = TypeUtil.toShort(saturation == null ? 1 : saturation);
		short brightnessValue = TypeUtil.toShort(brightness == null ? 1 : brightness);
		short colorTemperatureValue = colorTemperature == null ? 4000 : colorTemperature.shortValue(); // MAGIC_NUMBER

		getConnection().requestWithResponse(
				new LightSetWaveformOptional(isTransient, new Color(hueValue, saturationValue, brightnessValue, colorTemperatureValue),
						period, floatCycles, (short) (TypeUtil.toShort(skewRatio) + Short.MIN_VALUE), waveform,
						hue != null, saturation != null, brightness != null, colorTemperature != null));
		if (wait) {
			try {
				Thread.sleep((long) (period * floatCycles));
			}
			catch (InterruptedException e) {
				// ignore
			}

		}
	}

	/**
	 * Set the infrared brightness.
	 *
	 * @param brightness the infrared brightness.
	 * @throws SocketException Connection issues
	 */
	public void setInfraredBrightness(final short brightness) throws SocketException {
		getConnection().requestWithResponse(new LightSetInfrared(brightness));
	}

	/**
	 * Wait until the color fulfils a certain condition.
	 *
	 * @param filter The filtering condition.
	 * @param timeout Max waiting time in millis. No timeout in case of negative values.
	 */
	public void waitForColor(final ColorFilter filter, final long timeout) {
		long startTime = System.currentTimeMillis();
		Color color = getColor();
		boolean isMatching = filter.matches(color);
		while (!isMatching && (timeout < 0 || System.currentTimeMillis() - startTime < timeout)) {
			try {
				Thread.sleep(200); // MAGIC_NUMBER
			}
			catch (InterruptedException e) {
				// ignore
			}
			color = getColor();
			isMatching = filter.matches(color);
		}
	}

	/**
	 * Wait until the color matches a certain color.
	 *
	 * @param color The matching color.
	 * @param timeout Max waiting time in millis. No timeout in case of negative values.
	 */
	public void waitForColor(final Color color, final long timeout) {
		if (getProduct().hasColor()) {
			waitForColor(c -> color.isSimilar(c), timeout);
		}
		else {
			waitForColor(c -> color.isSimilarBlackWhite(c), timeout);
		}
	}

	/**
	 * Create a cycle.
	 *
	 * @param colors The colors of the cycle.
	 * @return The cycle.
	 */
	public Cycle cycle(final Color... colors) {
		return new Cycle(colors);
	}

	/**
	 * End the current cycle (if applicable). This interrupts and joins the cycle.
	 */
	public void endCycle() {
		synchronized (this) {
			if (mCycle != null) {
				mCycle.end();
			}
		}
	}

	/**
	 * Wait for the end of the current cycle. In contrast to endCycle, this does not interrupt.
	 */
	public void waitForCycleEnd() {
		synchronized (this) {
			try {
				mCycle.join();
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * Interface for filtering colors.
	 */
	public interface ColorFilter {
		/**
		 * Filtering method.
		 *
		 * @param color The color.
		 * @return true if the color matches the filter.
		 */
		boolean matches(Color color);
	}

	/**
	 * A thread running a cycle of colors.
	 */
	public class Cycle extends Thread {
		/**
		 * The duration of a cycle step in millis.
		 */
		private int mStepDuration = 1000; // MAGIC_NUMBER
		/**
		 * The colors of the cycle.
		 */
		private final Color[] mColors;
		/**
		 * The color to be reached after ending the thread.
		 */
		private Color mEndColor = null;
		/**
		 * The transition time to the start of the cycle.
		 */
		private int mStartTransitionTime = 200; // MAGIC_NUMBER
		/**
		 * The transition time to the end color.
		 */
		private int mEndTransitionTime = 200; // MAGIC_NUMBER
		/**
		 * The number of cycles. Value 0 runs eternally.
		 */
		private int mCycleCount = 0;
		/**
		 * The relative brightness of the colors.
		 */
		private double mRelativeBrightness = 1;

		/**
		 * Create a cycle thread.
		 *
		 * @param colors The colors of the cycle.
		 */
		public Cycle(final Color... colors) {
			mColors = colors;
		}

		/**
		 * Set the duration of the cycle in millis.
		 *
		 * @param duration The duration of the cycle in millis
		 * @return The updated cycle.
		 */
		public Cycle setCycleDuration(final int duration) {
			mStepDuration = duration / mColors.length;
			return this;
		}

		/**
		 * Set the duration of a cycle step in millis.
		 *
		 * @param duration The duration of a cycle step in millis
		 * @return The updated cycle.
		 */
		public Cycle setStepDuration(final int duration) {
			mStepDuration = duration;
			return this;
		}

		/**
		 * Set the color that the lamp should get after finishing the cycle.
		 *
		 * @param endColor The end color.
		 * @param endTransitionTime The transitioning time to the end color.
		 * @return The updated cycle.
		 */
		public Cycle setEndColor(final Color endColor, final int endTransitionTime) {
			mEndColor = endColor;
			mEndTransitionTime = endTransitionTime;
			return this;
		}

		/**
		 * Set the transitioning time to the start color of the cycle.
		 *
		 * @param startTransitionTime The transitioning time.
		 * @return The updated cycle.
		 */
		public Cycle setStartTransitionTime(final int startTransitionTime) {
			mStartTransitionTime = startTransitionTime;
			return this;
		}

		/**
		 * Set the number of times the cycle should run.
		 *
		 * @param cycleCount The number of times the cycle should run. Value 0 runs eternally.
		 * @return The updated cycle.
		 */
		public Cycle setCycleCount(final int cycleCount) {
			mCycleCount = cycleCount;
			return this;
		}

		/**
		 * Set the relative brightness of the cycle colors.
		 *
		 * @param relativeBrightness The relative brightness of the colors.
		 * @return The updated cycle.
		 */
		public Cycle setRelativeBrightness(final double relativeBrightness) {
			mRelativeBrightness = relativeBrightness;
			return this;
		}

		@Override
		public final void start() {
			synchronized (Light.this) {
				if (mCycle != null) {
					mCycle.end();
				}
				mCycle = this;
			}
			super.start();
		}

		@Override
		public final void run() {
			boolean firstRun = true;
			int count = 0;
			try {
				try {
					while (!isInterrupted() && (count < mCycleCount || mCycleCount == 0)) {
						for (Color color : mColors) {
							long startTime = System.currentTimeMillis();
							setColor(color.withRelativeBrightness(mRelativeBrightness), firstRun ? mStartTransitionTime : mStepDuration, false);
							Thread.sleep(Math.max(0, (firstRun ? mStartTransitionTime : mStepDuration) + startTime - System.currentTimeMillis()));
							firstRun = false;
						}
						count++;
					}
				}
				catch (InterruptedException e) {
					// do nothing
				}

				if (mEndColor != null) {
					setColor(mEndColor, mEndTransitionTime, true);
				}
				else {
					// stop the previous color transition by setting the current color.
					setColor(getColor());
				}
			}
			catch (SocketException e) {
				Logger.error(e);
			}
		}

		/**
		 * End the cycle and wait for the end of the cycle thread.
		 */
		public void end() {
			interrupt();
			try {
				join();
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

}
