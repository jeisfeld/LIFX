package de.jeisfeld.lifx.lan.message;

import de.jeisfeld.lifx.lan.type.Service;

/**
 * Request message of type GetService.
 */
public class GetService extends RequestMessage {
	/**
	 * Constructor.
	 *
	 * @param targetAddress The target address.
	 */
	public GetService(final String targetAddress) {
		super(targetAddress);
	}

	/**
	 * Constructor for broadcast to all addresses.
	 */
	public GetService() {
		super();
	}

	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_SERVICE;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_SERVICE;
	}

	@Override
	public final boolean matches(final ResponseMessage otherMessage) {
		return super.matches(otherMessage) && ((StateService) otherMessage).getService() != Service.UNKNOWN;
	}
}
