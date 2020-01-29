package de.jeisfeld.lifx.lan;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import de.jeisfeld.lifx.lan.util.Logger;

/**
 * Utility class to find addresses in the LAN.
 */
public class LanCheck {
	/**
	 * The list of found addresses.
	 */
	private List<InetAddress> mAddresses;

	/**
	 * Get the list of broadcast addresses.
	 *
	 * @return The list of broadcase addresses.
	 * @throws SocketException Exception with socket.
	 */
	public static List<InetAddress> getBroadcastAddresses() throws SocketException {
		List<InetAddress> result = new ArrayList<>();
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			if (networkInterface != null && !networkInterface.isLoopback() && networkInterface.isUp()) {
				for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcastAddress = address.getBroadcast();
					if (broadcastAddress != null) {
						result.add(broadcastAddress);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Find hosts in local lan.
	 *
	 * @return The list of round hosts.
	 */
	public List<InetAddress> getHostsInLan() {
		mAddresses = new ArrayList<InetAddress>();
		List<CheckerThread> threads = new ArrayList<>();
		try {
			InetAddress local = InetAddress.getLocalHost();
			byte[] ipAddress = local.getAddress();

			for (int i = 1; i <= 255; i++) { // MAGIC_NUMBER
				ipAddress[3] = (byte) i; // MAGIC_NUMBER
				InetAddress address = InetAddress.getByAddress(ipAddress);
				CheckerThread thread = new CheckerThread(address);
				threads.add(thread);
				thread.start();
				Thread.sleep(5); // MAGIC_NUMBER
			}
			for (CheckerThread thread : threads) {
				thread.join();
			}
		}
		catch (Exception e) {
			Logger.error(e);
		}
		return mAddresses;
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

	/**
	 * Utility thread to check reachability of a single host.
	 */
	private final class CheckerThread extends Thread {
		/**
		 * The address to be checked.
		 */
		private final InetAddress mAddress;

		/**
		 * Constructor of a checker thread.
		 *
		 * @param address The address to be checked.
		 */
		private CheckerThread(final InetAddress address) {
			mAddress = address;
		}

		@Override
		public void run() {
			try {
				if (mAddress.isReachable(3000)) { // MAGIC_NUMBER
					mAddresses.add(mAddress);
				}
			}
			catch (Exception e) {
				Logger.error(e);
			}
		}
	}

}
