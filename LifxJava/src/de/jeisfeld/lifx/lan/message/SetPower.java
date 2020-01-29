package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Power;

/**
 * Request message of type SetPower.
 */
public class SetPower extends RequestMessage {
	/**
	 * The power.
	 */
	private final Power mPower;

	/**
	 * Create SetPower.
	 *
	 * @param power The Power.
	 */
	public SetPower(final Power power) {
		mPower = power;
	}

	@Override
	protected final byte[] getPayload() {
		if (mPower == Power.UNKNOWN) {
			return new byte[0];
		}
		ByteBuffer byteBuffer = ByteBuffer.allocate(2);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putShort(mPower == Power.ON ? (short) -1 : 0);
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.SET_POWER;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
