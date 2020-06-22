package de.jeisfeld.lifx.app.alarms;

import android.content.Context;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * A registry holding information about alarms.
 */
public final class AlarmRegistry {
	/**
	 * The singleton instance of AlarmRegistry.
	 */
	private static AlarmRegistry mInstance = null;
	/**
	 * The stored colors.
	 */
	private final SparseArray<Alarm> mAlarms = new SparseArray<>();

	/**
	 * Create the color registry and retrieve stored entries.
	 */
	private AlarmRegistry() {
		List<Integer> alarmIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids);
		for (int alarmId : alarmIds) {
			mAlarms.put(alarmId, new Alarm(alarmId));
		}
	}

	/**
	 * Get the list of alarms.
	 *
	 * @return The list of alarms.
	 */
	public List<Alarm> getAlarms() {
		List<Alarm> result = new ArrayList<>();
		for (int alarmId : PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids)) {
			Alarm alarm = mAlarms.get(alarmId);
			if (alarm != null) {
				result.add(alarm);
			}
		}
		return result;
	}

	/**
	 * Get a new automatic alarm name.
	 *
	 * @param context The context.
	 * @return The alarm name.
	 */
	public String getNewAlarmName(final Context context) {
		List<String> existingAlarmNames = getAlarms().stream().map(Alarm::getName).collect(Collectors.toList());
		String alarmName = null;
		int count = 1;
		while (alarmName == null) {
			alarmName = context.getResources().getString(R.string.default_alarm_name, count);
			if (existingAlarmNames.contains(alarmName)) {
				alarmName = null;
				count++;
			}
		}
		return alarmName;
	}


	/**
	 * Add or update an alarm in local store.
	 *
	 * @param alarm the stored alarm.
	 * @return the stored alarm.
	 */
	protected Alarm addOrUpdate(final Alarm alarm) {
		Alarm newAlarm = alarm.store();
		mAlarms.put(newAlarm.getId(), newAlarm);
		return newAlarm;
	}

	/**
	 * Remove an alarm from local store.
	 *
	 * @param alarm The alarm to be deleted.
	 */
	protected void remove(final Alarm alarm) {
		int alarmId = alarm.getId();
		mAlarms.remove(alarmId);

		List<Integer> alarmIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids);
		alarmIds.remove((Integer) alarmId);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_alarm_ids, alarmIds);

		for (Step step : alarm.getSteps()) {
			remove(step, alarmId);
		}
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_active, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_start_time, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_week_days, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_name, alarmId);
	}

	/**
	 * Remove an alarm step from local store.
	 *
	 * @param step    The alarm step to be deleted.
	 * @param alarmId The alarmId from which to remove the step.
	 */
	protected void remove(final Step step, final int alarmId) {
		int stepId = step.getId();

		List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId);
		stepIds.remove((Integer) stepId);
		PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId, stepIds);

		removePreferencesForStepId(stepId);
	}

	/**
	 * Remove the preferences for a certain stepId.
	 *
	 * @param stepId The stepId.
	 */
	private void removePreferencesForStepId(final int stepId) {
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_step_delay, stepId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_step_stored_color_id, stepId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_step_duration, stepId);
	}

	/**
	 * Get the ColorRegistry as singleton.
	 *
	 * @return The ColorRegistry as singleton.
	 */
	public static synchronized AlarmRegistry getInstance() {
		if (AlarmRegistry.mInstance == null) {
			AlarmRegistry.mInstance = new AlarmRegistry();
		}
		return AlarmRegistry.mInstance;
	}
}
