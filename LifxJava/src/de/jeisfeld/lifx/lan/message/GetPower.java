package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetPower.
 */
public class GetPower extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_POWER;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_POWER;
	}

}
