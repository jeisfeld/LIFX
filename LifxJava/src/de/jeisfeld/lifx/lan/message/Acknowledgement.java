package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.util.Map;

/**
 * Response message of type Acknowledgement.
 */
public class Acknowledgement extends ResponseMessage {
	/**
	 * Create an acknowledgement from message data.
	 *
	 * @param packet The message data.
	 */
	public Acknowledgement(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

	@Override
	protected final void evaluatePayload() {
		// do nothing
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		return null;
	}

}
