package de.jeisfeld.lifx.app.alarms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.content.ContextCompat;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.MainActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.AlarmType;
import de.jeisfeld.lifx.app.alarms.Alarm.LightSteps;
import de.jeisfeld.lifx.app.alarms.Alarm.RingtoneStep;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.app.util.ImageUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationCallback;
import de.jeisfeld.lifx.lan.Light.BaseAnimationThread;
import de.jeisfeld.lifx.lan.MultiZoneLight;
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
	 * The request code for the main notification.
	 */
	private static final int REQUEST_CODE = -1;
	/**
	 * The id for the service.
	 */
	private static final int SERVICE_ID = 1;
	/**
	 * The duration of the alarm in case of wait for manual stop, if no one stops.
	 */
	private static final int STOP_MANUAL_DURATION = (int) TimeUnit.DAYS.toMillis(1);
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
	 * Map from alarmId to Alarm for pending alarms.
	 */
	private static final Map<Integer, Alarm> PENDING_ALARMS = new HashMap<>();

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
	 * @param context The context.
	 * @param action the action.
	 * @param alarmId the alarm id.
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
		alarm = new Alarm(alarm.getId(), alarm.isActive(), alarmDate, alarm.getWeekDays(), alarm.getName(), alarm.getSteps(),
				alarm.getAlarmType(), alarm.getStopSequence());
		Logger.info("LifxAlarmService start " + action + " - " + alarm.getName());

		if (ACTION_CREATE_ALARM.equals(action)) {
			synchronized (PENDING_ALARMS) {
				PENDING_ALARMS.put(alarmId, alarm);
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
					Logger.info("LifxAlarmService stop after cancel - no alarms left");
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
	 * @param alarm the alarm
	 * @param alarmDate the alarm date
	 */
	private void runAnimations(final Alarm alarm, final Date alarmDate) {
		synchronized (ANIMATED_ALARMS) {
			ANIMATED_ALARMS.add(alarm.getId());
		}
		for (BaseAnimationThread animationThread : getAnimationThreads(alarm, alarmDate)) {
			animationThread.start();
		}

		startNotification();
		startRunningNotification(alarm);
	}

	/**
	 * Get the animation threads for an alarm.
	 *
	 * @param alarm The alarm
	 * @param alarmDate The alarm start date
	 * @return The animation threads
	 */
	private List<BaseAnimationThread> getAnimationThreads(final Alarm alarm, final Date alarmDate) {
		final WakeLock wakeLock = acquireWakelock(alarm);

		final List<LightSteps> lightStepsList = alarm.getLightSteps();
		final List<BaseAnimationThread> animationThreads = new ArrayList<>();
		final List<Light> animatedLights = new ArrayList<>();

		for (final LightSteps lightSteps : lightStepsList) {
			if (lightSteps.getSteps().size() == 0) {
				continue;
			}

			final Light light = lightSteps.getLight();
			animatedLights.add(light);

			AnimationCallback callback = new AnimationCallback() {
				@Override
				public void onException(final IOException e) {
					Logger.debug("Finished alarm threads on " + light.getLabel() + " with Exception " + e.getMessage());
					updateOnEndAnimation(alarm, wakeLock, light, animatedLights);
				}

				@Override
				public void onAnimationEnd(final boolean isInterrupted) {
					Logger.debug("Finished alarm threads on " + light.getLabel() + (isInterrupted ? " with interruption" : ""));
					updateOnEndAnimation(alarm, wakeLock, light, animatedLights);
				}
			};

			if (DeviceRegistry.getInstance().getRingtoneDummyLight().equals(light)) {
				animationThreads.add(new RingtoneAnimationThread(
						(RingtoneAnimationDefinition) getAnimationDefiniton(alarm, alarmDate, light, lightSteps.getSteps()))
						.setAnimationCallback(callback));
			}
			else {
				animationThreads.add(light.animation(getAnimationDefiniton(alarm, alarmDate, light, lightSteps.getSteps()))
						.setAnimationCallback(callback));
			}
		}
		return animationThreads;
	}

	/**
	 * Create the animation definition for a certain light.
	 *
	 * @param alarm The alarm.
	 * @param alarmDate The alarm start date
	 * @param light The light.
	 * @param steps The steps.
	 * @return The animation definition.
	 */
	private Light.AnimationDefinition getAnimationDefiniton(final Alarm alarm, final Date alarmDate, final Light light, final List<Step> steps) {
		final Light.AnimationDefinition baseDefinition = new Light.AnimationDefinition() {
			@Override
			public Color getColor(final int n) {
				if (n < steps.size() || alarm.getAlarmType() == AlarmType.CYCLIC) {
					return steps.get(n % steps.size()).getStoredColor().getColor();
				}
				else if (n == steps.size() && alarm.getAlarmType() == AlarmType.STOP_MANUALLY) {
					return Color.OFF;
				}
				else {
					return null;
				}
			}

			@Override
			public int getDuration(final int n) {
				return (int) steps.get(n % steps.size()).getDuration();
			}

			@Override
			public Date getStartTime(final int n) {
				switch (alarm.getAlarmType()) {
				case CYCLIC:
					return new Date(alarmDate.getTime() + (n / steps.size()) * alarm.getDuration() + steps.get(n % steps.size()).getDelay());
				case STOP_MANUALLY:
					// Maintain max. one day
					return n < steps.size() ? new Date(alarmDate.getTime() + steps.get(n).getDelay())
							: new Date(System.currentTimeMillis() + STOP_MANUAL_DURATION);
				default:
					return n >= steps.size() ? null : new Date(alarmDate.getTime() + steps.get(n).getDelay());
				}
			}

			@Override
			public boolean waitForPreviousAnimationEnd() {
				return true;
			}
		};

		if (DeviceRegistry.getInstance().getRingtoneDummyLight().equals(light)) {
			return new RingtoneAnimationDefinition() {
				@Override
				public Ringtone getRingtone(final int n) {
					if (n < steps.size() || alarm.getAlarmType() == AlarmType.CYCLIC) {
						return ((RingtoneStep) steps.get(n % steps.size())).getRingtone(LifxAlarmService.this);
					}
					else {
						return null;
					}
				}

				@Override
				public Color getColor(final int n) {
					return null;
				}

				@Override
				public int getDuration(final int n) {
					if (n == steps.size() - 1 && alarm.getAlarmType() == AlarmType.STOP_MANUALLY) {
						return STOP_MANUAL_DURATION;
					}
					else {
						return baseDefinition.getDuration(n);
					}
				}

				@Override
				public Date getStartTime(final int n) {
					return baseDefinition.getStartTime(n);
				}
			};
		}
		else if (light instanceof MultiZoneLight) {
			return new MultiZoneLight.AnimationDefinition() {
				@Override
				public MultizoneColors getColors(final int n) {
					if (n < steps.size() || alarm.getAlarmType() == AlarmType.CYCLIC) {
						StoredColor storedColor = steps.get(n % steps.size()).getStoredColor();
						if (storedColor instanceof StoredMultizoneColors) {
							return ((StoredMultizoneColors) storedColor).getColors();
						}
						else {
							return new MultizoneColors.Fixed(storedColor.getColor());
						}
					}
					else if (n == steps.size() && alarm.getAlarmType() == AlarmType.STOP_MANUALLY) {
						return MultizoneColors.OFF;
					}
					else {
						return null;
					}
				}

				@Override
				public int getDuration(final int n) {
					return baseDefinition.getDuration(n);
				}

				@Override
				public Date getStartTime(final int n) {
					return baseDefinition.getStartTime(n);
				}
			};
		}
		else if (light instanceof TileChain) {
			return new TileChain.AnimationDefinition() {
				@Override
				public TileChainColors getColors(final int n) {
					if (n < steps.size() || alarm.getAlarmType() == AlarmType.CYCLIC) {
						StoredColor storedColor = steps.get(n % steps.size()).getStoredColor();
						if (storedColor instanceof StoredTileColors) {
							return ((StoredTileColors) storedColor).getColors();
						}
						else {
							return new TileChainColors.Fixed(storedColor.getColor());
						}
					}
					else if (n == steps.size() && alarm.getAlarmType() == AlarmType.STOP_MANUALLY) {
						return TileChainColors.OFF;
					}
					else {
						return null;
					}
				}

				@Override
				public int getDuration(final int n) {
					return baseDefinition.getDuration(n);
				}

				@Override
				public Date getStartTime(final int n) {
					return baseDefinition.getStartTime(n);
				}
			};
		}
		else {
			return baseDefinition;
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
		PendingIntent contentIntent = PendingIntent.getActivity(this, REQUEST_CODE,
				MainActivity.createIntent(this, R.id.nav_alarms), PendingIntent.FLAG_CANCEL_CURRENT);
		String notificationMessage = getRunningAlarmsString();
		Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_alarm))
				.setStyle(new BigTextStyle().bigText(notificationMessage))
				.setContentText(notificationMessage)
				.setSmallIcon(R.drawable.ic_notification_icon_alarm)
				.setContentIntent(contentIntent)
				.build();
		startForeground(SERVICE_ID, notification);
	}

	/**
	 * Start the running notification for a certain alarm.
	 *
	 * @param alarm The alarm.
	 */
	private void startRunningNotification(final Alarm alarm) {
		PendingIntent contentIntent = PendingIntent.getActivity(this, alarm.getId(),
				MainActivity.createIntent(this, R.id.nav_alarms), PendingIntent.FLAG_IMMUTABLE);
		PendingIntent stopIntent = PendingIntent.getService(this, alarm.getId(),
				LifxAlarmService.createIntent(this, ACTION_INTERRUPT_ALARM, alarm.getId(), null), PendingIntent.FLAG_IMMUTABLE);
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
		for (LightSteps lightSteps : alarm.getLightSteps()) {
			lightSteps.getLight().endAnimation(false);
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
	 * @param alarm The alarm
	 * @param wakeLock The wakelock on that light
	 * @param light The light
	 * @param animatedLights The list of animated lights
	 */
	private void updateOnEndAnimation(final Alarm alarm, final WakeLock wakeLock, final Light light, final List<Light> animatedLights) {
		boolean isLastLight;
		// noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (animatedLights) {
			animatedLights.remove(light);
			isLastLight = animatedLights.size() == 0;
		}
		Logger.debug("LifxAlarmService end (" + alarm.getName() + "," + light.getLabel() + ") (" + animatedLights.size() + ")");

		if (isLastLight) {
			if (wakeLock != null) {
				wakeLock.release();
			}
			synchronized (ANIMATED_ALARMS) {
				ANIMATED_ALARMS.remove((Integer) alarm.getId());
				Logger.debug("LifxAlarmService end (" + ANIMATED_ALARMS.size() + "," + PENDING_ALARMS.size() + ")");
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
				if (alarm.getStopSequence() != null && alarm.getStopSequence().isActive()) {
					triggerAlarmService(this, ACTION_TRIGGER_ALARM, alarm.getStopSequence().getId(), new Date());
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
			List<Alarm> pendingAlarms = new ArrayList<>(PENDING_ALARMS.values());
			pendingAlarms.sort((o1, o2) -> o1.getStartTime().compareTo(o2.getStartTime()));
			String dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEHHmm");
			for (Alarm alarm : pendingAlarms) {
				DateFormat.format(dateFormat, alarm.getStartTime());
				builder.append(getString(R.string.notification_text_alarm, alarm.getName(), DateFormat.format(dateFormat, alarm.getStartTime())));
			}
			// delete final linebreak
			builder.deleteCharAt(builder.length() - 1);
			return builder.toString();
		}
		else {
			return getString(R.string.notification_text_no_alarm);
		}
	}

	/**
	 * A thread handling ringtone animation.
	 */
	public static class RingtoneAnimationThread extends BaseAnimationThread {
		/**
		 * The animation definiation.
		 */
		private RingtoneAnimationDefinition mDefinition;
		/**
		 * An exception callback called in case of SocketException.
		 */
		private AnimationCallback mAnimationCallback = null;

		/**
		 * Create an animation thread.
		 *
		 * @param definition The rules for the animation.
		 */
		protected RingtoneAnimationThread(final RingtoneAnimationDefinition definition) {
			super(DeviceRegistry.getInstance().getRingtoneDummyLight());
			setDefinition(definition);
		}

		/**
		 * Set the animation definition.
		 *
		 * @param definition The rules for the animation.
		 */
		protected void setDefinition(final RingtoneAnimationDefinition definition) {
			mDefinition = definition;
		}

		/**
		 * Set the exception callback called in case of Exception.
		 *
		 * @param callback The callback.
		 * @return The updated animation thread.
		 */
		public RingtoneAnimationThread setAnimationCallback(final AnimationCallback callback) {
			mAnimationCallback = callback;
			return this;
		}

		// OVERRIDABLE
		@Override
		public void run() {
			int count = 0;
			if (mDefinition.waitForPreviousAnimationEnd()) {
				waitForPreviousAnimationEnd();
			}
			boolean isInterrupted = false;
			Ringtone ringtone = null;
			try {
				while (!isInterrupted() && mDefinition.getRingtone(count) != null) {
					ringtone = mDefinition.getRingtone(count);
					Date givenStartTime = mDefinition.getStartTime(count);
					final long startTime = givenStartTime == null ? System.currentTimeMillis() : givenStartTime.getTime();
					if (givenStartTime != null) {
						long waitTime = givenStartTime.getTime() - System.currentTimeMillis();
						if (waitTime > 0) {
							Thread.sleep(waitTime);
						}
					}

					ringtone.play();
					int duration = Math.max(mDefinition.getDuration(count), 0);
					Thread.sleep(Math.max(0, duration + startTime - System.currentTimeMillis()));
					ringtone.stop();
					count++;
				}
			}
			catch (InterruptedException e) {
				isInterrupted = true;
				if (ringtone != null) {
					ringtone.stop();
				}
			}

			if (getAnimationCallback() != null) {
				getAnimationCallback().onAnimationEnd(isInterrupted);
			}
		}

		/**
		 * Get the exception callback.
		 *
		 * @return The exception callback.
		 */
		protected AnimationCallback getAnimationCallback() {
			return mAnimationCallback;
		}
	}

	/**
	 * Interface for defining an animation.
	 */
	public interface RingtoneAnimationDefinition extends Light.AnimationDefinition {
		/**
		 * The n-th ringtone of the animation.
		 *
		 * @param n counter starting with 0
		 * @return The n-th ringtone. Null will end the animation.
		 */
		Ringtone getRingtone(int n);
	}
}
