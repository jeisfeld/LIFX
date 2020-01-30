package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type LightStateInfrared.
 */
public class LightStateInfrared extends ResponseMessage {
	/**
	 * The brightness level.
	 */
	private short mBrightness;

	/**
	 * Create a LightStateInfrared from message data.
	 *
	 * @param packet The message data.
	 */
	public LightStateInfrared(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.LIGHT_STATE_INFRARED;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mBrightness = byteBuffer.getShort();
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Brightness", TypeUtil.toUnsignedString(mBrightness));
		return payloadFields;
	}

	/**
	 * Get the brightness level.
	 *
	 * @return the brightness level
	 */
	public short getBrightness() {
		return mBrightness;
	}
}
