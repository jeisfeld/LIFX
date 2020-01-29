package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateHostInfo.
 */
public class StateWifiInfo extends ResponseMessage {
	/**
	 * The signal strength.
	 */
	private float mSignalStrength;
	/**
	 * The number of bytes sent.
	 */
	private int mBytesSent;
	/**
	 * The number of bytes received.
	 */
	private int mBytesReceived;

	/**
	 * Create a StateLabel from message data.
	 *
	 * @param packet The message data.
	 */
	public StateWifiInfo(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_WIFI_INFO;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mSignalStrength = byteBuffer.getFloat();
		mBytesSent = byteBuffer.getInt();
		mBytesReceived = byteBuffer.getInt();
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Signal Strength", Float.toString(mSignalStrength));
		payloadFields.put("Bytes Sent", TypeUtil.toUnsignedString(mBytesSent));
		payloadFields.put("Bytes Received", TypeUtil.toUnsignedString(mBytesReceived));
		return payloadFields;
	}

	/**
	 * Get the signal strength.
	 *
	 * @return the signal strength
	 */
	public final float getSignalStrength() {
		return mSignalStrength;
	}

	/**
	 * Get the number of bytes sent.
	 *
	 * @return the number of bytes sent.
	 */
	public final int getBytesSent() {
		return mBytesSent;
	}

	/**
	 * Get the number of bytes received.
	 *
	 * @return the number of bytes received.
	 */
	public final int getBytesReceived() {
		return mBytesReceived;
	}

}
