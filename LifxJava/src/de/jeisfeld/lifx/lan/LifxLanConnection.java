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
	 * The UDP socket.
	 */
	private final DatagramSocket mSocket;
	/**
	 * The sourceId.
	 */
	private final int mSourceId;
	/**
	 * The sequence number.
	 */
	private final byte mSequenceNumber;
	/**
	 * The target address.
	 */
	private final String mTargetAddress;
	/**
	 * The number of devices to be called.
	 */
	private int mExpectedNumDevices;
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
	 * @param sequenceNumber the sequence number
	 * @param timeout the timeout
	 * @param attempts the number of attempts
	 * @param expectedNumDevices the expected number of devices
	 * @param filter a filter for devices. Only relevant for GetService.
	 * @throws SocketException Exception while connecting.
	 */
	public LifxLanConnection(final int sourceId, final byte sequenceNumber, final int timeout, final int attempts,
			final Integer expectedNumDevices, final DeviceFilter filter) throws SocketException {
		mSourceId = sourceId;
		mSequenceNumber = sequenceNumber;
		mTargetAddress = RequestMessage.BROADCAST_MAC;
		mTimeout = timeout;
		mAttempts = attempts;
		mExpectedNumDevices = expectedNumDevices == null ? Integer.MAX_VALUE : expectedNumDevices;
		mInetAddress = null;
		mPort = LifxLanConnection.UDP_BROADCAST_PORT;
		mFilter = filter;
		mSocket = new DatagramSocket();
		mSocket.setBroadcast(true);
		mSocket.setReuseAddress(true);
		mSocket.setSoTimeout(LifxLanConnection.DEFAULT_TIMEOUT);
	}

	/**
	 * Create a UDP connection.
	 *
	 * @param sourceId the sourceId
	 * @param sequenceNumber the sequence number
	 * @param timeout the timeout.
	 * @param attempts the number of attempts
	 * @param targetAddress the target address
	 * @param inetAddress the internet address to be used.
	 * @param port the port to be used.
	 * @throws SocketException Exception while connecting.
	 */
	public LifxLanConnection(final int sourceId, final byte sequenceNumber, final int timeout, final int attempts, final String targetAddress,
			final InetAddress inetAddress, final int port) throws SocketException {
		mSourceId = sourceId;
		mSequenceNumber = sequenceNumber;
		mTargetAddress = targetAddress == null ? RequestMessage.BROADCAST_MAC : targetAddress;
		mTimeout = timeout;
		mAttempts = attempts;
		mExpectedNumDevices = 1;
		mInetAddress = inetAddress;
		mPort = port;
		mFilter = null;
		mSocket = new DatagramSocket();
		mSocket.setBroadcast(true);
		mSocket.setReuseAddress(true);
		mSocket.setSoTimeout(LifxLanConnection.DEFAULT_TIMEOUT);
	}

	/**
	 * Create a UDP connection.
	 *
	 * @param sourceId the sourceId
	 * @param sequenceNumber the sequence number
	 * @param targetAddress the target address
	 * @param inetAddress the internet address to be used.
	 * @param port the port to be used.
	 * @throws SocketException Exception while connecting.
	 */
	public LifxLanConnection(final int sourceId, final byte sequenceNumber, final String targetAddress, final InetAddress inetAddress, final int port)
			throws SocketException {
		this(sourceId, sequenceNumber, LifxLanConnection.DEFAULT_TIMEOUT, LifxLanConnection.DEFAULT_ATTEMPTS, targetAddress, inetAddress, port);
	}

	/**
	 * Close the connection.
	 */
	public void close() {
		mSocket.close();
	}

	/**
	 * Broadcast a request and receive responses.
	 *
	 * @param request The request to be sent.
	 * @return the list of responses.
	 */
	public List<ResponseMessage> broadcastWithResponse(final RequestMessage request) {
		request.setSourceId(mSourceId);
		request.setSequenceNumber(mSequenceNumber);
		request.setTargetAddress(mTargetAddress);

		int attempts = 0;
		int numDevicesSeen = 0;
		byte[] message = request.getPackedMessage();
		Logger.traceRequest(request);

		List<ResponseMessage> responses = new ArrayList<>();
		List<String> targetAddresses = new ArrayList<>();

		while (numDevicesSeen < mExpectedNumDevices && attempts < mAttempts) {
			boolean isSent = false;
			long startTime = System.currentTimeMillis();
			boolean timedOut = false;

			while (numDevicesSeen < mExpectedNumDevices && !timedOut) {
				if (!isSent) {
					if (mInetAddress == null) {
						for (InetAddress address : LifxLanConnection.UDP_BROADCAST_ADDRESSES) {
							DatagramPacket requestPacket = new DatagramPacket(message, message.length, address, mPort);
							try {
								mSocket.send(requestPacket);
							}
							catch (IOException e) {
								Logger.error(e);
							}
						}
					}
					else {
						DatagramPacket requestPacket = new DatagramPacket(message, message.length, mInetAddress, mPort);
						try {
							mSocket.send(requestPacket);
						}
						catch (IOException e) {
							Logger.error(e);
						}
					}
					isSent = true;
				}
				DatagramPacket responsePacket = new DatagramPacket(new byte[LifxLanConnection.BUFFER_SIZE], LifxLanConnection.BUFFER_SIZE);
				try {
					mSocket.receive(responsePacket);
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
		close();
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
		mExpectedNumDevices = 1;
		List<ResponseMessage> responses = broadcastWithResponse(request);
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
