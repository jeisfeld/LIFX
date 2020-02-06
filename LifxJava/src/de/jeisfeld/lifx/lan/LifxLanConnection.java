package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.message.GetService;
import de.jeisfeld.lifx.lan.message.RequestMessage;
import de.jeisfeld.lifx.lan.message.ResponseMessage;
import de.jeisfeld.lifx.lan.message.StateService;
import de.jeisfeld.lifx.lan.util.Logger;

/**
 * Handler for a UDP connection.
 */
public class LifxLanConnection {
	/**
	 * The default timeout.
	 */
	private static final int DEFAULT_TIMEOUT = 1000;
	/**
	 * The default number of attempts.
	 */
	private static final int DEFAULT_ATTEMPTS = 2;
	/**
	 * The buffer size.
	 */
	private static final int BUFFER_SIZE = 1024;
	/**
	 * The UDP port.
	 */
	private static final int UDP_BROADCAST_PORT = 56700;
	/**
	 * The ist of UDP broadcast addresses.
	 */
	private static final InetAddress[] UDP_BROADCAST_ADDRESSES;
	/**
	 * The sourceId.
	 */
	private final int mSourceId;
	/**
	 * The target address.
	 */
	private final String mTargetAddress;
	/**
	 * The Internet Address to be called.
	 */
	private final InetAddress mInetAddress;
	/**
	 * The port to be used.
	 */
	private final int mPort;
	/**
	 * A filter for devices. Only relevant for GetService.
	 */
	private final DeviceFilter mFilter;

	static {
		InetAddress[] udpAddresses;
		try {
			udpAddresses = LanCheck.getBroadcastAddresses().toArray(new InetAddress[0]);
		}
		catch (SocketException e) {
			udpAddresses = new InetAddress[0];
			Logger.error(e);
		}
		UDP_BROADCAST_ADDRESSES = udpAddresses;
	}

	/**
	 * Create a UDP connection.
	 *
	 * @param sourceId the sourceId
	 * @param filter a filter for devices. Only relevant for GetService.
	 */
	public LifxLanConnection(final int sourceId, final DeviceFilter filter) {
		mSourceId = sourceId;
		mTargetAddress = RequestMessage.BROADCAST_MAC;
		mInetAddress = null;
		mPort = LifxLanConnection.UDP_BROADCAST_PORT;
		mFilter = filter;
	}

	/**
	 * Create a UDP connection.
	 *
	 * @param sourceId the sourceId
	 * @param targetAddress the target address
	 * @param inetAddress the internet address to be used.
	 * @param port the port to be used.
	 */
	public LifxLanConnection(final int sourceId, final String targetAddress, final InetAddress inetAddress, final int port) {
		mSourceId = sourceId;
		mTargetAddress = targetAddress == null ? RequestMessage.BROADCAST_MAC : targetAddress;
		mInetAddress = inetAddress;
		mPort = port;
		mFilter = null;
	}

	/**
	 * Determine a valid sequence number.
	 *
	 * @return a sequence number.
	 */
	private byte getSequenceNumber() {
		return (byte) Thread.currentThread().getId();
	}

	/**
	 * Broadcast a request and receive responses.
	 *
	 * @param request The request to be sent.
	 * @param retryPolicy The retry policy.
	 * @return the list of responses.
	 * @throws SocketException Exception while connecting.
	 */
	public List<ResponseMessage> broadcastWithResponse(final RequestMessage request, final RetryPolicy retryPolicy) throws SocketException {
		byte sequenceNumber = getSequenceNumber();
		request.setSourceId(mSourceId);
		request.setSequenceNumber(sequenceNumber);
		request.setTargetAddress(mTargetAddress);
		final byte[] message = request.getPackedMessage();
		Logger.traceRequest(request);

		int attempt = 0;
		int numDevicesSeen = 0;
		List<ResponseMessage> responses = new ArrayList<>();
		List<String> targetAddresses = new ArrayList<>();

		while (numDevicesSeen < retryPolicy.getExpectedResponses() && attempt < retryPolicy.getAttempts()) {
			try {
				boolean isSent = false;
				long startTime = System.currentTimeMillis();

				DatagramSocket socket = new DatagramSocket();
				socket.setBroadcast(true);
				socket.setReuseAddress(true);
				socket.setSoTimeout(retryPolicy.getTimeout(attempt));
				boolean timedOut = false;

				while (numDevicesSeen < retryPolicy.getExpectedResponses() && !timedOut) {
					if (!isSent) {
						if (mInetAddress == null) {
							for (InetAddress address : LifxLanConnection.UDP_BROADCAST_ADDRESSES) {
								DatagramPacket requestPacket = new DatagramPacket(message, message.length, address, mPort);
								try {
									socket.send(requestPacket);
								}
								catch (IOException e) {
									Logger.error(e);
								}
							}
						}
						else {
							DatagramPacket requestPacket = new DatagramPacket(message, message.length, mInetAddress, mPort);
							try {
								socket.send(requestPacket);
							}
							catch (IOException e) {
								Logger.error(e);
							}
						}
						isSent = true;
					}
					DatagramPacket responsePacket = new DatagramPacket(new byte[LifxLanConnection.BUFFER_SIZE], LifxLanConnection.BUFFER_SIZE);
					try {
						socket.receive(responsePacket);
						ResponseMessage responseMessage = ResponseMessage.createResponseMessage(responsePacket);
						boolean isMatch = request.matches(responseMessage);
						if (mFilter != null && request instanceof GetService) {
							Device device = ((StateService) responseMessage).getDevice().getDeviceProduct();
							isMatch = isMatch && mFilter.matches(device);
						}

						if (isMatch) {
							Logger.traceResponse(responseMessage);

							if (!targetAddresses.contains(responseMessage.getTargetAddress())) {
								targetAddresses.add(responseMessage.getTargetAddress());
								numDevicesSeen++;
								responses.add(responseMessage);
							}
						}
						else {
							Logger.info("Ignoring response " + responseMessage);
						}

					}
					catch (SocketTimeoutException e) {
						retryPolicy.onException(attempt, e);
					}
					catch (IOException e) {
						Logger.error(e);
					}

					timedOut = System.currentTimeMillis() - startTime > retryPolicy.getTimeout(attempt);
				}
				socket.close();
			}
			catch (SocketException e) {
				if (attempt < retryPolicy.getAttempts() - 1) {
					retryPolicy.onException(attempt, e);
				}
				else {
					throw e;
				}
			}
			attempt++;
		}
		return responses;
	}

	/**
	 * Send a request and receive single response.
	 *
	 * @param request The request to be sent.
	 * @return the response.
	 * @exception IOException No response.
	 */
	public ResponseMessage requestWithResponse(final RequestMessage request) throws IOException {
		List<ResponseMessage> responses = broadcastWithResponse(request, new RetryPolicy() {
		});
		if (responses.size() == 0) {
			throw new IOException("Did not get response from socket.");
		}
		else {
			return responses.get(0);
		}
	}

	/**
	 * An interface filtering devices.
	 */
	public interface DeviceFilter {
		/**
		 * Filter method for devices.
		 *
		 * @param device The device.
		 * @return true if the device matches the filter.
		 */
		boolean matches(Device device);
	}

	/**
	 * A retry policy for the connection.
	 */
	public interface RetryPolicy {
		/**
		 * Get the number of attempts/retries.
		 *
		 * @return The number of attempts/retries.
		 */
		default int getAttempts() {
			return LifxLanConnection.DEFAULT_ATTEMPTS;
		}

		/**
		 * Get the timeout.
		 *
		 * @param attempt The attempt number (starting with 0).
		 * @return The timeout for this attempt.
		 */
		default int getTimeout(final int attempt) {
			return LifxLanConnection.DEFAULT_TIMEOUT;
		}

		/**
		 * Get the expected number of responses.
		 *
		 * @return The expected number of responses.
		 */
		default int getExpectedResponses() {
			return 1;
		}

		/**
		 * Action to be done if an IOException occurs.
		 *
		 * @param attempt The attempt number (starting with 0).
		 * @param e The exception.
		 */
		default void onException(final int attempt, final IOException e) {
			// do nothing.
		}
	}

}
