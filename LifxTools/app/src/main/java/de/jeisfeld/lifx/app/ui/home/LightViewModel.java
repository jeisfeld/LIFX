package de.jeisfeld.lifx.app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.lan.Light;

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
	 * @param light The light.
	 */
	public LightViewModel(final Light light) {
		super(light);
		mAnimationStatus = new MutableLiveData<>();
		mAnimationStatus.setValue(false);
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

}
