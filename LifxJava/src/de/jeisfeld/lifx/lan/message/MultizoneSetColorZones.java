package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Color;

/**
 * Request message of type MultizoneSetColorZones.
 */
public class MultizoneSetColorZones extends RequestMessage {
	/**
	 * The start index.
	 */
	private final byte mStartIndex;
	/**
	 * The end index.
	 */
	private final byte mEndIndex;
	/**
	 * The color.
	 */
	private final Color mColor;
	/**
	 * The duration in millis.
	 */
	private final int mDuration;
	/**
	 * The apply flag.
	 */
	private final Apply mApply;

	/**
	 * Create MultizoneSetColorZones.
	 *
	 * @param startIndex The start index.
	 * @param endIndex The end index.
	 * @param color The target color.
	 * @param duration The color change duration in millis.
	 * @param apply the apply flag.
	 */
	public MultizoneSetColorZones(final byte startIndex, final byte endIndex, final Color color, final int duration, final Apply apply) {
		mStartIndex = startIndex;
		mEndIndex = endIndex;
		mColor = color;
		mDuration = duration;
		mApply = apply;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(15); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(mStartIndex);
		byteBuffer.put(mEndIndex);
		byteBuffer.putShort(mColor.getHue());
		byteBuffer.putShort(mColor.getSaturation());
		byteBuffer.putShort(mColor.getBrightness());
		byteBuffer.putShort(mColor.getColorTemperature());
		byteBuffer.putInt(mDuration);
		byteBuffer.put((byte) mApply.ordinal());
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.MULTIZONE_SET_COLOR_ZONES;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

	/**
	 * Enumeration for the apply flag.
	 */
	public enum Apply {
		/**
		 * Do not apply the change.
		 */
		NO_APPLY,
		/**
		 * Apply all pending changes.
		 */
		APPLY,
		/**
		 * Apply all previous changes, but not the current change.
		 */
		APPLY_ONLY;
	}

}
