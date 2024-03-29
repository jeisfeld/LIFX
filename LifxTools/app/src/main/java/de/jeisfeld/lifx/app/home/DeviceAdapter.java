package de.jeisfeld.lifx.app.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
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

import com.skydoves.colorpickerview.ColorPickerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.animation.AnimationData;
import de.jeisfeld.lifx.app.animation.LifxAnimationService;
import de.jeisfeld.lifx.app.animation.MultizoneAnimationDialogFragment;
import de.jeisfeld.lifx.app.animation.MultizoneAnimationDialogFragment.MultizoneAnimationDialogListener;
import de.jeisfeld.lifx.app.animation.TileChainAnimationDialogFragment;
import de.jeisfeld.lifx.app.animation.TileChainAnimationDialogFragment.TileChainAnimationDialogListener;
import de.jeisfeld.lifx.app.home.HomeFragment.NoDeviceCallback;
import de.jeisfeld.lifx.app.home.MultizoneViewModel.FlaggedMultizoneColors;
import de.jeisfeld.lifx.app.managedevices.DeviceHolder;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry.DeviceUpdateCallback;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredAnimation;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoreColorType;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoredColorsDialogListener;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.view.ColorPickerDialog;
import de.jeisfeld.lifx.app.view.ColorPickerDialog.Builder;
import de.jeisfeld.lifx.app.view.MultiColorPickerDialogFragment;
import de.jeisfeld.lifx.app.view.MultiColorPickerDialogFragment.MultiColorPickerDialogListener;
import de.jeisfeld.lifx.app.view.PickedImageDialogFragment;
import de.jeisfeld.lifx.app.view.PickedImageDialogFragment.PickedImageDialogListener;
import de.jeisfeld.lifx.app.view.TuneTilesDialogFragment;
import de.jeisfeld.lifx.app.view.TuneTilesDialogFragment.TuneTilesDialogListener;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.MultizoneColors.Interpolated;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * An adapter for the list of devices in the home fragment.
 */
public class DeviceAdapter extends BaseAdapter {
	/**
	 * Request code used for picking an image.
	 */
	protected static final int REQUESTCODE_PICK_IMAGE = 1;

	/**
	 * The list of devices.
	 */
	private final List<DeviceHolder> mDevices;
	/**
	 * The list of view models.
	 */
	private final List<MainViewModel> mViewModels = new ArrayList<>();

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
	 * The current tile view model - used as temporary storage for callback handling.
	 */
	private TileViewModel mCurrentTileModel = null;

	/**
	 * Constructor.
	 *
	 * @param fragment The triggering fragment.
	 * @param callback A callback called in case of no devices.
	 */
	DeviceAdapter(final Fragment fragment, final NoDeviceCallback callback) {
		super();
		mDevices = DeviceRegistry.getInstance().getDevices(true);
		mContext = fragment.getContext();
		mFragment = new WeakReference<>(fragment);
		mLifeCycleOwner = fragment.getViewLifecycleOwner();
		mNoDeviceCallback = callback;
		for (DeviceHolder device : mDevices) {
			addViewModel(device);
		}

		DeviceRegistry.getInstance().update(new DeviceUpdateCallback() {
			@Override
			public void onDeviceUpdated(final Device device, final boolean isNew, final boolean isMissing) {
				if (device != null && isNew) {
					if (mDevices.size() == 0) {
						mNoDeviceCallback.onChange(true);
					}
					addDevice(new DeviceHolder(device, (int) device.getParameter(DeviceRegistry.DEVICE_ID), true));
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
	public final MainViewModel getItem(final int position) {
		return mViewModels.get(position);
	}

	@Override
	public final long getItemId(final int position) {
		return position;
	}

	/**
	 * Add the view model for a device or group.
	 *
	 * @param deviceHolder The device holder.
	 */
	private void addViewModel(final DeviceHolder deviceHolder) {
		if (deviceHolder.isGroup()) {
			mViewModels.add(new GroupViewModel(mContext, deviceHolder.getGroup(), deviceHolder.getId(), this));
		}
		else {
			Device device = deviceHolder.getDevice();
			if (device instanceof MultiZoneLight) {
				mViewModels.add(new MultizoneViewModel(mContext, (MultiZoneLight) device));
			}
			else if (device instanceof TileChain) {
				mViewModels.add(new TileViewModel(mContext, (TileChain) device));
			}
			else if (device instanceof Light) {
				mViewModels.add(new LightViewModel(mContext, (Light) device));
			}
			else {
				mViewModels.add(new DeviceViewModel(mContext, device));
			}
		}
	}

	/**
	 * Refresh view data for all devices.
	 *
	 * @param isHighPriority flag indicating if this is high priority refresh.
	 */
	protected void refresh(final boolean isHighPriority) {
		for (MainViewModel model : mViewModels) {
			model.refresh(isHighPriority);
		}
	}

	/**
	 * Refresh the color for all devices of a group after setting group color.
	 *
	 * @param groupId The group id.
	 * @param color   The color.
	 */
	protected void refreshGroupColor(final int groupId, final Color color) {
		for (MainViewModel model : mViewModels) {
			if (model instanceof LightViewModel) {
				Light light = ((LightViewModel) model).getLight();
				if (light.getParameter(DeviceRegistry.DEVICE_GROUP_ID) != null
						&& (int) light.getParameter(DeviceRegistry.DEVICE_GROUP_ID) == groupId) {
					try {
						((LightViewModel) model).updateStoredColor(color);
					}
					catch (Exception e) {
						// ignore NullPointerException in case of call before instantiation
					}
				}
			}
		}
	}

	/**
	 * Refresh the power for all devices of a group after setting group power.
	 *
	 * @param groupId The group id.
	 * @param power   The power.
	 */
	protected void refreshGroupPower(final int groupId, final Power power) {
		for (MainViewModel model : mViewModels) {
			if (model instanceof DeviceViewModel) {
				Device device = ((DeviceViewModel) model).getDevice();
				if (device.getParameter(DeviceRegistry.DEVICE_GROUP_ID) != null
						&& (int) device.getParameter(DeviceRegistry.DEVICE_GROUP_ID) == groupId) {
					try {
						((DeviceViewModel) model).updatePowerFromGroup(power);
					}
					catch (Exception e) {
						// ignore NullPointerException in case of call before instantiation
					}
				}
			}
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

		final MainViewModel model = getItem(position);

		view = LayoutInflater.from(mContext).inflate(R.layout.list_view_home, parent, false);

		final TextView text = view.findViewById(R.id.textViewHome);
		text.setText(model.getLabel());

		final CheckBox checkBoxSelectLight = view.findViewById(R.id.checkboxSelectLight);
		if (checkBoxSelectLight != null) {
			checkBoxSelectLight.setOnClickListener(v -> {
				model.mIsSelected.setValue(((CheckBox) v).isChecked());
				Fragment fragment = mFragment.get();
				if (fragment != null && model instanceof LightViewModel && ((CheckBox) v).isChecked()) {
					ColorPickerView colorPickerView = fragment.requireView().findViewById(R.id.colorPickerMain);
					if (colorPickerView != null) {
						ColorPickerDialog.updateColorPickerFromLight(colorPickerView, model.getColor().getValue());
					}
					ColorPickerView brightnessColorTempPickerView =
							fragment.requireView().findViewById(R.id.colorPickerBrightnessColorTemp);
					if (brightnessColorTempPickerView != null) {
						ColorPickerDialog.updateBrightnessColorTempFromLight(brightnessColorTempPickerView, model.getColor().getValue());
					}
				}
			});
		}

		preparePowerButton(view.findViewById(R.id.buttonPower), model);

		synchronized (mViews) {
			if (mViews.size() > position) {
				try {
					mViews.set(position, view);
				}
				catch (ArrayIndexOutOfBoundsException e) {
					mViews.add(position, view);
				}
			}
			else {
				mViews.add(position, view);
			}
		}

		model.checkPower();

		if (model instanceof GroupViewModel) {
			view.findViewById(R.id.toggleButtonAnimation).setVisibility(View.GONE);
			GroupViewModel groupModel = (GroupViewModel) model;

			Button buttonColorPicker = view.findViewById(R.id.buttonColorPicker);
			if (buttonColorPicker != null) {
				prepareColorPicker(buttonColorPicker, groupModel);
			}
			Button buttonBrightnessColorTemp = view.findViewById(R.id.buttonBrightnessColortemp);
			if (buttonBrightnessColorTemp != null) {
				prepareBrightnessColortempPicker(buttonBrightnessColorTemp, groupModel);
			}
			Button saveButton = view.findViewById(R.id.buttonSave);
			if (saveButton != null) {
				prepareSaveButton(saveButton, model);
			}

			prepareBrightnessSeekbar(view.findViewById(R.id.seekBarBrightness), model);
			groupModel.checkColor();
		}
		if (model instanceof LightViewModel) {
			LightViewModel lightModel = (LightViewModel) model;

			if (lightModel.getDevice().getProduct().hasColor()) {
				Button buttonColorPicker = view.findViewById(R.id.buttonColorPicker);
				if (buttonColorPicker != null) {
					prepareColorPicker(buttonColorPicker, model);
				}
			}
			Button buttonBrightnessColorTemp = view.findViewById(R.id.buttonBrightnessColortemp);
			if (buttonBrightnessColorTemp != null) {
				prepareBrightnessColortempPicker(buttonBrightnessColorTemp, model);
			}
			Button saveButton = view.findViewById(R.id.buttonSave);
			if (saveButton != null) {
				prepareSaveButton(saveButton, model);
			}

			prepareBrightnessSeekbar(view.findViewById(R.id.seekBarBrightness), model);
			prepareAnimationButton(view.findViewById(R.id.toggleButtonAnimation), lightModel);

			lightModel.checkColor();
		}
		if (model instanceof MultizoneViewModel) {
			MultizoneViewModel lightModel = (MultizoneViewModel) model;
			Button buttonMultiColorPicker = view.findViewById(R.id.buttonMultiColorPicker);
			if (buttonMultiColorPicker != null) {
				prepareMultiColorPicker(buttonMultiColorPicker, lightModel);
			}
		}
		if (model instanceof TileViewModel) {
			TileViewModel lightModel = (TileViewModel) model;
			Button imageButton = view.findViewById(R.id.buttonImage);
			if (imageButton != null) {
				prepareImageButton(imageButton, lightModel, position);
			}
			Button tuneButton = view.findViewById(R.id.buttonTune);
			if (tuneButton != null) {
				prepareTuneButton(tuneButton, lightModel, position);
			}
		}

		return view;
	}

	/**
	 * Prepare the power button.
	 *
	 * @param powerButton The power button.
	 * @param model       The device view model.
	 */
	private void preparePowerButton(final Button powerButton, final MainViewModel model) {
		model.mPower.observe(mLifeCycleOwner, power -> {
			if (power == null) {
				powerButton.setBackground(ContextCompat.getDrawable(mContext, R.drawable.powerbutton_offline));
			}
			else if (power.isOff()) {
				powerButton.setBackground(ContextCompat.getDrawable(mContext, R.drawable.powerbutton_off));
			}
			else if (power.isOn()) {
				powerButton.setBackground(ContextCompat.getDrawable(mContext, R.drawable.powerbutton_on));
			}
			// do not update power button if undefined
		});

		powerButton.setOnClickListener(v -> model.togglePower());
	}

	/**
	 * Prepare the color picker started via button.
	 *
	 * @param colorPickerButton The color picker button.
	 * @param model             The light view model.
	 */
	private void prepareColorPicker(final Button colorPickerButton, final MainViewModel model) {
		colorPickerButton.setVisibility(View.VISIBLE);

		colorPickerButton.setOnClickListener(v -> new Builder(mContext, R.layout.dialog_colorpicker)
				.initializeFromLight(model)
				.setColorListener((color, fromUser) -> {
					if (fromUser) {
						// Use alpha as color temperature
						short colorTemperature = progressBarToColorTemperature(android.graphics.Color.alpha(color) * 120 / 255); // MAGIC_NUMBER
						updateColor(model, ColorUtil.convertAndroidColorToColor(color, colorTemperature, false));
					}
				}).show());
	}

	/**
	 * Prepare the picker for brightness and color temperature started via button.
	 *
	 * @param brightnessColorTempButton The brightness/colortemp picker button.
	 * @param model                     The group view model.
	 */
	private void prepareBrightnessColortempPicker(final Button brightnessColorTempButton, final MainViewModel model) {
		brightnessColorTempButton.setVisibility(View.VISIBLE);

		brightnessColorTempButton.setOnClickListener(v -> new Builder(mContext, R.layout.dialog_brightness_colortemp)
				.initializeFromBrightnessColorTemp(model)
				.setColorListener((color, fromUser) -> {
					if (fromUser) {
						updateColor(model, convertBrightnessColorTempPickerColor(color));
					}
				}).show());
	}

	/**
	 * Update the color via a model.
	 *
	 * @param model The model.
	 * @param color The color.
	 */
	private void updateColor(final MainViewModel model, final Color color) {
		if (model instanceof LightViewModel) {
			((LightViewModel) model).updateColor(color, true);
		}
		else if (model instanceof GroupViewModel) {
			((GroupViewModel) model).updateColor(color);
		}
	}

	/**
	 * Prepare the multizone color picker started via button.
	 *
	 * @param multiColorPickerButton The multizone color picker button.
	 * @param model                  The multizone view model.
	 */
	private void prepareMultiColorPicker(final Button multiColorPickerButton, final MultizoneViewModel model) {
		multiColorPickerButton.setVisibility(View.VISIBLE);

		multiColorPickerButton.setOnClickListener(v -> {
			final int zoneCount = model.getLight().getZoneCount();
			if (zoneCount == 0) {
				return;
			}
			Fragment fragment = mFragment.get();
			FragmentActivity activity = fragment == null ? null : fragment.getActivity();
			if (activity == null) {
				return;
			}
			ArrayList<Color> initialColors = new ArrayList<>();

			final MultizoneColors colors = model.getColors().getValue();
			final double relativeBrightness = model.getRelativeBrightness().getValue() == null ? 1 : model.getRelativeBrightness().getValue();
			if (colors != null) {
				for (int index = 0; index < MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT; index++) {
					final int zone = (int) Math.round(index * (zoneCount - 1) / (MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT - 1.0));
					if (colors instanceof FlaggedMultizoneColors && ((FlaggedMultizoneColors) colors).getInterpolationColors() != null) {
						if (((FlaggedMultizoneColors) colors).getFlags()[index]) {
							Color interpolationColor = ((FlaggedMultizoneColors) colors).getInterpolationColor(index);
							if (interpolationColor != null) {
								initialColors.add(interpolationColor.withRelativeBrightness(relativeBrightness));
							}
						}
						else {
							initialColors.add(null);
						}
					}
					else {
						initialColors.add(colors.getColor(zone, zoneCount).withRelativeBrightness(relativeBrightness));
					}
				}
			}

			boolean isCyclic = false;
			if (colors instanceof FlaggedMultizoneColors) {
				boolean[] flags = ((FlaggedMultizoneColors) colors).getFlags();
				if (flags.length > MultiColorPickerDialogFragment.CYCLIC_FLAG_INDEX) {
					isCyclic = flags[MultiColorPickerDialogFragment.CYCLIC_FLAG_INDEX];
				}
			}

			MultiColorPickerDialogFragment.displayMultiColorPickerDialog(activity, initialColors, isCyclic, new MultiColorPickerDialogListener() {
				@Override
				public void onColorUpdate(final ArrayList<Color> colors, final boolean isCyclic, final boolean[] flags) {
					model.updateColors(new FlaggedMultizoneColors(new Interpolated(isCyclic, colors), flags), 1, true, true);
				}

				@Override
				public void onDialogPositiveClick(final DialogFragment dialog, final ArrayList<Color> colors, final boolean isCyclic,
												  final boolean[] flags) {
					model.updateColors(new FlaggedMultizoneColors(new Interpolated(isCyclic, colors), flags), 1, true, true);
				}

				@Override
				public void onDialogNegativeClick(final DialogFragment dialog) {
					model.updateColors(colors, relativeBrightness, true, false);
				}
			});
		});
	}

	/**
	 * Load a tile image via button.
	 *
	 * @param imageButton The image button.
	 * @param model       The tile view model.
	 * @param position    The position where the button was clicked.
	 */
	private void prepareImageButton(final Button imageButton, final TileViewModel model, final int position) {
		imageButton.setVisibility(View.VISIBLE);

		imageButton.setOnClickListener(v -> {
			Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			Fragment fragment = mFragment.get();
			if (fragment == null) {
				return;
			}
			mCurrentTileModel = model;
			fragment.startActivityForResult(galleryIntent, REQUESTCODE_PICK_IMAGE);
		});
	}

	/**
	 * Handle the picked bitmap.
	 *
	 * @param bitmap The bitmap.
	 */
	protected void handleBitmap(final Bitmap bitmap) {
		TileViewModel model = mCurrentTileModel;
		mCurrentTileModel = null;
		Fragment fragment = mFragment.get();
		FragmentActivity activity = fragment == null ? null : fragment.getActivity();
		if (activity == null) {
			return;
		}

		if (model.getLight().getTileInfo() == null) {
			Log.w(Application.TAG, "Missing tile info");
			return;
		}

		final TileChainColors oldColors = model.getColors().getValue();
		final Double oldBrightness = model.getRelativeBrightness().getValue();

		PickedImageDialogFragment.displayPickedImageDialog(activity, model, bitmap, new PickedImageDialogListener() {
			@Override
			public void onImageUpdate(final TileChainColors colors) {
				model.updateColors(colors, 1, true, true);
			}

			@Override
			public void onDialogPositiveClick(final DialogFragment dialog, final TileChainColors colors) {
				// Convert anonymous colors to PerTile, so that it is storable.
				model.updateColors(new TileChainColors.PerTile(model.getLight(), colors), 1, true, true);
			}

			@Override
			public void onDialogNegativeClick(final DialogFragment dialog) {
				if (oldColors != null) {
					model.updateColors(oldColors, oldBrightness == null ? 1 : oldBrightness, true, true);
				}
			}
		});
	}

	/**
	 * Tune brightness, contrast, saturation, hue.
	 *
	 * @param tuneButton The tune button.
	 * @param model      The tile view model.
	 * @param position   The position where the button was clicked.
	 */
	private void prepareTuneButton(final Button tuneButton, final TileViewModel model, final int position) {
		tuneButton.setVisibility(View.VISIBLE);

		tuneButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			FragmentActivity activity = fragment == null ? null : fragment.getActivity();
			if (activity != null) {
				TuneTilesDialogFragment.displayPickedImageDialog(activity, model, new TuneTilesDialogListener() {
					@Override
					public void onImageUpdate(final TileChainColors colors) {
						model.updateColors(colors, 1, true, true);
					}

					@Override
					public void onDialogPositiveClick(final DialogFragment dialog, final TileChainColors colors) {
						// Convert anonymous colors to PerTile, so that it is storable.
						model.updateColors(new TileChainColors.PerTile(model.getLight(), colors), 1, true, true);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog, final TileChainColors initialColors) {
						model.updateColors(initialColors, 1, true, true);
					}
				});
			}
		});
	}

	/**
	 * Prepare the brightness button and seekbar.
	 *
	 * @param seekBar The brightness seekbar.
	 * @param model   The light view model.
	 */
	private void prepareBrightnessSeekbar(final SeekBar seekBar, final MainViewModel model) {
		if (model instanceof MultizoneViewModel) {
			((MultizoneViewModel) model).getRelativeBrightness().observe(mLifeCycleOwner, relativeBrightness -> {
				if (relativeBrightness != null) {
					seekBar.setProgress(TypeUtil.toUnsignedInt(TypeUtil.toShort(relativeBrightness)));
				}
			});
		}
		else if (model instanceof TileViewModel) {
			((TileViewModel) model).getRelativeBrightness().observe(mLifeCycleOwner, relativeBrightness -> {
				if (relativeBrightness != null) {
					seekBar.setProgress(TypeUtil.toUnsignedInt(TypeUtil.toShort(relativeBrightness)));
				}
			});
		}
		else if (model instanceof LightViewModel) {
			model.getColor().observe(mLifeCycleOwner, color -> {
				if (color != null) {
					seekBar.setProgress(TypeUtil.toUnsignedInt(color.getBrightness()));
				}
			});
		}
		else if (model instanceof GroupViewModel) {
			model.getColor().observe(mLifeCycleOwner, color -> {
				if (color != null) {
					seekBar.setProgress(TypeUtil.toUnsignedInt(color.getBrightness()));
				}
			});
		}

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				double brightness = TypeUtil.toDouble((short) (progress + 1));
				if (fromUser) {
					model.updateBrightness(brightness);
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				model.updateBrightness(TypeUtil.toDouble((short) (seekBar.getProgress() + 1)));
			}
		});
	}

	/**
	 * Prepare the animation button.
	 *
	 * @param animationButton The animation button.
	 * @param model           The light view model.
	 */
	private void prepareAnimationButton(final ToggleButton animationButton, final LightViewModel model) {
		model.getAnimationStatus().observe(mLifeCycleOwner, animationButton::setChecked);

		if (model instanceof MultizoneViewModel) {
			animationButton.setOnClickListener(view -> {
				if (animationButton.isChecked()) {
					if (model.mPower.getValue() == null || !model.mPower.getValue().isOn()) {
						animationButton.setChecked(false);
						return;
					}
					Fragment fragment = mFragment.get();
					if (fragment != null && fragment.getActivity() != null) {
						MultizoneAnimationDialogFragment.displayMultizoneAnimationDialog(fragment.getActivity(), (MultizoneViewModel) model,
								new MultizoneAnimationDialogListener() {
									@Override
									public void onDialogPositiveClick(final DialogFragment dialog, final AnimationData animationData) {
										model.startAnimation(animationData);
									}

									@Override
									public void onDialogNegativeClick(final DialogFragment dialog) {
										animationButton.setChecked(false);
									}
								});
					}
				}
				else {
					model.stopAnimation();
				}
			});
		}
		else if (model instanceof TileViewModel) {
			animationButton.setOnClickListener(view -> {
				if (animationButton.isChecked()) {
					if (model.mPower.getValue() == null || !model.mPower.getValue().isOn()) {
						animationButton.setChecked(false);
						return;
					}
					Fragment fragment = mFragment.get();
					if (fragment != null && fragment.getActivity() != null) {
						TileChainAnimationDialogFragment.displayTileChainAnimationDialog(fragment.getActivity(), (TileViewModel) model,
								new TileChainAnimationDialogListener() {
									@Override
									public void onDialogPositiveClick(final DialogFragment dialog, final AnimationData animationData) {
										model.startAnimation(animationData);
									}

									@Override
									public void onDialogNegativeClick(final DialogFragment dialog) {
										animationButton.setChecked(false);
									}
								});
					}
				}
				else {
					model.stopAnimation();
				}
			});
		}
		else {
			animationButton.setVisibility(View.GONE);
		}

	}

	/**
	 * Prepare the save button.
	 *
	 * @param saveButton The save button.
	 * @param model      The light view model.
	 */
	private void prepareSaveButton(final Button saveButton, final MainViewModel model) {
		saveButton.setVisibility(View.VISIBLE);

		saveButton.setOnClickListener(v -> {
			final Color color = model.getColor().getValue();
			final MultizoneColors multizoneColors;
			if (model instanceof MultizoneViewModel) {
				multizoneColors = ((MultizoneViewModel) model).getColorsWithBrightness();
			}
			else {
				multizoneColors = null;
			}
			final TileChainColors tileChainColors;
			if (model instanceof TileViewModel) {
				tileChainColors = ((TileViewModel) model).getColorsWithBrightness();
			}
			else {
				tileChainColors = null;
			}
			final boolean isAnimationRunning;
			if (model instanceof LightViewModel) {
				isAnimationRunning = Boolean.TRUE.equals(((LightViewModel) model).getAnimationStatus().getValue());
			}
			else {
				isAnimationRunning = false;
			}

			final int deviceId;
			final String mac;
			if (model instanceof LightViewModel) {
				final Light light = ((LightViewModel) model).getLight();
				if (light == null || light.getParameter(DeviceRegistry.DEVICE_ID) == null) {
					return;
				}
				deviceId = (int) light.getParameter(DeviceRegistry.DEVICE_ID);
				mac = light.getTargetAddress();
			}
			else if (model instanceof GroupViewModel) {
				deviceId = ((GroupViewModel) model).getGroupId();
				mac = null;
			}
			else {
				return;
			}

			final Fragment fragment = mFragment.get();
			if (fragment == null) {
				return;
			}
			FragmentActivity activity = fragment.getActivity();
			if (activity == null) {
				return;
			}

			final StoreColorType storeColorType;
			if (isAnimationRunning) {
				storeColorType = StoreColorType.ANIMATION;
			}
			else if (color == null && multizoneColors == null && tileChainColors == null) {
				storeColorType = StoreColorType.ONLYSELECT;
			}
			else {
				storeColorType = StoreColorType.COLOR;
			}

			StoredColorsDialogFragment.displayStoredColorsDialog(activity, deviceId, storeColorType, false, new StoredColorsDialogListener() {
				/**
				 * Flag indicating if color was changed during this dialog.
				 */
				private boolean mIsColorChanged = false;

				@Override
				public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
					if (text != null && !text.trim().isEmpty()) {
						String name = text.trim();
						StoredColor storedColor;
						if (isAnimationRunning) {
							AnimationData animationData = LifxAnimationService.getAnimationData(mac);
							double relativeBrightness = AnimationData.getSelectedBrightness(((LightViewModel) model).getLight());
							storedColor = new StoredAnimation(animationData, relativeBrightness, deviceId, name);
						}
						else if (multizoneColors != null) {
							storedColor = new StoredMultizoneColors(multizoneColors, deviceId, name);
						}
						else if (tileChainColors != null) {
							storedColor = new StoredTileColors(tileChainColors, deviceId, name);
						}
						else {
							storedColor = new StoredColor(color, deviceId, name);
						}
						ColorRegistry.getInstance().addOrUpdate(storedColor);
						DialogUtil.displayToast(fragment.getContext(), R.string.toast_saved_color, name);
					}
					else {
						if (!mIsColorChanged) {
							DialogUtil.displayConfirmationMessage(fragment.requireActivity(),
									R.string.title_did_not_save_empty_name, R.string.message_did_not_save_empty_name);
						}
					}
				}

				@Override
				public void onStoredColorClick(final DialogFragment dialog, final StoredColor storedColor) {
					storedColor.apply(mContext, model);
					mIsColorChanged = true;
				}
			});
		});
	}

	/**
	 * Add a device to the list.
	 *
	 * @param device The device to be added.
	 */
	public void addDevice(final DeviceHolder device) {
		mDevices.add(device);
		addViewModel(device);
		notifyDataSetChanged();
	}

	/**
	 * Get the view model from a MAC (if available).
	 *
	 * @param mac The MAC
	 * @return the view model
	 */
	protected MainViewModel getViewModel(final String mac) {
		for (int i = 0; i < mDevices.size(); i++) {
			DeviceHolder holder = mDevices.get(i);
			if (!holder.isGroup() && mac.equals(holder.getDevice().getTargetAddress())) {
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
	protected List<MainViewModel> getCheckedDevices() {
		List<MainViewModel> checkedDevices = new ArrayList<>();
		for (MainViewModel model : mViewModels) {
			if (Boolean.TRUE.equals(model.getIsSelected().getValue())) {
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
	public static short progressBarToColorTemperature(final int progress) {
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
