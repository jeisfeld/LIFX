package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import de.jeisfeld.lifx.app.Application;
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
					return power.isOn() ? Power.OFF : Power.ON;
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
