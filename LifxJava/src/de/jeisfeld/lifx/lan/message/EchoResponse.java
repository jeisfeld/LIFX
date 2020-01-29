package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

/**
 * Response message of type EchoResponse.
 */
public class EchoResponse extends ResponseMessage {
	/**
	 * Create an EchoResponse from message data.
	 *
	 * @param packet The message data.
	 */
	public EchoResponse(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.ECHO_RESPONSE;
	}

	@Override
	protected final void evaluatePayload() {
		// do nothing
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		return new HashMap<>();
	}
}
