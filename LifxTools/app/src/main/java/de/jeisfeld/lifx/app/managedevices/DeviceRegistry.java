package de.jeisfeld.lifx.app.managedevices;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter.MultizoneOrientation;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Group;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.LifxLanConnection.RetryPolicy;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.message.ResponseMessage;
import de.jeisfeld.lifx.lan.message.StateService;
import de.jeisfeld.lifx.lan.type.Product;
import de.jeisfeld.lifx.lan.type.TileInfo;
import de.jeisfeld.lifx.lan.type.TileInfo.Rotation;
import de.jeisfeld.lifx.lan.type.Vendor;
import de.jeisfeld.lifx.os.DeviceRegistryInterface;
import de.jeisfeld.lifx.os.OsTools;

/**
 * A registry holding information about devices.
 */
public final class DeviceRegistry implements DeviceRegistryInterface {
	/**
	 * The default port.
	 */
	private static final int DEFAULT_PORT = 56700;
	/**
	 * Device parameter for device id.
	 */
	public static final String DEVICE_ID = "deviceId";
	/**
	 * Device parameter for "show" flag of device.
	 */
	public static final String DEVICE_PARAMETER_SHOW = "showDevice";
	/**
	 * Device parameter for the groupId of the device.
	 */
	public static final String DEVICE_GROUP_ID = "deviceGroupId";
	/**
	 * Device parameter for multizone orientation.
	 */
	public static final String DEVICE_PARAMETER_MULTIZONE_ORIENTATION = "multizoneOrientation";

	/**
	 * The singleton instance of DeviceRegistry.
	 */
	private static DeviceRegistry mInstance = null;
	/**
	 * The devices and groups.
	 */
	private final SparseArray<DeviceHolder> mDevices = new SparseArray<>();
	/**
	 * A map from MAC address to device id.
	 */
	private final Map<String, Integer> mMacToIdMap = new HashMap<>();
	/**
	 * A map from group byte id to group app id.
	 */
	private final Map<String, Integer> mByteIdToGroupIdMap = new HashMap<>();
	/**
	 * The ringtone dummy light.
	 */
	private final Light mRingtoneDummyLight = new Light("RINGTONE", null, -1, 0, null, null, 0,
			Application.getResourceString(R.string.list_entry_ringtone), null) {
		/**
		 * The default serial version uid.
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public boolean equals(final Object other) {
			if (other instanceof Light) {
				Light otherLight = (Light) other;
				return getTargetAddress().equals(otherLight.getTargetAddress()) && getPort() == otherLight.getPort();
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String toString() {
			return "Ringtone Dummy Light";
		}
	};

	/**
	 * The source id.
	 */
	private final int mSourceId;

	/**
	 * Create the device registry and retrieve stored entries.
	 */
	private DeviceRegistry() {
		mSourceId = OsTools.getPid();
		List<Integer> deviceIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);

		for (int deviceId : deviceIds) {
			DeviceType type = DeviceType.fromOrdinal(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_type, deviceId, 0));

			if (type == DeviceType.GROUP) {
				addGroup(deviceId);
			}
			else {
				addDevice(type, deviceId);
			}
		}
	}

	/**
	 * Add a device from local storage to the registry during creation.
	 *
	 * @param type     The device type.
	 * @param deviceId The device id.
	 */
	private void addDevice(final DeviceType type, final int deviceId) {
		String mac = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_device_mac, deviceId);
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getByAddress(
					PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_device_address, deviceId).getBytes(StandardCharsets.ISO_8859_1));
		}
		catch (Exception e) {
			Log.w(Application.TAG, e);
		}
		int port = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_port, deviceId, DeviceRegistry.DEFAULT_PORT);
		String label = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_device_label, deviceId);
		if (label == null) {
			label = "???";
		}
		Vendor vendor = Vendor.fromInt(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_vendor, deviceId, 0));
		Product product = Product.fromId(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_product, deviceId, 0));
		int version = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_product, deviceId, 0);
		mMacToIdMap.put(mac, deviceId);

		int groupId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_group_id, deviceId, -1);
		Group group = addGroup(groupId);

		Device device;
		if (type == DeviceType.DEVICE) {
			device = new Device(mac, inetAddress, port, mSourceId, vendor, product, version, label, group);
		}
		else if (type == DeviceType.MULTIZONE) {
			byte zoneCount = (byte) PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_zone_count, deviceId, -1);
			MultizoneOrientation multizoneOrientation = MultizoneOrientation.fromOrdinal(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_multizone_orientation, deviceId, 0));
			long buildTimestamp = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_device_build_timestamp, deviceId, -1);
			device = new MultiZoneLight(mac, inetAddress, port, mSourceId, vendor, product, version, label, group, zoneCount, buildTimestamp);
			device.setParameter(DEVICE_PARAMETER_MULTIZONE_ORIENTATION, multizoneOrientation);
		}
		else if (type == DeviceType.TILECHAIN) {
			byte tileCount = (byte) PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_tile_count, deviceId, -1);
			int totalWidth = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_tile_totalwidth, deviceId, -1);
			int totalHeight = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_tile_totalheight, deviceId, -1);

			List<Integer> tileParameters =
					PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_device_tile_tileinfo_parameters, deviceId);
			List<TileInfo> tileInfoList = null;
			if (tileParameters.size() == 5 * tileCount) { // MAGIC_NUMBER
				tileInfoList = new ArrayList<>();
				Iterator<Integer> paramIterator = tileParameters.iterator();
				for (int i = 0; i < tileCount; i++) {
					tileInfoList.add(new TileInfo(paramIterator.next().byteValue(), paramIterator.next().byteValue(),
							paramIterator.next(), paramIterator.next(), Rotation.fromOrdinal(paramIterator.next())));

				}
			}

			device = new TileChain(mac, inetAddress, port, mSourceId, vendor, product, version, label, group,
					tileCount, totalWidth, totalHeight, tileInfoList);
		}
		else {
			device = new Light(mac, inetAddress, port, mSourceId, vendor, product, version, label, group);
		}

		boolean isShow = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_device_show, deviceId, true);
		device.setParameter(DEVICE_ID, deviceId);
		device.setParameter(DEVICE_PARAMETER_SHOW, isShow);
		device.setParameter(DEVICE_GROUP_ID, groupId);
		mDevices.put(deviceId, new DeviceHolder(device, deviceId, isShow));
	}

	/**
	 * Add a group from local storage to the registry during creation.
	 *
	 * @param groupId The group id.
	 * @return the added group.
	 */
	private Group addGroup(final int groupId) {
		DeviceHolder existing = mDevices.get(groupId);
		if (existing != null && existing.isGroup()) {
			return existing.getGroup();
		}
		String label = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_device_label, groupId);
		if (label == null) {
			// not yet stored, hence cannot add group.
			return null;
		}
		byte[] byteId = PreferenceUtil.getIndexedSharedPreferenceByteArray(R.string.key_group_byte_id, groupId);
		Date updateTime = new Date(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_group_update_time, groupId, 0));
		Group group = new Group(byteId, label, updateTime);
		mByteIdToGroupIdMap.put(Base64.encodeToString(byteId, Base64.DEFAULT), groupId);

		boolean isShow = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_device_show, groupId, true);
		group.setParameter(DEVICE_ID, groupId);
		group.setParameter(DEVICE_PARAMETER_SHOW, isShow);
		mDevices.put(groupId, new DeviceHolder(group, groupId, isShow));
		return group;
	}

	/**
	 * Get the list of known devices and groups.
	 *
	 * @param onlyFlagged Get only devices and groups which are flagged.
	 * @return The list of known devices and groups.
	 */
	public List<DeviceHolder> getDevices(final boolean onlyFlagged) {
		List<DeviceHolder> result = new ArrayList<>();
		for (int deviceId : PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids)) {
			DeviceHolder device = mDevices.get(deviceId);
			if (device != null && !(onlyFlagged && !device.isShow())) {
				result.add(device);
			}
		}
		return result;
	}

	/**
	 * Get the devices of a group.
	 *
	 * @param groupId     The group id.
	 * @param onlyFlagged flag indicating if only flagged devices should be returned.
	 * @return The devices of this group.
	 */
	public List<Device> getDevices(final int groupId, final boolean onlyFlagged) {
		return getDevices(onlyFlagged).stream().filter(holder -> !holder.isGroup()).map(DeviceHolder::getDevice)
				.filter(device -> device.getParameter(DEVICE_GROUP_ID) != null && (int) device.getParameter(DEVICE_GROUP_ID) == groupId)
				.collect(Collectors.toList());
	}

	@Override
	public List<Device> getDevices() {
		List<Device> result = new ArrayList<>();
		for (int deviceId : PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids)) {
			DeviceHolder device = mDevices.get(deviceId);
			if (device != null && !device.isGroup()) {
				result.add(device.getDevice());
			}
		}
		return result;
	}

	@Override
	public Device getDeviceByMac(final String mac) {
		Integer deviceId = mMacToIdMap.get(mac);
		return deviceId == null ? null : getDeviceById(deviceId).getDevice();
	}

	/**
	 * Get a known device by its storage id.
	 *
	 * @param id the storage id
	 * @return The device
	 */
	public DeviceHolder getDeviceById(final int id) {
		return mDevices.get(id);
	}

	/**
	 * Get a known light by its mac.
	 *
	 * @param mac The mac.
	 * @return The light.
	 */
	public Light getLightByMac(final String mac) {
		Device device = getDeviceByMac(mac);
		return device instanceof Light ? (Light) device : null;
	}

	/**
	 * Add or update a device in local store.
	 *
	 * @param device the device
	 */
	public void addOrUpdate(final Device device) {
		if (!mMacToIdMap.containsKey(device.getTargetAddress())) {
			// new device
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_device_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_device_max_id, newId);

			List<Integer> deviceIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
			deviceIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_device_ids, deviceIds);
			mMacToIdMap.put(device.getTargetAddress(), newId);
		}
		Integer deviceId = mMacToIdMap.get(device.getTargetAddress());
		if (deviceId == null) {
			return;
		}

		String label = device.getLabel();
		if (label == null) {
			label = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_device_label, deviceId);
			if (label == null) {
				device.storeLabel("???");
			}
			else {
				device.storeLabel(label);
			}
		}

		boolean isShow = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_device_show, deviceId, true);
		device.setParameter(DEVICE_ID, deviceId);
		device.setParameter(DEVICE_PARAMETER_SHOW, isShow);
		DeviceHolder otherDevice = mDevices.get(deviceId);
		if (otherDevice != null && !otherDevice.isGroup() && otherDevice.getDevice() instanceof Light && device instanceof Light) {
			((Light) device).fetchAnimationThread((Light) otherDevice.getDevice());
		}
		mDevices.put(deviceId, new DeviceHolder(device, deviceId, isShow));

		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_device_mac, deviceId, device.getTargetAddress());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_device_address, deviceId,
				new String(device.getInetAddress().getAddress(), StandardCharsets.ISO_8859_1));
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_port, deviceId, device.getPort());
		if (label != null) {
			PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_device_label, deviceId, label);
		}
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_type, deviceId, DeviceType.fromDevice(device).ordinal());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_vendor, deviceId, device.getVendor().value());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_product, deviceId, device.getProduct().getId());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_version, deviceId, device.getVersion());
		if (device instanceof MultiZoneLight) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_zone_count, deviceId, ((MultiZoneLight) device).getZoneCount());
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_device_build_timestamp, deviceId, device.getFirmwareBuildTime().getTime());
			MultizoneOrientation multizoneOrientation = (MultizoneOrientation) device.getParameter(DEVICE_PARAMETER_MULTIZONE_ORIENTATION);
			if (multizoneOrientation == null) {
				multizoneOrientation = MultizoneOrientation.fromOrdinal(
						PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_multizone_orientation, deviceId, 0));
				device.setParameter(DEVICE_PARAMETER_MULTIZONE_ORIENTATION, multizoneOrientation);
			}
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_multizone_orientation, deviceId, multizoneOrientation.ordinal());
		}
		if (device instanceof TileChain) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_tile_count, deviceId, ((TileChain) device).getTileCount());
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_tile_totalwidth, deviceId, ((TileChain) device).getTotalWidth());
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_tile_totalheight, deviceId, ((TileChain) device).getTotalHeight());
			List<TileInfo> tileInfos = ((TileChain) device).getTileInfo();
			if (tileInfos != null && tileInfos.size() == ((TileChain) device).getTileCount()) {
				List<Integer> tileParameters = new ArrayList<>();
				for (TileInfo tileInfo : tileInfos) {
					tileParameters.add((int) tileInfo.getWidth());
					tileParameters.add((int) tileInfo.getHeight());
					tileParameters.add(tileInfo.getMinX());
					tileParameters.add(tileInfo.getMinY());
					tileParameters.add(tileInfo.getRotation().ordinal());
				}
				PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_device_tile_tileinfo_parameters, deviceId, tileParameters);
			}
		}

		Integer groupId = addOrUpdate(device.getGroup());
		if (groupId != null) {
			device.setParameter(DEVICE_GROUP_ID, groupId);
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_group_id, deviceId, groupId);
		}
	}

	/**
	 * Remove a device from local store.
	 *
	 * @param device The device to be deleted.
	 */
	public void remove(final Device device) {
		Integer deviceId = mMacToIdMap.get(device.getTargetAddress());
		if (deviceId == null) {
			return;
		}
		mMacToIdMap.remove(device.getTargetAddress());
		mDevices.remove(deviceId);

		List<Integer> deviceIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
		deviceIds.remove(deviceId);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_device_ids, deviceIds);

		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_mac, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_address, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_port, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_label, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_type, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_vendor, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_product, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_version, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_zone_count, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_multizone_orientation, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_tile_count, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_tile_totalwidth, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_tile_totalheight, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_tile_tileinfo_parameters, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_build_timestamp, deviceId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_show, deviceId);

		for (StoredColor storedColor : ColorRegistry.getInstance().getStoredColors()) {
			if (storedColor.getDeviceId() == deviceId) {
				ColorRegistry.getInstance().remove(storedColor);
			}
		}
	}

	/**
	 * Add or update a group in local store.
	 *
	 * @param group the  group
	 * @return the groupId
	 */
	public Integer addOrUpdate(final Group group) {
		if (group == null) {
			return null;
		}
		if (!mByteIdToGroupIdMap.containsKey(Base64.encodeToString(group.getGroupId(), Base64.DEFAULT))) {
			// new group
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_device_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_device_max_id, newId);

			List<Integer> groupIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
			groupIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_device_ids, groupIds);
			mByteIdToGroupIdMap.put(Base64.encodeToString(group.getGroupId(), Base64.DEFAULT), newId);
		}
		Integer groupId = mByteIdToGroupIdMap.get(Base64.encodeToString(group.getGroupId(), Base64.DEFAULT));
		if (groupId == null) {
			return null;
		}

		DeviceHolder oldGroup = mDevices.get(groupId);
		if (oldGroup != null && oldGroup.isGroup() && !oldGroup.getGroup().getUpdateTime().before(group.getUpdateTime())) {
			// update only with newer data.
			return groupId;
		}

		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_device_type, groupId, DeviceType.GROUP.ordinal());

		boolean isShow = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_device_show, groupId, true);
		group.setParameter(DEVICE_ID, groupId);
		group.setParameter(DEVICE_PARAMETER_SHOW, isShow);

		mDevices.put(groupId, new DeviceHolder(group, groupId, isShow));
		PreferenceUtil.setIndexedSharedPreferenceByteArray(R.string.key_group_byte_id, groupId, group.getGroupId());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_device_label, groupId, group.getGroupLabel());
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_group_update_time, groupId, group.getUpdateTime().getTime());
		return groupId;
	}

	/**
	 * Remove a group from local store.
	 *
	 * @param group The group to be deleted.
	 */
	public void remove(final Group group) {
		Integer groupId = mByteIdToGroupIdMap.get(Base64.encodeToString(group.getGroupId(), Base64.DEFAULT));
		if (groupId == null) {
			return;
		}
		mByteIdToGroupIdMap.remove(Base64.encodeToString(group.getGroupId(), Base64.DEFAULT));
		mDevices.remove(groupId);

		List<Integer> groupIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
		groupIds.remove(groupId);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_device_ids, groupIds);

		PreferenceUtil.removeIndexedSharedPreference(R.string.key_group_byte_id, groupId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_label, groupId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_group_update_time, groupId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_device_show, groupId);
	}

	/**
	 * Remove a device or group.
	 *
	 * @param holder The device holder.
	 */
	public void remove(final DeviceHolder holder) {
		if (holder.isGroup()) {
			remove(holder.getGroup());
		}
		else {
			remove(holder.getDevice());
		}
	}

	/**
	 * Get the DeviceRegistry as singleton.
	 *
	 * @return The DeviceRegistry as singleton.
	 */
	public static synchronized DeviceRegistry getInstance() {
		if (DeviceRegistry.mInstance == null) {
			DeviceRegistry.mInstance = new DeviceRegistry();
		}
		return DeviceRegistry.mInstance;
	}

	/**
	 * Cleanup the device registry, so that it is recreated next time.
	 */
	public static synchronized void cleanUp() {
		DeviceRegistry.mInstance = null;
	}

	/**
	 * Update the list of devices.
	 *
	 * @param callback Callback called in case of found devices.
	 */
	public void update(final DeviceUpdateCallback callback) {
		new DeviceUpdateTask(this, callback).execute();
	}

	/**
	 * Get the ringtone dummy light.
	 *
	 * @return The ringtone dummy light.
	 */
	public Light getRingtoneDummyLight() {
		return mRingtoneDummyLight;
	}

	/**
	 * Enumeration of device types.
	 */
	private enum DeviceType {
		/**
		 * A device which is no light.
		 */
		DEVICE,
		/**
		 * A light which is neither multizone nor tilechain.
		 */
		LIGHT,
		/**
		 * A multizone light.
		 */
		MULTIZONE,
		/**
		 * A tile chain.
		 */
		TILECHAIN,
		/**
		 * A group of devices.
		 */
		GROUP;

		/**
		 * Get the device type by its ordinal.
		 *
		 * @param i The ordinal
		 * @return The device type.
		 */
		public static DeviceType fromOrdinal(final int i) {
			for (DeviceType deviceType : DeviceType.values()) {
				if (deviceType.ordinal() == i) {
					return deviceType;
				}
			}
			return DEVICE;
		}

		/**
		 * Get the type of a device.
		 *
		 * @param device The device
		 * @return The type
		 */
		public static DeviceType fromDevice(final Device device) {
			if (device instanceof MultiZoneLight) {
				return MULTIZONE;
			}
			else if (device instanceof TileChain) {
				return TILECHAIN;
			}
			else if (device instanceof Light) {
				return LIGHT;
			}
			else {
				return DEVICE;
			}
		}

	}

	/**
	 * Asynchronous task for updating the list of devices.
	 */
	private static final class DeviceUpdateTask extends AsyncTask<String, Device, List<Device>> {
		/**
		 * The callback called for found devices.
		 */
		private final DeviceUpdateCallback mCallback;
		/**
		 * The MACs of new devices.
		 */
		private final List<String> mNewDeviceMacs = new ArrayList<>();
		/**
		 * The device registry.
		 */
		private final DeviceRegistry mDeviceRegistry;

		/**
		 * Create a DeviceUpdateTask.
		 *
		 * @param deviceRegistry The calling deviceRegistry.
		 * @param callback       The callback to be called for found devices.
		 */
		private DeviceUpdateTask(final DeviceRegistry deviceRegistry, final DeviceUpdateCallback callback) {
			mDeviceRegistry = deviceRegistry;
			mCallback = callback;
		}

		@Override
		protected List<Device> doInBackground(final String... params) {
			try {
				return LifxLan.getInstance().retrieveDeviceInformation(new RetryPolicy() {
					@Override
					public int getAttempts() {
						return 2;
					}

					@Override
					public int getTimeout(final int attempt) {
						return 2500; // MAGIC_NUMBER
					}

					@Override
					public int getExpectedResponses() {
						return Integer.MAX_VALUE;
					}

					@Override
					public void onResponse(final ResponseMessage responseMessage) {
						try {
							Device device = ((StateService) responseMessage).getDevice().getDeviceProduct();
							if (!mDeviceRegistry.mMacToIdMap.containsKey(device.getTargetAddress())) {
								mNewDeviceMacs.add(device.getTargetAddress());
							}
							mDeviceRegistry.addOrUpdate(device);
							publishProgress(device);
						}
						catch (IOException e) {
							Log.w(Application.TAG, e);
						}
					}
				}, null);
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return new ArrayList<>();
			}
		}

		@Override
		protected void onProgressUpdate(final Device... devices) {
			for (Device device : devices) {
				mCallback.onDeviceUpdated(device, mNewDeviceMacs.contains(device.getTargetAddress()), false);
			}
		}

		@Override
		protected void onPostExecute(final List<Device> devices) {
			List<String> foundMacs = new ArrayList<>();
			for (Device device : devices) {
				foundMacs.add(device.getTargetAddress());
			}
			for (String mac : mDeviceRegistry.mMacToIdMap.keySet()) {
				if (!foundMacs.contains(mac)) {
					mCallback.onDeviceUpdated(mDeviceRegistry.getDeviceByMac(mac), false, true);
				}
			}
			if (devices.size() == 0) {
				mCallback.onNoDevicesFound();
			}
		}
	}

	/**
	 * Callback for device search.
	 */
	public interface DeviceUpdateCallback {
		/**
		 * Method called on device search results.
		 *
		 * @param device    the device.
		 * @param isNew     true if the device is unknown.
		 * @param isMissing true if the device is known but was not found.
		 */
		void onDeviceUpdated(Device device, boolean isNew, boolean isMissing);

		/**
		 * Method called if no devices are found.
		 */
		default void onNoDevicesFound() {
			// do nothing.
		}
	}

}
