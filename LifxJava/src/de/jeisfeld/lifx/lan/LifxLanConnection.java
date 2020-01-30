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
	private static final int DEFAULT_TIMEOUT = 2000;
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
	 * The timeout.
	 */
	private final int mTimeout;
	/**
	 * The number of attempts.
	 */
	private final int mAttempts;
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
	 * @param timeout the timeout
	 * @param attempts the number of attempts
	 * @param filter a filter for devices. Only relevant for GetService.
	 */
	public LifxLanConnection(final int sourceId, final int timeout, final int attempts, final DeviceFilter filter) {
		mSourceId = sourceId;
		mTargetAddress = RequestMessage.BROADCAST_MAC;
		mTimeout = timeout;
		mAttempts = attempts;
		mInetAddress = null;
		mPort = LifxLanConnection.UDP_BROADCAST_PORT;
		mFilter = filter;
	}

	/**
	 * Create a UDP connection.
	 *
	 * @param sourceId the sourceId
	 * @param timeout the timeout.
	 * @param attempts the number of attempts
	 * @param targetAddress the target address
	 * @param inetAddress the internet address to be used.
	 * @param port the port to be used.
	 */
	public LifxLanConnection(final int sourceId, final int timeout, final int attempts, final String targetAddress,
			final InetAddress inetAddress, final int port) {
		mSourceId = sourceId;
		mTargetAddress = targetAddress == null ? RequestMessage.BROADCAST_MAC : targetAddress;
		mTimeout = timeout;
		mAttempts = attempts;
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
	 * Create a UDP connection.
	 *
	 * @param sourceId the sourceId
	 * @param targetAddress the target address
	 * @param inetAddress the internet address to be used.
	 * @param port the port to be used.
	 */
	public LifxLanConnection(final int sourceId, final String targetAddress, final InetAddress inetAddress, final int port) {
		this(sourceId, LifxLanConnection.DEFAULT_TIMEOUT, LifxLanConnection.DEFAULT_ATTEMPTS, targetAddress, inetAddress, port);
	}

	/**
	 * Broadcast a request and receive responses.
	 *
	 * @param request The request to be sent.
	 * @param numResponses the expected number of responses.
	 * @return the list of responses.
	 * @throws SocketException Exception while connecting.
	 */
	public List<ResponseMessage> broadcastWithResponse(final RequestMessage request, final Integer numResponses) throws SocketException {
		byte sequenceNumber = getSequenceNumber();
		request.setSourceId(mSourceId);
		request.setSequenceNumber(sequenceNumber);
		request.setTargetAddress(mTargetAddress);

		DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);
		socket.setReuseAddress(true);
		socket.setSoTimeout(LifxLanConnection.DEFAULT_TIMEOUT);

		int attempts = 0;
		int numDevicesSeen = 0;
		byte[] message = request.getPackedMessage();
		Logger.traceRequest(request);

		List<ResponseMessage> responses = new ArrayList<>();
		List<String> targetAddresses = new ArrayList<>();

		while ((numResponses == null || numDevicesSeen < numResponses) && attempts < mAttempts) {
			boolean isSent = false;
			long startTime = System.currentTimeMillis();
			boolean timedOut = false;

			while ((numResponses == null || numDevicesSeen < numResponses) && !timedOut) {
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
					// ignore
				}
				catch (IOException e) {
					Logger.error(e);
				}

				timedOut = System.currentTimeMillis() - startTime > mTimeout;
			}
			attempts++;
		}
		socket.close();
		return responses;
	}

	/**
	 * Send a request and receive single response.
	 *
	 * @param request The request to be sent.
	 * @return the response.
	 * @exception SocketException No response.
	 */
	public ResponseMessage requestWithResponse(final RequestMessage request) throws SocketException {
		List<ResponseMessage> responses = broadcastWithResponse(request, 1);
		if (responses.size() == 0) {
			throw new SocketException("Did not get response from socket.");
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

}
