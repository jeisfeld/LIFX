package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.jeisfeld.lifx.lan.LifxLanConnection.RetryPolicy;
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
import de.jeisfeld.lifx.lan.message.SetGroup;
import de.jeisfeld.lifx.lan.message.SetLabel;
import de.jeisfeld.lifx.lan.message.SetLocation;
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
import de.jeisfeld.lifx.lan.util.TypeUtil;
import de.jeisfeld.lifx.os.Logger;

import static de.jeisfeld.lifx.lan.util.TypeUtil.INDENT;

/**
 * Class managing a LIFX device.
 */
public class Device implements Serializable {
	/**
	 * The default serializable version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Source ID. 32 bits. Unique ID sent by client. If zero, broadcast reply requested. If non-zero, unicast reply requested.
	 */
	private final int mSourceId;
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
	private Location mLocation = null;
	/**
	 * The group.
	 */
	private Group mGroup = null;
	/**
	 * The host firmware version.
	 */
	private String mHostFirmwareVersion = null;
	/**
	 * The wifi firmware version.
	 */
	private String mWifiFirmwareVersion = null;
	/**
	 * The firmware build time.
	 */
	private Date mFirmwareBuildTime = null;

	/**
	 * Additional parameters which may be stored in the device.
	 */
	private final Map<String, Object> mParameters = new HashMap<>();

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
	 * Constructor including version information.
	 *
	 * @param targetAddress The target address.
	 * @param inetAddress The internet address.
	 * @param port The port.
	 * @param sourceId The sourceId.
	 * @param vendor The vendor.
	 * @param product The product.
	 * @param version The version.
	 * @param label The label.
	 */
	public Device(final String targetAddress, final InetAddress inetAddress, final int port, final int sourceId, // SUPPRESS_CHECKSTYLE
			final Vendor vendor, final Product product, final int version, final String label) {
		this(targetAddress, inetAddress, port, sourceId);
		setVersionInformation(vendor, product, version);
		mLabel = label;
	}

	/**
	 * Get a Lifx LAN Connection for this device.
	 *
	 * @return A connection.
	 */
	public LifxLanConnection getConnection() {
		return new LifxLanConnection(mSourceId, mTargetAddress, mInetAddress, mPort);
	}

	/**
	 * Get version information via GetVersion call.
	 *
	 * @return the device including version information.
	 * @throws IOException Exception while retrieving data.
	 */
	public Device getDeviceProduct() throws IOException {
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
	 * @throws IOException Exception while retrieving version info.
	 */
	public void reset() throws IOException {
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
	 * @throws IOException Exception while retrieving data.
	 */
	private void retrieveVersionInformation() throws IOException {
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
	 * @throws IOException Exception while retrieving data.
	 */
	private void retrieveLabel() throws IOException {
		StateLabel stateLabel = (StateLabel) getConnection().requestWithResponse(new GetLabel());
		mLabel = stateLabel.getLabel();
	}

	/**
	 * Get Location via GetLocation call.
	 *
	 * @throws IOException Exception while retrieving data.
	 */
	private void retrieveLocation() throws IOException {
		StateLocation stateLocation = (StateLocation) getConnection().requestWithResponse(new GetLocation());
		mLocation = stateLocation.getLocation();
	}

	/**
	 * Get Group via GetGroup call.
	 *
	 * @throws IOException Exception while retrieving data.
	 */
	private void retrieveGroup() throws IOException {
		StateGroup stateGroup = (StateGroup) getConnection().requestWithResponse(new GetGroup());
		mGroup = stateGroup.getGroup();
	}

	/**
	 * Get Group via GetHostFirmware call.
	 *
	 * @throws IOException Exception while retrieving data.
	 */
	private void retrieveHostFirmware() throws IOException {
		StateHostFirmware stateHostFirmware = (StateHostFirmware) getConnection().requestWithResponse(new GetHostFirmware());
		mHostFirmwareVersion = stateHostFirmware.getMajorVersion() + "." + stateHostFirmware.getMinorVersion();
		mFirmwareBuildTime = stateHostFirmware.getBuildTime(); // MAGIC_NUMBER
	}

	/**
	 * Get Group via GetWifiFirmware call.
	 *
	 * @throws IOException Exception while retrieving data.
	 */
	private void retrieveWifiFirmware() throws IOException {
		StateWifiFirmware stateWifiFirmware = (StateWifiFirmware) getConnection().requestWithResponse(new GetWifiFirmware());
		mWifiFirmwareVersion = stateWifiFirmware.getMajorVersion() + "." + stateWifiFirmware.getMinorVersion();
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + mTargetAddress + "] (" + getLabel() + "@" + mInetAddress.getHostAddress() + ":"
				+ TypeUtil.toUnsignedString(mPort) + ")";
	}

	// OVERRIDABLE

	/**
	 * Get the device information as String.
	 *
	 * @return The device information as String.
	 */
	public String getFullInformation() {
		StringBuilder result = new StringBuilder(getClass().getSimpleName()).append(":\n");
		result.append(INDENT).append("MAC: ").append(mTargetAddress).append("\n");
		result.append(INDENT).append("IP Address: ").append(mInetAddress.getHostAddress()).append("\n");
		result.append(INDENT).append("Port: ").append(mPort).append("\n");
		result.append(INDENT).append("Vendor: ").append(mVendor).append("\n");
		result.append(INDENT).append("Product: ").append(getProduct()).append("\n");
		result.append(INDENT).append("Version: ").append(TypeUtil.toUnsignedString(mVersion)).append("\n");
		result.append(INDENT).append("Colored: ").append(getProduct().hasColor()).append("\n");
		result.append(INDENT).append("Label: ").append(getLabel()).append("\n");
		result.append(INDENT).append("Location: ").append(getLocation().getLocationLabel()).append("\n");
		result.append(INDENT).append("Group: ").append(getGroup().getGroupLabel()).append("\n");
		result.append(INDENT).append("Host Firmware Version: ").append(getHostFirmwareVersion()).append("\n");
		result.append(INDENT).append("Firmware time: ").append(getFirmwareBuildTime().getTime() / 1000).append("\n"); // MAGIC_NUMBER
		result.append(INDENT).append("WiFi Firmware Version: ").append(getWifiFirmwareVersion()).append("\n");
		result.append(INDENT).append("Uptime: ").append(TypeUtil.toString(getUptime())).append("\n");
		ConnectionInfo wifiInfo = getWifiInfo();
		if (wifiInfo != null) {
			result.append(INDENT).append("WiFi Signal Strength: ").append(wifiInfo.getSignalStrength()).append("\n");
		}
		result.append(INDENT).append("Power: ").append(getPower()).append("\n");
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
			catch (IOException e) {
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
	public final Location getLocation() {
		if (mLocation == null) {
			try {
				retrieveLocation();
			}
			catch (IOException e) {
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
	public final Group getGroup() {
		if (mGroup == null) {
			try {
				retrieveGroup();
			}
			catch (IOException e) {
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
			catch (IOException e) {
				Logger.error(e);
			}
		}
		return mHostFirmwareVersion;
	}

	/**
	 * Get the host firmware build time (in seconds).
	 *
	 * @return the host firmware build time.
	 */
	public final Date getFirmwareBuildTime() {
		if (mFirmwareBuildTime == null) {
			try {
				retrieveHostFirmware();
			}
			catch (IOException e) {
				Logger.error(e);
			}
		}
		return mFirmwareBuildTime;
	}

	/**
	 * Set the firmware build time.
	 *
	 * @param timestamp The firmware build timestamp.
	 */
	protected final void setFirmwareBuildTime(final long timestamp) {
		mFirmwareBuildTime = new Date(timestamp);
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
			catch (IOException e) {
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
		catch (IOException e) {
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
		catch (IOException e) {
			Logger.connectionError(this, "Power", e);
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
		catch (IOException e) {
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
		catch (IOException e) {
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
			return new LifxLanConnection(mSourceId, mTargetAddress, mInetAddress, mPort) // MAGIC_NUMBER
					.broadcastWithResponse(new EchoRequest(), new RetryPolicy() {
						@Override
						public int getAttempts() {
							return 1;
						}

						@Override
						public int getTimeout(final int attempt) {
							return 100; // MAGIC_NUMBER
						}
					}).size() > 0;
		}
		catch (SocketException e) {
			return false;
		}
	}

	/**
	 * Set the power.
	 *
	 * @param status true for switching on, false for switching off
	 * @throws IOException Connection issues
	 */
	public final void setPower(final boolean status) throws IOException {
		getConnection().requestWithResponse(new SetPower(status));
	}

	/**
	 * Set the label.
	 *
	 * @param label the new label.
	 * @throws IOException Connection issues
	 */
	public final void setLabel(final String label) throws IOException {
		getConnection().requestWithResponse(new SetLabel(label));
		mLabel = null;
	}

	/**
	 * Set the group.
	 *
	 * @param group the group.
	 * @throws IOException Connection issues
	 */
	public final void setGroup(final Group group) throws IOException {
		getConnection().requestWithResponse(new SetGroup(group));
		mGroup = null;
	}

	/**
	 * Set the location.
	 *
	 * @param location the location.
	 * @throws IOException Connection issues
	 */
	public final void setLocation(final Location location) throws IOException {
		getConnection().requestWithResponse(new SetLocation(location));
		mLocation = null;
	}

	/**
	 * Set a parameter on the device.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void setParameter(final String key, final Object value) {
		mParameters.put(key, value);
	}

	/**
	 * Get a parameter from the device.
	 *
	 * @param key The key.
	 * @return The value.
	 */
	public Object getParameter(final String key) {
		return mParameters.get(key);
	}

	/**
	 * Store the label in the object.. May be helpful if there are connection issues.
	 *
	 * @param label The label to be stored.
	 */
	public void storeLabel(final String label) {
		mLabel = label;
	}
}
