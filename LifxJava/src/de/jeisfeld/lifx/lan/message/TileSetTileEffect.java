package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;

/**
 * Request message of type TileSetTileEffect.
 */
public class TileSetTileEffect extends RequestMessage {
	/**
	 * The tile effect info.
	 */
	private final TileEffectInfo mTileEffectInfo;
	/**
	 * The duration.
	 */
	private final long mDuration;

	/**
	 * Create TileSetTileEffect request.
	 *
	 * @param tileEffectInfo the tile effect info.
	 * @param duration the duration.
	 */
	public TileSetTileEffect(final TileEffectInfo tileEffectInfo, final long duration) {
		mTileEffectInfo = tileEffectInfo;
		mDuration = duration;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(188); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) 0);
		byteBuffer.put((byte) 0);
		byteBuffer.putInt(mTileEffectInfo.getInstanceId());
		byteBuffer.put((byte) mTileEffectInfo.getType().ordinal());
		byteBuffer.putInt(mTileEffectInfo.getSpeed());
		byteBuffer.putLong(mDuration);
		byteBuffer.putLong(0);
		for (int i = 0; i < TileEffectInfo.TILE_EFFECT_PARAMETER_COUNT; i++) {
			byteBuffer.putInt(mTileEffectInfo.getParameters()[i]);
		}
		byteBuffer.put((byte) mTileEffectInfo.getPaletteColors().size());
		for (Color color : mTileEffectInfo.getPaletteColors()) {
			byteBuffer.putShort(color.getHue());
			byteBuffer.putShort(color.getSaturation());
			byteBuffer.putShort(color.getBrightness());
			byteBuffer.putShort(color.getColorTemperature());
		}
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.TILE_SET_TILE_EFFECT;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
