package de.jeisfeld.lifx.app.ui.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.ui.home.HomeFragment.NoDeviceCallback;
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
class DeviceAdapter extends BaseAdapter {
	/**
	 * The IDs of seekbars within this view.
	 */
	private static final int[] SEEKBAR_IDS = {R.id.seekBarBrightness, R.id.seekBarHue, R.id.seekBarSaturation, R.id.seekBarColorTemperature};
	/**
	 * Divisor for transform from short to byte.
	 */
	private static final int SHORT_TO_BYTE_FACTOR = 256;

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
	public int getCount() {
		return mDevices.size();
	}

	@Override
	public Device getItem(final int position) {
		return mDevices.get(position);
	}

	@Override
	public long getItemId(final int position) {
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
	public synchronized View getView(final int position, final View convertView, final ViewGroup parent) {
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
			layoutId = R.layout.list_view_home_multizone;
		}
		else if (device instanceof Light) {
			layoutId = R.layout.list_view_home_light;
		}

		view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);

		final TextView text = view.findViewById(R.id.textViewHome);
		text.setText(device.getLabel());

		DeviceViewModel model = mViewModels.get(position);

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
				prepareColorPicker(view.findViewById(R.id.buttonColorPicker), lightModel);
				prepareHueButton(view, view.findViewById(R.id.buttonHue), view.findViewById(R.id.seekBarHue), lightModel);
				prepareSaturationButton(view, view.findViewById(R.id.buttonSaturation), view.findViewById(R.id.seekBarSaturation), lightModel);
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
	 * Prepare the color picker.
	 *
	 * @param colorPickerButton The color picker button.
	 * @param model The device view model.
	 */
	private void prepareColorPicker(final Button colorPickerButton, final LightViewModel model) {
		colorPickerButton.setVisibility(View.VISIBLE);

		colorPickerButton.setOnClickListener(v -> {
			ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(mContext)
					.setTitle("ColorPicker Dialog")
					.setPositiveButton("OK",
							(ColorEnvelopeListener) (envelope, fromUser) -> {
								// TODO
							})
					.setNegativeButton("Cancel",
							(dialogInterface, i) -> dialogInterface.dismiss())
					.attachAlphaSlideBar(true)
					.attachBrightnessSlideBar(true);
			final ColorPickerView colorPickerView = builder.getColorPickerView();
			colorPickerView.setPaletteDrawable(Objects.requireNonNull(mContext.getDrawable(R.drawable.ic_color_wheel)));
			colorPickerView.setColorListener((ColorListener) (color, fromUser) -> {
				if (fromUser) {
					float[] hsv = new float[3]; // MAGIC_NUMBER
					android.graphics.Color.colorToHSV(color, hsv);
					// Use alpha as color temperature
					short colorTemperature = progressBarToColorTemperature(android.graphics.Color.alpha(color) * 57 / 255); // MAGIC_NUMBER
					model.updateColor(new Color(hsv[0], hsv[1], hsv[2], colorTemperature), false);
				}
			});

			// setup data on start
			colorPickerView.getViewTreeObserver().addOnGlobalLayoutListener(
					() -> {
						Color color = model.getColor().getValue();
						if (color != null) {
							int xRadius = colorPickerView.getMeasuredWidth() / 2;
							int yRadius = colorPickerView.getMeasuredHeight() / 2;
							double hue01 = TypeUtil.toDouble(color.getHue());
							double saturation01 =
									color.getSaturation() == 0 ? 0 : 0.1 + 0.9 * TypeUtil.toDouble(color.getSaturation()); // MAGIC_NUMBER

							double x = saturation01 * Math.cos(2 * Math.PI * hue01);
							double y = saturation01 * Math.sin(2 * Math.PI * hue01);

							int pointX = (int) ((x + 1) * xRadius);
							int pointY = (int) ((1 - y) * yRadius);
							colorPickerView.moveSelectorPoint(pointX, pointY, getAndroidColor(color));

							BrightnessSlideBar brightnessSlideBar = colorPickerView.getBrightnessSlider();
							double realWidth = (brightnessSlideBar.getMeasuredWidth() - brightnessSlideBar.getMeasuredHeight())
									/ (double) brightnessSlideBar.getMeasuredWidth();
							brightnessSlideBar.setSelectorPosition(
									(float) (TypeUtil.toDouble(color.getBrightness()) * realWidth + (1 - realWidth) / 2));

							AlphaSlideBar alphaSlideBar = colorPickerView.getAlphaSlideBar();
							double relativeColorTemp = colorTemperatureToProgress(color.getColorTemperature()) / 57.0; // MAGIC_NUMBER
							alphaSlideBar.setSelectorPosition((float) (relativeColorTemp * realWidth + (1 - realWidth) / 2));
						}
					});
			builder.show();
		});
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
	 * Prepare the hue button and seekbar.
	 *
	 * @param listView The listView.
	 * @param button The hue button.
	 * @param seekBar The hue seekbar.
	 * @param model The light view model.
	 */
	private void prepareHueButton(final View listView, final Button button, final SeekBar seekBar, final LightViewModel model) {
		button.setVisibility(View.VISIBLE);

		model.getColor().observe(mLifeCycleOwner, color -> {
			if (color == null) {
				seekBar.setEnabled(false);
			}
			else {
				seekBar.setEnabled(true);
				seekBar.setProgress(TypeUtil.toUnsignedInt(color.getHue()));
			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				if (fromUser) {
					model.updateColor((short) progress, null, null, null, false);
				}
				seekBar.getThumb().setColorFilter(android.graphics.Color.HSVToColor(
						new float[] {(float) (TypeUtil.toDouble((short) progress) * 360), 1f, 1f}), Mode.MULTIPLY); // MAGIC_NUMBER
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				model.updateColor((short) seekBar.getProgress(), null, null, null, true);
			}
		});

		button.setOnClickListener(v -> {
			boolean isSeekbarVisible = seekBar.getVisibility() == View.VISIBLE;
			hideSeekBars(listView);
			seekBar.setVisibility(isSeekbarVisible ? View.GONE : View.VISIBLE);
		});
	}

	/**
	 * Prepare the saturation button and seekbar.
	 *
	 * @param listView The listView.
	 * @param button The saturation button.
	 * @param seekBar The saturation seekbar.
	 * @param model The light view model.
	 */
	private void prepareSaturationButton(final View listView, final Button button, final SeekBar seekBar, final LightViewModel model) {
		button.setVisibility(View.VISIBLE);

		model.getColor().observe(mLifeCycleOwner, color -> {
			if (color == null) {
				seekBar.setEnabled(false);
			}
			else {
				seekBar.setEnabled(true);
				seekBar.setProgress(TypeUtil.toUnsignedInt(color.getSaturation()));
			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				if (fromUser) {
					model.updateColor(null, (short) progress, null, null, false);
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				model.updateColor(null, (short) seekBar.getProgress(), null, null, false);
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
	 * Convert seekbar value to color temperature.
	 *
	 * @param progress The seekbar value
	 * @return The color temperature
	 */
	private static short progressBarToColorTemperature(final int progress) {
		double p = 38.3 + progress; // MAGIC_NUMBER
		return (short) Math.max(1500, Math.min(9000, p * p)); // MAGIC_NUMBER
	}

	/**
	 * Convert color temperature to seekbar value.
	 *
	 * @param colorTemperature The color temperature
	 * @return The seekbar value
	 */
	private static int colorTemperatureToProgress(final short colorTemperature) {
		int progress = (int) Math.round(Math.sqrt(TypeUtil.toUnsignedInt(colorTemperature)) - 38.3); // MAGIC_NUMBER
		return Math.max(0, Math.min(57, progress)); // MAGIC_NUMBER
	}

	/**
	 * Gonvert a color to Android format.
	 *
	 * @param color The Android color.
	 * @return The color as int.
	 */
	private static Integer getAndroidColor(final Color color) {
		Color.RGBK rgbk = color.toRgbk();
		return android.graphics.Color.rgb(
				TypeUtil.toUnsignedInt(rgbk.getRed()) / SHORT_TO_BYTE_FACTOR,
				TypeUtil.toUnsignedInt(rgbk.getGreen()) / SHORT_TO_BYTE_FACTOR,
				TypeUtil.toUnsignedInt(rgbk.getBlue()) / SHORT_TO_BYTE_FACTOR);
	}

}
