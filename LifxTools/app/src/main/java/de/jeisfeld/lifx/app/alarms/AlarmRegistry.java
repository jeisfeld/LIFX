package de.jeisfeld.lifx.app.alarms;

import android.content.Context;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.LightSteps;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;

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
		if (newAlarm.getAlarmType().isPrimary()) {
			mAlarms.put(newAlarm.getId(), newAlarm);
		}
		else {
			Alarm parent = alarm.getParent();
			if (parent != null) {
				Alarm newParent = new Alarm(parent.getId(), parent.isActive(), parent.getStartTime(), parent.getWeekDays(), parent.getName(),
						parent.getSteps(), parent.getAlarmType(), newAlarm);
				mAlarms.put(newParent.getId(), newParent);
			}
		}
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

		if (alarm.getAlarmType().isPrimary()) {
			// update list of alarms
			List<Integer> alarmIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids);
			alarmIds.remove((Integer) alarmId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_alarm_ids, alarmIds);
		}
		else {
			// remove reference from parent
			for (int parentAlarmId : PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids)) {
				if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_stop_sequence_id, parentAlarmId, -1) == alarmId) {
					PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_stop_sequence_id, parentAlarmId);
				}
			}
		}

		Alarm stopSequence = alarm.getStopSequence();
		if (stopSequence != null) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_stop_sequence_id, stopSequence.getId());
			remove(stopSequence);
		}

		for (Step step : alarm.getSteps()) {
			remove(step, alarmId);
		}
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_active, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_start_time, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_week_days, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_name, alarmId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_type, alarmId);
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
	 * Check if a stored color is used by an alarm.
	 *
	 * @param storedColor a stored color.
	 * @return True if used by an alarm.
	 */
	public boolean isUsed(final StoredColor storedColor) {
		for (Alarm alarm : getAlarms()) {
			for (Step step : alarm.getSteps()) {
				if (step.getStoredColor().equals(storedColor)) {
					return true;
				}
			}
			if (alarm.getStopSequence() != null) {
				for (Step step : alarm.getStopSequence().getSteps()) {
					if (step.getStoredColor().equals(storedColor)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Check if a device is used by an alarm.
	 *
	 * @param device a device
	 * @return True if used by an alarm.
	 */
	public boolean isUsed(final Device device) {
		for (Alarm alarm : getAlarms()) {
			for (LightSteps lightSteps : alarm.getLightSteps()) {
				if (lightSteps.getLight().equals(device)) {
					return true;
				}
			}
			if (alarm.getStopSequence() != null) {
				for (LightSteps lightSteps : alarm.getStopSequence().getLightSteps()) {
					if (lightSteps.getLight().equals(device)) {
						return true;
					}
				}
			}
		}
		return false;
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

	/**
	 * Cleanup the alarm registry, so that it is recreated next time.
	 */
	public static synchronized void cleanUp() {
		AlarmRegistry.mInstance = null;
	}

}
