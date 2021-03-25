package de.jeisfeld.lifx.app.home;

import android.content.Context;

import java.lang.ref.WeakReference;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.type.Color;
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
	 * Check if light should be automatically switched on when selecting a stored color.
	 *
	 * @return True if light should be automatically switched on.
	 */
	protected static boolean isAutoOn() {
		return PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_auto_on, true);
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

	/**
	 * Update the brightness.
	 *
	 * @param brightness The new brightness.
	 */
	protected void updateBrightness(final double brightness) {
		// to be overridden in subclasses
	}

	/**
	 * Get the color.
	 *
	 * @return The color.
	 */
	public LiveData<Color> getColor() {
		return null; // to be overridden in subclasses
	}

	/**
	 * Update from a stored color.
	 *
	 * @param storedColor The stored color.
	 */
	protected void updateStoredColor(final StoredColor storedColor) {
		// to be overridden in subclasses
	}

	/**
	 * Interface for an async task.
	 */
	protected interface AsyncExecutable {
		/**
		 * Execute the task.
		 */
		void execute();
	}
}
