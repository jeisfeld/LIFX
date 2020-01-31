package de.jeisfeld.lifx.lan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import de.jeisfeld.lifx.lan.Location;

/**
 * Request message of type SetLocation.
 */
public class SetLocation extends RequestMessage {
	/**
	 * The location.
	 */
	private final Location mLocation;

	/**
	 * Create SetLocation.
	 *
	 * @param location the location.
	 */
	public SetLocation(final Location location) {
		mLocation = location;
	}

	@Override
	protected final byte[] getPayload() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(56); // MAGIC_NUMBER
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(mLocation.getLocationId());

		byte[] labelBytes = mLocation.getLocationLabel().getBytes(StandardCharsets.UTF_8);
		byte[] bytes = new byte[32]; // MAGIC_NUMBER
		System.arraycopy(labelBytes, 0, bytes, 0, Math.min(labelBytes.length, bytes.length));
		byteBuffer.put(bytes);

		byteBuffer.putLong(mLocation.getUpdateTime().getTime() * 1000000); // MAGIC_NUMBER - timestamp in nanoseconds
		return byteBuffer.array();
	}

	@Override
	protected final MessageType getMessageType() {
		return MessageType.SET_LOCATION;
	}

	@Override
	protected final MessageType getResponseType() {
		return MessageType.ACKNOWLEDGEMENT;
	}

}
