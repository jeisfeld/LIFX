package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateInfo.
 */
public class StateInfo extends ResponseMessage {
	/**
	 * The current time.
	 */
	private Date mTime;
	/**
	 * The uptime.
	 */
	private Duration mUptime;
	/**
	 * The downtime.
	 */
	private Duration mDowntime;

	/**
	 * Create a StateLabel from message data.
	 *
	 * @param packet The message data.
	 */
	public StateInfo(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_INFO;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mTime = new Date(byteBuffer.getLong() / 1000000); // MAGIC_NUMBER
		mUptime = Duration.ofNanos(byteBuffer.getLong()); // MAGIC_NUMBER
		mDowntime = Duration.ofNanos(byteBuffer.getLong()); // MAGIC_NUMBER
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Current Time", mTime.toString());
		payloadFields.put("Uptime", TypeUtil.toString(mUptime));
		payloadFields.put("Downtime", TypeUtil.toString(mDowntime));
		return payloadFields;
	}

	/**
	 * Get the current time.
	 *
	 * @return the current time
	 */
	public final Date getTime() {
		return mTime;
	}

	/**
	 * Get the uptime.
	 *
	 * @return the uptime
	 */
	public final Duration getUptime() {
		return mUptime;
	}

	/**
	 * Get the last downtime duration.
	 *
	 * @return the last downtime duration.
	 */
	public final Duration getDowntime() {
		return mDowntime;
	}

}
