package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import de.jeisfeld.lifx.R;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding data for the display view of a device.
 */
public class DeviceViewModel extends ViewModel {
	/**
	 * The device.
	 */
	private final Device mDevice;
	/**
	 * The stored power of the device.
	 */
	private final MutableLiveData<Power> mPower;

	/**
	 * Constructor.
	 *
	 * @param device The device.
	 */
	public DeviceViewModel(final Device device) {
		mDevice = device;
		mPower = new MutableLiveData<>();
	}

	/**
	 * Get the last checked power of the device.
	 *
	 * @return The power.
	 */
	public LiveData<Power> getPower() {
		return mPower;
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
		new AsyncTask<String, String, Power>() {
			@Override
			protected Power doInBackground(final String... strings) {
				return mDevice.getPower();
			}

			@Override
			protected void onPostExecute(final Power power) {
				mPower.setValue(power);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Refresh the device. If offline, first check if online again.
	 */
	protected void refresh() {
		if (isRefreshAllowed()) {
			if (mPower.getValue() == null) {
				refreshAfterCheckReachabiliby();
			}
			else {
				doRefresh();
			}
		}
	}

	/**
	 * Check if refresh is allowed.
	 *
	 * @return true if refresh is allowed.
	 */
	protected boolean isRefreshAllowed() {
		return PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, 0) > 0;
	}

	/**
	 * Check the power of the device.
	 */
	public void refreshAfterCheckReachabiliby() {
		new AsyncTask<String, String, Boolean>() {
			@Override
			protected Boolean doInBackground(final String... strings) {
				return mDevice.isReachable();
			}

			@Override
			protected void onPostExecute(final Boolean isReachable) {
				if (isReachable) {
					doRefresh();
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Refresh the device.
	 */
	private void doRefresh() {
		checkPower();
	}

	/**
	 * Toggle the power state.
	 */
	public void togglePower() {
		new AsyncTask<String, String, Power>() {
			@Override
			protected Power doInBackground(final String... strings) {
				Power power = mPower.getValue();
				if (power == null) {
					power = mDevice.getPower();
					if (power == null) {
						return null;
					}
				}
				try {
					mDevice.setPower(!power.isOn());
					if (isRefreshAllowed()) {
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
				mPower.setValue(power);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

}
