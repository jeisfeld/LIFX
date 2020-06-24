package de.jeisfeld.lifx.app.alarms;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.AlarmType;
import de.jeisfeld.lifx.app.alarms.Alarm.LightSteps;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.lifx.lan.Light;

/**
 * Fragment for configuration of an alarm.
 */
public class AlarmConfigurationFragment extends Fragment {
	/**
	 * Parameter to pass the alarmId.
	 */
	private static final String PARAM_ALARM_ID = "alarmId";
	/**
	 * The default duration of alarm steps.
	 */
	private static final long DEFAULT_DURATION = 10000;
	/**
	 * The hour.
	 */
	private int mHour = 0;
	/**
	 * The minute.
	 */
	private int mMinute = 0;
	/**
	 * The alarm.
	 */
	private Alarm mAlarm = null;
	/**
	 * The adapter of the list of steps.
	 */
	private AlarmStepExpandableListAdapter mAdapter;
	/**
	 * The last number of lights (used to identify if a light is added).
	 */
	private Map<Light, Boolean> mInitialExpandingStatus = new HashMap<>();

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
		final ExpandableListView listViewAlarmSteps = root.findViewById(R.id.listViewAlarmSteps);

		// the following fills also mAlarmSteps, mHour, mMinute
		final int alarmId = getArguments() == null ? -1 : getArguments().getInt(PARAM_ALARM_ID, -1);
		mAlarm = fillFromAlarmId(root, mAlarm == null ? alarmId : mAlarm.getId());

		configurePrimaryAlarmElements(root);

		((Switch) root.findViewById(R.id.switchAlarmActive)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));

		for (LightSteps lightSteps : mAlarm.getLightSteps()) {
			if (mInitialExpandingStatus.get(lightSteps.getLight()) == null) {
				mInitialExpandingStatus.put(lightSteps.getLight(), true);
			}
		}
		mAdapter = new AlarmStepExpandableListAdapter(getActivity(), mAlarm, mInitialExpandingStatus);
		listViewAlarmSteps.setAdapter(mAdapter);
		mInitialExpandingStatus = new HashMap<>();

		root.findViewById(R.id.imageViewAddAlarmLight).setOnClickListener(v -> {
			List<Light> lightsWithStoredColors = ColorRegistry.getInstance().getLightsWithStoredColors();
			lightsWithStoredColors.removeAll(mAlarm.getLightSteps().stream().map(LightSteps::getLight).collect(Collectors.toSet()));
			SelectDeviceDialogFragment.displaySelectDeviceDialog(requireActivity(), device ->
							StoredColorsDialogFragment.displayStoredColorsDialog(
									requireActivity(), (int) device.getParameter(DeviceRegistry.DEVICE_ID), true,
									storedColor -> {
										mAlarm.getSteps().add(new Step(0, storedColor.getId(), DEFAULT_DURATION));
										mAlarm = AlarmRegistry.getInstance().addOrUpdate(mAlarm);
										mAdapter.notifyDataSetChanged(mAlarm);
									}),
					new ArrayList<>(lightsWithStoredColors));
		});

		root.findViewById(R.id.imageViewTestAlarm).setOnClickListener(v ->
				LifxAlarmService.triggerAlarmService(getContext(), LifxAlarmService.ACTION_TEST_ALARM, mAlarm.getId(), new Date()));

		return root;
	}

	/**
	 * Configure the elements relevant for primary alarm.
	 *
	 * @param root The view root.
	 */
	private void configurePrimaryAlarmElements(final View root) {
		if (mAlarm.getAlarmType().isPrimary()) {
			final TextView textViewAlarmName = root.findViewById(R.id.textViewAlarmName);
			textViewAlarmName.setOnClickListener(v -> DialogUtil.displayInputDialog(requireActivity(), new RequestInputDialogListener() {
				@Override
				public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
					if (text == null || text.trim().isEmpty()) {
						DialogUtil.displayConfirmationMessage(requireActivity(),
								R.string.title_did_not_save_empty_name, R.string.message_did_not_save_empty_name);
					}
					else {
						textViewAlarmName.setText(text.trim());
						mAlarm = mAlarm.withChangedName(getContext(), text.trim());
						AlarmRegistry.getInstance().addOrUpdate(mAlarm);
						mAdapter.notifyDataSetChanged(mAlarm);
					}
				}

				@Override
				public void onDialogNegativeClick(final DialogFragment dialog) {
					// do nothing
				}
			}, R.string.title_dialog_change_alarm_name, R.string.button_rename, mAlarm.getName(), R.string.message_dialog_new_alarm_name));

			final TextView textViewAlarmStartTime = root.findViewById(R.id.textViewStartTime);
			textViewAlarmStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", mHour, mMinute));
			textViewAlarmStartTime.setOnClickListener(v -> {
				TimePickerDialog mTimePicker = new TimePickerDialog(getContext(),
						(timePicker, selectedHour, selectedMinute) -> {
							textViewAlarmStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
							mHour = selectedHour;
							mMinute = selectedMinute;
							saveAlarm(root);
						},
						mHour, mMinute, true);
				mTimePicker.show();
			});

			final ImageView imageViewAlarmType = root.findViewById(R.id.imageViewAlarmType);
			imageViewAlarmType.setImageResource(mAlarm.getAlarmType().getButtonResource());
			imageViewAlarmType.setOnClickListener(v -> {
				mAlarm = new Alarm(mAlarm.getId(), mAlarm.isActive(), mAlarm.getStartTime(), mAlarm.getWeekDays(), mAlarm.getName(),
						mAlarm.getSteps(), mAlarm.getAlarmType().getNext(), mAlarm.getStopSequence());
				imageViewAlarmType.setImageResource(mAlarm.getAlarmType().getButtonResource());
				AlarmRegistry.getInstance().addOrUpdate(mAlarm);
				mAdapter.notifyDataSetChanged(mAlarm);
			});

			((ImageView) root.findViewById(R.id.imageViewCopyAlarm)).setOnClickListener(v ->
					DialogUtil.displayInputDialog(requireActivity(), new RequestInputDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
							if (text == null || text.trim().isEmpty()) {
								DialogUtil.displayConfirmationMessage(requireActivity(),
										R.string.title_did_not_save_empty_name, R.string.message_did_not_save_empty_name);
							}
							else {
								textViewAlarmName.setText(text.trim());
								mAlarm = mAlarm.clone(getContext(), text.trim());
								mAdapter.notifyDataSetChanged(mAlarm);
							}
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
							// do nothing
						}
					}, R.string.title_dialog_copy_alarm, R.string.button_save, mAlarm.getName(), R.string.message_dialog_new_alarm_name));

			ImageView imageViewStopSequence = root.findViewById(R.id.imageViewStopSequence);
			imageViewStopSequence.setOnClickListener((OnClickListener) v -> {
				if (mAlarm.getStopSequence() == null) {
					List<Step> alarmSteps = new ArrayList<>();
					for (LightSteps lightSteps : mAlarm.getLightSteps()) {
						alarmSteps.add(new Step(0,
								StoredColor.fromDeviceOff((int) lightSteps.getLight().getParameter(DeviceRegistry.DEVICE_ID)).getId(),
								DEFAULT_DURATION));
					}
					Alarm stopSequence = new Alarm(true, new Date(), new HashSet<>(), getString(R.string.alarm_stopsequence_name, mAlarm.getName()),
							alarmSteps, AlarmType.STOP_SEQUENCE, null);
					mAlarm = new Alarm(mAlarm.getId(), mAlarm.isActive(), mAlarm.getStartTime(), mAlarm.getWeekDays(), mAlarm.getName(),
							mAlarm.getSteps(), mAlarm.getAlarmType(), stopSequence);
					mAlarm = AlarmRegistry.getInstance().addOrUpdate(mAlarm);
				}
				navigate(this, mAlarm.getStopSequence().getId());
			});

			((ToggleButton) root.findViewById(R.id.toggleButtonMonday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
			((ToggleButton) root.findViewById(R.id.toggleButtonTuesday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
			((ToggleButton) root.findViewById(R.id.toggleButtonWednesday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
			((ToggleButton) root.findViewById(R.id.toggleButtonThursday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
			((ToggleButton) root.findViewById(R.id.toggleButtonFriday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
			((ToggleButton) root.findViewById(R.id.toggleButtonSaturday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
			((ToggleButton) root.findViewById(R.id.toggleButtonSunday)).setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(root));
		}
		else {
			((TextView) root.findViewById(R.id.labelStartTime)).setVisibility(View.GONE);
			TextView labelWeekDays = root.findViewById(R.id.labelWeekDays);
			if (labelWeekDays != null) {
				labelWeekDays.setVisibility(View.GONE);
			}
			((TextView) root.findViewById(R.id.textViewStartTime)).setVisibility(View.GONE);
			((ImageView) root.findViewById(R.id.imageViewCopyAlarm)).setVisibility(View.GONE);
			((ImageView) root.findViewById(R.id.imageViewAlarmType)).setVisibility(View.GONE);
			((ImageView) root.findViewById(R.id.imageViewStopSequence)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonMonday)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonTuesday)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonWednesday)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonThursday)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonFriday)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonSaturday)).setVisibility(View.GONE);
			((ToggleButton) root.findViewById(R.id.toggleButtonSunday)).setVisibility(View.GONE);
		}
	}


	@Override
	public final void onDestroyView() {
		super.onDestroyView();
		mInitialExpandingStatus = mAdapter == null ? new HashMap<>() : mAdapter.getExpandingStatus();
	}

	/**
	 * Get the selected weekdays.
	 *
	 * @param root The view.
	 * @return The selected weekdays.
	 */
	private Set<Integer> getSelectecWeekDays(final View root) {
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
		return weekDays;
	}

	/**
	 * Save the current alarm.
	 *
	 * @param root The view.
	 */
	private void saveAlarm(final View root) {
		final Switch switchAlarmActive = root.findViewById(R.id.switchAlarmActive);
		Date startDate = Alarm.getDate(mHour, mMinute);
		Set<Integer> weekDays = getSelectecWeekDays(root);

		mAlarm = new Alarm(mAlarm.getId(), switchAlarmActive.isChecked(), startDate, weekDays,
				mAlarm.getName(), mAlarm.getSteps(), mAlarm.getAlarmType(), mAlarm.getStopSequence());
		AlarmRegistry.getInstance().addOrUpdate(mAlarm);
		mAdapter.notifyDataSetChanged(mAlarm);
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
		}
		else {
			alarm = new Alarm(true, new Date(), new HashSet<>(), AlarmRegistry.getInstance().getNewAlarmName(getContext()),
					new ArrayList<>(), AlarmType.STANDARD, null);
			alarm = AlarmRegistry.getInstance().addOrUpdate(alarm);
		}

		((TextView) root.findViewById(R.id.textViewAlarmName)).setText(alarm.getName());
		((Switch) root.findViewById(R.id.switchAlarmActive)).setChecked(alarm.isActive());
		((ToggleButton) root.findViewById(R.id.toggleButtonMonday)).setChecked(alarm.getWeekDays().contains(Calendar.MONDAY));
		((ToggleButton) root.findViewById(R.id.toggleButtonTuesday)).setChecked(alarm.getWeekDays().contains(Calendar.TUESDAY));
		((ToggleButton) root.findViewById(R.id.toggleButtonWednesday)).setChecked(alarm.getWeekDays().contains(Calendar.WEDNESDAY));
		((ToggleButton) root.findViewById(R.id.toggleButtonThursday)).setChecked(alarm.getWeekDays().contains(Calendar.THURSDAY));
		((ToggleButton) root.findViewById(R.id.toggleButtonFriday)).setChecked(alarm.getWeekDays().contains(Calendar.FRIDAY));
		((ToggleButton) root.findViewById(R.id.toggleButtonSaturday)).setChecked(alarm.getWeekDays().contains(Calendar.SATURDAY));
		((ToggleButton) root.findViewById(R.id.toggleButtonSunday)).setChecked(alarm.getWeekDays().contains(Calendar.SUNDAY));
		((ImageView) root.findViewById(R.id.imageViewStopSequence)).setImageResource(
				alarm.getStopSequence() == null ? R.drawable.ic_alarm_stopsequence_off : R.drawable.ic_alarm_stopsequence_on);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(alarm.getStartTime());
		mHour = calendar.get(Calendar.HOUR_OF_DAY);
		mMinute = calendar.get(Calendar.MINUTE);

		Collections.sort(alarm.getSteps());

		return alarm;
	}
}
