package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import de.jeisfeld.lifx.lan.Group;

/**
 * Request message of type SetGroup.
 */
public class SetGroup extends RequestMessage {
	/**
	 * The group.
	 */
	private final Group mGroup;

	/**
	 * Create SetGroup.
	 *
	 * @param group the group.
	 */
	public SetGroup(final Group group) {
		mGroup = group;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(56); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(mGroup.getGroupId());

		byte[] labelBytes = mGroup.getGroupLabel().getBytes(StandardCharsets.UTF_8);
		byte[] bytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(labelBytes, 0, bytes, 0, Math.min(labelBytes.length, bytes.length));
		byteBuffer.put(bytes);

		byteBuffer.putLong(mGroup.getUpdateTime().getTime() * 1000000); // MAGIC_NUMBER - timestamp in nanoseconds
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.SET_GROUP;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
