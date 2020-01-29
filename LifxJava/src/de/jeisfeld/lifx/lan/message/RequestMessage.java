package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * The superclass for generating and holding LIFX messages.
 */
public abstract class RequestMessage {
	/**
	 * The broadcast MAC used if there is no MAC given.
	 */
	public static final String BROADCAST_MAC = "00:00:00:00:00:00";
	/**
	 * The size of the header.
	 */
	protected static final short HEADER_SIZE_BYTES = 36;

	/**
	 * Message size - 16 bits.
	 */
	private short mSize = 0;
	/**
	 * Source ID. 32 bits. Unique ID sent by client. If zero, broadcast reply requested. If non-zero, unicast reply requested.
	 */
	private int mSourceId = 0;
	/**
	 * Target address. 64 bits. Either single MAC address or all zeroes for broadcast.
	 */
	private String mTargetAddress;
	/**
	 * The sequence number. 8 bits.
	 */
	private byte mSequenceNumber = 0;
	/**
	 * The header.
	 */
	private byte[] mHeader;
	/**
	 * The payload.
	 */
	private byte[] mPayload;
	/**
	 * The packed message.
	 */
	private byte[] mPackedMessage = null;

	/**
	 * Generate the byte message from the input.
	 *
	 * @return The message as byte array.
	 */
	private byte[] generatePackedMessage() {
		mPayload = getPayload();
		mHeader = getHeader(); // must come after generation of payload, as header contains total message size.
		byte[] result = new byte[mHeader.length + mPayload.length];
		System.arraycopy(mHeader, 0, result, 0, mHeader.length);
		System.arraycopy(mPayload, 0, result, mHeader.length, mPayload.length);
		return result;
	}

	/**
	 * Get the payload for this message.
	 *
	 * @return The payload.
	 */
	protected abstract byte[] getPayload();

	/**
	 * Get the message type for this message.
	 *
	 * @return The message type.
	 */
	protected abstract MessageType getMessageType();

	/**
	 * Get the message type of the expected response.
	 *
	 * @return The message type of the expected response.
	 */
	protected abstract MessageType getResponseType();

	/**
	 * Get the header bytes.
	 *
	 * @return the header bytes.
	 */
	private byte[] getHeader() {
		mSize = (short) (RequestMessage.HEADER_SIZE_BYTES + mPayload.length);
		byte[] frameAddress = getFrameAddress();
		byte[] frame = getFrame();
		byte[] protocolHeader = getProtocolHeader();
		byte[] result = new byte[frameAddress.length + frame.length + protocolHeader.length];
		System.arraycopy(frame, 0, result, 0, frame.length);
		System.arraycopy(frameAddress, 0, result, frame.length, frameAddress.length);
		System.arraycopy(protocolHeader, 0, result, frame.length + frameAddress.length, protocolHeader.length);
		return result;
	}

	/**
	 * Get the frame bytes.
	 *
	 * @return the frame bytes.
	 */
	private byte[] getFrame() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putShort(mSize);
		byteBuffer.putShort(
				RequestMessage.BROADCAST_MAC.equals(mTargetAddress) ? (short) 0b0011010000000000 : (short) 0b0001010000000000); // MAGIC_NUMBER
		byteBuffer.putInt(mSourceId);
		return byteBuffer.array();
	}

	/**
	 * Get the frameAddress bytes.
	 *
	 * @return the frameAddress bytes.
	 */
	private byte[] getFrameAddress() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(16); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		// 6 bytes for MAC address
		String[] macAddressParts = mTargetAddress.split(":");
		for (int i = 0; i < 6; i++) { // MAGIC_NUMBER
			byteBuffer.put((byte) Integer.parseInt(macAddressParts[i], 16)); // MAGIC_NUMBER
		}

		// 8 empty bytes - 2 from MAC address and 6 reserved.
		byteBuffer.putLong(0);

		boolean acknowledgementRequired = getResponseType() == MessageType.ACKNOWLEDGEMENT;
		boolean responseRequired = getResponseType() != null && getResponseType() != MessageType.ACKNOWLEDGEMENT;

		byteBuffer.put((byte) ((responseRequired ? 1 : 0) + (acknowledgementRequired ? 2 : 0)));
		byteBuffer.put(mSequenceNumber);

		return byteBuffer.array();
	}

	/**
	 * Get the protocolHeader bytes.
	 *
	 * @return the protocolHeader bytes.
	 */
	private byte[] getProtocolHeader() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(12); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		byteBuffer.putLong(0);
		byteBuffer.putShort(getMessageType().getValue());
		byteBuffer.putShort((short) 0);

		return byteBuffer.array();
	}

	@Override
	public final String toString() {
		StringBuilder printer = new StringBuilder(getMessageType().toString())
				.append(" [")
				.append(mSourceId)
				.append(",")
				.append(mSequenceNumber)
				.append(",")
				.append(mTargetAddress)
				.append("] ")
				.append(TypeUtil.toHex(mPackedMessage, true));
		return printer.toString();
	}

	/**
	 * Set the sequence number.
	 *
	 * @param sequenceNumber The sequence number.
	 */
	public void setSequenceNumber(final byte sequenceNumber) {
		mSequenceNumber = sequenceNumber;
		mPackedMessage = null;
	}

	/**
	 * Set the sourceId.
	 *
	 * @param sourceId The sourceId.
	 */
	public void setSourceId(final int sourceId) {
		mSourceId = sourceId;
		mPackedMessage = null;
	}

	/**
	 * Set the target address.
	 *
	 * @param targetAddress the target address
	 */
	public void setTargetAddress(final String targetAddress) {
		mTargetAddress = targetAddress;
		mPackedMessage = null;
	}

	/**
	 * Get the packed message.
	 *
	 * @return the packed message.
	 */
	public byte[] getPackedMessage() {
		if (mPackedMessage == null) {
			mPackedMessage = generatePackedMessage();
		}
		return mPackedMessage;
	}

	/**
	 * Check if the other message has matching messageType, sourceId and sequenceNumber.
	 *
	 * @param otherMessage The other message.
	 * @return true if matching.
	 */
	public boolean matches(final ResponseMessage otherMessage) {
		boolean isTargetAddressValid = RequestMessage.BROADCAST_MAC.equals(mTargetAddress)
				? !RequestMessage.BROADCAST_MAC.equals(otherMessage.getTargetAddress())
				: (RequestMessage.BROADCAST_MAC.equals(otherMessage.getTargetAddress()) || mTargetAddress.equals(otherMessage.getTargetAddress()));

		return getResponseType() == otherMessage.getMessageType() && mSourceId == otherMessage.getSourceId()
				&& mSequenceNumber == otherMessage.getSequenceNumber() && isTargetAddressValid;
	}

}
