package de.jeisfeld.lifx.lan;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.lan.message.GetLabel;
import de.jeisfeld.lifx.lan.message.GetLocation;
import de.jeisfeld.lifx.lan.message.GetVersion;
import de.jeisfeld.lifx.lan.message.StateLabel;
import de.jeisfeld.lifx.lan.message.StateLocation;
import de.jeisfeld.lifx.lan.message.StateVersion;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class managing a LIFX device.
 */
public class Device {
	/**
	 * The list of products which are no lights. Currently none.
	 */
	private static final List<Integer> NON_LIGHT_PRODUCTS = new ArrayList<>();

	/**
	 * Source ID. 32 bits. Unique ID sent by client. If zero, broadcast reply requested. If non-zero, unicast reply requested.
	 */
	private int mSourceId = 0;
	/**
	 * Target address. 64 bits. Either single MAC address or all zeroes for broadcast.
	 */
	private final String mTargetAddress;
	/**
	 * The port.
	 */
	private final int mPort;
	/**
	 * The Internet address of this device.
	 */
	private final InetAddress mInetAddress;
	/**
	 * The vendor.
	 */
	private int mVendor = 0;
	/**
	 * The product.
	 */
	private int mProduct = 0;
	/**
	 * The version.
	 */
	private int mVersion = 0;
	/**
	 * The label.
	 */
	private String mLabel = null;
	/**
	 * The location.
	 */
	private String mLocation = null;

	/**
	 * Constructor.
	 *
	 * @param targetAddress The target address.
	 * @param inetAddress The internet address.
	 * @param port The port.
	 * @param sourceId The sourceId.
	 */
	public Device(final String targetAddress, final InetAddress inetAddress, final int port, final int sourceId) {
		mTargetAddress = targetAddress;
		mInetAddress = inetAddress;
		mPort = port;
		mSourceId = sourceId;
	}

	/**
	 * Get version information via GetVersion call.
	 *
	 * @return the device including version information.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	public Device getDeviceProduct() throws SocketException {
		Device device = this;
		StateVersion stateVersion =
				(StateVersion) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort).requestWithResponse(new GetVersion(mTargetAddress));
		setVersionInformation(stateVersion.getVendor(), stateVersion.getProduct(), stateVersion.getVersion());
		if (!Device.NON_LIGHT_PRODUCTS.contains(mProduct)) {
			device = new Light(this);
		}
		device.retrieveInformation();
		return device;
	}

	/**
	 * Set the version information.
	 *
	 * @param vendor The vendor.
	 * @param product The product.
	 * @param version The version.
	 */
	protected void setVersionInformation(final int vendor, final int product, final int version) {
		mVendor = vendor;
		mProduct = product;
		mVersion = version;
	}

	/**
	 * Retrieve the device information.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	// OVERRIDABLE
	public void retrieveInformation() throws SocketException {
		if (mVendor == 0) {
			getDeviceProduct();
		}
		retrieveLabel();
		retrieveLocation();
		// GetLabel
		// GetLocation
		// GetGroup
		// LightGetPower
		// GetHostFirmware
		// GetWifiFirmware
		// LightGet
		// GetInfo
		// GetWifiInfo
	}

	/**
	 * Get Label via GetLabel call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveLabel() throws SocketException {
		StateLabel stateLabel =
				(StateLabel) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort).requestWithResponse(new GetLabel(mTargetAddress));
		mLabel = stateLabel.getLabel();
	}

	/**
	 * Get Location via GetLocation call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveLocation() throws SocketException {
		StateLocation stateLocation =
				(StateLocation) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort).requestWithResponse(new GetLocation(mTargetAddress));
		mLocation = stateLocation.getLabel();
	}

	// OVERRIDABLE
	@Override
	public String toString() {
		return "Device: " + mTargetAddress + ", " + mInetAddress.getHostAddress() + ":" + TypeUtil.toUnsignedString(mPort);
	}

	// OVERRIDABLE
	/**
	 * Get the device information as String.
	 *
	 * @return The device information as String.
	 */
	public String getFullInformation() {
		String linebreak = "\n  ";
		StringBuilder result = new StringBuilder(getClass().getSimpleName()).append(":");
		result.append(linebreak).append("MAC: ").append(mTargetAddress);
		result.append(linebreak).append("IP Address: ").append(mInetAddress.getHostAddress());
		result.append(linebreak).append("Port: ").append(mPort);
		result.append(linebreak).append("Vendor: ").append(TypeUtil.toUnsignedString(mVendor));
		result.append(linebreak).append("Product: ").append(TypeUtil.toUnsignedString(mProduct));
		result.append(linebreak).append("Version: ").append(TypeUtil.toUnsignedString(mVersion));
		if (mLabel != null) {
			result.append(linebreak).append("Label: ").append(mLabel);
		}
		if (mLocation != null) {
			result.append(linebreak).append("Location: ").append(mLocation);
		}
		return result.toString();
	}

	/**
	 * Get the sourceId.
	 *
	 * @return the sourceId
	 */
	protected final int getSourceId() {
		return mSourceId;
	}

	/**
	 * Get the targetAddress.
	 *
	 * @return the targetAddress
	 */
	protected final String getTargetAddress() {
		return mTargetAddress;
	}

	/**
	 * Get the port.
	 *
	 * @return the port
	 */
	protected final int getPort() {
		return mPort;
	}

	/**
	 * Get the internet address.
	 *
	 * @return the internet address
	 */
	protected final InetAddress getInetAddress() {
		return mInetAddress;
	}

	/**
	 * Get the vendor.
	 *
	 * @return the vendor
	 */
	protected final int getVendor() {
		return mVendor;
	}

	/**
	 * Get the product.
	 *
	 * @return the product
	 */
	protected final int getProduct() {
		return mProduct;
	}

	/**
	 * Get the version.
	 *
	 * @return the version
	 */
	protected final int getVersion() {
		return mVersion;
	}

	/**
	 * Get the label.
	 *
	 * @return the label
	 */
	public final String getLabel() {
		return mLabel;
	}

	/**
	 * Get the location.
	 *
	 * @return the location
	 */
	public final String getLocation() {
		return mLocation;
	}

}
