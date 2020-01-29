package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetVersion.
 */
public class GetVersion extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_VERSION;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_VERSION;
	}

}
