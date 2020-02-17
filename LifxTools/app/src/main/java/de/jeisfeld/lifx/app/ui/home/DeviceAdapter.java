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
	public final DeviceViewModel getItem(final int position) {
		return mViewModels.get(position);
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

		final DeviceViewModel model = getItem(position);

		int layoutId = R.layout.list_view_home_device;
		if (model instanceof MultizoneViewModel) {
			layoutId = R.layout.list_view_home_light;
		}
		else if (model instanceof LightViewModel) {
			layoutId = R.layout.list_view_home_light;
		}

		view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);

		final TextView text = view.findViewById(R.id.textViewHome);
		text.setText(model.getDevice().getLabel());

		final CheckBox checkBoxSelectLight = view.findViewById(R.id.checkboxSelectLight);
		if (checkBoxSelectLight != null) {
			checkBoxSelectLight.setOnClickListener(v -> {
				model.mIsSelected.setValue(((CheckBox) v).isChecked());
				Fragment fragment = mFragment.get();
				if (fragment != null && model instanceof LightViewModel && ((CheckBox) v).isChecked()) {
					ColorPickerView colorPickerView = Objects.requireNonNull(fragment.getView()).findViewById(R.id.colorPickerMain);
					if (colorPickerView != null) {
						ColorPickerDialog.updateColorPickerFromLight(colorPickerView, ((LightViewModel) model).getColor().getValue());
					}
					ColorPickerView brightnessColorTempPickerView =
							Objects.requireNonNull(fragment.getView()).findViewById(R.id.colorPickerBrightnessColorTemp);
					if (brightnessColorTempPickerView != null) {
						ColorPickerDialog.updateBrightnessColorTempFromLight(brightnessColorTempPickerView,
								((LightViewModel) model).getColor().getValue());
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

		if (model instanceof LightViewModel) {
			LightViewModel lightModel = (LightViewModel) model;

			if (lightModel.getDevice().getProduct().hasColor()) {
				Button buttonColorPicker = view.findViewById(R.id.buttonColorPicker);
				if (buttonColorPicker != null) {
					prepareColorPicker(buttonColorPicker, lightModel);
				}
			}
			Button buttonBrightnessColorTemp = view.findViewById(R.id.buttonBrightnessColortemp);
			if (buttonBrightnessColorTemp != null) {
				prepareBrightnessColortempButton(buttonBrightnessColorTemp, lightModel);
			}

			prepareBrightnessSeekbar(view.findViewById(R.id.seekBarBrightness), lightModel);
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
						short colorTemperature = progressBarToColorTemperature(android.graphics.Color.alpha(color) * 120 / 255); // MAGIC_NUMBER
						double brightness = hsv[2] == 0 ? 1 / 65535.0 : hsv[2]; // MAGIC_NUMBER
						model.updateColor(new Color(hsv[0], hsv[1], brightness, colorTemperature));
					}
				}).show());
	}

	/**
	 * Prepare the picker for brightness and color temperature started via button.
	 *
	 * @param brightnessColorTempButton The color picker button.
	 * @param model The device view model.
	 */
	private void prepareBrightnessColortempButton(final Button brightnessColorTempButton, final LightViewModel model) {
		brightnessColorTempButton.setVisibility(View.VISIBLE);

		brightnessColorTempButton.setOnClickListener(v -> new Builder(mContext, R.layout.dialog_brightness_colortemp)
				.initializeFromBrightnessColorTemp(model)
				.setColorListener((color, fromUser) -> {
					if (fromUser) {
						model.updateColor(convertBrightnessColorTempPickerColor(color));
					}
				}).show());
	}

	/**
	 * Prepare the brightness button and seekbar.
	 *
	 * @param seekBar The brightness seekbar.
	 * @param model The light view model.
	 */
	private void prepareBrightnessSeekbar(final SeekBar seekBar, final LightViewModel model) {
		seekBar.setVisibility(View.VISIBLE);

		if (model instanceof MultizoneViewModel) {
			((MultizoneViewModel) model).getRelativeBrightness().observe(mLifeCycleOwner, relativeBrightness -> {
				if (relativeBrightness != null) {
					seekBar.setProgress(TypeUtil.toUnsignedInt(TypeUtil.toShort(relativeBrightness)));
				}
			});
		}
		else {
			model.getColor().observe(mLifeCycleOwner, color -> {
				if (color != null) {
					seekBar.setProgress(TypeUtil.toUnsignedInt(color.getBrightness()));
				}
			});
		}

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				if (fromUser) {
					model.updateBrightness((short) (progress + 1));
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				model.updateBrightness((short) (seekBar.getProgress() + 1));
			}
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
	 * Convert seekbar value (0 to 120) to color temperature.
	 *
	 * @param progress The seekbar value
	 * @return The color temperature
	 */
	protected static short progressBarToColorTemperature(final int progress) {
		int colorTemp;
		if (progress <= 72) { // MAGIC_NUMBER
			colorTemp = (1000 * progress / 36) + 1500; // MAGIC_NUMBER
		}
		else if (progress <= 96) { // MAGIC_NUMBER
			colorTemp = (1000 * (progress - 72) / 16) + 3500; // MAGIC_NUMBER
		}
		else {
			colorTemp = (1000 * (progress - 96) / 6) + 5000; // MAGIC_NUMBER
		}
		return (short) Math.max(1500, Math.min(9000, colorTemp)); // MAGIC_NUMBER
	}

	/**
	 * Convert color temperature to seekbar value (0 to 120).
	 *
	 * @param colorTemperature The color temperature
	 * @return The seekbar value
	 */
	public static int colorTemperatureToProgress(final short colorTemperature) {
		int progress;
		if (colorTemperature <= 3500) { // MAGIC_NUMBER
			progress = (colorTemperature - 1500) * 36 / 1000; // MAGIC_NUMBER
		}
		else if (colorTemperature <= 5000) { // MAGIC_NUMBER
			progress = 72 + (colorTemperature - 3500) * 16 / 1000; // MAGIC_NUMBER
		}
		else {
			progress = 96 + (colorTemperature - 5000) * 6 / 1000; // MAGIC_NUMBER
		}
		return Math.max(0, Math.min(120, progress)); // MAGIC_NUMBER
	}

	/**
	 * Convert the color from brightness/colorTemp picker to light color.
	 *
	 * @param color The brightness/colotTemp picker color
	 * @return The light color
	 */
	protected static Color convertBrightnessColorTempPickerColor(final int color) {
		float[] hsv = new float[3]; // MAGIC_NUMBER
		android.graphics.Color.colorToHSV(color, hsv);

		double red = android.graphics.Color.red(color) / 255.0; // MAGIC_NUMBER
		double green = android.graphics.Color.green(color) / 255.0; // MAGIC_NUMBER
		double blue = android.graphics.Color.blue(color) / 255.0; // MAGIC_NUMBER
		double colorTemp;
		if (blue == 0) {
			colorTemp = (green / red - 0.375) * 0.48; // MAGIC_NUMBER
		}
		else if (blue < green) {
			colorTemp = 0.3 * (1 + blue / green); // MAGIC_NUMBER
		}
		else {
			colorTemp = (1 - red / green) * 0.8 + 0.6; // MAGIC_NUMBER
		}
		double brightness = (hsv[2] - 1.0 / 16) * 16 / 15; // MAGIC_NUMBER
		if (brightness <= 0) {
			brightness = 1.0 / 65535; // MAGIC_NUMBER
		}
		return new Color(0.0, 0.0, brightness, progressBarToColorTemperature((int) (colorTemp * 120))); // MAGIC_NUMBER
	}

}
