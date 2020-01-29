package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetGroup.
 */
public class GetGroup extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_GROUP;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_GROUP;
	}

}
