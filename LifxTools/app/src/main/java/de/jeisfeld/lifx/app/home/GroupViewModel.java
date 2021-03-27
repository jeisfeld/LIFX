package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Group;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding data for the display view of a group.
 */
public class GroupViewModel extends MainViewModel {
	/**
	 * The group.
	 */
	private final Group mGroup;
	/**
	 * The groupId.
	 */
	private final int mGroupId;
	/**
	 * The stored Color of the group.
	 */
	private final MutableLiveData<Color> mColor;
	/**
	 * A storage to keep track on running setColor tasks.
	 */
	protected final Map<Device, List<AsyncExecutable>> mRunningSetColorTasks = new HashMap<>(); // SUPPRESS_CHECKSTYLE
	/**
	 * The device adapter referring this model.
	 */
	private WeakReference<DeviceAdapter> mAdapter;

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param group   The group.
	 * @param groupId The groupId.
	 * @param adapter The calling device adapter.
	 */
	public GroupViewModel(final Context context, final Group group, final int groupId, final DeviceAdapter adapter) {
		super(context);
		mGroup = group;
		mGroupId = groupId;
		mColor = new MutableLiveData<>();
		mAdapter = new WeakReference<>(adapter);
	}

	/**
	 * Get the group.
	 *
	 * @return the group.
	 */
	protected Group getGroup() {
		return mGroup;
	}

	/**
	 * Get the group id.
	 *
	 * @return The group id.
	 */
	protected int getGroupId() {
		return mGroupId;
	}

	@Override
	public final LiveData<Color> getColor() {
		return mColor;
	}

	@Override
	public final CharSequence getLabel() {
		return Html.fromHtml("<b>" + mGroup.getGroupLabel() + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	@Override
	public final void checkPower() {
		new CheckPowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public final void togglePower() {
		new TogglePowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Check the Color of the device.
	 */
	public void checkColor() {
		new CheckColorTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected final void updateBrightness(final double brightness) {
		synchronized (mRunningSetColorTasks) {
			for (Device device : DeviceRegistry.getInstance().getDevices(mGroupId, false)) {
				if (device instanceof Light) {
					List<AsyncExecutable> tasksForDevice = mRunningSetColorTasks.get(device);
					if (tasksForDevice == null) {
						tasksForDevice = new ArrayList<>();
						mRunningSetColorTasks.put(device, tasksForDevice);
					}
					tasksForDevice.add(new SetColorTask(this, brightness, (Light) device));

					if (tasksForDevice.size() > 2) {
						tasksForDevice.remove(1);
					}
					if (tasksForDevice.size() == 1) {
						tasksForDevice.get(0).execute();
					}
				}
			}
		}
	}

	@Override
	protected final void updateStoredColor(final StoredColor storedColor) {
		updateColor(storedColor.getColor());
	}

	/**
	 * Set the color.
	 *
	 * @param color the color to be set.
	 */
	public void updateColor(final Color color) {
		mColor.postValue(color);
		synchronized (mRunningSetColorTasks) {
			for (Device device : DeviceRegistry.getInstance().getDevices(mGroupId, false)) {
				if (device instanceof Light) {
					List<AsyncExecutable> tasksForDevice = mRunningSetColorTasks.get(device);
					if (tasksForDevice == null) {
						tasksForDevice = new ArrayList<>();
						mRunningSetColorTasks.put(device, tasksForDevice);
					}
					tasksForDevice.add(new SetColorTask(this, color, (Light) device));

					if (tasksForDevice.size() > 2) {
						tasksForDevice.remove(1);
					}
					if (tasksForDevice.size() == 1) {
						tasksForDevice.get(0).execute();
					}
				}
			}
		}
	}

	/**
	 * An async task for checking the power.
	 */
	private static final class CheckPowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<GroupViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckPowerTask(final GroupViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			Power result = Power.UNDEFINED;
			for (Device device : DeviceRegistry.getInstance().getDevices(model.mGroupId, false)) {
				Power devicePower = device.getPower();
				if (devicePower != null) {
					if (devicePower.isOn()) {
						return Power.ON;
					}
					else if (devicePower.isOff()) {
						result = Power.OFF;
						model.mPower.postValue(Power.OFF);
					}
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(final Power power) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mPower.postValue(power);
		}
	}

	/**
	 * An async task for toggling the power.
	 */
	private static final class TogglePowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<GroupViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private TogglePowerTask(final GroupViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			Power power = model.mPower.getValue();
			if (power == null) {
				return null;
			}

			int powerDuration = PreferenceUtil.getSharedPreferenceIntString(
					R.string.key_pref_power_duration, R.string.pref_default_power_duration);
			for (Device device : DeviceRegistry.getInstance().getDevices(model.mGroupId, false)) {
				new Thread() {
					@Override
					public void run() {
						try {
							if (device instanceof Light) {
								((Light) device).setPower(!power.isOn(), powerDuration, false);
							}
							else {
								device.setPower(!power.isOn());
							}
						}
						catch (IOException e) {
							Log.w(Application.TAG, e);
						}
					}
				}.start();
			}
			return power.isOn() ? Power.OFF : Power.ON;
		}

		@Override
		protected void onPostExecute(final Power power) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mPower.postValue(power);

			DeviceAdapter adapter = model.mAdapter.get();
			if (adapter != null) {
				adapter.refreshGroupPower(model.getGroupId(), power);
			}
		}
	}

	/**
	 * An async task for checking the color.
	 */
	private static final class CheckColorTask extends AsyncTask<String, String, Color> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<GroupViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckColorTask(final GroupViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Color doInBackground(final String... strings) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			Color foundColor = null;

			for (Device device : DeviceRegistry.getInstance().getDevices(model.mGroupId, false)) {
				if (device instanceof Light) {
					Color color = ((Light) device).getColor();
					if (color != null) {
						if (foundColor == null) {
							foundColor = color;
						}
						else if (!foundColor.isSimilar(color)) {
							return null;
						}
					}
				}
			}
			return foundColor;
		}

		@Override
		protected void onPostExecute(final Color color) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mColor.postValue(color);
		}
	}

	/**
	 * An async task for setting the color on one device.
	 */
	public static final class SetColorTask extends AsyncTask<Color, String, Color> implements AsyncExecutable {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<GroupViewModel> mModel;
		/**
		 * The color to be set.
		 */
		private final Color mColor;
		/**
		 * The light.
		 */
		private final Light mLight;
		/**
		 * The brightness.
		 */
		private final double mBrightness;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 * @param color The color.
		 * @param light The light.
		 */
		public SetColorTask(final GroupViewModel model, final Color color, final Light light) {
			mModel = new WeakReference<>(model);
			mColor = color;
			mLight = light;
			mBrightness = 0;
		}

		/**
		 * Constructor, passing only brightness.
		 *
		 * @param model      The underlying model.
		 * @param brightness The brightness.
		 * @param light      The light.
		 */
		public SetColorTask(final GroupViewModel model, final double brightness, final Light light) {
			mModel = new WeakReference<>(model);
			mColor = null;
			mLight = light;
			mBrightness = brightness;
		}

		@Override
		protected Color doInBackground(final Color... colors) {
			try {
				if (mColor == null) {
					mLight.setBrightness(mBrightness);
				}
				else {
					mLight.setColor(mColor, 0, false);
				}
				if (isAutoOn()) {
					mLight.setPower(true, 0, false);
				}
				return mColor;
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Color color) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			synchronized (model.mRunningSetColorTasks) {
				List<AsyncExecutable> tasksForDevice = model.mRunningSetColorTasks.get(mLight);
				if (tasksForDevice != null) {
					tasksForDevice.remove(this);
					if (tasksForDevice.size() > 0) {
						tasksForDevice.get(0).execute();
					}
				}
			}
			if (color != null) {
				model.mColor.postValue(color);
			}
			if (isAutoOn()) {
				model.mPower.postValue(Power.ON);
				DeviceAdapter adapter = model.mAdapter.get();
				if (adapter != null) {
					adapter.refreshGroupPower(model.getGroupId(), Power.ON);
					adapter.refreshGroupColor(model.getGroupId(), color);
				}
			}
		}

		@Override
		public void execute() {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		}
	}
}
