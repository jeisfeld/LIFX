package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetLabel.
 */
public class GetLabel extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public GetLabel(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_LABEL;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_LABEL;
	}

}
