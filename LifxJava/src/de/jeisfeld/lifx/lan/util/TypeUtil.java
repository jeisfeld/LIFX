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
	public static String toUnsignedString(final Byte b) {
		return b == null ? null : Integer.toString(TypeUtil.toUnsignedInt(b));
	}

	/**
	 * Get unsigned String representation of a short.
	 *
	 * @param s The short
	 * @return The unsigned String.
	 */
	public static String toUnsignedString(final Short s) {
		return s == null ? null : Integer.toString(TypeUtil.toUnsignedInt(s));
	}

	/**
	 * Get unsigned String representation of an integer.
	 *
	 * @param i The integer
	 * @return The unsigned String.
	 */
	public static String toUnsignedString(final Integer i) {
		return i == null ? null : Long.toString(0xffffffffL & i); // MAGIC_NUMBER
	}

	/**
	 * Convert an unsigned byte to integer.
	 *
	 * @param b The unsigned byte.
	 * @return The corresponding integer.
	 */
	public static int toUnsignedInt(final byte b) {
		return 0xff & b; // MAGIC_NUMBER
	}

	/**
	 * Convert an unsigned short to integer.
	 *
	 * @param s The unsigned short.
	 * @return The corresponding integer.
	 */
	public static int toUnsignedInt(final short s) {
		return 0xffff & s; // MAGIC_NUMBER
	}

	/**
	 * Convert an unsigned integer to unsigned short.
	 *
	 * @param i the unsigned integer
	 * @return the unsigned short
	 */
	public static short toUnsignedShort(final double i) {
		return (short) Math.min(0xffff, Math.max(0, i)); // MAGIC_NUMBER
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
		if (duration == null) {
			return null;
		}
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
		return (short) (65535.0 * Math.min(1, Math.max(0, value))); // MAGIC_NUMBER
	}

	/**
	 * Convert short value to 0 - 1 double value.
	 *
	 * @param value the short value
	 * @return The double value
	 */
	public static double toDouble(final short value) {
		return TypeUtil.toUnsignedInt(value) / 65535.0; // MAGIC_NUMBER
	}

	/**
	 * Convert an array of boolean flags to a long. The lower byte on the long is used to store the size of the array.
	 *
	 * @param flags The array of boolean flags.
	 * @return The long.
	 */
	public static long toLong(final boolean[] flags) {
		long n = 0;
		for (boolean b : flags) {
			n = (n << 1) | (b ? 1 : 0);
		}
		return n * 256 + flags.length; // MAGIC_NUMBER
	}

	/**
	 * Convert a long into an array of boolean flags.
	 *
	 * @param flags The long representing boolean flags.
	 * @return The array of flags.
	 */
	public static boolean[] toBooleanArray(final long flags) {
		int size = (int) (flags % 256); // MAGIC_NUMBER
		long n = flags / 256; // MAGIC_NUMBER
		boolean[] result = new boolean[size];
		for (int i = size - 1; i >= 0; i--) {
			result[i] = (n & 1) == 1;
			n = n / 2;
		}
		return result;
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
