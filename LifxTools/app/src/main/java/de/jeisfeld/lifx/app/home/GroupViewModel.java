package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.core.text.HtmlCompat;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Group;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding data for the display view of a group.
 */
public class GroupViewModel extends MainViewModel {
	/**
	 * The group.
	 */
	private final Group mGroup;
	/**
	 * The groupId.
	 */
	private final int mGroupId;

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param group   The group.
	 * @param groupId The groupId.
	 */
	public GroupViewModel(final Context context, final Group group, final int groupId) {
		super(context);
		mGroup = group;
		mGroupId = groupId;
	}

	/**
	 * Get the group.
	 *
	 * @return the group.
	 */
	protected Group getGroup() {
		return mGroup;
	}

	@Override
	public final CharSequence getLabel() {
		return Html.fromHtml("<b>" + mGroup.getGroupLabel() + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	@Override
	public final void checkPower() {
		new CheckPowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public final void togglePower() {
		new TogglePowerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * An async task for checking the power.
	 */
	private static final class CheckPowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<GroupViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckPowerTask(final GroupViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			Power result = Power.UNDEFINED;
			for (Device device : DeviceRegistry.getInstance().getDevices(model.mGroupId, false)) {
				Power devicePower = device.getPower();
				if (devicePower != null) {
					if (devicePower.isOn()) {
						return Power.ON;
					}
					else if (devicePower.isOff()) {
						result = Power.OFF;
					}
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(final Power power) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mPower.postValue(power);
		}
	}

	/**
	 * An async task for toggling the power.
	 */
	private static final class TogglePowerTask extends AsyncTask<String, String, Power> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<GroupViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private TogglePowerTask(final GroupViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected Power doInBackground(final String... strings) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			Power power = model.mPower.getValue();
			if (power == null) {
				return null;
			}

			int powerDuration = PreferenceUtil.getSharedPreferenceIntString(
					R.string.key_pref_power_duration, R.string.pref_default_power_duration);
			for (Device device : DeviceRegistry.getInstance().getDevices(model.mGroupId, false)) {
				new Thread() {
					@Override
					public void run() {
						try {
							if (device instanceof Light) {
								((Light) device).setPower(!power.isOn(), powerDuration, false);
							}
							else {
								device.setPower(!power.isOn());
							}
						}
						catch (IOException e) {
							Log.w(Application.TAG, e);
						}
					}
				}.start();
			}
			return power.isOn() ? Power.OFF : Power.ON;
		}

		@Override
		protected void onPostExecute(final Power power) {
			GroupViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			model.mPower.postValue(power);
		}
	}
}
