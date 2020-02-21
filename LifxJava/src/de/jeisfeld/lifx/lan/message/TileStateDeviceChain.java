package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.jeisfeld.lifx.lan.type.TileInfo;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type TileStateDeviceChain.
 */
public class TileStateDeviceChain extends ResponseMessage {
	/**
	 * The number of tiles in the message.
	 */
	private static final int TILE_COUNT_IN_MESSAGE = 16;

	/**
	 * The start index.
	 */
	private byte mStartIndex;
	/**
	 * The tile count.
	 */
	private byte mTileCount;
	/**
	 * The tiles.
	 */
	private TileInfo[] mTiles;

	/**
	 * Create a TileStateDeviceChain from message data.
	 *
	 * @param packet The message data.
	 */
	public TileStateDeviceChain(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.TILE_STATE_DEVICE_CHAIN;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		mStartIndex = byteBuffer.get();
		mTiles = new TileInfo[TILE_COUNT_IN_MESSAGE];
		for (int i = 0; i < TILE_COUNT_IN_MESSAGE; i++) {
			mTiles[i] = TileInfo.readFromByteBuffer(byteBuffer);
		}
		mTileCount = byteBuffer.get();
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("TileCount", TypeUtil.toUnsignedString(mTileCount));
		for (int i = 0; i < mTileCount; i++) {
			payloadFields.put("Tile[" + i + "]", mTiles[mStartIndex + i].toString());
		}
		return payloadFields;
	}

	/**
	 * Get the tile info.
	 *
	 * @return the tile info..
	 */
	public List<TileInfo> getTileInfo() {
		List<TileInfo> result = new ArrayList<>();
		for (int i = 0; i < mTileCount; i++) {
			result.add(mTiles[mStartIndex + i]);
		}

		return result;
	}

}
