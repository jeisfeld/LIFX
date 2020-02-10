package de.jeisfeld.lifx.app.ui.home;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.ListFragment;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DeviceRegistry;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * The home fragment of the app.
 */
public class HomeFragment extends ListFragment {
	/**
	 * Executor service for tasks run while the fragment is active.
	 */
	private ScheduledExecutorService mExecutor;

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home, container, false);
	}

	@Override
	public final void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (DeviceRegistry.getInstance().getDevices().size() == 0) {
			getListView().setVisibility(View.GONE);
			getView().findViewById(R.id.textViewNoDevice).setVisibility(View.VISIBLE);
		}
		DeviceAdapter adapter = new DeviceAdapter(this, new NoDeviceCallback());
		setListAdapter(adapter);
	}

	@Override
	public final void onResume() {
		super.onResume();

		// TODO: Move to preferences
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_pref_refresh_period, 500);

		mExecutor = Executors.newScheduledThreadPool(1);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				((DeviceAdapter) getListAdapter()).refresh();
			}
		};

		if (PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, 0) > 0) {
			mExecutor.scheduleAtFixedRate(runnable, 0,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, 0), TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public final void onPause() {
		super.onPause();
		mExecutor.shutdown();
		mExecutor = null;
	}

	/**
	 * Callback to be called if device availability changes.
	 */
	protected class NoDeviceCallback {
		/**
		 * Device availability changes.
		 *
		 * @param hasDevice true if there is a device.
		 */
		void onChange(final boolean hasDevice) {
			TextView textViewNoDevice = getView().findViewById(R.id.textViewNoDevice);
			if (hasDevice) {
				textViewNoDevice.setVisibility(View.GONE);
				getListView().setVisibility(View.VISIBLE);
			}
			else {
				textViewNoDevice.setVisibility(View.VISIBLE);
				textViewNoDevice.setText(getString(R.string.message_no_devices_found));
				getListView().setVisibility(View.GONE);
			}
		}
	}

}
