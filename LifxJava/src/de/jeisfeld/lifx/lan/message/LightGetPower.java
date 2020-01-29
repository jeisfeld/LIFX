package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type LightGetPower.
 */
public class LightGetPower extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public LightGetPower(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.LIGHT_GET_POWER;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.LIGHT_STATE_POWER;
	}

}
