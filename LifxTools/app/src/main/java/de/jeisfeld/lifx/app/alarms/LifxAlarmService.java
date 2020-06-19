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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import de.jeisfeld.lifx.app.util.ImageUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationCallback;
import de.jeisfeld.lifx.lan.Light.AnimationThread;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.MultiZoneLight.AnimationDefinition;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.TileChainColors;
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
	 * Action for interrupting an alarm.
	 */
	protected static final String ACTION_INTERRUPT_ALARM = "de.jeisfeld.lifx.app.ACTION_INTERRUPT_ALARM";
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
		ContextCompat.startForegroundService(context, createIntent(context, action, alarmId, alarmTime));
	}

	/**
	 * Create an intent for alarm service.
	 *
	 * @param context   The context.
	 * @param action    the action.
	 * @param alarmId   the alarm id.
	 * @param alarmTime the alarm time.
	 * @return the intent.
	 */
	private static Intent createIntent(final Context context, final String action, final int alarmId, final Date alarmTime) {
		Intent intent = new Intent(context, LifxAlarmService.class);
		intent.setAction(action);
		intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
		if (alarmTime != null) {
			intent.putExtra(AlarmReceiver.EXTRA_ALARM_TIME, alarmTime);
		}
		return intent;
	}

	@Override
	public final void onCreate() {
		super.onCreate();
		createNotificationChannels();
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		final String action = intent.getAction();
		final int alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);
		final Date alarmDate = (Date) intent.getSerializableExtra(AlarmReceiver.EXTRA_ALARM_TIME);
		Alarm alarm = new Alarm(alarmId);

		if (ACTION_CREATE_ALARM.equals(action)) {
			synchronized (PENDING_ALARMS) {
				PENDING_ALARMS.add(alarmId);
			}
			startNotification();
		}
		else if (ACTION_CANCEL_ALARM.equals(action)) {
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
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.remove(alarmId);
			}
			runAnimations(alarm, alarmDate);
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_IMMEDIATE_ALARM.equals(action)) {
			runAnimations(alarm, alarmDate);
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_TEST_ALARM.equals(action)) {
			runAnimations(alarm, alarmDate);
		}
		else if (ACTION_INTERRUPT_ALARM.equals(action)) {
			interruptAlarm(alarm);
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
	 * Run the animations for an alarm.
	 *
	 * @param alarm     the alarm
	 * @param alarmDate the alarm date
	 */
	private void runAnimations(final Alarm alarm, final Date alarmDate) {
		synchronized (ANIMATED_ALARMS) {
			ANIMATED_ALARMS.add(alarm.getId());
		}
		for (AnimationThread animationThread : getAnimationThreads(alarm, alarmDate)) {
			animationThread.start();
		}
		Logger.info("Started Alarm " + alarm.getName());

		startNotification();
		startRunningNotification(alarm);
	}

	/**
	 * Get the animation threads for an alarm.
	 *
	 * @param alarm     The alarm
	 * @param alarmDate The alarm start date
	 * @return The animation threads
	 */
	private List<AnimationThread> getAnimationThreads(final Alarm alarm, final Date alarmDate) {
		final WakeLock wakeLock = acquireWakelock(alarm);

		final Map<Light, List<Step>> lightStepMap = alarm.getLightStepMap();
		for (Light light : lightStepMap.keySet()) {
			light.endAnimation(false);
		}

		final List<AnimationThread> animationThreads = new ArrayList<>();
		final List<Light> animatedLights = new ArrayList<>();

		for (final Entry<Light, List<Step>> entry : lightStepMap.entrySet()) {
			final Light light = entry.getKey();
			final List<Step> steps = entry.getValue();
			animatedLights.add(light);

			AnimationThread animationThread = light.animation(getAnimationDefiniton(alarmDate, light, steps))
					.setAnimationCallback(new AnimationCallback() {
						@Override
						public void onException(final IOException e) {
							Logger.info("Finished alarm threads on " + light.getLabel() + " with Exception " + e.getMessage());
							updateOnEndAnimation(alarm, wakeLock, light, animatedLights);
						}

						@Override
						public void onAnimationEnd(final boolean isInterrupted) {
							Logger.info("Finished alarm threads on " + light.getLabel() + (isInterrupted ? " with interruption" : ""));
							updateOnEndAnimation(alarm, wakeLock, light, animatedLights);
						}
					});
			animationThreads.add(animationThread);
		}
		return animationThreads;
	}

	/**
	 * Create the animation definition for a certain light.
	 *
	 * @param alarmDate The alarm start date
	 * @param light     The light.
	 * @param steps     The steps.
	 * @return The animation definition.
	 */
	private Light.AnimationDefinition getAnimationDefiniton(final Date alarmDate, final Light light, final List<Step> steps) {
		if (light instanceof MultiZoneLight) {
			return new AnimationDefinition() {
				@Override
				public MultizoneColors getColors(final int n) {
					if (n >= steps.size()) {
						return null;
					}
					StoredColor storedColor = steps.get(n).getStoredColor();
					if (storedColor instanceof StoredMultizoneColors) {
						return ((StoredMultizoneColors) storedColor).getColors();
					}
					else {
						return new MultizoneColors.Fixed(storedColor.getColor());
					}
				}

				@Override
				public int getDuration(final int n) {
					return n >= steps.size() ? 0 : (int) steps.get(n).getDuration();
				}

				@Override
				public Date getStartTime(final int n) {
					return n >= steps.size() ? null : new Date(alarmDate.getTime() + steps.get(n).getDelay());
				}
			};
		}
		else if (light instanceof TileChain) {
			return new TileChain.AnimationDefinition() {
				@Override
				public TileChainColors getColors(final int n) {
					if (n >= steps.size()) {
						return null;
					}
					StoredColor storedColor = steps.get(n).getStoredColor();
					if (storedColor instanceof StoredTileColors) {
						return ((StoredTileColors) storedColor).getColors();
					}
					else {
						return new TileChainColors.Fixed(storedColor.getColor());
					}
				}

				@Override
				public int getDuration(final int n) {
					return n >= steps.size() ? 0 : (int) steps.get(n).getDuration();
				}

				@Override
				public Date getStartTime(final int n) {
					return n >= steps.size() ? null : new Date(alarmDate.getTime() + steps.get(n).getDelay());
				}
			};
		}
		else {
			return new Light.AnimationDefinition() {
				@Override
				public Color getColor(final int n) {
					return n >= steps.size() ? null : steps.get(n).getStoredColor().getColor();
				}

				@Override
				public int getDuration(final int n) {
					return n >= steps.size() ? 0 : (int) steps.get(n).getDuration();
				}

				@Override
				public Date getStartTime(final int n) {
					return n >= steps.size() ? null : new Date(alarmDate.getTime() + steps.get(n).getDelay());
				}
			};
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
			WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.jeisfeld.lifx.alarm." + System.currentTimeMillis());
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
		PendingIntent stopIntent = PendingIntent.getService(this, 0,
				LifxAlarmService.createIntent(this, ACTION_INTERRUPT_ALARM, alarm.getId(), null), PendingIntent.FLAG_CANCEL_CURRENT);
		Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_EXECUTION)
				.setContentTitle(getString(R.string.notification_title_alarm_execution, alarm.getName()))
				.setSmallIcon(R.drawable.ic_notification_icon_alarm)
				.setLargeIcon(ImageUtil.createBitmapFromDrawable(this, R.drawable.ic_notification_icon_large_alarm))
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
	 * End the animation for an alarm.
	 *
	 * @param alarm The alarm.
	 */
	private void interruptAlarm(final Alarm alarm) {
		for (Light light : alarm.getLightStepMap().keySet()) {
			light.endAnimation(false);
		}
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
	 * @param alarm          The alarm
	 * @param wakeLock       The wakelock on that light
	 * @param light          The light
	 * @param animatedLights The list of animated lights
	 */
	private void updateOnEndAnimation(final Alarm alarm, final WakeLock wakeLock, final Light light, final List<Light> animatedLights) {
		animatedLights.remove(light);
		if (wakeLock != null && animatedLights.size() == 0) {
			wakeLock.release();
			synchronized (ANIMATED_ALARMS) {
				ANIMATED_ALARMS.remove((Integer) alarm.getId());
				if (ANIMATED_ALARMS.size() == 0 && PENDING_ALARMS.size() == 0) {
					Intent serviceIntent = new Intent(this, LifxAlarmService.class);
					stopService(serviceIntent);
				}
				else {
					startNotification();
				}
				if (!ANIMATED_ALARMS.contains(alarm.getId())) {
					endRunningNotification(alarm);
				}
			}
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
