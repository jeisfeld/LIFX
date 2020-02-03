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
 * Response message of type MultizoneStateExtendedColorZones.
 */
public class MultizoneStateExtendedColorZones extends ResponseMessage {
	/**
	 * The count of zones on the device.
	 */
	private short mCount;
	/**
	 * The zone index where this message starts.
	 */
	private short mIndex;
	/**
	 * The number of colors.
	 */
	private byte mColorsCount;
	/**
	 * The list of colors.
	 */
	private List<Color> mColors;

	/**
	 * Create a MultizoneStateExtendedColorZones from message data.
	 *
	 * @param packet The message data.
	 */
	public MultizoneStateExtendedColorZones(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.MULTIZONE_STATE_EXTENDED_COLOR_ZONES;
	}

	@Override
	protected final void evaluatePayload() {
		byte[] payload = getPayload();
		ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		mCount = byteBuffer.getShort();
		mIndex = byteBuffer.getShort();
		mColorsCount = byteBuffer.get();
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
		payloadFields.put("ColorsCount", TypeUtil.toUnsignedString(mColorsCount));
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
	public short getCount() {
		return mCount;
	}

	/**
	 * Get the zone index.
	 *
	 * @return the zone index
	 */
	public short getIndex() {
		return mIndex;
	}

	/**
	 * Get the number of colors.
	 *
	 * @return the number of colors
	 */
	public byte getColorsCount() {
		return mColorsCount;
	}
}
