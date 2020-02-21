package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type TileStateTileState64.
 */
public class TileStateTileState64 extends ResponseMessage {
	/**
	 * The tile index.
	 */
	private byte mTileIndex;
	/**
	 * The start x position.
	 */
	private byte mX;
	/**
	 * The start y position.
	 */
	private byte mY;
	/**
	 * The tile width.
	 */
	private byte mWidth;
	/**
	 * The colors.
	 */
	private List<Color> mColors;

	/**
	 * Create a TileStateTileState64 from message data.
	 *
	 * @param packet The message data.
	 */
	public TileStateTileState64(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.TILE_STATE_TILE_STATE_64;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		mTileIndex = byteBuffer.get();
		byteBuffer.get();
		mX = byteBuffer.get();
		mY = byteBuffer.get();
		mWidth = byteBuffer.get();

		mColors = new ArrayList<>();
		while (byteBuffer.hasRemaining()) {
			mColors.add(new Color(byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort()));
		}
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("WileIndex", TypeUtil.toUnsignedString(mTileIndex));
		payloadFields.put("x", TypeUtil.toUnsignedString(mX));
		payloadFields.put("y", TypeUtil.toUnsignedString(mY));
		payloadFields.put("Width", TypeUtil.toUnsignedString(mWidth));
		payloadFields.put("Colors", mColors.toString());
		return payloadFields;
	}

	/**
	 * Get the colors.
	 *
	 * @return The colors.
	 */
	public List<Color> getColors() {
		return mColors;
	}

}
