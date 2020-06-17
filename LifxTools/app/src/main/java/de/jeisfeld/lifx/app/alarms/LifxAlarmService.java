package de.jeisfeld.lifx.app.alarms;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
	 * The id of the notification channel.
	 */
	private static final String CHANNEL_ID = "LifxAlarmChannel";
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
		createNotificationChannel();
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
				PENDING_ALARMS.remove((Integer) alarmId);
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
				PENDING_ALARMS.remove((Integer) alarmId);
				ANIMATED_ALARMS.add(alarmId);
			}
			getAlarmAnimationThread(alarm, alarmDate).start();
			startNotification();
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_IMMEDIATE_ALARM.equals(action)) {
			Logger.debugAlarm("Immediately started alarm for " + alarm.getName() + " at " + alarmDate);
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.remove((Integer) alarmId);
				ANIMATED_ALARMS.add(alarmId);
			}
			getAlarmAnimationThread(alarm, alarmDate).start();
			startNotification();
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_TEST_ALARM.equals(action)) {
			Logger.debugAlarm("Testing alarm for " + alarm.getName() + " at " + alarmDate);
			synchronized (ANIMATED_ALARMS) {
				ANIMATED_ALARMS.add(alarmId);
			}
			getAlarmAnimationThread(alarm, alarmDate).start();
			startNotification();
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

				Set<Light> lights = new HashSet<>();
				for (Step step : alarmSteps) {
					lights.add(step.getStoredColor().getLight());
				}

				for (Light light : lights) {
					light.endAnimation(false);
				}

				List<Thread> actionThreads = new ArrayList<>();

				for (Step step : alarmSteps) {
					actionThreads.add(new Thread() {
						@Override
						public void run() {
							try {
								Thread.sleep(step.getDelay());
							}
							catch (InterruptedException e) {
								// ignore
							}
							Logger.debugAlarm("Started step " + String.format(Locale.getDefault(), "%1$tM:%1$tS", step.getDelay()));

							StoredColor storedColor = step.getStoredColor();
							Light light = storedColor.getLight();

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
											storedMultizoneColors.getLight().setColors(0, false,
													storedMultizoneColors.getColors().withRelativeBrightness(0.01)); // MAGIC_NUMBER
										}
										else if (storedColor instanceof StoredTileColors) {
											StoredTileColors storedTileColors = (StoredTileColors) storedColor;
											storedTileColors.getLight().setColors(0,
													storedTileColors.getColors().withRelativeBrightness(0.01)); // MAGIC_NUMBER
										}
										else {
											storedColor.getLight().setColor(storedColor.getColor().withBrightness(0.01)); // MAGIC_NUMBER
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
								try {
									if (storedColor instanceof StoredMultizoneColors) {
										StoredMultizoneColors storedMultizoneColors = (StoredMultizoneColors) storedColor;
										storedMultizoneColors.getLight().setColors((int) step.getDuration(), false,
												storedMultizoneColors.getColors());
									}
									else if (storedColor instanceof StoredTileColors) {
										StoredTileColors storedTileColors = (StoredTileColors) storedColor;
										storedTileColors.getLight().setColors((int) step.getDuration(), storedTileColors.getColors());
									}
									else {
										storedColor.getLight().setColor(storedColor.getColor(), (int) step.getDuration(), false);
									}
									success = true;
								}
								catch (IOException e) {
									Logger.error(e);
									count++;
								}
							}
						}
					});
				}

				long waitTime = alarmDate.getTime() - new Date().getTime();
				Logger.debugAlarm("Waiting " + waitTime + " milliseconds");
				if (waitTime > 0) {
					try {
						Thread.sleep(waitTime);
					}
					catch (InterruptedException e) {
						// ignore
					}
				}

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
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel animationChannel = new NotificationChannel(
					CHANNEL_ID, getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager manager = getSystemService(NotificationManager.class);
			assert manager != null;
			manager.createNotificationChannel(animationChannel);
		}
	}

	/**
	 * Start the notification.
	 */
	private void startNotification() {
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_alarm))
				.setContentText(getAnimatedDevicesString())
				.setSmallIcon(R.drawable.ic_notification_icon_alarm)
				.setContentIntent(pendingIntent)
				.build();
		startForeground(1, notification);
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
		}
	}

	/**
	 * Get a display String for all animated devices.
	 *
	 * @return a display String for all animated devices.
	 */
	public String getAnimatedDevicesString() {
		StringBuilder builder = new StringBuilder();
		if (ANIMATED_ALARMS.size() > 0) {
			StringBuilder runningBuilder = new StringBuilder();
			for (Integer alarmId : ANIMATED_ALARMS) {
				if (runningBuilder.length() > 0) {
					runningBuilder.append(", ");
				}
				runningBuilder.append(new Alarm(alarmId).getName());
			}
			builder.append(getString(R.string.notification_text_alarm_running, runningBuilder.toString()));
		}
		if (PENDING_ALARMS.size() > 0) {
			if (ANIMATED_ALARMS.size() > 0) {
				builder.append(", ");
			}
			StringBuilder pendingBuilder = new StringBuilder();
			for (Integer alarmId : PENDING_ALARMS) {
				if (pendingBuilder.length() > 0) {
					pendingBuilder.append(", ");
				}
				pendingBuilder.append(new Alarm(alarmId).getName());
			}
			builder.append(getString(R.string.notification_text_alarm_pending, pendingBuilder.toString()));
		}
		return builder.toString();
	}

}
