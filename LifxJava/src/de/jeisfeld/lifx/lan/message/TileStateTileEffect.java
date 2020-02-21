package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.lan.type.TileEffectType;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type TileStateTileEffect.
 */
public class TileStateTileEffect extends ResponseMessage {
	/**
	 * The number of palette colors in the message.
	 */
	private static final int PALETTE_COUNT_IN_MESSAGE = 16;

	/**
	 * The instance id.
	 */
	private int mInstanceId;
	/**
	 * The effect type.
	 */
	private TileEffectType mType;
	/**
	 * The effect speed.
	 */
	private int mSpeed;
	/**
	 * The effect duration.
	 */
	private long mDuration;
	/**
	 * The effect parameters.
	 */
	private int[] mParameters;
	/**
	 * The palette size.
	 */
	private byte mPaletteCount;
	/**
	 * The palette colors.
	 */
	private Color[] mPaletteColors;

	/**
	 * Create a TileStateTileEffect from message data.
	 *
	 * @param packet The message data.
	 */
	public TileStateTileEffect(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.TILE_STATE_TILE_EFFECT;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.get();
		mInstanceId = byteBuffer.getInt();
		mType = TileEffectType.fromInt(byteBuffer.get());
		mSpeed = byteBuffer.getInt();
		mDuration = byteBuffer.getLong();
		byteBuffer.getLong();
		mParameters = new int[TileEffectInfo.TILE_EFFECT_PARAMETER_COUNT];
		for (int i = 0; i < TileEffectInfo.TILE_EFFECT_PARAMETER_COUNT; i++) {
			mParameters[i] = byteBuffer.getInt();
		}
		mPaletteCount = byteBuffer.get();
		mPaletteColors = new Color[PALETTE_COUNT_IN_MESSAGE];
		for (int i = 0; i < PALETTE_COUNT_IN_MESSAGE; i++) {
			mPaletteColors[i] = new Color(byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort());
		}
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("InstanceId", TypeUtil.toUnsignedString(mInstanceId));
		payloadFields.put("EffectType", mType.name());
		payloadFields.put("Speed", TypeUtil.toUnsignedString(mSpeed));
		payloadFields.put("Duration", Long.toString(mDuration));

		StringBuilder parameterString = new StringBuilder("[");
		for (int parameter : mParameters) {
			parameterString.append(TypeUtil.toUnsignedString(parameter)).append(", ");
		}
		parameterString.replace(parameterString.length() - 2, parameterString.length(), "]");
		payloadFields.put("Parameters", parameterString.toString());
		payloadFields.put("PaletteCount", TypeUtil.toUnsignedString(mPaletteCount));
		payloadFields.put("Colors", Arrays.asList(mPaletteColors).toString());
		return payloadFields;
	}

	/**
	 * Get the palette colors.
	 *
	 * @return the palette colors.
	 */
	private List<Color> getPaletteColors() {
		List<Color> result = new ArrayList<>();
		for (int i = 0; i < mPaletteCount; i++) {
			result.add(mPaletteColors[i]);
		}
		return result;
	}

	/**
	 * Get the effet info.
	 *
	 * @return The effect info.
	 */
	public TileEffectInfo getEffectInfo() {
		return new TileEffectInfo(mInstanceId, mType, mSpeed, mParameters, getPaletteColors());
	}

	/**
	 * Get the duration.
	 *
	 * @return the duration.
	 */
	public long getDuration() {
		return mDuration;
	}

}
