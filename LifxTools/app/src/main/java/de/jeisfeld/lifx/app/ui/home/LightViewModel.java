package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.service.LifxAnimationService;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;

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
	protected final MutableLiveData<Color> mColor; // SUPPRESS_CHECKSTYLE
	/**
	 * A storage to keep track on running setColor tasks.
	 */
	private final List<SetColorTask> mRunningSetColorTasks = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param light The light.
	 */
	public LightViewModel(final Context context, final Light light) {
		super(context, light);
		mAnimationStatus = new MutableLiveData<>();
		mColor = new MutableLiveData<>();
		mAnimationStatus.setValue(LifxAnimationService.hasRunningAnimation(light.getTargetAddress()));
	}

	/**
	 * Get the light.
	 *
	 * @return The light.
	 */
	private Light getLight() {
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

	/**
	 * Get the color.
	 *
	 * @return The color.
	 */
	public LiveData<Color> getColor() {
		return mColor;
	}

	// OVERRIDABLE
	@Override
	protected boolean isRefreshAllowed() {
		return super.isRefreshAllowed() && !Boolean.TRUE.equals(mAnimationStatus.getValue());
	}

	@Override
	protected final void refreshRemoteData() {
		super.refreshRemoteData();
		checkColor();
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
		updateColor(newColor);
	}

	/**
	 * Set the color.
	 *
	 * @param color the color to be set.
	 */
	public void updateColor(final Color color) {
		mColor.postValue(color);

		synchronized (mRunningSetColorTasks) {
			mRunningSetColorTasks.add(new SetColorTask(this, color));
			if (mRunningSetColorTasks.size() > 2) {
				mRunningSetColorTasks.remove(1);
			}
			if (mRunningSetColorTasks.size() == 1) {
				mRunningSetColorTasks.get(0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, color);
			}
		}
	}

	/**
	 * Switch the animation on or off.
	 *
	 * @param status true to switch on, false to switch off.
	 */
	protected void updateAnimation(final boolean status) {
		Context context = getContext().get();
		if (context == null) {
			return;
		}
		mAnimationStatus.setValue(status);
		if (status) {
			Intent serviceIntent = new Intent(context, LifxAnimationService.class);
			serviceIntent.putExtra(LifxAnimationService.EXTRA_DEVICE_MAC, getLight().getTargetAddress());
			serviceIntent.putExtra(LifxAnimationService.EXTRA_DEVICE_LABEL, getLight().getLabel());
			ContextCompat.startForegroundService(context, serviceIntent);
			mPower.setValue(Power.ON);
		}
		else {
			LifxAnimationService.stopAnimationForMac(context, getLight().getTargetAddress());
		}
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
			model.mColor.postValue(color);
		}
	}

	/**
	 * An async task for setting the color.
	 */
	private static final class SetColorTask extends AsyncTask<Color, String, Color> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<LightViewModel> mModel;
		/**
		 * The color to be set.
		 */
		private final Color mColor;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 * @param color The color.
		 */
		private SetColorTask(final LightViewModel model, final Color color) {
			mModel = new WeakReference<>(model);
			mColor = color;
		}

		@Override
		protected Color doInBackground(final Color... colors) {
			LightViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			try {
				model.getLight().setColor(mColor);
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
					model.mRunningSetColorTasks.get(0).executeOnExecutor(THREAD_POOL_EXECUTOR);
				}
			}
			model.mColor.postValue(color);
		}
	}
}
