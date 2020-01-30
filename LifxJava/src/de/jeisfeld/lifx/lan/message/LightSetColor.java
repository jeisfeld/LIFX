package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Color;

/**
 * Request message of type LightSetColor.
 */
public class LightSetColor extends RequestMessage {
	/**
	 * The color.
	 */
	private final Color mColor;
	/**
	 * The duration in millis.
	 */
	private final int mDuration;

	/**
	 * Create LightSetColor.
	 *
	 * @param color The target color.
	 * @param duration The color change duration in millis.
	 */
	public LightSetColor(final Color color, final int duration) {
		mColor = color;
		mDuration = duration;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(13); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) 0);
		byteBuffer.putShort(mColor.getHue());
		byteBuffer.putShort(mColor.getSaturation());
		byteBuffer.putShort(mColor.getBrightness());
		byteBuffer.putShort(mColor.getColorTemperature());
		byteBuffer.putInt(mDuration);
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_SET_COLOR;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
