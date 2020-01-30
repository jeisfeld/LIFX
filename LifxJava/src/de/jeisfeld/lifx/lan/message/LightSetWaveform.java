package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Waveform;

/**
 * Request message of type LightSetWaveform.
 */
public class LightSetWaveform extends RequestMessage {
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
	 * Create LightSetWaveform.
	 *
	 * @param isTransient the transient flag indicating if the color should finally return to prior value.
	 * @param color The target color.
	 * @param period the cycle period.
	 * @param cycles the number of cycles.
	 * @param skewRatio the skew ratio.
	 * @param waveform the waveform.
	 */
	public LightSetWaveform(final boolean isTransient, final Color color, final int period, final float cycles, final short skewRatio,
			final Waveform waveform) {
		mIsTransient = isTransient;
		mColor = color;
		mPeriod = period;
		mCycles = cycles;
		mSkewRatio = skewRatio;
		mWaveform = waveform;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(21); // MAGIC_NUMBER
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
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_SET_WAVEFORM;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
