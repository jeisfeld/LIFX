package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type EchoRequest.
 */
public class EchoRequest extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.ECHO_REQUEST;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ECHO_RESPONSE;
	}

}
