package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding data for the display view of a device.
 */
public class DeviceViewModel extends MainViewModel {
	/**
	 * The device.
	 */
	private final Device mDevice;
	/**
	 * Brightness when switching off. Used for quick power.
	 */
	private double mBrightnessAtSwitchOff = 0;

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param device  The device.
	 */
	public DeviceViewModel(final Context context, final Device device) {
		super(context);
		mDevice = device;
	}

	/**
	 * Get the device.
	 *
	 * @return the device.
	 */
	protected Device getDevice() {
		return mDevice;
	}

	@Override
	public final void checkPower() {
		new CheckPowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected final void refresh(final boolean isHighPriority) {
		if (isRefreshPowerAllowed()) {
			if (mPower.getValue() == null) {
				new RefreshAfterCheckReachabilityTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else {
				refreshRemoteData(true, isHighPriority || isRefreshColorsAllowed());
			}
		}
	}

	@Override
	public final String getLabel() {
		return mDevice.getLabel();
	}

	/**
	 * Check if refresh of colors is allowed.
	 *
	 * @return true if refresh of colors is allowed.
	 */
	// OVERRIDABLE
	protected boolean isRefreshColorsAllowed() {
		return true;
	}

	/**
	 * Check if refresh of power is allowed.
	 *
	 * @return true if refresh of power is allowed.
	 */
	private boolean isRefreshPowerAllowed() {
		return PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_power_duration, R.string.pref_default_power_duration) > 0;
	}

	/**
	 * Refresh the data retrievable from the device.
	 *
	 * @param checkPower  Flag indicating if refresh includes check of power
	 * @param checkColors Flag indicating if refresh includes check of colors
	 */
	protected void refreshRemoteData(final boolean checkPower, final boolean checkColors) {
		if (checkPower) {
			checkPower();
		}
	}

	@Override
	public final void togglePower() {
		new TogglePowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Update the power in the model after setting power from group.
	 *
	 * @param power The power.
	 */
	protected void updatePowerFromGroup(final Power power) {
		if (mPower.getValue() != null && !mPower.getValue().isUndefined()) {
			updatePowerButton(power);
		}
	}

	/**
	 * An async task for checking the power.
	 */
	private static final class CheckPowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<DeviceViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		@SuppressWarnings("deprecation")
		private CheckPowerTask(final DeviceViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			return model.mDevice.getPower();
		}

		@Override
		protected void onPostExecute(final Power power) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.updatePowerButton(power);
		}
	}

	/**
	 * An async task for first checking reachability and in case device is reachable do refresh.
	 */
	private static final class RefreshAfterCheckReachabilityTask extends AsyncTask<String, String, Boolean> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<DeviceViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		@SuppressWarnings("deprecation")
		private RefreshAfterCheckReachabilityTask(final DeviceViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Boolean doInBackground(final String... strings) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return false;
			}
			return model.mDevice.isReachable();
		}

		@Override
		protected void onPostExecute(final Boolean isReachable) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			if (isReachable) {
				model.refreshRemoteData(true, model.isRefreshColorsAllowed());
			}
		}
	}

	/**
	 * An async task for toggling the power.
	 */
	private static final class TogglePowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<DeviceViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		@SuppressWarnings("deprecation")
		private TogglePowerTask(final DeviceViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			Power power = model.mPower.getValue();
			if (power == null) {
				power = model.mDevice.getPower();
				if (power == null) {
					return null;
				}
			}
			try {
				if (model instanceof LightViewModel) {
					boolean isMultizone = model instanceof MultizoneViewModel;
					boolean isTileChain = model instanceof TileViewModel;
					LightViewModel lightModel = (LightViewModel) model;
					int powerDuration = PreferenceUtil.getSharedPreferenceIntString(
							R.string.key_pref_power_duration, R.string.pref_default_power_duration);
					if (powerDuration == 0) {
						double brightness =
								lightModel.getColor().getValue() == null ? 0 : TypeUtil.toDouble(lightModel.getColor().getValue().getBrightness());
						if (isMultizone) {
							Double multizoneBrightness = ((MultizoneViewModel) model).getRelativeBrightness().getValue();
							if (multizoneBrightness != null) {
								brightness = multizoneBrightness;
							}
						}
						else if (isTileChain) {
							Double tileBrightness = ((TileViewModel) model).getRelativeBrightness().getValue();
							if (tileBrightness != null) {
								brightness = tileBrightness;
							}
						}
						if (power.isOff() && brightness > 0) {
							lightModel.getLight().setPower(true);
						}
						else if (brightness == 0) {
							lightModel.updateBrightness(model.mBrightnessAtSwitchOff);
						}
						else {
							model.mBrightnessAtSwitchOff = brightness;
							lightModel.updateBrightness((short) 0);
						}
					}
					else {
						lightModel.getLight().setPower(!power.isOn(), powerDuration, isMultizone);
						if (isTileChain || isMultizone && !power.isOn()) {
							if (power.isOn()) {
								// When switching off tile chain, after becomes OFF when power is 0
								try {
									Thread.sleep(powerDuration);
								}
								catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							model.refreshRemoteData(false, true);
						}
					}
				}
				else {
					model.mDevice.setPower(!power.isOn());
				}
				return power.isOn() ? Power.OFF : Power.ON;
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Power power) {
			DeviceViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.updatePowerButton(power);

			if (model instanceof MultizoneViewModel && ((MultizoneViewModel) model).getColors().getValue() == null) {
				((MultizoneViewModel) model).checkColor();
			}
		}
	}

}
