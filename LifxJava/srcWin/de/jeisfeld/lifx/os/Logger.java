package de.jeisfeld.lifx.os;

import de.jeisfeld.lifx.lan.message.RequestMessage;
import de.jeisfeld.lifx.lan.message.ResponseMessage;

/**
 * Helper class for logging.
 */
public final class Logger {
	/**
	 * Flag indicating if details should be logged.
	 */
	private static boolean mLogDetails = false;

	/**
	 * Indicate if details should be logged.
	 *
	 * @param logDetails Flag indicating if details should be logged.
	 */
	public static void setLogDetails(final boolean logDetails) {
		Logger.mLogDetails = logDetails;
	}

	// SYSTEMOUT:OFF
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
		if (Logger.mLogDetails) {
			System.out.println("SEND: " + message.toString());
		}
	}

	/**
	 * Log a UDP response message.
	 *
	 * @param message The response message.
	 */
	public static void traceResponse(final ResponseMessage message) {
		if (Logger.mLogDetails) {
			System.out.println("RECV: " + message.toString());
		}
	}

	/**
	 * Log an exception.
	 *
	 * @param e The exception
	 */
	public static void error(final Exception e) {
		if (Logger.mLogDetails) {
			e.printStackTrace();
		}
		else {
			System.err.println(e.toString());
//			e.printStackTrace();
		}
	}

	/**
	 * Log a message.
	 *
	 * @param message the message
	 */
	public static void info(final String message) {
		if (Logger.mLogDetails) {
			System.out.println(message);
		}
	}
}
