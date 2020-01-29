package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateService.
 */
public class StateService extends ResponseMessage {
	/**
	 * The service.
	 */
	private byte mService;
	/**
	 * The port.
	 */
	private int mPort;

	/**
	 * Create a StateService from message data.
	 *
	 * @param packet The message data.
	 */
	public StateService(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_SERVICE;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mService = byteBuffer.get();
		mPort = byteBuffer.getInt();
	}

	/**
	 * Get the device associated to this message.
	 *
	 * @return The device.
	 */
	public Device getDevice() {
		return new Device(getTargetAddress(), getInetAddress(), mPort, getSourceId());
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Service", TypeUtil.toUnsignedString(mService));
		payloadFields.put("Port", TypeUtil.toUnsignedString(mPort));
		return payloadFields;
	}

	/**
	 * Get the service.
	 *
	 * @return the service
	 */
	public final byte getService() {
		return mService;
	}

	/**
	 * Get the port.
	 *
	 * @return the port
	 */
	public final int getPort() {
		return mPort;
	}

}
