package de.jeisfeld.lifx.lan.message;

/**
 * Request message of type TileGetTileState64.
 */
public class TileGetTileState64 extends RequestMessage {
	/**
	 * The tile index.
	 */
	private final byte mTileIndex;
	/**
	 * The length.
	 */
	private final byte mLength;
	/**
	 * The start x position.
	 */
	private final byte mX;
	/**
	 * The start y position.
	 */
	private final byte mY;
	/**
	 * The tile width.
	 */
	private final byte mWidth;

	/**
	 * Create TileGetTileState64 request.
	 *
	 * @param tileIndex the tile index.
	 * @param length the number of tiles for which response is awaited. Should be 1.
	 * @param x the start x position. Should be 0.
	 * @param y the start y position. Should be 0.
	 * @param width the tile width.
	 */
	public TileGetTileState64(final byte tileIndex, final byte length, final byte x, final byte y, final byte width) {
		mTileIndex = tileIndex;
		mLength = length;
		mX = x;
		mY = y;
		mWidth = width;
	}

	@Override
	protected final byte[] getPayload() {
		byte[] result = new byte[6]; // MAGIC_NUMBER
		result[0] = mTileIndex;
		result[1] = mLength;
		result[3] = mX; // MAGIC_NUMBER
		result[4] = mY; // MAGIC_NUMBER
		result[5] = mWidth; // MAGIC_NUMBER
		return result;
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.TILE_GET_TILE_STATE_64;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.TILE_STATE_TILE_STATE_64;
	}
}
