package de.jeisfeld.lifx.app.util;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.lan.type.Color;

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
			Color color = PreferenceUtil.getIndexedSharedPreferenceColor(R.string.key_color_color, colorId, Color.NONE);
			String name = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_color_name, colorId);
			int deviceId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_color_device_id, colorId, -1);
			StoredColor storedColor = new StoredColor(colorId, color, deviceId, name);
			mStoredColors.put(colorId, storedColor);
		}

	}

	/**
	 * Get the list of stored colors.
	 *
	 * @return The list of stored colors.
	 */
	public List<StoredColor> getStoredColors() {
		List<StoredColor> result = new ArrayList<>();
		for (int i = 0; i < mStoredColors.size(); i++) {
			result.add(mStoredColors.valueAt(i));
		}
		return result;
	}

	/**
	 * Add or update a stored color in local store.
	 *
	 * @param storedColor the stored color
	 */
	public void addOrUpdate(final StoredColor storedColor) {
		StoredColor newStoredColor = storedColor;
		if (storedColor.getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_color_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_color_max_id, newId);

			List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
			colorIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, colorIds);
			newStoredColor = new StoredColor(newId, storedColor);
			mStoredColors.put(newId, newStoredColor);
		}
		PreferenceUtil.setIndexedSharedPreferenceColor(R.string.key_color_color, newStoredColor.getId(), newStoredColor.getColor());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_device_id, newStoredColor.getId(), newStoredColor.getDeviceId());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_color_name, newStoredColor.getId(), newStoredColor.getName());
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
