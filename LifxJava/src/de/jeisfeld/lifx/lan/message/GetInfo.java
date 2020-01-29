package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetInfo.
 */
public class GetInfo extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public GetInfo(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_INFO;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_INFO;
	}

}
