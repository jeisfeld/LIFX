package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Request message of type LightSetInfrared.
 */
public class LightSetInfrared extends RequestMessage {
	/**
	 * The brightness.
	 */
	private final short mBrightness;

	/**
	 * Create LightSetInfrared.
	 *
	 * @param brightness the brightness.
	 */
	public LightSetInfrared(final short brightness) {
		mBrightness = brightness;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(2);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putShort(mBrightness);
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_SET_INFRARED;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
