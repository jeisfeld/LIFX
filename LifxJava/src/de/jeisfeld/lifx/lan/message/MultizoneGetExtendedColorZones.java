package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type MultizoneGetExtendedColorZones.
 */
public class MultizoneGetExtendedColorZones extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.MULTIZONE_GET_EXTENDED_COLOR_ZONES;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.MULTIZONE_STATE_EXTENDED_COLOR_ZONES;
	}
}
