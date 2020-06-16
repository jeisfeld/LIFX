package de.jeisfeld.lifx.app.alarms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

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
	 * The id of the notification channel.
	 */
	public static final String CHANNEL_ID = "LifxAlarmChannel";
	/**
	 * Key for the alarm id within the intent.
	 */
	public static final String EXTRA_ALARM_ID = "de.jeisfeld.lifx.ALARM_ID";
	/**
	 * Key for the alarm id within the intent.
	 */
	public static final String EXTRA_ALARM_TIME = "de.jeisfeld.lifx.ALARM_TIME";
	/**
	 * The retry count for alarms.
	 */
	private static final int ALARM_RETRY_COUNT = 3;
	/**
	 * Map from alarm Ids to Light labels for all lights with running animations.
	 */
	private static final List<Alarm> ANIMATED_ALARMS = new ArrayList<>();

	@Override
	public final void onCreate() {
		super.onCreate();
		Logger.log("Created LifxAlarmService");
		createNotificationChannel();
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		final int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);

		final Date alarmDate = (Date) intent.getSerializableExtra(EXTRA_ALARM_TIME);
		Alarm alarm = new Alarm(alarmId);
		Logger.log("Started service for " + alarmDate);

		synchronized (ANIMATED_ALARMS) {
			ANIMATED_ALARMS.add(alarm);
		}

		startNotification();
		getAlarmAnimationThread(alarm, alarmDate).start();

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
				List<Step> alarmSteps = alarm.getSteps();
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
							Logger.log("Started step " + String.format(Locale.getDefault(), "%1$tM:%1$tS", step.getDelay()));

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
				Logger.log("Waiting " + waitTime + " milliseconds");
				if (waitTime > 0) {
					try {
						Thread.sleep(waitTime);
					}
					catch (InterruptedException e) {
						// ignore
					}
				}

				Logger.log("Starting alarm threads");
				for (Thread thread : actionThreads) {
					thread.start();
				}
				Logger.log("Started alarm threads");

				for (Thread thread : actionThreads) {
					try {
						thread.join();
					}
					catch (InterruptedException e) {
						// ignore
					}
				}
				Logger.log("Finished alarm threads");

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
				.setContentText(getString(R.string.notification_text_alarm_running, getAnimatedDevicesString()))
				.setSmallIcon(R.drawable.ic_notification_icon_logo)
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
			ANIMATED_ALARMS.remove(alarm);
			if (ANIMATED_ALARMS.size() == 0) {
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
	public static String getAnimatedDevicesString() {
		StringBuilder builder = new StringBuilder();
		for (Alarm alarm : ANIMATED_ALARMS) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(alarm.getName());
		}
		return builder.toString();
	}

}
