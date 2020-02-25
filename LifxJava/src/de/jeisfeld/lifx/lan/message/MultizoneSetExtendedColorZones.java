package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.message.MultizoneSetColorZones.Apply;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Request message of type MultizoneSetExtendedColorZones.
 */
public class MultizoneSetExtendedColorZones extends RequestMessage {
	/**
	 * The start index.
	 */
	private final short mStartIndex;
	/**
	 * The colors.
	 */
	private final Color[] mColors;
	/**
	 * The duration in millis.
	 */
	private final int mDuration;
	/**
	 * The apply flag.
	 */
	private final Apply mApply;

	/**
	 * Create MultizoneSetExtendedColorZones.
	 *
	 * @param startIndex The start index.
	 * @param duration The color change duration in millis.
	 * @param apply the apply flag.
	 * @param colors The target colors.
	 */
	public MultizoneSetExtendedColorZones(final short startIndex, final int duration, final Apply apply, final Color... colors) {
		mStartIndex = startIndex;
		mDuration = duration;
		mApply = apply;
		mColors = colors;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(664); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putInt(mDuration);
		byteBuffer.put((byte) mApply.ordinal());
		byteBuffer.putShort(mStartIndex);
		byteBuffer.put((byte) mColors.length);
		for (Color color : mColors) {
			byteBuffer.putShort(color.getHue());
			byteBuffer.putShort(color.getSaturation());
			byteBuffer.putShort(color.getBrightness());
			byteBuffer.putShort(color.getColorTemperature());
		}
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.MULTIZONE_SET_EXTENDED_COLOR_ZONES;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}
}
