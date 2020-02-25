package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;

/**
 * Request message of type MultizoneSetMultizoneEffect.
 */
public class MultizoneSetMultizoneEffect extends RequestMessage {
	/**
	 * The multizone effect info.
	 */
	private final MultizoneEffectInfo mMultizoneEffectInfo;
	/**
	 * The duration.
	 */
	private final long mDuration;

	/**
	 * Create MultizoneSetMultizoneEffect request.
	 *
	 * @param multizoneEffectInfo the multizone effect info.
	 * @param duration the duration.
	 */
	public MultizoneSetMultizoneEffect(final MultizoneEffectInfo multizoneEffectInfo, final long duration) {
		mMultizoneEffectInfo = multizoneEffectInfo;
		mDuration = duration;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(59); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putInt(mMultizoneEffectInfo.getInstanceId());
		byteBuffer.put((byte) mMultizoneEffectInfo.getType().ordinal());
		byteBuffer.putShort((short) 0);
		byteBuffer.putInt(mMultizoneEffectInfo.getSpeed());
		byteBuffer.putLong(mDuration);
		byteBuffer.putLong(0);
		for (int i = 0; i < MultizoneEffectInfo.MULTIZONE_EFFECT_PARAMETER_COUNT; i++) {
			byteBuffer.putInt(mMultizoneEffectInfo.getParameters()[i]);
		}
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.MULTIZONE_SET_MULTIZONE_EFFECT;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
