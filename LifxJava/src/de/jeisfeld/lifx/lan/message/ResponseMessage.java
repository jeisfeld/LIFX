package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Map.Entry;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class for holding LIFX response messages.
 */
public abstract class ResponseMessage {
	/**
	 * The bytes from the message.
	 */
	private byte[] mBytes = null;
	/**
	 * The Internet address of this message.
	 */
	private final InetAddress mInetAddress;
	/**
	 * The target address.
	 */
	private final String mTargetAddress;
	/**
	 * The sequence number.
	 */
	private final byte mSequenceNumber;
	/**
	 * The sourceId.
	 */
	private final int mSourceId;

	/**
	 * Create a response message from message data.
	 *
	 * @param packet The message data.
	 */
	public ResponseMessage(final DatagramPacket packet) {
		mBytes = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), 0, mBytes, 0, packet.getLength());
		ByteBuffer byteBuffer = ByteBuffer.wrap(mBytes);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mTargetAddress = String.format(
				"%02X:%02X:%02X:%02X:%02X:%02X", mBytes[8], mBytes[9], mBytes[10], mBytes[11], mBytes[12], mBytes[13]); // MAGIC_NUMBER
		mSequenceNumber = byteBuffer.get(23); // MAGIC_NUMBER
		mSourceId = byteBuffer.getInt(4); // MAGIC_NUMBER
		mInetAddress = packet.getAddress();
		evaluatePayload();
	}

	/**
	 * Get the message type for this message.
	 *
	 * @return The message type.
	 */
	public abstract MessageType getMessageType();

	/**
	 * Evaluate the message payload.
	 */
	protected abstract void evaluatePayload();

	/**
	 * Get the sourceId of the packed message.
	 *
	 * @return The sourceId.
	 */
	public int getSourceId() {
		return mSourceId;
	}

	/**
	 * Get the sequenceNumber of the packed message.
	 *
	 * @return The sequence number.
	 */
	public byte getSequenceNumber() {
		return mSequenceNumber;
	}

	/**
	 * Get the target address of the packed message.
	 *
	 * @return The target address.
	 */
	public String getTargetAddress() {
		return mTargetAddress;
	}

	/**
	 * Get the Internet Address from which this was sent.
	 *
	 * @return The Internet Address.
	 */
	public InetAddress getInetAddress() {
		return mInetAddress;
	}

	/**
	 * Get the payload from the message.
	 *
	 * @return The payload.
	 */
	public byte[] getPayload() {
		byte[] payload = new byte[mBytes.length - RequestMessage.HEADER_SIZE_BYTES];
		if (payload.length > 0) {
			System.arraycopy(mBytes, RequestMessage.HEADER_SIZE_BYTES, payload, 0, payload.length);
		}
		return payload;
	}

	@Override
	public final String toString() {
		if (mBytes == null) {
			return "[]";
		}
		StringBuilder printer = new StringBuilder(getMessageType().toString())
				.append(" [")
				.append(getSourceId())
				.append(",")
				.append(getSequenceNumber())
				.append(",")
				.append(getTargetAddress())
				.append("] ")
				.append(getPayloadString())
				.append(TypeUtil.toHex(getPayload(), true));
		return printer.toString();
	}

	/**
	 * Get a String to be printed for the payload.
	 *
	 * @return The payload String.
	 */
	protected String getPayloadString() {
		Map<String, String> payloadMap = getPayloadMap();
		if (payloadMap == null || payloadMap.size() == 0) {
			return "";
		}
		StringBuilder result = new StringBuilder("[");
		for (Entry<String, String> entry : payloadMap.entrySet()) {
			result.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
		}
		result.replace(result.length() - 1, result.length(), "] ");
		return result.toString();
	}

	/**
	 * Get a formatted String with the payload content.
	 *
	 * @param prefix a Prefix to be written before each entry.
	 * @return The String.
	 */
	public String getPayloadFormattedString(final String prefix) {
		StringBuilder result = new StringBuilder();
		for (Entry<String, String> entry : getPayloadMap().entrySet()) {
			result.append(TypeUtil.INDENT).append(prefix).append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		}
		return result.toString();
	}

	/**
	 * Get a map containing payload data for String representation.
	 *
	 * @return The map of payload data.
	 */
	protected abstract Map<String, String> getPayloadMap();

	/**
	 * Create a response message from message data.
	 *
	 * @param packet the message data.
	 * @return The response message.
	 */
	public static ResponseMessage createResponseMessage(final DatagramPacket packet) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		MessageType messageType = MessageType.fromValue(byteBuffer.getShort(32)); // MAGIC_NUMBER

		switch (messageType) {
		case STATE_SERVICE:
			return new StateService(packet);
		case STATE_VERSION:
			return new StateVersion(packet);
		case STATE_LABEL:
			return new StateLabel(packet);
		case STATE_LOCATION:
			return new StateLocation(packet);
		case STATE_GROUP:
			return new StateGroup(packet);
		case STATE_HOST_FIRMWARE:
			return new StateHostFirmware(packet);
		case STATE_WIFI_FIRMWARE:
			return new StateWifiFirmware(packet);
		case STATE_INFO:
			return new StateInfo(packet);
		case STATE_HOST_INFO:
			return new StateHostInfo(packet);
		case STATE_WIFI_INFO:
			return new StateWifiInfo(packet);
		case STATE_POWER:
			return new StatePower(packet);
		case LIGHT_STATE_POWER:
			return new LightStatePower(packet);
		case LIGHT_STATE:
			return new LightState(packet);
		case LIGHT_STATE_INFRARED:
			return new LightStateInfrared(packet);
		case ECHO_RESPONSE:
			return new EchoResponse(packet);
		case ACKNOWLEDGEMENT:
			return new Acknowledgement(packet);
		case MULTIZONE_STATE_ZONE:
			return new MultizoneStateZone(packet, false);
		case MULTIZONE_STATE_MULTIZONE:
			return new MultizoneStateZone(packet, true);
		case MULTIZONE_STATE_EXTENDED_COLOR_ZONES:
			return new MultizoneStateExtendedColorZones(packet);
		default:
			return null;
		}
	}

}
