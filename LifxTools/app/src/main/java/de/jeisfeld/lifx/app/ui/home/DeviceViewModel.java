package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding data for the display view of a device.
 */
public class DeviceViewModel extends ViewModel {
	/**
	 * The context.
	 */
	private final WeakReference<Context> mContext;

	/**
	 * The device.
	 */
	private final Device mDevice;
	/**
	 * The stored power of the device.
	 */
	protected final MutableLiveData<Power> mPower; // SUPPRESS_CHECKSTYLE
	/**
	 * The flag if the device is selected.
	 */
	protected final MutableLiveData<Boolean> mIsSelected; // SUPPRESS_CHECKSTYLE


	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param device The device.
	 */
	public DeviceViewModel(final Context context, final Device device) {
		mContext = new WeakReference<>(context);
		mDevice = device;
		mPower = new MutableLiveData<>();
		mIsSelected = new MutableLiveData<>();
		mIsSelected.setValue(false);
	}

	/**
	 * Get the last checked power of the device.
	 *
	 * @return The power.
	 */
	protected LiveData<Power> getPower() {
		return mPower;
	}

	/**
	 * Get the selected flag.
	 *
	 * @return The power.
	 */
	protected LiveData<Boolean> getIsSelected() {
		return mIsSelected;
	}

	/**
	 * Get the context.
	 *
	 * @return the context.
	 */
	protected WeakReference<Context> getContext() {
		return mContext;
	}

	/**
	 * Get the device.
	 *
	 * @return the device.
	 */
	protected Device getDevice() {
		return mDevice;
	}

	/**
	 * Check the power of the device.
	 */
	public void checkPower() {
		new CheckPowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Refresh the device. If offline, first check if online again.
	 */
	protected final void refresh() {
		if (isRefreshAllowed()) {
			if (mPower.getValue() == null) {
				new RefreshAfterCheckReachabilityTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else {
				refreshRemoteData();
			}
		}
	}

	/**
	 * Check if refresh is allowed.
	 *
	 * @return true if refresh is allowed.
	 */
	protected boolean isRefreshAllowed() {
		return PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period) > 0;
	}

	/**
	 * Refresh the data retrievable from the device.
	 */
	protected void refreshRemoteData() {
		checkPower();
	}

	/**
	 * Toggle the power state.
	 */
	public void togglePower() {
		new TogglePowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * An async task for checking the power.
	 */
	private static final class CheckPowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<DeviceViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckPowerTask(final DeviceViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			return model.mDevice.getPower();
		}

		@Override
		protected void onPostExecute(final Power power) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mPower.postValue(power);
		}
	}

	/**
	 * An async task for first checking reachability and in case device is reachable do refresh.
	 */
	private static final class RefreshAfterCheckReachabilityTask extends AsyncTask<String, String, Boolean> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<DeviceViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private RefreshAfterCheckReachabilityTask(final DeviceViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Boolean doInBackground(final String... strings) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return false;
			}
			return model.mDevice.isReachable();
		}

		@Override
		protected void onPostExecute(final Boolean isReachable) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			if (isReachable) {
				model.refreshRemoteData();
			}
		}
	}

	/**
	 * An async task for toggling the power.
	 */
	private static final class TogglePowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<DeviceViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private TogglePowerTask(final DeviceViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			Power power = model.mPower.getValue();
			if (power == null) {
				power = model.mDevice.getPower();
				if (power == null) {
					return null;
				}
			}
			try {
				// TODO: enable quick power switch via setColor. Requires to know the current color.
				if (model.mDevice instanceof Light) {
					((Light) model.mDevice).setPower(!power.isOn(),
							PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_power_duration, R.string.pref_default_power_duration),
							false);
				}
				else {
					model.mDevice.setPower(!power.isOn());
				}
				if (model.isRefreshAllowed()) {
					return Power.UNDEFINED;
				}
				else {
					return power.isOn() ? Power.OFF : Power.ON;
				}
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Power power) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mPower.postValue(power);

			if(model instanceof MultizoneViewModel && ((MultizoneViewModel) model).getColors().getValue() == null) {
				((MultizoneViewModel) model).checkColor();
			}
		}
	}

}
