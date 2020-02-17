package de.jeisfeld.lifx.app.ui.home;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.ListFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.service.LifxAnimationService;
import de.jeisfeld.lifx.app.util.ColorPickerDialog;
import de.jeisfeld.lifx.app.util.ColorPickerDialog.Builder;
import de.jeisfeld.lifx.app.util.DeviceRegistry;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.type.Color;

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
			Objects.requireNonNull(getView()).findViewById(R.id.textViewNoDevice).setVisibility(View.VISIBLE);
		}
		final DeviceAdapter adapter = new DeviceAdapter(this, new NoDeviceCallback());
		setListAdapter(adapter);

		ConstraintLayout layoutColorPicker = Objects.requireNonNull(getView()).findViewById(R.id.layoutColorPicker);
		ConstraintLayout layoutBrightnessColorTempPicker = Objects.requireNonNull(getView()).findViewById(R.id.layoutBrightnessColorTempPicker);
		prepareColorPickers(layoutColorPicker, layoutBrightnessColorTempPicker);

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

		LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).registerReceiver(mReceiver,
				new IntentFilter(LifxAnimationService.EXTRA_ANIMATION_STOP_INTENT));

		mExecutor = Executors.newScheduledThreadPool(1);
		if (PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period) > 0) {
			mExecutor.scheduleAtFixedRate(() -> ((DeviceAdapter) Objects.requireNonNull(getListAdapter())).refresh(), 0,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period),
					TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public final void onPause() {
		super.onPause();
		mExecutor.shutdown();
		mExecutor = null;

		LocalBroadcastManager.getInstance(Objects.requireNonNull(getContext())).unregisterReceiver(mReceiver);
	}

	/**
	 * Prepare the color pickers shown in landscape view.
	 *
	 * @param layoutColorPicker The color picker layout.
	 * @param layoutBrightnessColorTempPicker The brightness/contrast picker layout.
	 */
	private void prepareColorPickers(final ConstraintLayout layoutColorPicker, final ConstraintLayout layoutBrightnessColorTempPicker) {
		final ColorPickerView colorPickerView;
		if (layoutColorPicker != null) {
			Builder builder = new Builder(getContext(), layoutColorPicker, R.id.colorPickerMain);
			colorPickerView = builder.getColorPickerView();
		}
		else {
			colorPickerView = null;
		}
		final ColorPickerView brightnessColorTempPickerView;
		if (layoutBrightnessColorTempPicker != null) {
			Builder builder = new Builder(getContext(), layoutBrightnessColorTempPicker, R.id.colorPickerBrightnessColorTemp);
			brightnessColorTempPickerView = builder.getColorPickerView();
		}
		else {
			brightnessColorTempPickerView = null;
		}

		if (colorPickerView != null) {
			colorPickerView.setColorListener((ColorListener) (color, fromUser) -> {
				if (fromUser) {
					float[] hsv = new float[3]; // MAGIC_NUMBER
					android.graphics.Color.colorToHSV(color, hsv);
					double brightness = hsv[2] == 0 ? 1 / 65535.0 : hsv[2]; // MAGIC_NUMBER
					// Use alpha as color temperature
					short colorTemperature =
							DeviceAdapter.progressBarToColorTemperature(android.graphics.Color.alpha(color) * 120 / 255); // MAGIC_NUMBER
					Color newColor = new Color(hsv[0], hsv[1], brightness, colorTemperature);

					List<DeviceViewModel> checkedDevices = ((DeviceAdapter) Objects.requireNonNull(getListAdapter())).getCheckedDevices();
					for (DeviceViewModel model : checkedDevices) {
						if (model instanceof LightViewModel) {
							((LightViewModel) model).updateColor(newColor);
						}
					}

					if (brightnessColorTempPickerView != null) {
						ColorPickerDialog.updateBrightnessColorTempFromLight(brightnessColorTempPickerView, newColor);
					}
				}
			});
		}

		if (brightnessColorTempPickerView != null) {
			brightnessColorTempPickerView.setColorListener((ColorListener) (color, fromUser) -> {
				if (fromUser) {
					Color newColor = DeviceAdapter.convertBrightnessColorTempPickerColor(color);

					List<DeviceViewModel> checkedDevices = ((DeviceAdapter) Objects.requireNonNull(getListAdapter())).getCheckedDevices();
					for (DeviceViewModel model : checkedDevices) {
						if (model instanceof LightViewModel) {
							((LightViewModel) model).updateColor(newColor);
						}
					}

					if (colorPickerView != null) {
						ColorPickerDialog.updateColorPickerFromLight(colorPickerView, newColor);
					}
				}
			});
		}
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
			if (getView() == null) {
				return;
			}
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
