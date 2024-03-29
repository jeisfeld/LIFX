package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.animation.AnimationData;
import de.jeisfeld.lifx.app.animation.LifxAnimationService;
import de.jeisfeld.lifx.app.animation.LifxAnimationService.AnimationStatus;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding data for the display view of a light.
 */
public class LightViewModel extends DeviceViewModel {
	/**
	 * The status of animation thread.
	 */
	protected final MutableLiveData<Boolean> mAnimationStatus; // SUPPRESS_CHECKSTYLE
	/**
	 * The stored Color of the device.
	 */
	private final MutableLiveData<Color> mColor;
	/**
	 * A storage to keep track on running setColor tasks.
	 */
	protected final List<AsyncExecutable> mRunningSetColorTasks = new ArrayList<>(); // SUPPRESS_CHECKSTYLE

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param light   The light.
	 */
	public LightViewModel(final Context context, final Light light) {
		super(context, light);
		mAnimationStatus = new MutableLiveData<>();
		mColor = new MutableLiveData<>();
		mAnimationStatus.setValue(LifxAnimationService.getAnimationStatus(light.getTargetAddress()) != AnimationStatus.OFF);
	}

	/**
	 * Get the light.
	 *
	 * @return The light.
	 */
	protected Light getLight() {
		return (Light) getDevice();
	}

	/**
	 * Get the animation status.
	 *
	 * @return The animation status.
	 */
	public LiveData<Boolean> getAnimationStatus() {
		return mAnimationStatus;
	}

	@Override
	public final LiveData<Color> getColor() {
		return mColor;
	}

	// OVERRIDABLE
	@Override
	protected boolean isRefreshColorsAllowed() {
		return super.isRefreshColorsAllowed() && !Boolean.TRUE.equals(mAnimationStatus.getValue());
	}

	// OVERRIDABLE
	@Override
	protected void refreshRemoteData(final boolean checkPower, final boolean checkColors) {
		super.refreshRemoteData(checkPower, checkColors);
		if (checkColors) {
			checkColor();
		}
	}

	/**
	 * Check the Color of the device.
	 */
	public void checkColor() {
		new CheckColorTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Set the hue, saturation, brightness and/or color temperature.
	 *
	 * @param hue the new hue. May be null to keep unchanged.
	 * @param saturation the new saturation. May be null to keep unchanged.
	 * @param brightness the new brightness. May be null to keep unchanged.
	 * @param colorTemperature the new color temperature. May be null to keep unchanged.
	 */
	public void updateColor(final Short hue, final Short saturation, final Short brightness, final Short colorTemperature) {
		Color color = mColor.getValue();
		if (color == null) {
			return;
		}
		Color newColor = new Color(hue == null ? color.getHue() : hue, saturation == null ? color.getSaturation() : saturation,
				brightness == null ? color.getBrightness() : brightness, colorTemperature == null ? color.getColorTemperature() : colorTemperature);
		updateColor(newColor, true);
	}

	@Override
	public final void updateBrightness(final double brightness) {
		updateSelectedBrightness(brightness);
		if (LifxAnimationService.getAnimationStatus(getLight().getTargetAddress()) != AnimationStatus.CUSTOM) {
			doUpdateBrightness(brightness);
		}
	}

	/**
	 * Update the brightness on the light.
	 *
	 * @param brightness The new brightness.
	 */
	protected void doUpdateBrightness(final double brightness) {
		updateColor(null, null, TypeUtil.toShort(brightness), null);
	}

	/**
	 * Update the stored selected brightness.
	 *
	 * @param brightness The brightness.
	 */
	// OVERRIDABLE
	public void updateSelectedBrightness(final double brightness) {
		Integer deviceId = (Integer) getLight().getParameter(DeviceRegistry.DEVICE_ID);
		if (deviceId != null) {
			PreferenceUtil.setIndexedSharedPreferenceDouble(R.string.key_device_selected_brightness, deviceId, brightness);
		}
	}

	/**
	 * Set the color.
	 *
	 * @param color the color to be set.
	 * @param isImmediate Flag indicating if the change should be immediate.
	 */
	public void updateColor(final Color color, final boolean isImmediate) {
		updateStoredColor(color);
		stopAnimationOrAlarm();
		synchronized (mRunningSetColorTasks) {
			mRunningSetColorTasks.add(new SetColorTask(this, color, isImmediate));
			if (mRunningSetColorTasks.size() > 2) {
				mRunningSetColorTasks.remove(1);
			}
			if (mRunningSetColorTasks.size() == 1) {
				mRunningSetColorTasks.get(0).execute();
			}
		}
	}

	/**
	 * Update the color in the model.
	 *
	 * @param color The color.
	 */
	public void updateStoredColor(final Color color) {
		mColor.postValue(color);
	}

	/**
	 * Start the animation.
	 *
	 * @param animationData Data for the animation.
	 */
	public void startAnimation(final AnimationData animationData) {
		Context context = getContext().get();
		if (context == null) {
			return;
		}
		mAnimationStatus.postValue(true);
		LifxAnimationService.triggerAnimationService(context, getLight(), animationData);
	}

	/**
	 * Stop the animation.
	 */
	protected void stopAnimation() {
		Context context = getContext().get();
		if (context == null) {
			return;
		}
		mAnimationStatus.postValue(false);
		LifxAnimationService.stopAnimationForMac(context, getLight().getTargetAddress());
	}

	/**
	 * Stop the animation or any alarm on this device.
	 */
	protected void stopAnimationOrAlarm() {
		mAnimationStatus.postValue(false);
		getLight().endAnimation(false);
	}

	/**
	 * An async task for checking the color.
	 */
	private static final class CheckColorTask extends AsyncTask<String, String, Color> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<LightViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		@SuppressWarnings("deprecation")
		private CheckColorTask(final LightViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Color doInBackground(final String... strings) {
			LightViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			return model.getLight().getColor();
		}

		@Override
		protected void onPostExecute(final Color color) {
			LightViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.updateStoredColor(color);
		}
	}

	/**
	 * An async task for setting the color.
	 */
	private static final class SetColorTask extends AsyncTask<Color, String, Color> implements AsyncExecutable {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<LightViewModel> mModel;
		/**
		 * The color to be set.
		 */
		private final Color mColor;
		/**
		 * Flag indicating if the change should be immediate.
		 */
		private final boolean mIsImmediate;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 * @param color The color.
		 * @param isImmediate Flag indicating if the change should be immediate.
		 */
		@SuppressWarnings("deprecation")
		private SetColorTask(final LightViewModel model, final Color color, final boolean isImmediate) {
			mModel = new WeakReference<>(model);
			mColor = color;
			mIsImmediate = isImmediate;
		}

		@Override
		protected Color doInBackground(final Color... colors) {
			LightViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			try {
				int colorDuration = mIsImmediate ? 0
						: PreferenceUtil.getSharedPreferenceIntString(
						R.string.key_pref_color_duration, R.string.pref_default_color_duration);
				if (model.mPower.getValue() != null && model.mPower.getValue().isOff() && isAutoOn()) {
					model.getLight().setColor(mColor, 0, false);
					model.getLight().setPower(true, colorDuration, false);
				}
				else {
					model.getLight().setColor(mColor, colorDuration, false);
				}
				return mColor;
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Color color) {
			LightViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			synchronized (model.mRunningSetColorTasks) {
				model.mRunningSetColorTasks.remove(this);
				if (model.mRunningSetColorTasks.size() > 0) {
					model.mRunningSetColorTasks.get(0).execute();
				}
			}
			model.updateStoredColor(color);
			if (isAutoOn()) {
				model.updatePowerButton(Power.ON);
			}
		}

		@Override
		public void execute() {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		}
	}
}
