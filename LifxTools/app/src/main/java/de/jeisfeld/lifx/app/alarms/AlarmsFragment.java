package de.jeisfeld.lifx.app.alarms;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.AlarmType;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.lan.Light;

/**
 * Fragment for management of alarms.
 */
public class AlarmsFragment extends Fragment {
	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_alarms, container, false);
		final RecyclerView recyclerView = root.findViewById(R.id.recyclerViewAlarms);
		populateRecyclerView(recyclerView);

		root.findViewById(R.id.buttonAddAlarm).setOnClickListener(v -> createNewAlarm());

		return root;
	}

	/**
	 * Populate the recycler view for the alarms.
	 *
	 * @param recyclerView The recycler view.
	 */
	private void populateRecyclerView(final RecyclerView recyclerView) {
		AlarmsViewAdapter adapter = new AlarmsViewAdapter(this, recyclerView);
		ItemTouchHelper.Callback callback = new AlarmsItemMoveCallback(adapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		adapter.setStartDragListener(touchHelper::startDrag);
		touchHelper.attachToRecyclerView(recyclerView);

		recyclerView.setAdapter(adapter);
	}

	private void createNewAlarm() {
		if (ColorRegistry.getInstance().getLightsWithStoredColors().size() == 0) {
			DialogUtil.displayConfirmationMessage(requireActivity(), R.string.title_no_stored_colors, R.string.message_no_stored_colors);
			return;
		}

		final Calendar calendar = Calendar.getInstance();
		TimePickerDialog mTimePicker = new TimePickerDialog(getContext(),
				(timePicker, selectedHour, selectedMinute) -> {
					final Date startDate = Alarm.getDate(selectedHour, selectedMinute);
					List<Light> lightsWithStoredColors = ColorRegistry.getInstance().getLightsWithStoredColors();
					SelectDeviceDialogFragment.displaySelectDeviceDialog(requireActivity(),
							device ->
									StoredColorsDialogFragment.displayStoredColorsDialog(
											requireActivity(), (int) device.getParameter(DeviceRegistry.DEVICE_ID), true, true,
											(dialog, storedColor) -> {
												List<Step> steps = new ArrayList<>();
												steps.add(new Step(0, storedColor.getId(), 10000)); // MAGIC_NUMBER
												Alarm alarm = new Alarm(true, startDate, new HashSet<>(),
														AlarmRegistry.getInstance().getNewAlarmName(getContext()), steps,
														AlarmType.STANDARD, null, true);
												alarm = AlarmRegistry.getInstance().addOrUpdate(alarm);
												AlarmConfigurationFragment.navigate(AlarmsFragment.this, alarm.getId());
											}),
							new ArrayList<>(lightsWithStoredColors));
				},
				calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
		mTimePicker.setMessage(getString(R.string.title_dialog_alarm_time));
		mTimePicker.show();
	}


}
