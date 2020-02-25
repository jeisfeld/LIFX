package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;
import de.jeisfeld.lifx.lan.type.MultizoneEffectType;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type MultizoneStateMultizoneEffect.
 */
public class MultizoneStateMultizoneEffect extends ResponseMessage {
	/**
	 * The instance id.
	 */
	private int mInstanceId;
	/**
	 * The effect type.
	 */
	private MultizoneEffectType mType;
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
	 * Create a MultizoneStateMultizoneEffect from message data.
	 *
	 * @param packet The message data.
	 */
	public MultizoneStateMultizoneEffect(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.MULTIZONE_STATE_MULTIZONE_EFFECT;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mInstanceId = byteBuffer.getInt();
		mType = MultizoneEffectType.fromInt(byteBuffer.get());
		byteBuffer.getShort();
		mSpeed = byteBuffer.getInt();
		mDuration = byteBuffer.getLong();
		byteBuffer.getLong();
		mParameters = new int[MultizoneEffectInfo.MULTIZONE_EFFECT_PARAMETER_COUNT];
		for (int i = 0; i < MultizoneEffectInfo.MULTIZONE_EFFECT_PARAMETER_COUNT; i++) {
			mParameters[i] = byteBuffer.getInt();
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
		return payloadFields;
	}

	/**
	 * Get the effect info.
	 *
	 * @return The effect info.
	 */
	public MultizoneEffectInfo getEffectInfo() {
		return new MultizoneEffectInfo(mInstanceId, mType, mSpeed, mParameters);
	}

}
