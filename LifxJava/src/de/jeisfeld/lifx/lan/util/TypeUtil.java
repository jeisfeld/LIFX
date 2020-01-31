package de.jeisfeld.lifx.lan.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Helper class for converting types.
 */
public final class TypeUtil {
	/**
	 * The indentation used in output.
	 */
	public static final String INDENT = "  ";

	/**
	 * Hide the default constructor.
	 */
	private TypeUtil() {
	}

	/**
	 * Get unsigned String representation of a byte.
	 *
	 * @param b The byte
	 * @return The unsigned String.
	 */
	public static String toUnsignedString(final byte b) {
		return Integer.toString(0xff & b); // MAGIC_NUMBER
	}

	/**
	 * Get unsigned String representation of a short.
	 *
	 * @param s The short
	 * @return The unsigned String.
	 */
	public static String toUnsignedString(final short s) {
		return Integer.toString(0xffff & s); // MAGIC_NUMBER
	}

	/**
	 * Get unsigned String representation of an integer.
	 *
	 * @param i The integer
	 * @return The unsigned String.
	 */
	public static String toUnsignedString(final int i) {
		return Long.toString(0xffffffffL & i); // MAGIC_NUMBER
	}

	/**
	 * Convert byte array to String.
	 *
	 * @param b the byte array.
	 * @return The String.
	 */
	public static String toString(final byte[] b) {
		int size = b.length;
		while (b[size - 1] == 0) {
			size--;
		}
		byte[] realBytes = new byte[size];
		System.arraycopy(b, 0, realBytes, 0, size);
		return new String(realBytes, StandardCharsets.UTF_8);
	}

	/**
	 * Convert duration to String.
	 *
	 * @param duration the duration.
	 * @return The String.
	 */
	public static String toString(final Duration duration) {
		StringBuilder result = new StringBuilder();
		long days = duration.toDays();
		if (days > 0) {
			result.append(days).append(" days, ");
		}
		long hours = duration.toHours() - 24 * days; // MAGIC_NUMBER
		if (hours > 0) {
			result.append(hours).append(" hours, ");
		}
		long minutes = duration.toMinutes() - 60 * duration.toHours(); // MAGIC_NUMBER
		if (minutes > 0) {
			result.append(minutes).append(" minutes, ");
		}
		long seconds = duration.getSeconds() - 60 * duration.toMinutes(); // MAGIC_NUMBER
		result.append(seconds).append(" seconds");
		return result.toString();
	}

	/**
	 * Convert byte array to hex String.
	 *
	 * @param byteArray The byte array
	 * @param separated flag indicating if the bytes should be comma separated.
	 * @return The hex String.
	 */
	public static String toHex(final byte[] byteArray, final boolean separated) {
		StringBuilder sb = new StringBuilder();
		if (separated) {
			sb.append("[");
		}
		for (byte b : byteArray) {
			sb.append(String.format("%02X", b));
			if (separated) {
				sb.append(",");
			}
		}
		if (separated) {
			if (byteArray.length == 0) {
				sb.append("]");
			}
			else {
				sb.replace(sb.length() - 1, sb.length(), "]");
			}
		}
		return sb.toString();
	}

	/**
	 * Convert double value 0 - 1 to short value.
	 *
	 * @param value the double value
	 * @return The short value
	 */
	public static short toShort(final double value) {
		return (short) (65535.99999 * Math.min(1, Math.max(0, value))); // MAGIC_NUMBER
	}

	/**
	 * Generate a GUID to be used as new groupId or locationId.
	 *
	 * @return A GUID as byte array.
	 */
	public static byte[] generateGuid() {
		UUID uuid = UUID.randomUUID();
		ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]); // MAGIC_NUMBER
		byteBuffer.putLong(uuid.getMostSignificantBits());
		byteBuffer.putLong(uuid.getLeastSignificantBits());
		return byteBuffer.array();
	}

}
