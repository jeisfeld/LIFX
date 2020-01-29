package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetHostInfo.
 */
public class GetHostInfo extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public GetHostInfo(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_HOST_INFO;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_HOST_INFO;
	}

}
