package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetGroup.
 */
public class GetGroup extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public GetGroup(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_GROUP;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_GROUP;
	}

}
