package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type LightGet.
 */
public class LightGet extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_GET;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.LIGHT_STATE;
	}

}
