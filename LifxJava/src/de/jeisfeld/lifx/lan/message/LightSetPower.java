package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Power;

/**
 * Request message of type LightSetPower.
 */
public class LightSetPower extends RequestMessage {
	/**
	 * The status.
	 */
	private final boolean mStatus;
	/**
	 * The duration in millis.
	 */
	private final int mDuration;

	/**
	 * Create LightSetPower.
	 *
	 * @param status The target power status.
	 * @param duration The status change duration in millis.
	 */
	public LightSetPower(final boolean status, final int duration) {
		mStatus = status;
		mDuration = duration;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(6); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putShort((mStatus ? Power.ON : Power.OFF).getLevel());
		byteBuffer.putInt(mDuration);
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_SET_POWER;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
