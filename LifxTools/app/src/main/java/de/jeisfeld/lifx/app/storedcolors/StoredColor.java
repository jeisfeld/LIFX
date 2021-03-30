package de.jeisfeld.lifx.app.storedcolors;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.GroupViewModel;
import de.jeisfeld.lifx.app.home.LightViewModel;
import de.jeisfeld.lifx.app.home.MainViewModel;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Group;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding information about a stored color.
 */
public class StoredColor {
	/**
	 * The color.
	 */
	private final Color mColor;
	/**
	 * The deviceId for the color.
	 */
	private final int mDeviceId;
	/**
	 * The name of the color.
	 */
	private final String mName;
	/**
	 * The id for storage. Negative values indicate power off for devices.
	 */
	private final int mId;

	/**
	 * Generate a stored color.
	 *
	 * @param id       The id for storage
	 * @param color    The color
	 * @param deviceId The device id
	 * @param name     The name
	 */
	public StoredColor(final int id, final Color color, final int deviceId, final String name) {
		mColor = color;
		mDeviceId = deviceId;
		mName = name;
		mId = id;
	}

	/**
	 * Generate a new stored color without id.
	 *
	 * @param color    The color
	 * @param deviceId The device id
	 * @param name     The name
	 */
	public StoredColor(final Color color, final int deviceId, final String name) {
		this(-1, color, deviceId, name);
	}

	/**
	 * Generate a new stored color by adding id.
	 *
	 * @param id          The id
	 * @param storedColor the base stored color.
	 */
	public StoredColor(final int id, final StoredColor storedColor) {
		this(id, storedColor.getColor(), storedColor.getDeviceId(), storedColor.getName());
	}

	/**
	 * Retrieve a stored color from storage via id.
	 *
	 * @param colorId The id.
	 */
	protected StoredColor(final int colorId) {
		if (colorId >= 0) {
			mId = colorId;
			mName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_color_name, colorId);
			mDeviceId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_color_device_id, colorId, -1);
			mColor = PreferenceUtil.getIndexedSharedPreferenceColor(R.string.key_color_color, colorId, Color.NONE);
		}
		else {
			mId = colorId;
			mName = Application.getAppContext().getString(R.string.color_off);
			mDeviceId = -colorId;
			mColor = Color.OFF;
		}
	}

	/**
	 * Retrieve a stored color from storage via id.
	 *
	 * @param colorId The id.
	 * @return The stored color.
	 */
	public static StoredColor fromId(final int colorId) {
		boolean isMultiZone = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_color_multizone_type, colorId, -1) >= 0;
		boolean isTileChain = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_color_tilechain_type, colorId, -1) >= 0;

		if (isMultiZone) {
			return new StoredMultizoneColors(colorId);
		}
		else if (isTileChain) {
			return new StoredTileColors(colorId);
		}
		else {
			return new StoredColor(colorId);
		}
	}

	/**
	 * Get the stored color for a certain device representing power off.
	 *
	 * @param deviceId The device id.
	 * @return The stored color.
	 */
	public static StoredColor fromDeviceOff(final int deviceId) {
		return new StoredColor(-deviceId, Color.OFF, deviceId, Application.getAppContext().getString(R.string.color_off));
	}

	/**
	 * Store this color.
	 *
	 * @return the stored color.
	 */
	public StoredColor store() {
		StoredColor storedColor = this;
		if (getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_color_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_color_max_id, newId);

			List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
			colorIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, colorIds);
			storedColor = new StoredColor(newId, this);
		}
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_device_id, storedColor.getId(), storedColor.getDeviceId());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_color_name, storedColor.getId(), storedColor.getName());
		PreferenceUtil.setIndexedSharedPreferenceColor(R.string.key_color_color, storedColor.getId(), storedColor.getColor());
		return storedColor;
	}

	/**
	 * Get the color.
	 *
	 * @return The color.
	 */
	public Color getColor() {
		return mColor;
	}

	/**
	 * Get the device id for the color.
	 *
	 * @return The device id for the color.
	 */
	public int getDeviceId() {
		return mDeviceId;
	}

	/**
	 * Get the light for the color.
	 *
	 * @return The light for the color.
	 */
	public Light getLight() {
		return (Light) DeviceRegistry.getInstance().getDeviceById(getDeviceId()).getDevice();
	}

	/**
	 * Get the group for the color.
	 *
	 * @return The group for the color.
	 */
	public Group getGroup() {
		return DeviceRegistry.getInstance().getDeviceById(getDeviceId()).getGroup();
	}

	/**
	 * Get the name of the color.
	 *
	 * @return The name of the color.
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Get the id for storage.
	 *
	 * @return The id for storage.
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Set the stored color.
	 *
	 * @param colorDuration The duration of color change.
	 * @param model         The model from which the change is triggered.
	 */
	// OVERRIDABLE
	protected void setColor(final int colorDuration, final MainViewModel model) throws IOException {
		getLight().setColor(getColor(), colorDuration, false);
		if (model instanceof LightViewModel) {
			((LightViewModel) model).updateStoredColor(getColor());
		}
	}

	/**
	 * Apply the stored color. This includes ending the animation and setting power on (if applicable).
	 *
	 * @param model The calling model.
	 */
	private void doApply(final MainViewModel model) {
		if (getLight() != null) {
			try {
				int colorDuration = PreferenceUtil.getSharedPreferenceIntString(
						R.string.key_pref_color_duration, R.string.pref_default_color_duration);
				getLight().endAnimation(false);
				setColor(colorDuration, model);
				if (PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_auto_on, true)) {
					getLight().setPower(true);
					if (model != null) {
						model.updatePowerButton(Power.ON);
					}
				}
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				Light light = getLight();
				DialogUtil.displayToast(Application.getAppContext(), R.string.toast_connection_failed, light == null ? "?" : light.getLabel());
			}
		}
		else if (getGroup() != null) {
			for (Device device : DeviceRegistry.getInstance().getDevices(getDeviceId(), false)) {
				GroupViewModel groupModel = model instanceof GroupViewModel ? (GroupViewModel) model : null;
				if (device instanceof Light) {
					new GroupViewModel.SetColorTask(groupModel, getColor(), (Light) device).execute();
				}
			}
		}
	}

	/**
	 * Apply the stored color.
	 *
	 * @param context The context.
	 * @param model   the calling model.
	 */
	public void apply(final Context context, final MainViewModel model) {
		new ApplyStoredColorTask(context, model).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
	}

	// OVERRIDABLE
	@NonNull
	@Override
	public String toString() {
		return "[" + getId() + "](" + getName() + ")(" + (getLight() == null ? getDeviceId() : getLight().getLabel() + ")-" + getColor());
	}


	/**
	 * An async task for setting the color.
	 */
	protected static final class ApplyStoredColorTask extends AsyncTask<StoredColor, String, StoredColor> {
		/**
		 * The context.
		 */
		private final WeakReference<Context> mContext;
		/**
		 * The calling model.
		 */
		private final MainViewModel mModel;

		/**
		 * Constructor.
		 *
		 * @param context The context.
		 * @param model   the calling model.
		 */
		protected ApplyStoredColorTask(final Context context, final MainViewModel model) {
			super();
			mContext = new WeakReference<>(context);
			mModel = model;
		}

		@Override
		protected StoredColor doInBackground(final StoredColor... storedColors) {
			StoredColor storedColor = storedColors[0];
			storedColor.doApply(mModel);
			return storedColor;
		}
	}

	/**
	 * Callback interface for applying a stored color.
	 */
	public interface ApplyStoredColorCallback {
		/**
		 * Action to be applied after successful application of stored color.
		 */
		void onStoredColorApplied();
	}
}
