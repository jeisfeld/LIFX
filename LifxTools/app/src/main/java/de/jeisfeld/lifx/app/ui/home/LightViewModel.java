package de.jeisfeld.lifx.app.ui.home;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.service.LifxAnimationService;
import de.jeisfeld.lifx.lan.Light;
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
	 * Constructor.
	 *
	 * @param context the context.
	 * @param light The light.
	 */
	public LightViewModel(final Context context, final Light light) {
		super(context, light);
		mAnimationStatus = new MutableLiveData<>();
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

	// OVERRIDABLE
	@Override
	protected boolean isRefreshAllowed() {
		return super.isRefreshAllowed() && !Boolean.TRUE.equals(mAnimationStatus.getValue());
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
}
