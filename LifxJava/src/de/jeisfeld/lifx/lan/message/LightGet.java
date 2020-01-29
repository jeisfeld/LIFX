package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type LightGet.
 */
public class LightGet extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public LightGet(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_GET;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.LIGHT_STATE;
	}

}
