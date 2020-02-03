package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type LightState.
 */
public class MultizoneStateZone extends ResponseMessage {
	/**
	 * Flag indicating if this is single zone or multizone response.
	 */
	private final boolean mIsMultizone;
	/**
	 * The count of zones.
	 */
	private byte mCount;
	/**
	 * The zone index.
	 */
	private byte mIndex;
	/**
	 * The list of colors.
	 */
	private List<Color> mColors;

	/**
	 * Create a LightState from message data.
	 *
	 * @param packet The message data.
	 * @param isMultizone Flag indicating if this is multizone message.
	 */
	public MultizoneStateZone(final DatagramPacket packet, final boolean isMultizone) {
		super(packet);
		mIsMultizone = isMultizone;
	}

	@Override
	public final MessageType getMessageType() {
		return mIsMultizone ? MessageType.MULTIZONE_STATE_MULTIZONE : MessageType.MULTIZONE_STATE_ZONE;
	}

	@Override
	protected final void evaluatePayload() {
		byte[] payload = getPayload();
		ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		mCount = byteBuffer.get();
		mIndex = byteBuffer.get();
		mColors = new ArrayList<>();
		while (byteBuffer.hasRemaining()) {
			mColors.add(new Color(byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort(), byteBuffer.getShort()));
		}
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Count", TypeUtil.toUnsignedString(mCount));
		payloadFields.put("Index", TypeUtil.toUnsignedString(mIndex));
		payloadFields.put("Colors", mColors.toString());
		return payloadFields;
	}

	/**
	 * Get the colors.
	 *
	 * @return The colors.
	 */
	public List<Color> getColors() {
		return mColors;
	}

	/**
	 * Get the count of zones.
	 *
	 * @return the count of zones
	 */
	public byte getCount() {
		return mCount;
	}

	/**
	 * Get the zone index.
	 *
	 * @return the zone index
	 */
	public byte getIndex() {
		return mIndex;
	}
}
