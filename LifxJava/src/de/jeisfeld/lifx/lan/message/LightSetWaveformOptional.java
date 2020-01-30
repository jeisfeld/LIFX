package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Waveform;

/**
 * Request message of type LightSetWaveformOptional.
 */
public class LightSetWaveformOptional extends RequestMessage {
	/**
	 * The transient flag.
	 */
	private final boolean mIsTransient;
	/**
	 * The color.
	 */
	private final Color mColor;
	/**
	 * The cycle period.
	 */
	private final int mPeriod;
	/**
	 * The number of cycles.
	 */
	private final float mCycles;
	/**
	 * The skew ratio.
	 */
	private final short mSkewRatio;
	/**
	 * The waveform.
	 */
	private final Waveform mWaveform;
	/**
	 * Flag indicating if hue should be set.
	 */
	private final boolean mSetHue;
	/**
	 * Flag indicating if saturation should be set.
	 */
	private final boolean mSetSaturation;
	/**
	 * Flag indicating if brightness should be set.
	 */
	private final boolean mSetBrightness;
	/**
	 * Flag indicating if color temperature should be set.
	 */
	private final boolean mSetColorTemperature;

	/**
	 * Create LightSetWaveformOptional.
	 *
	 * @param isTransient the transient flag indicating if the color should finally return to prior value.
	 * @param color The target color.
	 * @param period the cycle period.
	 * @param cycles the number of cycles.
	 * @param skewRatio the skew ratio.
	 * @param waveform the waveform.
	 * @param setHue flag indicating if hue should be set.
	 * @param setSaturation flag indicating if saturation should be set.
	 * @param setBrightness flag indicating if brightness should be set.
	 * @param setColorTemperature flag indicating if color temperature should be set.
	 */
	public LightSetWaveformOptional(final boolean isTransient, final Color color, final int period, final float cycles, // SUPPRESS_CHECKSTYLE
			final short skewRatio, final Waveform waveform, final boolean setHue, final boolean setSaturation, final boolean setBrightness,
			final boolean setColorTemperature) {
		mIsTransient = isTransient;
		mColor = color;
		mPeriod = period;
		mCycles = cycles;
		mSkewRatio = skewRatio;
		mWaveform = waveform;
		mSetHue = setHue;
		mSetSaturation = setSaturation;
		mSetBrightness = setBrightness;
		mSetColorTemperature = setColorTemperature;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(25); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) 0);
		byteBuffer.put((byte) (mIsTransient ? 1 : 0));
		byteBuffer.putShort(mColor.getHue());
		byteBuffer.putShort(mColor.getSaturation());
		byteBuffer.putShort(mColor.getBrightness());
		byteBuffer.putShort(mColor.getColorTemperature());
		byteBuffer.putInt(mPeriod);
		byteBuffer.putFloat(mCycles);
		byteBuffer.putShort(mSkewRatio);
		byteBuffer.put((byte) mWaveform.ordinal());
		byteBuffer.put((byte) (mSetHue ? 1 : 0));
		byteBuffer.put((byte) (mSetSaturation ? 1 : 0));
		byteBuffer.put((byte) (mSetBrightness ? 1 : 0));
		byteBuffer.put((byte) (mSetColorTemperature ? 1 : 0));
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_SET_WAVEFORM_OPTIONAL;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
