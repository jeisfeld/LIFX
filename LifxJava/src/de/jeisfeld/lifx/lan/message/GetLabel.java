package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetLabel.
 */
public class GetLabel extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_LABEL;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_LABEL;
	}

}
