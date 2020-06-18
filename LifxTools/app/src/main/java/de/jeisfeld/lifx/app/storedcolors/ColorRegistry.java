package de.jeisfeld.lifx.app.storedcolors;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * A registry holding information about stored colors.
 */
public final class ColorRegistry {
	/**
	 * The singleton instance of DeviceRegistry.
	 */
	private static ColorRegistry mInstance = null;
	/**
	 * The stored colors.
	 */
	private final SparseArray<StoredColor> mStoredColors = new SparseArray<>();

	/**
	 * Create the color registry and retrieve stored entries.
	 */
	private ColorRegistry() {
		List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
		for (int colorId : colorIds) {
			mStoredColors.put(colorId, StoredColor.fromId(colorId));
		}
	}

	/**
	 * Get the list of stored colors.
	 *
	 * @return The list of stored colors.
	 */
	public List<StoredColor> getStoredColors() {
		List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
		List<Integer> deviceIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
		List<StoredColor> allStoredColors = new ArrayList<>();
		for (int colorId : colorIds) {
			allStoredColors.add(mStoredColors.get(colorId));
		}
		List<StoredColor> colorsWithoutDeviceId = new ArrayList<>(allStoredColors);
		List<StoredColor> result = new ArrayList<>();
		List<Integer> newColorIds = new ArrayList<>();
		for (int deviceId : deviceIds) {
			for (StoredColor storedColor : allStoredColors) {
				if (deviceId == storedColor.getDeviceId()) {
					result.add(storedColor);
					newColorIds.add(storedColor.getId());
					colorsWithoutDeviceId.remove(storedColor);
				}
			}
		}
		for (StoredColor colorWithoutDeviceId : colorsWithoutDeviceId) {
			result.add(colorWithoutDeviceId);
			newColorIds.add(colorWithoutDeviceId.getId());
		}
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, newColorIds);

		return result;
	}

	/**
	 * Get a stored color by its id.
	 *
	 * @param storedColorId The stored color id.
	 * @return The stored color.
	 */
	public StoredColor getStoredColor(final int storedColorId) {
		if (storedColorId >= 0) {
			return mStoredColors.get(storedColorId);
		}
		else {
			return new StoredColor(storedColorId);
		}
	}

	/**
	 * Get the stored colors for a specific device.
	 *
	 * @param deviceId The device id.
	 * @return The stored colors of this device.
	 */
	public List<StoredColor> getStoredColors(final int deviceId) {
		List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
		List<StoredColor> colorsOfDevice = new ArrayList<>();
		List<Integer> colorIdsOfDevice = new ArrayList<>();
		List<Integer> colorIdsOfOtherDevices = new ArrayList<>();
		for (int colorId : colorIds) {
			StoredColor storedColor = mStoredColors.get(colorId);
			if (storedColor.getDeviceId() == deviceId) {
				colorsOfDevice.add(storedColor);
				colorIdsOfDevice.add(colorId);
			}
			else {
				colorIdsOfOtherDevices.add(colorId);
			}
		}
		List<Integer> newColorIds = new ArrayList<>(colorIdsOfDevice);
		newColorIds.addAll(colorIdsOfOtherDevices);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, newColorIds);

		return colorsOfDevice;
	}

	/**
	 * Add or update a stored color in local store.
	 *
	 * @param storedColor the stored color
	 */
	public void addOrUpdate(final StoredColor storedColor) {
		StoredColor newStoredColor = storedColor.store();
		mStoredColors.put(newStoredColor.getId(), newStoredColor);
	}

	/**
	 * Remove a stored color from local store.
	 *
	 * @param storedColor The stored color to be deleted.
	 */
	public void remove(final StoredColor storedColor) {
		int colorId = storedColor.getId();
		mStoredColors.remove(colorId);

		List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
		colorIds.remove((Integer) colorId);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, colorIds);

		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_name, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_device_id, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_color, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_colors, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_multizone_type, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_multizone_flags, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_tilechain_type, colorId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_color_tilechain_sizes, colorId);
	}

	/**
	 * Get the ColorRegistry as singleton.
	 *
	 * @return The ColorRegistry as singleton.
	 */
	public static synchronized ColorRegistry getInstance() {
		if (ColorRegistry.mInstance == null) {
			ColorRegistry.mInstance = new ColorRegistry();
		}
		return ColorRegistry.mInstance;
	}
}
