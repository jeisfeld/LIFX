package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateLabel.
 */
public class StateLabel extends ResponseMessage {
	/**
	 * The label.
	 */
	private String mLabel;

	/**
	 * Create a StateLabel from message data.
	 *
	 * @param packet The message data.
	 */
	public StateLabel(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_LABEL;
	}

	@Override
	protected final void evaluatePayload() {
		byte[] payload = getPayload();
		mLabel = TypeUtil.toString(payload);
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Label", mLabel);
		return payloadFields;
	}

	/**
	 * Get the vendor.
	 *
	 * @return the vendor
	 */
	public String getLabel() {
		return mLabel;
	}
}
