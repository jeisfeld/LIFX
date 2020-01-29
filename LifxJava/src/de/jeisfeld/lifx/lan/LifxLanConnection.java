package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.message.RequestMessage;
import de.jeisfeld.lifx.lan.message.ResponseMessage;
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
	private static final int DEFAULT_ATTEMPTS = 1;
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
	 * The number of devices to be called.
	 */
	private int mExpectedNumDevices;
	/**
	 * The Internet Address to be called.
	 */
	private final InetAddress mInetAddress;
	/**
	 * The port to be used.
	 */
	private final int mPort;

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
	 * @param expectedNumDevices the expected number of devices
	 * @throws SocketException Exception while connecting.
	 */
	public LifxLanConnection(final int sourceId, final byte sequenceNumber, final Integer expectedNumDevices) throws SocketException {
		mSourceId = sourceId;
		mSequenceNumber = sequenceNumber;
		mExpectedNumDevices = expectedNumDevices == null ? Integer.MAX_VALUE : expectedNumDevices;
		mInetAddress = null;
		mPort = LifxLanConnection.UDP_BROADCAST_PORT;
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
	 * @param inetAddress the internet address to be used.
	 * @param port the port to be used.
	 * @throws SocketException Exception while connecting.
	 */
	public LifxLanConnection(final int sourceId, final byte sequenceNumber, final InetAddress inetAddress, final int port) throws SocketException {
		mSourceId = sourceId;
		mSequenceNumber = sequenceNumber;
		mExpectedNumDevices = 1;
		mInetAddress = inetAddress;
		mPort = port;
		mSocket = new DatagramSocket();
		mSocket.setBroadcast(true);
		mSocket.setReuseAddress(true);
		mSocket.setSoTimeout(LifxLanConnection.DEFAULT_TIMEOUT);
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

		int attempts = 0;
		int numDevicesSeen = 0;
		byte[] message = request.getPackedMessage();
		Logger.traceRequest(request);

		List<ResponseMessage> responses = new ArrayList<>();
		List<String> targetAddresses = new ArrayList<>();

		while (numDevicesSeen < mExpectedNumDevices && attempts < LifxLanConnection.DEFAULT_ATTEMPTS) {
			boolean isSent = false;
			long startTime = System.currentTimeMillis();
			boolean timedOut = false;

			while (numDevicesSeen < mExpectedNumDevices && !timedOut) {
				if (!isSent) {
					if (mInetAddress == null) {
						for (InetAddress address : LifxLanConnection.UDP_BROADCAST_ADDRESSES) {
							DatagramPacket requestPacket = new DatagramPacket(message, message.length, address, LifxLanConnection.UDP_BROADCAST_PORT);
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

				timedOut = System.currentTimeMillis() - startTime > LifxLanConnection.DEFAULT_TIMEOUT;
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
	 */
	public ResponseMessage requestWithResponse(final RequestMessage request) {
		mExpectedNumDevices = 1;
		List<ResponseMessage> responses = broadcastWithResponse(request);
		return responses.get(0);
	}

}
