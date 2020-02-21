package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type TileGetTileEffect.
 */
public class TileGetTileEffect extends RequestMessage {
	@Override
	protected final byte[] getPayload() {
		return new byte[0];
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.TILE_GET_TILE_EFFECT;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.TILE_STATE_TILE_EFFECT;
	}
}
