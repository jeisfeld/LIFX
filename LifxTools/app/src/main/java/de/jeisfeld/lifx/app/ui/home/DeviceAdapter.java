package de.jeisfeld.lifx.app.ui.home;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.skydoves.colorpickerview.ColorPickerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.ui.home.HomeFragment.NoDeviceCallback;
import de.jeisfeld.lifx.app.util.ColorPickerDialog;
import de.jeisfeld.lifx.app.util.ColorPickerDialog.Builder;
import de.jeisfeld.lifx.app.util.DeviceRegistry;
import de.jeisfeld.lifx.app.util.DeviceRegistry.DeviceUpdateCallback;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * An adapter for the list of devices in the home fragment.
 */
public class DeviceAdapter extends BaseAdapter {
	/**
	 * The IDs of seekbars within this view.
	 */
	private static final int[] SEEKBAR_IDS = {R.id.seekBarBrightness, R.id.seekBarHue, R.id.seekBarSaturation, R.id.seekBarColorTemperature};

	/**
	 * The list of devices.
	 */
	private final List<Device> mDevices;
	/**
	 * The list of view models.
	 */
	private final List<DeviceViewModel> mViewModels = new ArrayList<>();

	/**
	 * The context.
	 */
	private final Context mContext;
	/**
	 * Reference to the fragment.
	 */
	private final WeakReference<Fragment> mFragment;

	/**
	 * The lifecycle owner.
	 */
	private final LifecycleOwner mLifeCycleOwner;
	/**
	 * Callback on change of "no device" status.
	 */
	private final NoDeviceCallback mNoDeviceCallback;
	/**
	 * A store for the views.
	 */
	private final List<View> mViews = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param fragment The triggering fragment.
	 * @param callback A calllback called in case of no devices.
	 */
	DeviceAdapter(final Fragment fragment, final NoDeviceCallback callback) {
		super();
		mDevices = DeviceRegistry.getInstance().getDevices();
		mContext = fragment.getContext();
		mFragment = new WeakReference<>(fragment);
		mLifeCycleOwner = fragment.getViewLifecycleOwner();
		mNoDeviceCallback = callback;
		for (Device device : mDevices) {
			if (device instanceof MultiZoneLight) {
				mViewModels.add(new MultizoneViewModel(mContext, (MultiZoneLight) device));
			}
			else if (device instanceof Light) {
				mViewModels.add(new LightViewModel(mContext, (Light) device));
			}
			else {
				mViewModels.add(new DeviceViewModel(mContext, device));
			}
		}

		DeviceRegistry.getInstance().update(new DeviceUpdateCallback() {
			@Override
			public void onDeviceUpdated(final Device device, final boolean isNew, final boolean isMissing) {
				if (isNew) {
					if (mDevices.size() == 0) {
						mNoDeviceCallback.onChange(true);
					}
					addDevice(device);
				}
			}

			@Override
			public void onNoDevicesFound() {
				if (mDevices.size() == 0) {
					mNoDeviceCallback.onChange(false);
				}
			}
		});
	}

	@Override
	public final int getCount() {
		return mDevices.size();
	}

	@Override
	public final Device getItem(final int position) {
		return mDevices.get(position);
	}

	@Override
	public final long getItemId(final int position) {
		return position;
	}

	/**
	 * Refresh view data for all devices.
	 */
	protected void refresh() {
		for (DeviceViewModel model : mViewModels) {
			model.refresh();
		}
	}

	@SuppressLint("ViewHolder")
	@Override
	public final synchronized View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view;

		// do not use convertView, as information is stored in the views
		synchronized (mViews) {
			if (mViews.size() > position && mViews.get(position) != null) {
				return mViews.get(position);
			}
		}

		final Device device = getItem(position);

		int layoutId = R.layout.list_view_home_device;
		if (device instanceof MultiZoneLight) {
			layoutId = R.layout.list_view_home_light;
		}
		else if (device instanceof Light) {
			layoutId = R.layout.list_view_home_light;
		}

		view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);

		final TextView text = view.findViewById(R.id.textViewHome);
		text.setText(device.getLabel());

		DeviceViewModel model = mViewModels.get(position);

		final CheckBox checkBoxSelectLight = view.findViewById(R.id.checkboxSelectLight);
		if (checkBoxSelectLight != null) {
			checkBoxSelectLight.setOnClickListener(v -> {
				model.mIsSelected.setValue(((CheckBox) v).isChecked());
				Fragment fragment = mFragment.get();
				if (fragment != null && model instanceof LightViewModel && ((CheckBox) v).isChecked()) {
					ColorPickerView colorPickerView = Objects.requireNonNull(fragment.getView()).findViewById(R.id.ColorPickerView);
					if (colorPickerView != null) {
						ColorPickerDialog.updateColorPickerFromLight(colorPickerView, (LightViewModel) model);
					}
				}
			});
		}

		preparePowerButton(view.findViewById(R.id.buttonPower), model);

		synchronized (mViews) {
			if (mViews.size() > position) {
				mViews.set(position, view);
			}
			else {
				mViews.add(position, view);
			}
		}

		model.checkPower();

		if (device instanceof Light) {
			LightViewModel lightModel = (LightViewModel) model;

			if (device.getProduct().hasColor()) {
				Button buttonColorPicker = view.findViewById(R.id.buttonColorPicker);
				if (buttonColorPicker != null) {
					prepareColorPicker(buttonColorPicker, lightModel);
				}
			}
			prepareBrightnessButton(view, view.findViewById(R.id.buttonBrightness), view.findViewById(R.id.seekBarBrightness), lightModel);
			prepareColorTemperatureButton(view, view.findViewById(R.id.buttonColorTemperature),
					view.findViewById(R.id.seekBarColorTemperature), lightModel);
			prepareAnimationButton(view.findViewById(R.id.toggleButtonAnimation), lightModel);

			lightModel.checkColor();
		}

		return view;
	}

	/**
	 * Prepare the power button.
	 *
	 * @param powerButton The power button.
	 * @param model The device view model.
	 */
	private void preparePowerButton(final Button powerButton, final DeviceViewModel model) {
		model.getPower().observe(mLifeCycleOwner, power -> {
			if (power == null) {
				powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_offline));
			}
			else if (power.isOn()) {
				powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_on));
			}
			else if (power.isOff()) {
				powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_off));
			}
			else {
				powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_undefined));
			}
		});

		powerButton.setOnClickListener(v -> model.togglePower());
	}

	/**
	 * Prepare the color picker started via button.
	 *
	 * @param colorPickerButton The color picker button.
	 * @param model The device view model.
	 */
	private void prepareColorPicker(final Button colorPickerButton, final LightViewModel model) {
		colorPickerButton.setVisibility(View.VISIBLE);

		colorPickerButton.setOnClickListener(v -> new Builder(mContext, R.layout.dialog_colorpicker)
				.initializeFromLight(model)
				.setColorListener((color, fromUser) -> {
					if (fromUser) {
						float[] hsv = new float[3]; // MAGIC_NUMBER
						android.graphics.Color.colorToHSV(color, hsv);
						// Use alpha as color temperature
						short colorTemperature = progressBarToColorTemperature(android.graphics.Color.alpha(color) * 57 / 255); // MAGIC_NUMBER
						model.updateColor(new Color(hsv[0], hsv[1], hsv[2], colorTemperature), false);
					}
				}).show());
	}

	/**
	 * Prepare the brightness button and seekbar.
	 *
	 * @param listView The listView.
	 * @param button The brightness button.
	 * @param seekBar The brightness seekbar.
	 * @param model The light view model.
	 */
	private void prepareBrightnessButton(final View listView, final Button button, final SeekBar seekBar, final LightViewModel model) {
		model.getColor().observe(mLifeCycleOwner, color -> {
			if (color == null) {
				seekBar.setEnabled(false);
			}
			else {
				seekBar.setEnabled(true);
				seekBar.setProgress(TypeUtil.toUnsignedInt(color.getBrightness()));
			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				if (fromUser) {
					model.updateColor(null, null, (short) progress, null, false);
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				model.updateColor(null, null, (short) seekBar.getProgress(), null, true);
			}
		});

		button.setOnClickListener(v -> {
			boolean isSeekbarVisible = seekBar.getVisibility() == View.VISIBLE;
			hideSeekBars(listView);
			seekBar.setVisibility(isSeekbarVisible ? View.GONE : View.VISIBLE);
		});
	}

	/**
	 * Prepare the color temperature button and seekbar.
	 *
	 * @param listView The listView.
	 * @param button The color temperature button.
	 * @param seekBar The color temperature seekbar.
	 * @param model The light view model.
	 */
	private void prepareColorTemperatureButton(final View listView, final Button button, final SeekBar seekBar, final LightViewModel model) {
		button.setVisibility(View.VISIBLE);

		model.getColor().observe(mLifeCycleOwner, color -> {
			if (color == null) {
				seekBar.setEnabled(false);
			}
			else {
				seekBar.setEnabled(true);
				seekBar.setProgress(colorTemperatureToProgress(color.getColorTemperature()));
			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				if (fromUser) {
					model.updateColor(null, null, null, progressBarToColorTemperature(progress), false);
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				model.updateColor(null, null, null, progressBarToColorTemperature(seekBar.getProgress()), false);
			}
		});

		button.setOnClickListener(v -> {
			boolean isSeekbarVisible = seekBar.getVisibility() == View.VISIBLE;
			hideSeekBars(listView);
			seekBar.setVisibility(isSeekbarVisible ? View.GONE : View.VISIBLE);
		});
	}

	/**
	 * Prepare the animation button.
	 *
	 * @param animationButton The animation button.
	 * @param model The multizone device view model.
	 */
	private void prepareAnimationButton(final ToggleButton animationButton, final LightViewModel model) {
		model.getAnimationStatus().observe(mLifeCycleOwner, animationButton::setChecked);

		animationButton.setOnClickListener(v -> model.updateAnimation(((ToggleButton) v).isChecked()));
	}

	/**
	 * Hide all seekbars within the main view.
	 *
	 * @param view The main view.
	 */
	private void hideSeekBars(final View view) {
		for (int id : SEEKBAR_IDS) {
			SeekBar seekBar = view.findViewById(id);
			if (seekBar != null) {
				seekBar.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Add a device to the list.
	 *
	 * @param device The device to be added.
	 */
	public void addDevice(final Device device) {
		mDevices.add(device);
		mViewModels.add(new DeviceViewModel(mContext, device));
		notifyDataSetChanged();
	}

	/**
	 * Get the view model from a MAC (if available).
	 *
	 * @param mac The MAC
	 * @return the view model
	 */
	protected DeviceViewModel getViewModel(final String mac) {
		for (int i = 0; i < mDevices.size(); i++) {
			if (mac.equals(mDevices.get(i).getTargetAddress())) {
				return mViewModels.get(i);
			}
		}
		return null;
	}

	/**
	 * Get all checked devices.
	 *
	 * @return The list of checked devices.
	 */
	protected List<DeviceViewModel> getCheckedDevices() {
		List<DeviceViewModel> checkedDevices = new ArrayList<>();
		for (DeviceViewModel model : mViewModels) {
			if (model.getIsSelected().getValue()) {
				checkedDevices.add(model);
			}
		}
		return checkedDevices;
	}

	/**
	 * Convert seekbar value to color temperature.
	 *
	 * @param progress The seekbar value
	 * @return The color temperature
	 */
	protected static short progressBarToColorTemperature(final int progress) {
		double p = 38.3 + progress; // MAGIC_NUMBER
		return (short) Math.max(1500, Math.min(9000, p * p)); // MAGIC_NUMBER
	}

	/**
	 * Convert color temperature to seekbar value.
	 *
	 * @param colorTemperature The color temperature
	 * @return The seekbar value
	 */
	public static int colorTemperatureToProgress(final short colorTemperature) {
		int progress = (int) Math.round(Math.sqrt(TypeUtil.toUnsignedInt(colorTemperature)) - 38.3); // MAGIC_NUMBER
		return Math.max(0, Math.min(57, progress)); // MAGIC_NUMBER
	}

}
