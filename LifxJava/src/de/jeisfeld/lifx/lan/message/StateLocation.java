package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.Location;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateLocation.
 */
public class StateLocation extends ResponseMessage {
	/**
	 * The location.
	 */
	private Location mLocation;

	/**
	 * Create a StateLocation from message data.
	 *
	 * @param packet The message data.
	 */
	public StateLocation(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_LOCATION;
	}

	@Override
	protected final void evaluatePayload() {
		byte[] payload = getPayload();
		byte[] locationId = new byte[16]; // MAGIC_NUMBER
		System.arraycopy(payload, 0, locationId, 0, locationId.length);

		byte[] labelBytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(payload, 16, labelBytes, 0, labelBytes.length); // MAGIC_NUMBER
		String locationLabel = TypeUtil.toString(labelBytes);

		ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		Date updateTime = new Date(byteBuffer.getLong(48) / 1000000); // MAGIC_NUMBER

		mLocation = new Location(locationId, locationLabel, updateTime);
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Location ID", TypeUtil.toHex(mLocation.getLocationId(), false));
		payloadFields.put("Location Label", mLocation.getLocationLabel());
		payloadFields.put("Location Update Time", mLocation.getUpdateTime().toString());
		return payloadFields;
	}

	/**
	 * Get the location.
	 *
	 * @return the location
	 */
	public Location getLocation() {
		return mLocation;
	}
}
