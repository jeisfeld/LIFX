package de.jeisfeld.lifx.app.ui.home;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.ListFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.service.LifxAnimationService;
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
	/**
	 * Broadcast receiver for receiving messages while the fragment is active.
	 */
	private BroadcastReceiver mReceiver;

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
		final DeviceAdapter adapter = new DeviceAdapter(this, new NoDeviceCallback());
		setListAdapter(adapter);

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				String mac = intent.getStringExtra(LifxAnimationService.EXTRA_ANIMATION_STOP_MAC);
				DeviceViewModel viewModel = adapter.getViewModel(mac);
				if (viewModel instanceof LightViewModel) {
					((LightViewModel) viewModel).mAnimationStatus.setValue(false);
				}
			}
		};
	}

	@Override
	public final void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver,
				new IntentFilter(LifxAnimationService.EXTRA_ANIMATION_STOP_INTENT));

		mExecutor = Executors.newScheduledThreadPool(1);
		if (PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period) > 0) {
			mExecutor.scheduleAtFixedRate(() -> ((DeviceAdapter) getListAdapter()).refresh(), 0,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period),
					TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public final void onPause() {
		super.onPause();
		mExecutor.shutdown();
		mExecutor = null;

		LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
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
