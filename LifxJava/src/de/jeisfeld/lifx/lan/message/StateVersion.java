package de.jeisfeld.lifx.lan.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Response message of type StateVersion.
 */
public class StateVersion extends ResponseMessage {
	/**
	 * The vendor.
	 */
	private int mVendor;
	/**
	 * The product.
	 */
	private int mProduct;
	/**
	 * The version.
	 */
	private int mVersion;

	/**
	 * Create a StateVersion from message data.
	 *
	 * @param packet The message data.
	 */
	public StateVersion(final DatagramPacket packet) {
		super(packet);
	}

	@Override
	public final MessageType getMessageType() {
		return MessageType.STATE_VERSION;
	}

	@Override
	protected final void evaluatePayload() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPayload());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		mVendor = byteBuffer.getInt();
		mProduct = byteBuffer.getInt();
		mVersion = byteBuffer.getInt();
	}

	@Override
	protected final Map<String, String> getPayloadMap() {
		Map<String, String> payloadFields = new LinkedHashMap<>();
		payloadFields.put("Vendor", TypeUtil.toUnsignedString(mVendor));
		payloadFields.put("Product", TypeUtil.toUnsignedString(mProduct));
		payloadFields.put("Version", TypeUtil.toUnsignedString(mVersion));
		return payloadFields;
	}

	/**
	 * Get the vendor.
	 *
	 * @return the vendor
	 */
	public int getVendor() {
		return mVendor;
	}

	/**
	 * Get the product.
	 *
	 * @return the product
	 */
	public int getProduct() {
		return mProduct;
	}

	/**
	 * Get the version.
	 *
	 * @return the version
	 */
	public int getVersion() {
		return mVersion;
	}
}
