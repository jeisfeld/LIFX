package de.jeisfeld.lifx.app.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.ListFragment;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.animation.LifxAnimationService;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.ImageUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.app.view.ColorPickerDialog;
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
	/**
	 * The adapter used.
	 */
	private DeviceAdapter mAdapter;

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home, container, false);
	}

	@Override
	public final void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (DeviceRegistry.getInstance().getDevices(false).size() == 0) {
			getListView().setVisibility(View.GONE);
			requireView().findViewById(R.id.textViewNoDevice).setVisibility(View.VISIBLE);
		}
		mAdapter = new DeviceAdapter(this, new NoDeviceCallback());
		setListAdapter(mAdapter);

		ConstraintLayout layoutColorPicker = requireView().findViewById(R.id.layoutColorPicker);
		ConstraintLayout layoutBrightnessColorTempPicker = requireView().findViewById(R.id.layoutBrightnessColorTempPicker);
		prepareColorPickers(layoutColorPicker, layoutBrightnessColorTempPicker);

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				String mac = intent.getStringExtra(LifxAnimationService.EXTRA_ANIMATION_STOP_MAC);
				DeviceViewModel viewModel = mAdapter.getViewModel(mac);
				if (viewModel instanceof LightViewModel) {
					((LightViewModel) viewModel).mAnimationStatus.setValue(false);
				}
			}
		};
	}

	@Override
	public final void onResume() {
		super.onResume();

		requireActivity().registerReceiver(mReceiver, new IntentFilter(LifxAnimationService.EXTRA_ANIMATION_STOP_INTENT));

		mExecutor = Executors.newScheduledThreadPool(1);
		if (PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period) > 0) {
			mExecutor.scheduleAtFixedRate(() -> mAdapter.refresh(), 0,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_refresh_period, R.string.pref_default_refresh_period),
					TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public final void onPause() {
		super.onPause();
		mExecutor.shutdown();
		mExecutor = null;

		requireActivity().unregisterReceiver(mReceiver);
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
			colorPickerView = layoutColorPicker.findViewById(R.id.colorPickerMain);
			if (colorPickerView != null) {
				ColorPickerDialog.prepareColorPickerView(layoutColorPicker, colorPickerView);
			}
		}
		else {
			colorPickerView = null;
		}
		final ColorPickerView brightnessColorTempPickerView;
		if (layoutBrightnessColorTempPicker != null) {
			brightnessColorTempPickerView = layoutBrightnessColorTempPicker.findViewById(R.id.colorPickerBrightnessColorTemp);
			if (brightnessColorTempPickerView != null) {
				ColorPickerDialog.prepareColorPickerView(layoutBrightnessColorTempPicker, brightnessColorTempPickerView);
			}
		}
		else {
			brightnessColorTempPickerView = null;
		}

		if (colorPickerView != null) {
			colorPickerView.setColorListener((ColorListener) (color, fromUser) -> {
				if (fromUser) {
					// Use alpha as color temperature
					short colorTemperature =
							DeviceAdapter.progressBarToColorTemperature(android.graphics.Color.alpha(color) * 120 / 255); // MAGIC_NUMBER
					Color newColor = ColorUtil.convertAndroidColorToColor(color, colorTemperature, false);

					List<DeviceViewModel> checkedDevices = mAdapter.getCheckedDevices();
					for (DeviceViewModel model : checkedDevices) {
						if (model instanceof LightViewModel) {
							((LightViewModel) model).updateColor(newColor, true);
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

					List<DeviceViewModel> checkedDevices = mAdapter.getCheckedDevices();
					for (DeviceViewModel model : checkedDevices) {
						if (model instanceof LightViewModel) {
							((LightViewModel) model).updateColor(newColor, true);
						}
					}

					if (colorPickerView != null) {
						ColorPickerDialog.updateColorPickerFromLight(colorPickerView, newColor);
					}
				}
			});
		}
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case DeviceAdapter.REQUESTCODE_PICK_IMAGE:
				try {
					if (getActivity() == null) {
						return;
					}
					Bitmap bitmap = ImageUtil.getBitmapFromUri(getActivity(), intent.getData());
					if (bitmap != null) {
						mAdapter.handleBitmap(bitmap);
					}
				}
				catch (Exception e) {
					Log.e(Application.TAG, "Exception while retrieving bitmap", e);
					e.printStackTrace();
				}
				break;
			default:
				// nothing to do.
				break;
			}
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
