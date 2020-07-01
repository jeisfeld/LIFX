package de.jeisfeld.lifx.app.alarms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.LightSteps;
import de.jeisfeld.lifx.app.alarms.Alarm.RingtoneStep;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.RequestDurationDialogFragment.RequestDurationDialogListener;
import de.jeisfeld.lifx.lan.Light;

/**
 * Adapter for the expandable list of alarm steps.
 */
public class AlarmStepExpandableListAdapter extends BaseExpandableListAdapter {
	/**
	 * The triggering activity.
	 */
	private final AlarmConfigurationFragment mFragment;
	/**
	 * The alarm.
	 */
	private Alarm mAlarm;
	/**
	 * The list of lightSteps.
	 */
	private List<LightSteps> mLightStepsList;
	/**
	 * The initial expanding status.
	 */
	private final Map<Light, Boolean> mInitialExpandingStatus;
	/**
	 * Reference to the parent view.
	 */
	private WeakReference<ExpandableListView> mParent = new WeakReference<>(null);
	/**
	 * Number of seconds per minute.
	 */
	private static final int SECONDS_PER_MINUTE = (int) TimeUnit.MINUTES.toSeconds(1);

	/**
	 * Constructor.
	 *
	 * @param fragment               The triggering fragment
	 * @param alarm                  The alarm (if already saved)
	 * @param initialExpandingStatus The initial expanding status
	 */
	protected AlarmStepExpandableListAdapter(final AlarmConfigurationFragment fragment, final Alarm alarm,
											 final Map<Light, Boolean> initialExpandingStatus) {
		mFragment = fragment;
		mAlarm = alarm;
		mLightStepsList = alarm.getLightSteps();
		mInitialExpandingStatus = initialExpandingStatus;
	}

	/**
	 * Get the expanding status of the lights.
	 *
	 * @return The expanding status of the lights.
	 */
	protected Map<Light, Boolean> getExpandingStatus() {
		ExpandableListView parent = mParent.get();
		Map<Light, Boolean> result = new HashMap<>();
		if (parent != null) {
			for (int groupPosition = 0; groupPosition < getGroupCount(); groupPosition++) {
				LightSteps lightSteps = getGroup(groupPosition);
				result.put(lightSteps.getLight(), parent.isGroupExpanded(groupPosition));
			}
		}
		return result;
	}

	@Override
	public final int getGroupCount() {
		return mLightStepsList.size();
	}

	@Override
	public final int getChildrenCount(final int groupPosition) {
		return getGroup(groupPosition).getSteps().size();
	}

	@Override
	public final LightSteps getGroup(final int groupPosition) {
		return mLightStepsList.get(groupPosition);
	}

	@Override
	public final Step getChild(final int groupPosition, final int childPosition) {
		return getGroup(groupPosition).getSteps().get(childPosition);
	}

	@Override
	public final long getGroupId(final int groupPosition) {
		return groupPosition;
	}

	@Override
	public final long getChildId(final int groupPosition, final int childPosition) {
		return childPosition;
	}

	@Override
	public final boolean hasStableIds() {
		return false;
	}

	@Override
	public final View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent) {
		mParent = new WeakReference<>((ExpandableListView) parent);
		LightSteps lightSteps = getGroup(groupPosition);
		View view = convertView;
		if (convertView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) mFragment.requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			assert layoutInflater != null;
			view = layoutInflater.inflate(R.layout.list_view_alarm_lights, parent, false);
		}

		TextView listTitleTextView = (TextView) view.findViewById(R.id.textViewDeviceName);
		listTitleTextView.setText(lightSteps.getLight().getLabel());

		boolean isCollapsed = !isExpanded;

		// workaround for maintaining expending status after reordering lights due to adding light
		Boolean initialExpandingStatus = mInitialExpandingStatus.get(lightSteps.getLight());
		if (initialExpandingStatus != null) {
			if (initialExpandingStatus) {
				((ExpandableListView) parent).expandGroup(groupPosition);
				isCollapsed = false;
			}
			else {
				((ExpandableListView) parent).collapseGroup(groupPosition);
				isCollapsed = true;
			}
			mInitialExpandingStatus.remove(lightSteps.getLight());
		}

		TextView textViewStartTime = view.findViewById(R.id.textViewStartTime);
		TextView textViewEndTime = view.findViewById(R.id.textViewEndTime);
		ImageView imageViewAddStep = view.findViewById(R.id.imageViewAddAlarmStep);

		long minDelay = Long.MAX_VALUE;
		long maxEndTime = Long.MIN_VALUE;
		long maxStartTime = Long.MIN_VALUE;
		for (Step step : lightSteps.getSteps()) {
			minDelay = Math.min(minDelay, step.getDelay());
			maxStartTime = Math.max(maxStartTime, step.getDelay());
			maxEndTime = Math.max(maxEndTime, step.getDelay() + step.getDuration());
		}

		if (isCollapsed) {
			textViewStartTime.setText(getDelayString(minDelay));
			textViewStartTime.setVisibility(View.VISIBLE);
			textViewEndTime.setText(getDelayString(maxEndTime));
			textViewEndTime.setVisibility(View.VISIBLE);
			imageViewAddStep.setVisibility(View.GONE);
		}
		else {
			textViewStartTime.setVisibility(View.GONE);
			textViewEndTime.setVisibility(View.GONE);
			imageViewAddStep.setVisibility(View.VISIBLE);
			final long newStartTime = maxStartTime == maxEndTime ? maxEndTime + TimeUnit.SECONDS.toMillis(1) : maxEndTime;
			imageViewAddStep.setOnClickListener(v -> {
				Light light = getGroup(groupPosition).getLight();
				if (RingtoneStep.RINGTONE_DUMMY_LIGHT.equals(light)) {
					mFragment.startRingtoneDialog(newStartTime, null);
				}
				else {
					StoredColorsDialogFragment.displayStoredColorsDialog(
							mFragment.requireActivity(), (int) light.getParameter(DeviceRegistry.DEVICE_ID), true,
							storedColor -> {
								mAlarm.getSteps().add(new Step((int) newStartTime, storedColor.getId(), AlarmConfigurationFragment.DEFAULT_DURATION));
								mAlarm = AlarmRegistry.getInstance().addOrUpdate(mAlarm);
								notifyDataSetChanged();
							});
				}
			});
		}

		return view;
	}

	@Override
	public final void notifyDataSetChanged() {
		mLightStepsList = mAlarm.getLightSteps();
		super.notifyDataSetChanged();
	}

	/**
	 * Notify on changed alarm steps.
	 *
	 * @param alarm The new alarm.
	 */
	protected void notifyDataSetChanged(final Alarm alarm) {
		List<Light> newLights = alarm.getLightSteps().stream().map(LightSteps::getLight).collect(Collectors.toList());
		newLights.removeAll(mLightStepsList.stream().map(LightSteps::getLight).collect(Collectors.toList()));
		for (Light light : newLights) {
			mInitialExpandingStatus.put(light, true);
		}
		mAlarm = alarm;
		notifyDataSetChanged();
	}

	@Override
	public final View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView,
								   final ViewGroup parent) {
		final Step originalStep = getChild(groupPosition, childPosition);
		View view = convertView;
		if (convertView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) mFragment.requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			assert layoutInflater != null;
			view = layoutInflater.inflate(R.layout.list_view_alarm_steps, parent, false);
		}

		final TextView textViewStartTime = view.findViewById(R.id.textViewStartTime);
		final TextView textViewEndTime = view.findViewById(R.id.textViewEndTime);
		textViewStartTime.setText(getDelayString(originalStep.getDelay()));
		textViewEndTime.setText(getDelayString(originalStep.getDelay() + originalStep.getDuration()));

		final ImageView imageViewStoredColor = view.findViewById(R.id.imageViewStoredColor);
		final TextView textViewStoredColorName = view.findViewById(R.id.textViewStoredColorName);
		imageViewStoredColor.setImageDrawable(StoredColorsViewAdapter.getButtonDrawable(mFragment.requireContext(), originalStep.getStoredColor()));
		textViewStoredColorName.setText(originalStep.getStoredColor().getName());

		OnClickListener changeColorListener = v -> {
			final Step step = getChild(groupPosition, childPosition);

			if (step instanceof RingtoneStep) {
				mFragment.startRingtoneDialog(step.getDelay(), (RingtoneStep) step);
			}
			else {
				StoredColorsDialogFragment.displayStoredColorsDialog(
						mFragment.requireActivity(), step.getStoredColor().getDeviceId(), true,
						storedColor -> {
							Step newStep = new Step(step.getId(), step.getDelay(), storedColor.getId(), step.getDuration());
							mAlarm.getSteps().remove(step);
							mAlarm.getSteps().add(newStep);
							AlarmRegistry.getInstance().addOrUpdate(mAlarm);
							notifyDataSetChanged();
						});
			}
		};
		imageViewStoredColor.setOnClickListener(changeColorListener);
		textViewStoredColorName.setOnClickListener(changeColorListener);

		textViewStartTime.setOnClickListener(v -> {
			final Step step = getChild(groupPosition, childPosition);
			int delaySeconds = (int) (step.getDelay() / TimeUnit.SECONDS.toMillis(1));

			DialogUtil.displayDurationDialog(mFragment.requireActivity(), new RequestDurationDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog, final int minutes, final int seconds) {
							Step newStep = step.withDelay(TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds));
							mAlarm.updateDelay(newStep);
							AlarmRegistry.getInstance().addOrUpdate(mAlarm);
							notifyDataSetChanged();
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {

						}
					}, R.string.title_dialog_alarm_step_delay, R.string.button_ok, delaySeconds / SECONDS_PER_MINUTE,
					delaySeconds % SECONDS_PER_MINUTE, R.string.message_dialog_alarm_step_delay);
		});

		textViewEndTime.setOnClickListener(v -> {
			final Step step = getChild(groupPosition, childPosition);
			int durationSeconds = (int) (step.getDuration() / TimeUnit.SECONDS.toMillis(1));

			DialogUtil.displayDurationDialog(mFragment.requireActivity(), new RequestDurationDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog, final int minutes, final int seconds) {
							Step newStep = step.withDuration(TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds));
							mAlarm.updateDuration(newStep);
							AlarmRegistry.getInstance().addOrUpdate(mAlarm);
							notifyDataSetChanged();
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {

						}
					}, R.string.title_dialog_alarm_step_duration, R.string.button_ok, durationSeconds / SECONDS_PER_MINUTE,
					durationSeconds % SECONDS_PER_MINUTE, R.string.message_dialog_alarm_step_duration);
		});

		view.findViewById(R.id.imageViewDelete).setOnClickListener(v -> DialogUtil.displayConfirmationMessage(mFragment.requireActivity(), dialog -> {
			// Update duration, so that following steps are shifted
			mAlarm.updateDuration(originalStep.withDuration(0));
			mAlarm.removeStep(originalStep.getId());
			AlarmRegistry.getInstance().remove(originalStep, mAlarm.getId());
			AlarmRegistry.getInstance().addOrUpdate(mAlarm);
			notifyDataSetChanged();

			if (mAlarm.getSteps().size() == 0) {
				AlarmReceiver.cancelAlarm(mFragment.requireContext(), mAlarm.getId());
				AlarmRegistry.getInstance().remove(mAlarm);
				NavController navController = Navigation.findNavController(mFragment.requireActivity(), R.id.nav_host_fragment);
				navController.navigateUp();
			}
		}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_alarm_step));

		return view;
	}

	@Override
	public final boolean isChildSelectable(final int groupPosition, final int childPosition) {
		return false;
	}

	/**
	 * Get String representation of a delay.
	 *
	 * @param delay The delay.
	 * @return The String representation.
	 */
	protected static String getDelayString(final long delay) {
		return delay == 3600000 ? "60:00" : String.format(Locale.getDefault(), "%1$tM:%1$tS", new Date(delay)); // MAGIC_NUMBER
	}
}
