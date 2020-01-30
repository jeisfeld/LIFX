package de.jeisfeld.lifx.lan.message;

import java.nio.charset.StandardCharsets;

/**
 * Request message of type SetLabel.
 */
public class SetLabel extends RequestMessage {
	/**
	 * The label.
	 */
	private final String mLabel;

	/**
	 * Create SetLabel.
	 *
	 * @param label the label.
	 */
	public SetLabel(final String label) {
		mLabel = label;
	}

	@Override
	protected final byte[] getPayload() {
		byte[] labelBytes = mLabel.getBytes(StandardCharsets.UTF_8);
		byte[] bytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(labelBytes, 0, bytes, 0, Math.min(labelBytes.length, bytes.length));
		return bytes;
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.SET_LABEL;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
