package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type MultizoneGetMultizoneEffect.
 */
public class MultizoneGetMultizoneEffect extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.MULTIZONE_GET_MULTIZONE_EFFECT;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.MULTIZONE_STATE_MULTIZONE_EFFECT;
	}
}
