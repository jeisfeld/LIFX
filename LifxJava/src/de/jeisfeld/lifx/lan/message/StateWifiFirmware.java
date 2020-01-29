package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateWifiFirmware.
 */
public class StateWifiFirmware extends ResponseMessage {
	/**
	 * The build time.
	 */
	private Date mBuildTime;
	/**
	 * The minor version.
	 */
	private short mMinorVersion;
	/**
	 * The major version.
	 */
	private short mMajorVersion;

	/**
	 * Create a StateWifiFirmware from message data.
	 *
	 * @param packet The message data.
	 */
	public StateWifiFirmware(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_WIFI_FIRMWARE;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mBuildTime = new Date(byteBuffer.getLong() / 1000000); // MAGIC_NUMBER
		byteBuffer.getLong(); // reserved
		mMinorVersion = byteBuffer.getShort();
		mMajorVersion = byteBuffer.getShort();
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Build Time", mBuildTime.toString());
		payloadFields.put("Minor Version", TypeUtil.toUnsignedString(mMinorVersion));
		payloadFields.put("Major Version", TypeUtil.toUnsignedString(mMajorVersion));
		return payloadFields;
	}

	/**
	 * Get the build time.
	 *
	 * @return the build time
	 */
	public final Date getBuildTime() {
		return mBuildTime;
	}

	/**
	 * Get the minor version.
	 *
	 * @return the minor version
	 */
	public final short getMinorVersion() {
		return mMinorVersion;
	}

	/**
	 * Get the major version.
	 *
	 * @return the major version
	 */
	public final short getMajorVersion() {
		return mMajorVersion;
	}

}
