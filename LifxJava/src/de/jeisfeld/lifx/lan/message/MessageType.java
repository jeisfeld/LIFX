package de.jeisfeld.lifx.lan.message;

import java.util.HashMap;
import java.util.Map;

// JAVADOC:OFF
public enum MessageType {
	GET_SERVICE(2),
	STATE_SERVICE(3),
	GET_HOST_INFO(12),
	STATE_HOST_INFO(13),
	GET_HOST_FIRMWARE(14),
	STATE_HOST_FIRMWARE(15),
	GET_WIFI_INFO(16),
	STATE_WIFI_INFO(17),
	GET_WIFI_FIRMWARE(18),
	STATE_WIFI_FIRMWARE(19),
	GET_POWER(20),
	SET_POWER(21),
	STATE_POWER(22),
	GET_LABEL(23),
	SET_LABEL(24),
	STATE_LABEL(25),
	GET_VERSION(32),
	STATE_VERSION(33),
	GET_INFO(34),
	STATE_INFO(35),
	ACKNOWLEDGEMENT(45),
	GET_LOCATION(48),
	STATE_LOCATION(50),
	GET_GROUP(51),
	STATE_GROUP(53),
	ECHO_REQUEST(58),
	ECHO_RESPONSE(59),
	LIGHT_GET(101),
	LIGHT_SET_COLOR(102),
	LIGHT_SET_WAVEFORM(103),
	LIGHT_STATE(107),
	LIGHT_GET_POWER(116),
	LIGHT_SET_POWER(117),
	LIGHT_STATE_POWER(118),
	LIGHT_GET_INFRARED(120),
	LIGHT_STATE_INFRARED(121),
	LIGHT_SET_INFRARED(122),
	MULTIZONE_SET_COLOR_ZONES(501),
	MULTIZONE_GET_COLOR_ZONES(502),
	MULTIZONE_STATE_ZONE(503),
	MULTIZONE_STATE_MULTIZONE(506),
	GET_DEVICE_CHAIN(701),
	STATE_DEVICE_CHAIN(702),
	SET_USER_POSITION(703),
	GET_TILE_STATE_64(707),
	STATE_TILE_STATE_64(711),
	SET_TILE_STATE_64(715);
	// JAVADOC:ON

	/**
	 * The value of a messageType.
	 */
	private short mValue;
	/**
	 * A map for retrieving messageType from its value.
	 */
	private static final Map<Short, MessageType> MESSAGE_TYPE_MAP = new HashMap<>();

	static {
		for (MessageType messageType : MessageType.values()) {
			MessageType.MESSAGE_TYPE_MAP.put(messageType.getValue(), messageType);
		}
	}

	/**
	 * Constructor with value.
	 *
	 * @param value The value of the messageType.
	 */
	MessageType(final int value) {
		mValue = (short) value;
	}

	/**
	 * Get the numeric value of a messageType.
	 *
	 * @return The value.
	 */
	public short getValue() {
		return mValue;
	}

	/**
	 * Get the messageType from its value.
	 *
	 * @param value The value
	 * @return The messageType.
	 */
	public static MessageType fromValue(final short value) {
		return MessageType.MESSAGE_TYPE_MAP.get(value);
	}

}
