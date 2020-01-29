package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateGroup.
 */
public class StateGroup extends ResponseMessage {
	/**
	 * The location.
	 */
	private byte[] mLocation;
	/**
	 * The location label.
	 */
	private String mLabel;
	/**
	 * The update time.
	 */
	private Date mUpdateTime;

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
		mLocation = new byte[16]; // MAGIC_NUMBER
		System.arraycopy(payload, 0, mLocation, 0, mLocation.length);

		byte[] labelBytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(payload, 16, labelBytes, 0, labelBytes.length); // MAGIC_NUMBER
		mLabel = TypeUtil.toString(labelBytes);

		ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mUpdateTime = new Date(byteBuffer.getLong(48) / 1000000); // MAGIC_NUMBER
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Location", TypeUtil.toHex(mLocation, false));
		payloadFields.put("Label", mLabel);
		payloadFields.put("UpdateTime", mUpdateTime.toString());
		return payloadFields;
	}

	/**
	 * Get the vendor.
	 *
	 * @return the vendor
	 */
	public String getLabel() {
		return mLabel;
	}

	/**
	 * Get the location.
	 *
	 * @return the location
	 */
	public final byte[] getLocation() {
		return mLocation;
	}

	/**
	 * Get the updateTime.
	 *
	 * @return the updateTime
	 */
	public final Date getUpdateTime() {
		return mUpdateTime;
	}

}
