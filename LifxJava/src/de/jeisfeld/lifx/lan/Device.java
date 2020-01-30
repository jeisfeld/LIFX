package de.jeisfeld.lifx.lan;

import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;

import de.jeisfeld.lifx.lan.message.EchoRequest;
import de.jeisfeld.lifx.lan.message.GetGroup;
import de.jeisfeld.lifx.lan.message.GetHostFirmware;
import de.jeisfeld.lifx.lan.message.GetHostInfo;
import de.jeisfeld.lifx.lan.message.GetInfo;
import de.jeisfeld.lifx.lan.message.GetLabel;
import de.jeisfeld.lifx.lan.message.GetLocation;
import de.jeisfeld.lifx.lan.message.GetPower;
import de.jeisfeld.lifx.lan.message.GetVersion;
import de.jeisfeld.lifx.lan.message.GetWifiFirmware;
import de.jeisfeld.lifx.lan.message.GetWifiInfo;
import de.jeisfeld.lifx.lan.message.SetLabel;
import de.jeisfeld.lifx.lan.message.SetPower;
import de.jeisfeld.lifx.lan.message.StateGroup;
import de.jeisfeld.lifx.lan.message.StateHostFirmware;
import de.jeisfeld.lifx.lan.message.StateHostInfo;
import de.jeisfeld.lifx.lan.message.StateInfo;
import de.jeisfeld.lifx.lan.message.StateLabel;
import de.jeisfeld.lifx.lan.message.StateLocation;
import de.jeisfeld.lifx.lan.message.StatePower;
import de.jeisfeld.lifx.lan.message.StateVersion;
import de.jeisfeld.lifx.lan.message.StateWifiFirmware;
import de.jeisfeld.lifx.lan.message.StateWifiInfo;
import de.jeisfeld.lifx.lan.type.ConnectionInfo;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.Product;
import de.jeisfeld.lifx.lan.type.Vendor;
import de.jeisfeld.lifx.lan.util.Logger;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class managing a LIFX device.
 */
public class Device {
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
	private Vendor mVendor;
	/**
	 * The product.
	 */
	private Product mProduct;
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
	 * The group.
	 */
	private String mGroup = null;
	/**
	 * The host firmware version.
	 */
	private String mHostFirmwareVersion = null;
	/**
	 * The wifi firmware version.
	 */
	private String mWifiFirmwareVersion = null;

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
	 * Get a Lifx LAN Connection for this device.
	 *
	 * @return A connection.
	 * @throws SocketException Exception while creating connection.
	 */
	protected LifxLanConnection getConnection() throws SocketException {
		return new LifxLanConnection(mSourceId, mTargetAddress, mInetAddress, mPort);
	}

	/**
	 * Get version information via GetVersion call.
	 *
	 * @return the device including version information.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	public Device getDeviceProduct() throws SocketException {
		retrieveVersionInformation();
		Device device = this;
		if (mProduct.isChain()) {
			device = new TileChain(this);
		}
		else if (mProduct.isMultizone()) {
			device = new MultiZoneLight(this);
		}
		else if (mProduct.isLight()) {
			device = new Light(this);
		}
		return device;
	}

	/**
	 * Reset all stored device information.
	 *
	 * @throws SocketException Exception while retrieving version info.
	 */
	public void reset() throws SocketException {
		retrieveVersionInformation();
		mLabel = null;
		mLocation = null;
		mGroup = null;
		mHostFirmwareVersion = null;
		mWifiFirmwareVersion = null;
	}

	/**
	 * Retrieve the version information via GetVersion call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveVersionInformation() throws SocketException {
		StateVersion stateVersion = (StateVersion) getConnection().requestWithResponse(new GetVersion());
		setVersionInformation(stateVersion.getVendor(), stateVersion.getProduct(), stateVersion.getVersion());
	}

	/**
	 * Set the version information.
	 *
	 * @param vendor The vendor.
	 * @param product The product.
	 * @param version The version.
	 */
	protected void setVersionInformation(final Vendor vendor, final Product product, final int version) {
		mVendor = vendor;
		mProduct = product;
		mVersion = version;
	}

	/**
	 * Get Label via GetLabel call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveLabel() throws SocketException {
		StateLabel stateLabel = (StateLabel) getConnection().requestWithResponse(new GetLabel());
		mLabel = stateLabel.getLabel();
	}

	/**
	 * Get Location via GetLocation call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveLocation() throws SocketException {
		StateLocation stateLocation = (StateLocation) getConnection().requestWithResponse(new GetLocation());
		mLocation = stateLocation.getLabel();
	}

	/**
	 * Get Group via GetGroup call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveGroup() throws SocketException {
		StateGroup stateGroup = (StateGroup) getConnection().requestWithResponse(new GetGroup());
		mGroup = stateGroup.getLabel();
	}

	/**
	 * Get Group via GetHostFirmware call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveHostFirmware() throws SocketException {
		StateHostFirmware stateHostFirmware = (StateHostFirmware) getConnection().requestWithResponse(new GetHostFirmware());
		mHostFirmwareVersion = stateHostFirmware.getMajorVersion() + "." + stateHostFirmware.getMinorVersion();
	}

	/**
	 * Get Group via GetWifiFirmware call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveWifiFirmware() throws SocketException {
		StateWifiFirmware stateWifiFirmware = (StateWifiFirmware) getConnection().requestWithResponse(new GetWifiFirmware());
		mWifiFirmwareVersion = stateWifiFirmware.getMajorVersion() + "." + stateWifiFirmware.getMinorVersion();
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
		StringBuilder result = new StringBuilder(getClass().getSimpleName()).append(":\n");
		result.append(TypeUtil.INDENT).append("MAC: ").append(mTargetAddress).append("\n");
		result.append(TypeUtil.INDENT).append("IP Address: ").append(mInetAddress.getHostAddress()).append("\n");
		result.append(TypeUtil.INDENT).append("Port: ").append(mPort).append("\n");
		result.append(TypeUtil.INDENT).append("Vendor: ").append(mVendor).append("\n");
		result.append(TypeUtil.INDENT).append("Product: ").append(getProduct()).append("\n");
		result.append(TypeUtil.INDENT).append("Version: ").append(TypeUtil.toUnsignedString(mVersion)).append("\n");
		result.append(TypeUtil.INDENT).append("Colored: ").append(getProduct().hasColor()).append("\n");
		result.append(TypeUtil.INDENT).append("Label: ").append(getLabel()).append("\n");
		result.append(TypeUtil.INDENT).append("Location: ").append(getLocation()).append("\n");
		result.append(TypeUtil.INDENT).append("Group: ").append(getGroup()).append("\n");
		result.append(TypeUtil.INDENT).append("Host Firmware Version: ").append(getHostFirmwareVersion()).append("\n");
		result.append(TypeUtil.INDENT).append("WiFi Firmware Version: ").append(getWifiFirmwareVersion()).append("\n");
		result.append(TypeUtil.INDENT).append("Uptime: ").append(TypeUtil.toString(getUptime())).append("\n");
		result.append(TypeUtil.INDENT).append("WiFi Signal Strength: ").append(getWifiInfo().getSignalStrength()).append("\n");
		result.append(TypeUtil.INDENT).append("Power: ").append(getPower()).append("\n");
		return result.toString();
	}

	/**
	 * Get the sourceId.
	 *
	 * @return the sourceId
	 */
	public final int getSourceId() {
		return mSourceId;
	}

	/**
	 * Get the targetAddress.
	 *
	 * @return the targetAddress
	 */
	public final String getTargetAddress() {
		return mTargetAddress;
	}

	/**
	 * Get the port.
	 *
	 * @return the port
	 */
	public final int getPort() {
		return mPort;
	}

	/**
	 * Get the internet address.
	 *
	 * @return the internet address
	 */
	public final InetAddress getInetAddress() {
		return mInetAddress;
	}

	/**
	 * Get the vendor.
	 *
	 * @return the vendor
	 */
	public final Vendor getVendor() {
		return mVendor;
	}

	/**
	 * Get the product.
	 *
	 * @return the product
	 */
	public final Product getProduct() {
		return mProduct;
	}

	/**
	 * Get the version.
	 *
	 * @return the version
	 */
	public final int getVersion() {
		return mVersion;
	}

	/**
	 * Get the label.
	 *
	 * @return the label
	 */
	public final String getLabel() {
		if (mLabel == null) {
			try {
				retrieveLabel();
			}
			catch (SocketException e) {
				Logger.error(e);
			}
		}
		return mLabel;
	}

	/**
	 * Get the location.
	 *
	 * @return the location
	 */
	public final String getLocation() {
		if (mLocation == null) {
			try {
				retrieveLocation();
			}
			catch (SocketException e) {
				Logger.error(e);
			}
		}
		return mLocation;
	}

	/**
	 * Get the group.
	 *
	 * @return the group
	 */
	public final String getGroup() {
		if (mGroup == null) {
			try {
				retrieveGroup();
			}
			catch (SocketException e) {
				Logger.error(e);
			}
		}
		return mGroup;
	}

	/**
	 * Get the host firmware version.
	 *
	 * @return the host firmware version
	 */
	public final String getHostFirmwareVersion() {
		if (mHostFirmwareVersion == null) {
			try {
				retrieveHostFirmware();
			}
			catch (SocketException e) {
				Logger.error(e);
			}
		}
		return mHostFirmwareVersion;
	}

	/**
	 * Get the wifi firmware version.
	 *
	 * @return the wifi firmware version
	 */
	public final String getWifiFirmwareVersion() {
		if (mWifiFirmwareVersion == null) {
			try {
				retrieveWifiFirmware();
			}
			catch (SocketException e) {
				Logger.error(e);
			}
		}
		return mWifiFirmwareVersion;
	}

	/**
	 * Get the uptime.
	 *
	 * @return the uptime
	 */
	public final Duration getUptime() {
		StateInfo stateInfo;
		try {
			stateInfo = (StateInfo) getConnection().requestWithResponse(new GetInfo());
			return stateInfo.getUptime();
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the power level.
	 *
	 * @return the power level.
	 */
	public Power getPower() {
		StatePower statePower;
		try {
			statePower = (StatePower) getConnection().requestWithResponse(new GetPower());
			return new Power(statePower.getLevel());
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the host info.
	 *
	 * @return The host info.
	 */
	public final ConnectionInfo getHostInfo() {
		StateHostInfo stateHostInfo;
		try {
			stateHostInfo = (StateHostInfo) getConnection().requestWithResponse(new GetHostInfo());
			return stateHostInfo.getConnectionInfo();
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Get the wifi info.
	 *
	 * @return The wifi info.
	 */
	public final ConnectionInfo getWifiInfo() {
		StateWifiInfo stateWifiInfo;
		try {
			stateWifiInfo = (StateWifiInfo) getConnection().requestWithResponse(new GetWifiInfo());
			return stateWifiInfo.getConnectionInfo();
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Check if the device is reachable.
	 *
	 * @return true if reachable.
	 */
	public boolean isReachable() {
		try {
			return new LifxLanConnection(mSourceId, 200, 1, mTargetAddress, mInetAddress, mPort) // MAGIC_NUMBER
					.requestWithResponse(new EchoRequest()) != null;
		}
		catch (SocketException e) {
			return false;
		}
	}

	/**
	 * Set the power.
	 *
	 * @param status true for switching on, false for switching off
	 * @throws SocketException Connection issues
	 */
	public final void setPower(final boolean status) throws SocketException {
		getConnection().requestWithResponse(new SetPower(status));
	}

	/**
	 * Set the label.
	 *
	 * @param label the new label.
	 * @throws SocketException Connection issues
	 */
	public final void setLabel(final String label) throws SocketException {
		getConnection().requestWithResponse(new SetLabel(label));
		mLabel = null;
	}
}
