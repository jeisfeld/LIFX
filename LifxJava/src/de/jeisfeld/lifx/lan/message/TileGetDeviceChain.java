package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type TileGetDeviceChain.
 */
public class TileGetDeviceChain extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.TILE_GET_DEVICE_CHAIN;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.TILE_STATE_DEVICE_CHAIN;
	}
}
