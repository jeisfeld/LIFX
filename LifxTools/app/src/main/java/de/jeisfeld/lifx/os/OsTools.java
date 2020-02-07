package de.jeisfeld.lifx.os;

import android.os.Process;

/**
 * Windows specific utilities.
 */
public abstract class OsTools {
	/**
	 * Private constructor.
	 */
	private OsTools() {

	}

	/**
	 * Get the pid of the running process.
	 *
	 * @return The pid
	 */
	public static int getPid() {
		return Process.myPid();
	}
}
