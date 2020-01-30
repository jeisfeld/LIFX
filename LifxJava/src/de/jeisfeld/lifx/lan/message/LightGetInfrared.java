package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type LightGetInfrared.
 */
public class LightGetInfrared extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_GET_INFRARED;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.LIGHT_STATE_INFRARED;
	}

}
