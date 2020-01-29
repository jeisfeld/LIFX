package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetHostFirmware.
 */
public class GetHostFirmware extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_HOST_FIRMWARE;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_HOST_FIRMWARE;
	}

}
