package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type MultizoneGetColorZones.
 */
public class MultizoneGetColorZones extends RequestMessage {
	/**
	 * The start index.
	 */
	private final byte mStartIndex;
	/**
	 * The end index.
	 */
	private final byte mEndIndex;

	/**
	 * Create MultizoneGetColorZones request.
	 *
	 * @param startIndex The start index.
	 * @param endIndex The end index.
	 */
	public MultizoneGetColorZones(final byte startIndex, final byte endIndex) {
		mStartIndex = startIndex;
		mEndIndex = endIndex;
	}

	@Override
	protected final byte[] getPayload() {
		byte[] result = new byte[2];
		result[0] = mStartIndex;
		result[1] = mEndIndex;
		return result;
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.MULTIZONE_GET_COLOR_ZONES;
	}

	@Override
	protected final MessageType getResponseType() {
		// irrelevant, as matchesMessageType is overridden due to multiple possible responses.
		return null;
	}

	@Override
	protected final boolean matchesMessageType(final ResponseMessage otherMessage) {
		return otherMessage.getMessageType() == MessageType.MULTIZONE_STATE_ZONE
				|| otherMessage.getMessageType() == MessageType.MULTIZONE_STATE_MULTIZONE;
	}

}
