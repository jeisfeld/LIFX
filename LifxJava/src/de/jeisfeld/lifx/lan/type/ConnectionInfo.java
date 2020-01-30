package de.jeisfeld.lifx.lan.type;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding connection info.
 */
public class ConnectionInfo {
	/**
	 * The signal strength.
	 */
	private final float mSignalStrength;
	/**
	 * The number of bytes sent.
	 */
	private final int mBytesSent;
	/**
	 * The number of bytes received.
	 */
	private final int mBytesReceived;

	/**
	 * Constructor.
	 *
	 * @param signalStrength the signal strength.
	 * @param bytesSent the number of bytes sent.
	 * @param bytesReceived the number of bytes received.
	 */
	public ConnectionInfo(final float signalStrength, final int bytesSent, final int bytesReceived) {
		mSignalStrength = signalStrength;
		mBytesSent = bytesSent;
		mBytesReceived = bytesReceived;
	}

	@Override
	public final String toString() {
		return "[signal:" + mSignalStrength + ", tx:" + TypeUtil.toUnsignedString(mBytesSent) + ", rx:" + TypeUtil.toUnsignedString(mBytesReceived)
				+ "]";
	}

	/**
	 * Get the signal strength.
	 *
	 * @return the signal strength
	 */
	public final float getSignalStrength() {
		return mSignalStrength;
	}

	/**
	 * Get the number of bytes sent.
	 *
	 * @return the number of bytes sent.
	 */
	public final int getBytesSent() {
		return mBytesSent;
	}

	/**
	 * Get the number of bytes received.
	 *
	 * @return the number of bytes received.
	 */
	public final int getBytesReceived() {
		return mBytesReceived;
	}
}
