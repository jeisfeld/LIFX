package de.jeisfeld.lifx.app.home;

import android.content.Context;

import java.lang.ref.WeakReference;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding data for the display view of a device or group.
 */
public abstract class MainViewModel extends ViewModel {
	/**
	 * The context.
	 */
	private final WeakReference<Context> mContext;
	/**
	 * The stored power of the device.
	 */
	protected final MutableLiveData<Power> mPower; // SUPPRESS_CHECKSTYLE
	/**
	 * The flag if the device is selected.
	 */
	protected final MutableLiveData<Boolean> mIsSelected; // SUPPRESS_CHECKSTYLE

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 */
	public MainViewModel(final Context context) {
		mContext = new WeakReference<>(context);
		mPower = new MutableLiveData<>();
		mIsSelected = new MutableLiveData<>();
		mIsSelected.setValue(false);
	}

	/**
	 * Get the context.
	 *
	 * @return the context.
	 */
	protected WeakReference<Context> getContext() {
		return mContext;
	}

	/**
	 * Get the last checked power of the device.
	 *
	 * @return The power.
	 */
	protected LiveData<Power> getPower() {
		return mPower;
	}

	/**
	 * Get the selected flag.
	 *
	 * @return The power.
	 */
	protected LiveData<Boolean> getIsSelected() {
		return mIsSelected;
	}

	/**
	 * Refresh the device. If offline, first check if online again.
	 */
	// OVERRIDABLE
	protected void refresh() {
	}

	/**
	 * Get the label of the device or group.
	 *
	 * @return The label.
	 */
	public abstract CharSequence getLabel();

	/**
	 * Check the power of the device.
	 */
	public abstract void checkPower();

	/**
	 * Toggle the power state.
	 */
	public abstract void togglePower();
}
