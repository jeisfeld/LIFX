package de.jeisfeld.lifx.app.alarms;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.jeisfeld.lifx.app.Application;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class AlarmReceiver extends BroadcastReceiver {
	/**
	 * The number of seconds for early start.
	 */
	protected static final int EARLY_START_SECONDS = 5;
	/**
	 * The resource key for the notification id.
	 */
	public static final String EXTRA_ALARM_ID = "de.jeisfeld.lifx.app.ALARM_ID";
	/**
	 * The resource key for the notification id.
	 */
	public static final String EXTRA_ALARM_TIME = "de.jeisfeld.lifx.app.ALARM_TIME";

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);
		Date alarmTime = (Date) intent.getSerializableExtra(EXTRA_ALARM_TIME);
		if (alarmId != -1) {
			LifxAlarmService.triggerAlarmService(context, LifxAlarmService.ACTION_TRIGGER_ALARM, alarmId, alarmTime);
		}
	}

	/**
	 * Set an alarm.
	 *
	 * @param context     The context
	 * @param alarmId     The alarm id.
	 * @param alarmTime   The alarm time
	 * @param alarmIntent The alarm intent
	 */
	private static void setAlarm(final Context context, final int alarmId, final long alarmTime, final PendingIntent alarmIntent) {
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (alarmMgr != null) {
			alarmMgr.setAlarmClock(new AlarmClockInfo(alarmTime, alarmIntent), alarmIntent);
		}
	}

	/**
	 * Enable SdMountReceiver to automatically restart the alarm when the device is rebooted.
	 *
	 * @param context The context.
	 */
	private static void reEnableAlarmsOnBoot(final Context context) {
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

	}

	/**
	 * Retrigger alarm at time of previous alarm execution.
	 *
	 * @param context The context.
	 * @param alarm   The alarm.
	 */
	protected static void retriggerAlarm(final Context context, final Alarm alarm) {
		if (alarm.getAlarmType().isPrimary()) {
			if (alarm.getWeekDays().size() == 0) {
				// disable non-repeating alarm
				Alarm newAlarm = new Alarm(alarm.getId(), false, alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(),
						alarm.getSteps(), alarm.getAlarmType(), alarm.getStopSequence(), alarm.isMaximizeVolume());
				AlarmRegistry.getInstance().addOrUpdate(newAlarm);
			}
			else {
				// recreate repeating alarm
				setAlarm(context, alarm, true);
			}
		}
	}

	/**
	 * Sets a configured alarm.
	 *
	 * @param context The context in which the alarm is set.
	 * @param alarm   the alarm.
	 * @return true if the alarm has been created.
	 */
	public static boolean setAlarm(final Context context, final Alarm alarm) {
		return setAlarm(context, alarm, false);
	}

	/**
	 * Sets a configured alarm.
	 *
	 * @param context    The context in which the alarm is set.
	 * @param alarm      the alarm.
	 * @param onlyFuture flag indicating if the alarm should only be triggered if more then 1h in the future. Used for immediate retrigger.
	 * @return true if the alarm has been created.
	 */
	private static boolean setAlarm(final Context context, final Alarm alarm, final boolean onlyFuture) {
		if (!alarm.isActive()) {
			// only create active alarms.
			cancelAlarm(context, alarm.getId());
			return false;
		}
		Date startTime = alarm.getStartTime();
		if (alarm.getWeekDays().size() == 0 && startTime.before(new Date())) {
			// to not create one-time alarm that lies in the past.
			return false;
		}

		if (alarm.getWeekDays().size() > 0) {
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(startTime);
			startTime = Alarm.getDate(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
			calendar.setTime(startTime);
			if (onlyFuture && startTime.getTime() < new Date().getTime() + TimeUnit.HOURS.toMillis(1)) {
				// if onlyfuture flag is set, then ensure that it is at least 1h in future
				calendar.add(Calendar.DATE, 1);
			}

			// ensure that on proper day of week
			while (!alarm.getWeekDays().contains(calendar.get(Calendar.DAY_OF_WEEK))) {
				calendar.add(Calendar.DATE, 1);
			}
			startTime = calendar.getTime();
		}

		// Start alarm 30 seconds in advance, to allow time for preparation and getting connected
		long alarmTimeMillis = startTime.getTime() - TimeUnit.SECONDS.toMillis(EARLY_START_SECONDS); // MAGIC_NUMBER

		if (alarmTimeMillis < new Date().getTime() + TimeUnit.SECONDS.toMillis(EARLY_START_SECONDS)) {
			// Too late, hence no service - just start the animation.
			LifxAlarmService.triggerAlarmService(context, LifxAlarmService.ACTION_IMMEDIATE_ALARM, alarm.getId(), startTime);
			return false;
		}
		else {
			PendingIntent alarmIntent = createAlarmIntent(context, alarm.getId(), startTime, true);
			setAlarm(context, alarm.getId(), alarmTimeMillis, alarmIntent);
			LifxAlarmService.triggerAlarmService(context, LifxAlarmService.ACTION_CREATE_ALARM, alarm.getId(), startTime);
			reEnableAlarmsOnBoot(context);
			return true;
		}
	}

	/**
	 * Cancels one alarm.
	 *
	 * @param context The context in which the alarm is set.
	 * @param alarmId the alarm id.
	 */
	public static void cancelAlarm(final Context context, final int alarmId) {
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (alarmMgr != null) {
			alarmMgr.cancel(createAlarmIntent(context, alarmId, null, false));
			LifxAlarmService.triggerAlarmService(context, LifxAlarmService.ACTION_CANCEL_ALARM, alarmId, null);
		}
	}

	/**
	 * Create a PendingIntent which can be used for creating or cancelling an alarm.
	 *
	 * @param context   The context in which the alarm is set.
	 * @param alarmId   the alarm id.
	 * @param alarmTime the alarm time.
	 * @param isNew     flag indicating if this is a new intent or if an existing intent should be reused.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int alarmId, final Date alarmTime, final boolean isNew) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra(EXTRA_ALARM_ID, alarmId);
		if (alarmTime != null) {
			intent.putExtra(EXTRA_ALARM_TIME, alarmTime);
		}
		return PendingIntent.getBroadcast(context, alarmId, intent,
				isNew ? PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT);
	}

	/**
	 * Create all configured alarms.
	 */
	public static void createAllAlarms() {
		List<Alarm> alarms = AlarmRegistry.getInstance().getAlarms();
		for (Alarm alarm : alarms) {
			setAlarm(Application.getAppContext(), alarm);
		}
	}
}
