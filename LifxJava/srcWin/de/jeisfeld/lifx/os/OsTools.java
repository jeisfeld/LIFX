package de.jeisfeld.lifx.os;

import java.lang.management.ManagementFactory;

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
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		return Integer.parseInt(pid);
	}
}
