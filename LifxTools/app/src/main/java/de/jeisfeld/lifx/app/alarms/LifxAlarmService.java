package de.jeisfeld.lifx.app.alarms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.MainActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.os.Logger;

/**
 * A service handling LIFX animations in the background.
 */
public class LifxAlarmService extends Service {
	/**
	 * Action for creating an alarm.
	 */
	protected static final String ACTION_CREATE_ALARM = "de.jeisfeld.lifx.app.ACTION_CREATE_ALARM";
	/**
	 * Action for cancelling an alarm.
	 */
	protected static final String ACTION_CANCEL_ALARM = "de.jeisfeld.lifx.app.ACTION_CANCEL_ALARM";
	/**
	 * Action for triggering an alarm from alarmManager.
	 */
	protected static final String ACTION_TRIGGER_ALARM = "de.jeisfeld.lifx.app.ACTION_TRIGGER_ALARM";
	/**
	 * Action for triggering an alarm immediately.
	 */
	protected static final String ACTION_IMMEDIATE_ALARM = "de.jeisfeld.lifx.app.ACTION_IMMEDIATE_ALARM";
	/**
	 * Action for testing an alarm.
	 */
	protected static final String ACTION_TEST_ALARM = "de.jeisfeld.lifx.app.ACTION_TEST_ALARM";
	/**
	 * Relative brightness for color off.
	 */
	private static final double OFF_BRIGHTNESS = 0.01;
	/**
	 * Additional time in ms to really get the light off.
	 */
	private static final long OFF_TIME = 500;
	/**
	 * The id of the notification channel.
	 */
	private static final String NOTIFICATION_CHANNEL_ID = "LifxAlarmChannel";
	/**
	 * The id of the notification channel for alarm execution.
	 */
	private static final String NOTIFICATION_CHANNEL_ID_EXECUTION = "LifxAlarmExecutionChannel";
	/**
	 * The notification tag for alarm execution notification.
	 */
	private static final String NOTIFICATION_TAG_ALARM_EXECUTION = "AlarmExecution";
	/**
	 * The retry count for alarms.
	 */
	private static final int ALARM_RETRY_COUNT = 3;
	/**
	 * List of currently running alarms.
	 */
	private static final List<Integer> ANIMATED_ALARMS = new ArrayList<>();
	/**
	 * List of pending alarms.
	 */
	private static final Set<Integer> PENDING_ALARMS = new HashSet<>();

	/**
	 * Send message to alarm service.
	 *
	 * @param context   The context.
	 * @param action    the action.
	 * @param alarmId   the alarm id.
	 * @param alarmTime the alarm time.
	 */
	protected static void triggerAlarmService(final Context context, final String action, final int alarmId, final Date alarmTime) {
		Intent intent = new Intent(context, LifxAlarmService.class);
		intent.setAction(action);
		intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
		if (alarmTime != null) {
			intent.putExtra(AlarmReceiver.EXTRA_ALARM_TIME, alarmTime);
		}
		Logger.debugAlarm("Triggering alarm service " + action + " for " + new Alarm(alarmId).getName());
		ContextCompat.startForegroundService(context, intent);
	}

	@Override
	public final void onCreate() {
		super.onCreate();
		Logger.debugAlarm("Created LifxAlarmService");
		createNotificationChannels();
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		final String action = intent.getAction();
		final int alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);
		final Date alarmDate = (Date) intent.getSerializableExtra(AlarmReceiver.EXTRA_ALARM_TIME);
		Alarm alarm = new Alarm(alarmId);

		if (ACTION_CREATE_ALARM.equals(action)) {
			Logger.debugAlarm("Started alarm service for " + alarm.getName() + " at " + alarmDate);
			synchronized (PENDING_ALARMS) {
				PENDING_ALARMS.add(alarmId);
			}
			startNotification();
		}
		else if (ACTION_CANCEL_ALARM.equals(action)) {
			Logger.debugAlarm("Cancelled alarm for " + alarm.getName());
			synchronized (PENDING_ALARMS) {
				PENDING_ALARMS.remove(alarmId);
				// Still start notification to be safe in case of saving previously inactive alarm.
				startNotification();
				if (ANIMATED_ALARMS.size() == 0 && PENDING_ALARMS.size() == 0) {
					Intent serviceIntent = new Intent(this, LifxAlarmService.class);
					stopService(serviceIntent);
				}
			}
		}
		else if (ACTION_TRIGGER_ALARM.equals(action)) {
			Logger.debugAlarm("Triggered alarm for " + alarm.getName() + " at " + alarmDate);
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.remove(alarmId);
				ANIMATED_ALARMS.add(alarmId);
			}
			getAlarmAnimationThread(alarm, alarmDate).start();
			startNotification();
			startRunningNotification(alarm);
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_IMMEDIATE_ALARM.equals(action)) {
			Logger.debugAlarm("Immediately started alarm for " + alarm.getName() + " at " + alarmDate);
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.remove(alarmId);
				ANIMATED_ALARMS.add(alarmId);
			}
			getAlarmAnimationThread(alarm, alarmDate).start();
			startNotification();
			startRunningNotification(alarm);
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_TEST_ALARM.equals(action)) {
			Logger.debugAlarm("Testing alarm for " + alarm.getName() + " at " + alarmDate);
			synchronized (ANIMATED_ALARMS) {
				ANIMATED_ALARMS.add(alarmId);
			}
			getAlarmAnimationThread(alarm, alarmDate).start();
			startNotification();
			startRunningNotification(alarm);
		}
		else {
			Log.e(Application.TAG, "Unexpected action: " + action);
		}

		return START_STICKY;
	}

	@Override
	public final void onDestroy() {
		super.onDestroy();
	}

	@Override
	public final IBinder onBind(final Intent intent) {
		return null;
	}

	/**
	 * Get an alarm animation thread.
	 *
	 * @param alarm     the alarm
	 * @param alarmDate the alarm date
	 * @return The animation thread
	 */
	private Thread getAlarmAnimationThread(final Alarm alarm, final Date alarmDate) {
		return new Thread() {
			@Override
			public void run() {
				// Clone steps, so that there will be no issues if alarm is stored while this is executed.
				List<Step> alarmSteps = new ArrayList<>(alarm.getSteps());
				Collections.sort(alarmSteps);

				final WakeLock wakeLock = acquireWakelock(alarm);

				Map<Light, List<Step>> lightStepMap = new HashMap<>();
				for (Step step : alarmSteps) {
					List<Step> lightSteps = lightStepMap.get(step.getStoredColor().getLight());
					if (lightSteps == null) {
						lightSteps = new ArrayList<>();
						lightStepMap.put(step.getStoredColor().getLight(), lightSteps);
					}
					lightSteps.add(step);
				}

				for (Light light : lightStepMap.keySet()) {
					light.endAnimation(false);
				}

				List<Thread> actionThreads = new ArrayList<>();

				for (final Entry<Light, List<Step>> entry : lightStepMap.entrySet()) {
					final Light light = entry.getKey();
					final List<Step> steps = entry.getValue();
					actionThreads.add(new Thread() {
						@Override
						public void run() {
							for (final Step step : steps) {
								long expectedStartTime = alarmDate.getTime() + step.getDelay();
								LifxAlarmService.sleep(expectedStartTime - System.currentTimeMillis());

								Logger.debugAlarm("Started " + light.getLabel() + " step " + step.getStoredColor().getName() + " - "
										+ String.format(Locale.getDefault(), "%1$tM:%1$tS", step.getDelay()));
								StoredColor storedColor = step.getStoredColor();

								// First check if power is on
								Power power = null;
								int count = 0;
								boolean success;
								while (power == null && count < ALARM_RETRY_COUNT) {
									power = light.getPower();
									count++;
								}

								if (power != null && power.isOff()) {
									count = 0;
									success = false;
									while (!success && count < ALARM_RETRY_COUNT) {
										try {
											if (storedColor instanceof StoredMultizoneColors) {
												StoredMultizoneColors storedMultizoneColors = (StoredMultizoneColors) storedColor;
												storedMultizoneColors.getLight().setColors(
														storedMultizoneColors.getColors().withRelativeBrightness(OFF_BRIGHTNESS), 0, false);
											}
											else if (storedColor instanceof StoredTileColors) {
												StoredTileColors storedTileColors = (StoredTileColors) storedColor;
												storedTileColors.getLight().setColors(
														storedTileColors.getColors().withRelativeBrightness(OFF_BRIGHTNESS), 0, false);
											}
											else {
												storedColor.getLight().setColor(storedColor.getColor().withBrightness(OFF_BRIGHTNESS));
											}
											light.setPower(true);
											success = true;
										}
										catch (IOException e) {
											Logger.error(e);
											count++;
										}
									}
								}

								count = 0;
								success = false;
								while (!success && count < ALARM_RETRY_COUNT) {
									int duration = (int) Math.max(0, step.getDuration() + expectedStartTime - System.currentTimeMillis());
									try {
										if (Color.OFF.equals(storedColor.getColor())) {
											// do delayed power off
											storedColor.getLight().setPower(false, duration, true);
											LifxAlarmService.sleep(OFF_TIME);
										}
										else {
											if (storedColor instanceof StoredMultizoneColors) {
												StoredMultizoneColors storedMultizoneColors = (StoredMultizoneColors) storedColor;
												storedMultizoneColors.getLight().setColors(storedMultizoneColors.getColors(), duration, true);
											}
											else if (storedColor instanceof StoredTileColors) {
												StoredTileColors storedTileColors = (StoredTileColors) storedColor;
												storedTileColors.getLight().setColors(storedTileColors.getColors(), duration, true);
											}
											else {
												storedColor.getLight().setColor(storedColor.getColor(), duration, true);
											}
										}
										success = true;
									}
									catch (IOException e) {
										Logger.error(e);
										count++;
									}
								}
							}
						}
					});
				}

				LifxAlarmService.sleep(alarmDate.getTime() - new Date().getTime());

				Logger.debugAlarm("Starting alarm threads");
				for (Thread thread : actionThreads) {
					thread.start();
				}
				Logger.debugAlarm("Started alarm threads");

				for (Thread thread : actionThreads) {
					try {
						thread.join();
					}
					catch (InterruptedException e) {
						// ignore
					}
				}
				Logger.debugAlarm("Finished alarm threads");

				updateOnEndAnimation(alarm, wakeLock);
			}
		};
	}

	/**
	 * Sleep certain time, ignoring interruption.
	 *
	 * @param millis the time in millis
	 */
	private static void sleep(final long millis) {
		if (millis > 0) {
			try {
				Thread.sleep(millis);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * Get a wakelock for an alarm and acquire it.
	 *
	 * @param alarm the alarm.
	 * @return The wakelock.
	 */
	private WakeLock acquireWakelock(final Alarm alarm) {
		if (PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_wakelock, true)) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			assert powerManager != null;
			WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.jeisfeld.lifx.alarm." + alarm.getId());
			wakelock.acquire(TimeUnit.HOURS.toMillis(2));
			return wakelock;
		}
		else {
			return null;
		}
	}

	/**
	 * Create the channel for service animation notifications.
	 */
	private void createNotificationChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager manager = getSystemService(NotificationManager.class);
			assert manager != null;
			manager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID,
					getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_LOW));
			manager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_EXECUTION,
					getString(R.string.notification_channel_alarm_execution), NotificationManager.IMPORTANCE_HIGH));
		}
	}

	/**
	 * Start the notification.
	 */
	private void startNotification() {
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				MainActivity.createIntent(this, R.id.nav_alarms), PendingIntent.FLAG_CANCEL_CURRENT);
		Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_alarm))
				.setContentText(getRunningAlarmsString())
				.setSmallIcon(R.drawable.ic_notification_icon_alarm)
				.setContentIntent(contentIntent)
				.build();
		startForeground(1, notification);
	}

	/**
	 * Start the running notification for a certain alarm.
	 *
	 * @param alarm The alarm.
	 */
	private void startRunningNotification(final Alarm alarm) {
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				MainActivity.createIntent(this, R.id.nav_alarms), PendingIntent.FLAG_CANCEL_CURRENT);
		PendingIntent stopIntent = PendingIntent.getActivity(this, 0,
				MainActivity.createIntent(this, R.id.nav_alarms), PendingIntent.FLAG_CANCEL_CURRENT);
		Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_EXECUTION)
				.setContentTitle(getString(R.string.notification_title_alarm_execution, alarm.getName()))
				.setSmallIcon(R.drawable.ic_notification_icon_alarm)
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(contentIntent)
				.addAction(R.drawable.ic_action_alarm_off, getString(R.string.notification_alarm_action_stop), stopIntent)
				.build();
		NotificationManager manager = getSystemService(NotificationManager.class);
		assert manager != null;
		manager.notify(NOTIFICATION_TAG_ALARM_EXECUTION, alarm.getId(), notification);
	}

	/**
	 * End the running notification for a certain alarm.
	 *
	 * @param alarm The alarm.
	 */
	private void endRunningNotification(final Alarm alarm) {
		NotificationManager manager = getSystemService(NotificationManager.class);
		assert manager != null;
		manager.cancel(NOTIFICATION_TAG_ALARM_EXECUTION, alarm.getId());
	}

	/**
	 * Update the service after an alarm animation has ended.
	 *
	 * @param alarm    The alarm
	 * @param wakeLock The wakelock on that light.
	 */
	private void updateOnEndAnimation(final Alarm alarm, final WakeLock wakeLock) {
		if (wakeLock != null) {
			wakeLock.release();
		}
		synchronized (ANIMATED_ALARMS) {
			ANIMATED_ALARMS.remove((Integer) alarm.getId());
			if (ANIMATED_ALARMS.size() == 0 && PENDING_ALARMS.size() == 0) {
				Intent serviceIntent = new Intent(this, LifxAlarmService.class);
				stopService(serviceIntent);
			}
			else {
				startNotification();
			}
			endRunningNotification(alarm);
		}
	}

	/**
	 * Get a display String for all animated devices.
	 *
	 * @return a display String for all animated devices.
	 */
	public String getRunningAlarmsString() {
		StringBuilder builder = new StringBuilder();
		if (PENDING_ALARMS.size() > 0) {
			for (Integer alarmId : PENDING_ALARMS) {
				if (builder.length() > 0) {
					builder.append(", ");
				}
				builder.append(new Alarm(alarmId).getName());
			}
			return getString(R.string.notification_text_alarm, builder.toString());
		}
		else {
			return getString(R.string.notification_text_no_alarm);
		}
	}

}
