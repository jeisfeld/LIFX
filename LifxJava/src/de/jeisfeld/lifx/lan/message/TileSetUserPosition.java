package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Request message of type TileSetUserPosition.
 */
public class TileSetUserPosition extends RequestMessage {
	/**
	 * The tile index.
	 */
	private final byte mTileIndex;
	/**
	 * The x position of the tile.
	 */
	private final float mUserX;
	/**
	 * The y position of the tile.
	 */
	private final float mUserY;

	/**
	 * Create TileSetUserPosition.
	 *
	 * @param tileIndex The tile index.
	 * @param userX The x position of the tile.
	 * @param userY The y position of the tile.
	 */
	public TileSetUserPosition(final byte tileIndex, final float userX, final float userY) {
		mTileIndex = tileIndex;
		mUserX = userX;
		mUserY = userY;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(11); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(mTileIndex);
		byteBuffer.putShort((short) 0);
		byteBuffer.putFloat(mUserX);
		byteBuffer.putFloat(mUserY);
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.TILE_SET_USER_POSITION;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
