package de.jeisfeld.lifx.app.ui.home;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.service.LifxAnimationService;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.os.Logger;

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
	 * @param light   The light.
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
		return super.isRefreshAllowed() && !mAnimationStatus.getValue();
	}

	@Override
	protected void refreshRemoteData() {
		super.refreshRemoteData();
	}

	@Override
	protected void refreshLocalData() {
		boolean hasRunningAnimation = LifxAnimationService.hasRunningAnimation(getLight().getTargetAddress());
		if(hasRunningAnimation != mAnimationStatus.getValue()) {
			mAnimationStatus.postValue(hasRunningAnimation);
		}
	}
}
