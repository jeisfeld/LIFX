package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.Group;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateGroup.
 */
public class StateGroup extends ResponseMessage {
	/**
	 * The group.
	 */
	private Group mGroup;

	/**
	 * Create a StateGroup from message data.
	 *
	 * @param packet The message data.
	 */
	public StateGroup(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_GROUP;
	}

	@Override
	protected final void evaluatePayload() {
		byte[] payload = getPayload();
		byte[] groupId = new byte[16]; // MAGIC_NUMBER
		System.arraycopy(payload, 0, groupId, 0, groupId.length);

		byte[] labelBytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(payload, 16, labelBytes, 0, labelBytes.length); // MAGIC_NUMBER
		String groupLabel = TypeUtil.toString(labelBytes);

		ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		Date updateTime = new Date(byteBuffer.getLong(48) / 1000000); // MAGIC_NUMBER

		mGroup = new Group(groupId, groupLabel, updateTime);
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Group ID", TypeUtil.toHex(mGroup.getGroupId(), false));
		payloadFields.put("Group Label", mGroup.getGroupLabel());
		payloadFields.put("Location Update Time", mGroup.getUpdateTime().toString());
		return payloadFields;
	}

	/**
	 * Get the group.
	 *
	 * @return the group
	 */
	public Group getGroup() {
		return mGroup;
	}
}
