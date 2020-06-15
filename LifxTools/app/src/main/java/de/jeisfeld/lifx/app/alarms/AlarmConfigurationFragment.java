package de.jeisfeld.lifx.app.alarms;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * Fragment for configuration of an alarm.
 */
public class AlarmConfigurationFragment extends Fragment {
	/**
	 * Parameter to pass the alarmId.
	 */
	private static final String PARAM_ALARM_ID = "alarmId";
	/**
	 * The hour.
	 */
	private int mHour = 0;
	/**
	 * The minute.
	 */
	private int mMinute = 0;
	/**
	 * The alarm steps.
	 */
	private List<Step> mAlarmSteps = new ArrayList<>();
	/**
	 * The array adapter.
	 */
	private ArrayAdapter<Step> mArrayAdapter;

	/**
	 * Navigate to this fragment.
	 *
	 * @param fragment The source fragment.
	 * @param alarmId  The alarm id for which to start the fragment.
	 */
	public static void navigate(final Fragment fragment, final Integer alarmId) {
		FragmentActivity activity = fragment == null ? null : fragment.getActivity();
		if (activity != null) {
			NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
			Bundle bundle = new Bundle();
			if (alarmId != null) {
				bundle.putInt(AlarmConfigurationFragment.PARAM_ALARM_ID, alarmId);
			}
			navController.navigate(R.id.nav_alarm_configuration, bundle);
		}
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_alarm_configuration, container, false);
		final int alarmId = getArguments() == null ? -1 : getArguments().getInt(PARAM_ALARM_ID, -1);

		final TextView textViewAlarmStartTime = root.findViewById(R.id.textViewStartTime);
		final ListView listViewAlarmSteps = root.findViewById(R.id.listViewAlarmSteps);
		final Alarm alarm = fillFromAlarmId(root, alarmId);

		textViewAlarmStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", mHour, mMinute));

		textViewAlarmStartTime.setOnClickListener(v -> {
			TimePickerDialog mTimePicker = new TimePickerDialog(getContext(),
					(timePicker, selectedHour, selectedMinute) -> {
						textViewAlarmStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
						mHour = selectedHour;
						mMinute = selectedMinute;
					},
					mHour, mMinute, true);
			mTimePicker.setTitle(R.string.title_dialog_alarm_time);
			mTimePicker.show();
		});

		mArrayAdapter = createArrayAdapter(alarmId, alarm);
		listViewAlarmSteps.setAdapter(mArrayAdapter);

		root.findViewById(R.id.buttonAddAlarmStep).setOnClickListener(v ->
				AlarmStepConfigurationFragment.navigate(this, alarmId, null));

		root.findViewById(R.id.buttonTestAlarm).setOnClickListener(v -> alarm.startService(requireContext(), new Date()));

		root.findViewById(R.id.buttonCancel).setOnClickListener(v -> {
			NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
			navController.navigateUp();
		});

		root.findViewById(R.id.buttonSave).setOnClickListener(createOnSaveListener(root, alarmId, alarm));

		return root;
	}

	@Override
	public final void onDestroy() {
		super.onDestroy();
		AlarmRegistry.getInstance().removeTemporarySteps();
	}

	/**
	 * Create the array adapter for alarm steps.
	 *
	 * @param alarmId The alarm id.
	 * @param alarm   The alarm (if already stored)
	 * @return The array adapter.
	 */
	private ArrayAdapter<Step> createArrayAdapter(final int alarmId, final Alarm alarm) {
		return new ArrayAdapter<Step>(requireContext(), R.layout.list_view_alarm_steps, mAlarmSteps) {
			@Override
			@NonNull
			public View getView(final int position, final View view, @NonNull final ViewGroup parent) {
				final View newView;
				if (view == null) {
					newView = View.inflate(requireActivity(), R.layout.list_view_alarm_steps, null);
				}
				else {
					newView = view;
				}

				final Step step = mAlarmSteps.get(position);
				((ImageView) newView.findViewById(R.id.imageViewStoredColor))
						.setImageDrawable(StoredColorsViewAdapter.getButtonDrawable(requireContext(), step.getStoredColor()));
				((TextView) newView.findViewById(R.id.textViewDeviceName)).setText(step.getStoredColor().getLight().getLabel());
				((TextView) newView.findViewById(R.id.textViewStartTime)).setText(AlarmStepConfigurationFragment.getDelayString(step.getDelay()));
				((TextView) newView.findViewById(R.id.textViewEndTime)).setText(
						AlarmStepConfigurationFragment.getDelayString(step.getDelay() + step.getDuration()));

				newView.findViewById(R.id.imageViewDelete).setOnClickListener(v ->
						DialogUtil.displayConfirmationMessage(requireActivity(), new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								if (alarm != null) {
									alarm.removeStep(step);
								}
								AlarmRegistry.getInstance().remove(step, alarmId);
								mAlarmSteps.remove(step);
								if (mArrayAdapter != null) {
									mArrayAdapter.notifyDataSetChanged();
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_alarm_step));

				newView.setOnClickListener(v -> AlarmStepConfigurationFragment.navigate(AlarmConfigurationFragment.this, alarmId, step.getId()));
				return newView;
			}
		};
	}

	/**
	 * Create the listener on saving.
	 *
	 * @param root    The root view.
	 * @param alarmId The alarm id.
	 * @param alarm   the alarm (may be null)
	 * @return The listener on saving.
	 */
	private OnClickListener createOnSaveListener(final View root, final int alarmId, final Alarm alarm) {
		return v -> {
			final EditText editTextAlarmName = root.findViewById(R.id.editTextAlarmName);
			final Switch switchAlarmActive = root.findViewById(R.id.switchAlarmActive);

			if (editTextAlarmName.getText() == null || editTextAlarmName.getText().toString().isEmpty()) {
				DialogUtil.displayToast(AlarmConfigurationFragment.this.getContext(), R.string.toast_did_not_save_empty_name);
				return;
			}

			Date startDate = Alarm.getDate(mHour, mMinute);

			Set<Integer> weekDays = new HashSet<>();
			if (((ToggleButton) root.findViewById(R.id.toggleButtonMonday)).isChecked()) {
				weekDays.add(Calendar.MONDAY);
			}
			if (((ToggleButton) root.findViewById(R.id.toggleButtonTuesday)).isChecked()) {
				weekDays.add(Calendar.TUESDAY);
			}
			if (((ToggleButton) root.findViewById(R.id.toggleButtonWednesday)).isChecked()) {
				weekDays.add(Calendar.WEDNESDAY);
			}
			if (((ToggleButton) root.findViewById(R.id.toggleButtonThursday)).isChecked()) {
				weekDays.add(Calendar.THURSDAY);
			}
			if (((ToggleButton) root.findViewById(R.id.toggleButtonFriday)).isChecked()) {
				weekDays.add(Calendar.FRIDAY);
			}
			if (((ToggleButton) root.findViewById(R.id.toggleButtonSaturday)).isChecked()) {
				weekDays.add(Calendar.SATURDAY);
			}
			if (((ToggleButton) root.findViewById(R.id.toggleButtonSunday)).isChecked()) {
				weekDays.add(Calendar.SUNDAY);
			}

			if (alarm != null && !editTextAlarmName.getText().toString().equals(alarm.getName())) {
				DialogUtil.displayConfirmationMessage(AlarmConfigurationFragment.this.requireActivity(), new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								Alarm newAlarm = new Alarm(switchAlarmActive.isChecked(), startDate, weekDays,
										editTextAlarmName.getText().toString(), new ArrayList<>());
								AlarmRegistry.getInstance().addOrUpdate(newAlarm);
								NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
								navController.navigateUp();
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								Alarm newAlarm = new Alarm(alarmId, switchAlarmActive.isChecked(), startDate, weekDays,
										editTextAlarmName.getText().toString(), new ArrayList<>());
								AlarmRegistry.getInstance().addOrUpdate(newAlarm);
								NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
								navController.navigateUp();
							}
						}, R.string.title_dialog_rename_or_copy, R.string.button_rename, R.string.button_copy,
						R.string.message_confirm_rename_or_copy, alarm.getName(), editTextAlarmName.getText().toString());
			}
			else {
				Alarm newAlarm = new Alarm(alarmId, switchAlarmActive.isChecked(), startDate, weekDays,
						editTextAlarmName.getText().toString(), mAlarmSteps);
				AlarmRegistry.getInstance().addOrUpdate(newAlarm);
				if (alarmId < 0) {
					PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_step_ids, -1);
				}

				NavController navController = Navigation.findNavController(AlarmConfigurationFragment.this.requireActivity(), R.id.nav_host_fragment);
				navController.navigateUp();
			}
		};
	}

	/**
	 * Fill the view from an alarmId.
	 *
	 * @param root    The root view.
	 * @param alarmId The alarmId.
	 * @return The alarm.
	 */
	private Alarm fillFromAlarmId(final View root, final int alarmId) {
		Alarm alarm;
		if (alarmId >= 0) {
			alarm = new Alarm(alarmId);

			((TextView) root.findViewById(R.id.editTextAlarmName)).setText(alarm.getName());
			((Switch) root.findViewById(R.id.switchAlarmActive)).setChecked(alarm.isActive());
			((ToggleButton) root.findViewById(R.id.toggleButtonMonday)).setChecked(alarm.getWeekDays().contains(Calendar.MONDAY));
			((ToggleButton) root.findViewById(R.id.toggleButtonTuesday)).setChecked(alarm.getWeekDays().contains(Calendar.TUESDAY));
			((ToggleButton) root.findViewById(R.id.toggleButtonWednesday)).setChecked(alarm.getWeekDays().contains(Calendar.WEDNESDAY));
			((ToggleButton) root.findViewById(R.id.toggleButtonThursday)).setChecked(alarm.getWeekDays().contains(Calendar.THURSDAY));
			((ToggleButton) root.findViewById(R.id.toggleButtonFriday)).setChecked(alarm.getWeekDays().contains(Calendar.FRIDAY));
			((ToggleButton) root.findViewById(R.id.toggleButtonSaturday)).setChecked(alarm.getWeekDays().contains(Calendar.SATURDAY));
			((ToggleButton) root.findViewById(R.id.toggleButtonSunday)).setChecked(alarm.getWeekDays().contains(Calendar.SUNDAY));

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(alarm.getStartTime());
			mHour = calendar.get(Calendar.HOUR_OF_DAY);
			mMinute = calendar.get(Calendar.MINUTE);

			mAlarmSteps = alarm.getSteps();
		}
		else {
			alarm = null;

			List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, -1);
			mAlarmSteps = new ArrayList<>();
			for (Integer stepId : stepIds) {
				if (stepId != null) {
					mAlarmSteps.add(new Step(stepId));
				}
			}
		}

		Collections.sort(mAlarmSteps);

		return alarm;
	}

}
