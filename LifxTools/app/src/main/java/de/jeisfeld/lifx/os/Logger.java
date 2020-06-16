package de.jeisfeld.lifx.os;

import android.util.Log;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.lan.message.RequestMessage;
import de.jeisfeld.lifx.lan.message.ResponseMessage;

/**
 * Helper class for logging.
 */
public final class Logger {
	/**
	 * The application tag for logging.
	 */
	private static final String TAG = Application.TAG;

	/**
	 * Hide the default constructor.
	 */
	private Logger() {
	}

	/**
	 * Log a UDP request.
	 *
	 * @param message The request message.
	 */
	public static void traceRequest(final RequestMessage message) {
		Log.d(TAG, "SEND: " + message.toString());
	}

	/**
	 * Log a UDP response message.
	 *
	 * @param message The response message.
	 * @param isIgnored flag indicating if the message is ignored.
	 */
	public static void traceResponse(final ResponseMessage message, final boolean isIgnored) {
		Log.d(TAG, (isIgnored ? "RECX: " : "RECV: ") + message.toString());
	}

	/**
	 * Log an exception.
	 *
	 * @param e The exception
	 */
	public static void error(final Exception e) {
		Log.e(TAG, e.toString(), e);
	}

	/**
	 * Log a message.
	 *
	 * @param message the message
	 */
	public static void info(final String message) {
		Log.i(TAG, message);
	}

	/**
	 * Log a message temporarily.
	 *
	 * @param message the message
	 */
	public static void log(final String message) {
		Log.i(TAG, message);
	}
}
