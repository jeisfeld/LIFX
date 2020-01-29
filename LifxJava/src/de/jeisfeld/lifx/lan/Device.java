package de.jeisfeld.lifx.lan;

import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
import de.jeisfeld.lifx.lan.util.Logger;
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

	/**
	 * Get Group via GetGroup call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveGroup() throws SocketException {
		StateGroup stateGroup =
				(StateGroup) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort).requestWithResponse(new GetGroup(mTargetAddress));
		mGroup = stateGroup.getLabel();
	}

	/**
	 * Get Group via GetHostFirmware call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveHostFirmware() throws SocketException {
		StateHostFirmware stateHostFirmware =
				(StateHostFirmware) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort)
						.requestWithResponse(new GetHostFirmware(mTargetAddress));
		mHostFirmwareVersion = stateHostFirmware.getMajorVersion() + "." + stateHostFirmware.getMinorVersion();
	}

	/**
	 * Get Group via GetWifiFirmware call.
	 *
	 * @throws SocketException Exception while retrieving data.
	 */
	private void retrieveWifiFirmware() throws SocketException {
		StateWifiFirmware stateWifiFirmware =
				(StateWifiFirmware) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort)
						.requestWithResponse(new GetWifiFirmware(mTargetAddress));
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
		result.append(TypeUtil.INDENT).append("Vendor: ").append(TypeUtil.toUnsignedString(mVendor)).append("\n");
		result.append(TypeUtil.INDENT).append("Product: ").append(TypeUtil.toUnsignedString(mProduct)).append("\n");
		result.append(TypeUtil.INDENT).append("Version: ").append(TypeUtil.toUnsignedString(mVersion)).append("\n");
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
			stateInfo = (StateInfo) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort).requestWithResponse(new GetInfo(mTargetAddress));
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
	public final Power getPower() {
		StatePower statePower;
		try {
			statePower =
					(StatePower) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort).requestWithResponse(new GetPower(mTargetAddress));
			return Power.fromLevel(statePower.getLevel());
		}
		catch (SocketException e) {
			Logger.error(e);
			return Power.UNKNOWN;
		}
	}

	/**
	 * Get the host info.
	 *
	 * @return The host info.
	 */
	public final StateHostInfo getHostInfo() {
		StateHostInfo stateHostInfo;
		try {
			stateHostInfo = (StateHostInfo) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort)
					.requestWithResponse(new GetHostInfo(mTargetAddress));
			return stateHostInfo;
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
	public final StateWifiInfo getWifiInfo() {
		StateWifiInfo stateWifiInfo;
		try {
			stateWifiInfo = (StateWifiInfo) new LifxLanConnection(mSourceId, (byte) 0, mInetAddress, mPort)
					.requestWithResponse(new GetWifiInfo(mTargetAddress));
			return stateWifiInfo;
		}
		catch (SocketException e) {
			Logger.error(e);
			return null;
		}
	}

	/**
	 * Possible power values.
	 */
	public enum Power {
		/**
		 * Power is on.
		 */
		ON,
		/**
		 * Power is off.
		 */
		OFF,
		/**
		 * Power is unknown.
		 */
		UNKNOWN;

		/**
		 * Get the power value from level.
		 *
		 * @param level The power level.
		 * @return The power value.
		 */
		protected static Power fromLevel(final short level) {
			switch (level) {
			case -1:
				return ON;
			case 0:
				return OFF;
			default:
				return UNKNOWN;
			}
		}
	}

}
