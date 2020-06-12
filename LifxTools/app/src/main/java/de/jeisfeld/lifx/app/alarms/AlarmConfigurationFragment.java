package de.jeisfeld.lifx.app.alarms;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;

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
	 * Navigate to this fragment.
	 *
	 * @param fragment The source fragment.
	 * @param alarmId The alarm id for which to start the fragment.
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

		final EditText editTextAlarmName = root.findViewById(R.id.editTextAlarmName);
		final Switch switchAlarmActive = root.findViewById(R.id.switchAlarmActive);
		final TextView textViewAlarmStartTime = root.findViewById(R.id.textViewStartTime);
		final ToggleButton toggleButtonMonday = root.findViewById(R.id.toggleButtonMonday);
		final ToggleButton toggleButtonTuesday = root.findViewById(R.id.toggleButtonTuesday);
		final ToggleButton toggleButtonWednesday = root.findViewById(R.id.toggleButtonWednesday);
		final ToggleButton toggleButtonThursday = root.findViewById(R.id.toggleButtonThursday);
		final ToggleButton toggleButtonFriday = root.findViewById(R.id.toggleButtonFriday);
		final ToggleButton toggleButtonSaturday = root.findViewById(R.id.toggleButtonSaturday);
		final ToggleButton toggleButtonSunday = root.findViewById(R.id.toggleButtonSunday);

		final Alarm alarm;
		if (alarmId >= 0) {
			alarm = new Alarm(alarmId);
			editTextAlarmName.setText(alarm.getName());
			switchAlarmActive.setChecked(alarm.isActive());
			toggleButtonMonday.setChecked(alarm.getWeekDays().contains(Calendar.MONDAY));
			toggleButtonTuesday.setChecked(alarm.getWeekDays().contains(Calendar.TUESDAY));
			toggleButtonWednesday.setChecked(alarm.getWeekDays().contains(Calendar.WEDNESDAY));
			toggleButtonThursday.setChecked(alarm.getWeekDays().contains(Calendar.THURSDAY));
			toggleButtonFriday.setChecked(alarm.getWeekDays().contains(Calendar.FRIDAY));
			toggleButtonSaturday.setChecked(alarm.getWeekDays().contains(Calendar.SATURDAY));
			toggleButtonSunday.setChecked(alarm.getWeekDays().contains(Calendar.SUNDAY));

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(alarm.getStartTime());
			mHour = calendar.get(Calendar.HOUR_OF_DAY);
			mMinute = calendar.get(Calendar.MINUTE);
		}
		else {
			alarm = null;
		}
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

		root.findViewById(R.id.buttonCancel).setOnClickListener(v -> {
			if (getActivity() != null) {
				NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
				navController.navigateUp();
			}
		});

		root.findViewById(R.id.buttonSave).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				if (editTextAlarmName.getText() == null || editTextAlarmName.getText().toString().isEmpty()) {
					DialogUtil.displayToast(getContext(), R.string.toast_did_not_save_empty_name);
					return;
				}

				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR_OF_DAY, mHour);
				calendar.set(Calendar.MINUTE, mMinute);
				if (calendar.before(Calendar.getInstance())) {
					calendar.add(Calendar.DATE, 1);
				}

				Set<Integer> weekDays = new HashSet<>();
				if (toggleButtonMonday.isChecked()) {
					weekDays.add(Calendar.MONDAY);
				}
				if (toggleButtonTuesday.isChecked()) {
					weekDays.add(Calendar.TUESDAY);
				}
				if (toggleButtonWednesday.isChecked()) {
					weekDays.add(Calendar.WEDNESDAY);
				}
				if (toggleButtonThursday.isChecked()) {
					weekDays.add(Calendar.THURSDAY);
				}
				if (toggleButtonFriday.isChecked()) {
					weekDays.add(Calendar.FRIDAY);
				}
				if (toggleButtonSaturday.isChecked()) {
					weekDays.add(Calendar.SATURDAY);
				}
				if (toggleButtonSunday.isChecked()) {
					weekDays.add(Calendar.SUNDAY);
				}

				if (alarm != null && !editTextAlarmName.getText().toString().equals(alarm.getName())) {
					DialogUtil.displayConfirmationMessage(activity, new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog) {
							Alarm newAlarm = new Alarm(switchAlarmActive.isChecked(), calendar.getTime(), weekDays,
									editTextAlarmName.getText().toString(), new ArrayList<>());
							AlarmRegistry.getInstance().addOrUpdate(newAlarm);
							NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
							navController.navigateUp();
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
							Alarm newAlarm = new Alarm(alarmId, switchAlarmActive.isChecked(), calendar.getTime(), weekDays,
									editTextAlarmName.getText().toString(), new ArrayList<>());
							AlarmRegistry.getInstance().addOrUpdate(newAlarm);
							NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
							navController.navigateUp();
						}
					}, R.string.title_dialog_rename_or_copy, R.string.button_rename, R.string.button_copy,
							R.string.message_confirm_rename_or_copy, alarm.getName(), editTextAlarmName.getText().toString());
				}
				else {
					Alarm newAlarm = new Alarm(alarmId, switchAlarmActive.isChecked(), calendar.getTime(), weekDays,
							editTextAlarmName.getText().toString(), new ArrayList<>());
					AlarmRegistry.getInstance().addOrUpdate(newAlarm);
					NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
					navController.navigateUp();
				}

			}
		});

		return root;
	}

}
