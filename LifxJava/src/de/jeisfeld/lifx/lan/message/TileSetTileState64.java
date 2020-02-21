package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import de.jeisfeld.lifx.lan.type.Color;

/**
 * Request message of type TileSetTileState64.
 */
public class TileSetTileState64 extends RequestMessage {
	/**
	 * The number of colors required in the message.
	 */
	private static final int COLOR_COUNT = 64;

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
	 * The duration.
	 */
	private final int mDuration;
	/**
	 * The list of colors to be set.
	 */
	private final List<Color> mColors;

	/**
	 * Create TileSetTileState64 request.
	 *
	 * @param tileIndex the tile index.
	 * @param length the number of tiles for which response is awaited. Should be 1.
	 * @param x the start x position. Should be 0.
	 * @param y the start y position. Should be 0.
	 * @param width the tile width.
	 * @param duration the duration of the change.
	 * @param colors the list of colors to be set. Must be 64, othewise it will be filled up to 64.
	 */
	public TileSetTileState64(final byte tileIndex, final byte length, final byte x, final byte y, final byte width, final int duration,
			final List<Color> colors) {
		mTileIndex = tileIndex;
		mLength = length;
		mX = x;
		mY = y;
		mWidth = width;
		mDuration = duration;
		mColors = colors;

		if (mColors.size() < COLOR_COUNT) {
			int missingCount = COLOR_COUNT - mColors.size();
			for (int i = 0; i < missingCount; i++) {
				mColors.add(Color.OFF);
			}
		}
		while (mColors.size() > COLOR_COUNT) {
			mColors.remove(mColors.size() - 1);
		}
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(522); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(mTileIndex);
		byteBuffer.put(mLength);
		byteBuffer.put((byte) 0);
		byteBuffer.put(mX);
		byteBuffer.put(mY);
		byteBuffer.put(mWidth);
		byteBuffer.putInt(mDuration);
		for (Color color : mColors) {
			byteBuffer.putShort(color.getHue());
			byteBuffer.putShort(color.getSaturation());
			byteBuffer.putShort(color.getBrightness());
			byteBuffer.putShort(color.getColorTemperature());
		}
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.TILE_SET_TILE_STATE_64;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
