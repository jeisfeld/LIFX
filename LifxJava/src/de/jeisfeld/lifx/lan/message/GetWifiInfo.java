package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type GetWifiInfo.
 */
public class GetWifiInfo extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.GET_WIFI_INFO;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.STATE_WIFI_INFO;
	}

}
