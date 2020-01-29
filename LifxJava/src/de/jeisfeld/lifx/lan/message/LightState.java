package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type LightState.
 */
public class LightState extends ResponseMessage {
	/**
	 * The color.
	 */
	private Color mColor;
	/**
	 * The power level.
	 */
	private short mPower;
	/**
	 * The label.
	 */
	private String mLabel;

	/**
	 * Create a LightState from message data.
	 *
	 * @param packet The message data.
	 */
	public LightState(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.LIGHT_STATE;
	}

	@Override
	protected final void evaluatePayload() {
		byte[] payload = getPayload();
		ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mColor = new Color(byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort());
		byteBuffer.getShort();
		mPower = byteBuffer.getShort();

		byte[] labelBytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(payload, 12, labelBytes, 0, labelBytes.length); // MAGIC_NUMBER
		mLabel = TypeUtil.toString(labelBytes);
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Color", mColor.toString());
		payloadFields.put("Power", TypeUtil.toUnsignedString(mPower));
		payloadFields.put("Label", mLabel);
		return payloadFields;
	}

	/**
	 * Get the color.
	 *
	 * @return The color.
	 */
	public Color getColor() {
		return mColor;
	}

	/**
	 * Get the power level.
	 *
	 * @return the power level
	 */
	public short getPower() {
		return mPower;
	}

	/**
	 * Get the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return mLabel;
	}
}
