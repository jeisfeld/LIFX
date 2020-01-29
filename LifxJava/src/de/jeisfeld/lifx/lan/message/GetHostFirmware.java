package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetHostFirmware.
 */
public class GetHostFirmware extends RequestMessage {

	/**
	 * Constructor.
	 *
	 * @param targetAddress the target address.
	 */
	public GetHostFirmware(final String targetAddress) {
		super(targetAddress);
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_HOST_FIRMWARE;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_HOST_FIRMWARE;
	}

}
