package de.jeisfeld.lifx.lan.util;

import java.nio.charset.StandardCharsets;

/**
 * Helper class for converting types.
 */
public final class TypeUtil {
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
			sb.replace(sb.length() - 1, sb.length(), "]");
		}
		return sb.toString();
	}

}
