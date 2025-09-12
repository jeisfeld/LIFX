package de.jeisfeld.lifx.app.alarms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import de.jeisfeld.lifx.app.animation.LifxAnimationService;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.scenes.Scene;
import de.jeisfeld.lifx.app.scenes.SceneRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredAnimation;
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
import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
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
	 * Action for testing a scene.
	 */
	public static final String ACTION_TEST_SCENE = "de.jeisfeld.lifx.app.ACTION_TEST_SCENE";
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
	 * Map from alarm id to running animation threads.
	 */
	private static final Map<Integer, List<Thread>> RUNNING_THREADS = new HashMap<>();
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
	public static void triggerAlarmService(final Context context, final String action, final int alarmId, final Date alarmTime) {
		if (ACTION_CANCEL_ALARM.equals(action) || ACTION_INTERRUPT_ALARM.equals(action)) {
			context.startService(createIntent(context, action, alarmId, alarmTime));
		}
		else {
			ContextCompat.startForegroundService(context, createIntent(context, action, alarmId, alarmTime));
		}
	}

	/**
	 * Check if the alarm of certain id is pending.
	 *
	 * @param alarmId The alarmId
	 * @return true if this alarm is pending.
	 */
	protected static boolean isAlarmPending(final int alarmId) {
		return PENDING_ALARMS.containsKey(alarmId);
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
		if (intent == null) {
			return START_NOT_STICKY;
		}
		final String action = intent.getAction();
		final int alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);
		final Date alarmDate = (Date) intent.getSerializableExtra(AlarmReceiver.EXTRA_ALARM_TIME);
		Alarm alarm = new Alarm(alarmId);
		alarm = new Alarm(alarm.getId(), alarm.isActive(), alarmDate, alarm.getWeekDays(), alarm.getName(), alarm.getSteps(),
				alarm.getAlarmType(), alarm.getStopSequence(), alarm.isMaximizeVolume());
		Logger.info("LifxAlarmService start " + action + " - " + alarm.getName() + (alarmDate == null ? "" : " for " + alarmDate));

		if (ACTION_CREATE_ALARM.equals(action)) {
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.put(alarmId, alarm);
				startNotification();
			}
		}
		else if (ACTION_CANCEL_ALARM.equals(action)) {
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.remove(alarmId);
				startNotification();
			}
		}
		else if (ACTION_TRIGGER_ALARM.equals(action)) {
			synchronized (ANIMATED_ALARMS) {
				PENDING_ALARMS.remove(alarmId);
			}
			runAnimations(alarm, alarmDate, false);
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_IMMEDIATE_ALARM.equals(action)) {
			runAnimations(alarm, alarmDate, false);
			AlarmReceiver.retriggerAlarm(this, alarm);
		}
		else if (ACTION_TEST_ALARM.equals(action)) {
			runAnimations(alarm, alarmDate, false);
		}
		else if (ACTION_TEST_SCENE.equals(action)) {
			Scene scene = SceneRegistry.getInstance().getScene(alarmId);
			Alarm sceneAlarm = new Alarm(scene.getId(), true, alarmDate,
					new HashSet<>(), scene.getName(),
					scene.getSteps().stream().map(s -> new Step(s.getId(), s.getDelay(), s.getStoredColorId(), s.getDuration())).collect(Collectors.toList()),
					AlarmType.STANDARD, null, false);
			runAnimations(sceneAlarm, alarmDate, true);
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
	 * Run the animations for an alarm or scene.
	 *
	 * @param alarm     the alarm
	 * @param alarmDate the alarm date
	 * @param isScene   true if triggered for a scene
	 */
	private void runAnimations(final Alarm alarm, final Date alarmDate, final boolean isScene) {
		List<Thread> threads = getAnimationThreads(alarm, alarmDate);
		synchronized (RUNNING_THREADS) {
			RUNNING_THREADS.put(alarm.getId(), threads);
		}

		synchronized (ANIMATED_ALARMS) {
			ANIMATED_ALARMS.add(alarm.getId());
			startNotification();
		}

		for (Thread animationThread : threads) {
			animationThread.start();
		}

		startRunningNotification(alarm, isScene);
	}

	/**
	 * Get the animation threads for an alarm.
	 *
	 * @param alarm     The alarm
	 * @param alarmDate The alarm start date
	 * @return The animation threads
	 */
	private List<Thread> getAnimationThreads(final Alarm alarm, final Date alarmDate) {
		final WakeLock wakeLock = acquireWakelock(alarm);

		final List<LightSteps> lightStepsList = alarm.getLightSteps();
		final List<Thread> animationThreads = new ArrayList<>();
		final List<Light> animatedLights = new ArrayList<>();

		for (final LightSteps lightSteps : lightStepsList) {
			if (lightSteps.getSteps().isEmpty()) {
				continue;
			}

			final Light light = lightSteps.getLight();
			animatedLights.add(light);
			if (light instanceof TileChain || light instanceof MultiZoneLight) {
				Thread disableEffectThread = new Thread(() -> {
					try {
						if (light instanceof TileChain) {
							((TileChain) light).setEffect(TileEffectInfo.OFF);
						}
						else {
							((MultiZoneLight) light).setEffect(MultizoneEffectInfo.OFF);
						}
					}
					catch (IOException e) {
						Log.w(Application.TAG, e);
					}
				});
				disableEffectThread.start();
				try {
					disableEffectThread.join();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

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

			boolean hasStoredAnimation = false;
			for (Step step : lightSteps.getSteps()) {
				if (step.getStoredColor() instanceof StoredAnimation) {
					hasStoredAnimation = true;
					break;
				}
			}

                        if (hasStoredAnimation) {
                                animationThreads.add(new StoredAnimationThread(light, lightSteps.getSteps(), alarmDate,
                                                alarm.getAlarmType(), alarm.getDuration()).setAnimationCallback(callback));
                        }
			else if (DeviceRegistry.getInstance().getRingtoneDummyLight().equals(light)) {
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
	 * @param alarm     The alarm.
	 * @param alarmDate The alarm start date
	 * @param light     The light.
	 * @param steps     The steps.
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
				public boolean maximizeVolume() {
					return alarm.isMaximizeVolume();
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
	 * Start the notification, or stop it if not required any more.
	 */
	private void startNotification() {
		if (ANIMATED_ALARMS.isEmpty() && PENDING_ALARMS.isEmpty()) {
			Logger.info("LifxAlarmService stop - no alarms left");
			stopForeground(true);
			stopSelf();
			// sometimes deletion of notification is not reliable - therefore making explicit update.
			NotificationManager manager = getSystemService(NotificationManager.class);
			assert manager != null;
			manager.cancel(SERVICE_ID);
			return;
		}
		PendingIntent contentIntent = PendingIntent.getActivity(this, REQUEST_CODE,
				MainActivity.createIntent(this, R.id.nav_alarms),
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		String notificationMessage = getRunningAlarmsString();
		Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_alarm))
				.setStyle(new BigTextStyle().bigText(notificationMessage))
				.setContentText(notificationMessage)
				.setSmallIcon(R.drawable.ic_notification_icon_alarm)
				.setContentIntent(contentIntent)
				.build();
		startForeground(SERVICE_ID, notification);
		// sometimes update of notification text is not reliable - therefore making explicit update.
		NotificationManager manager = getSystemService(NotificationManager.class);
		assert manager != null;
		manager.notify(SERVICE_ID, notification);
	}

	/**
	 * Start the running notification for a certain alarm.
	 *
	 * @param alarm The alarm.
	 */
	private void startRunningNotification(final Alarm alarm, final boolean isScene) {
		PendingIntent contentIntent = PendingIntent.getActivity(this, alarm.getId(),
				MainActivity.createIntent(this, isScene ? R.id.nav_scenes : R.id.nav_alarms), PendingIntent.FLAG_IMMUTABLE);
		PendingIntent stopIntent = PendingIntent.getService(this, alarm.getId(),
				LifxAlarmService.createIntent(this, ACTION_INTERRUPT_ALARM, alarm.getId(), null), PendingIntent.FLAG_IMMUTABLE);
		int titleRes = isScene ? R.string.notification_title_scene_execution : R.string.notification_title_alarm_execution;
		int smallIcon = isScene ? R.drawable.ic_menu_scenes : R.drawable.ic_notification_icon_alarm;
		int largeIcon = isScene ? R.drawable.ic_menu_scenes : R.drawable.ic_notification_icon_large_alarm;
		Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_EXECUTION)
				.setContentTitle(getString(titleRes, alarm.getName()))
				.setSmallIcon(smallIcon)
				.setLargeIcon(ImageUtil.createBitmapFromDrawable(this, largeIcon))
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
		// stop animations and interrupt threads
		synchronized (RUNNING_THREADS) {
			List<Thread> threads = RUNNING_THREADS.get(alarm.getId());
			if (threads != null) {
				for (Thread thread : threads) {
					thread.interrupt();
				}
			}
		}

		for (LightSteps lightSteps : alarm.getLightSteps()) {
			LifxAnimationService.stopAnimationForMac(this, lightSteps.getLight().getTargetAddress());
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
	 * @param alarm          The alarm
	 * @param wakeLock       The wakelock on that light
	 * @param light          The light
	 * @param animatedLights The list of animated lights
	 */
	private void updateOnEndAnimation(final Alarm alarm, final WakeLock wakeLock, final Light light, final List<Light> animatedLights) {
		boolean isLastLight;
		// remove finished thread from running list
		synchronized (RUNNING_THREADS) {
			List<Thread> threads = RUNNING_THREADS.get(alarm.getId());
			if (threads != null) {
				threads.remove(Thread.currentThread());
				if (threads.isEmpty()) {
					RUNNING_THREADS.remove(alarm.getId());
				}
			}
		}
		// noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (animatedLights) {
			animatedLights.remove(light);
			isLastLight = animatedLights.isEmpty();
		}
		Logger.debug("LifxAlarmService end (" + alarm.getName() + "," + light.getLabel() + ") (" + animatedLights.size() + ")");

		if (isLastLight) {
			if (wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
			}
			synchronized (ANIMATED_ALARMS) {
				ANIMATED_ALARMS.remove((Integer) alarm.getId());
				Logger.debug("LifxAlarmService end (" + ANIMATED_ALARMS.size() + "," + PENDING_ALARMS.size() + ")");
				if (!ANIMATED_ALARMS.contains(alarm.getId())) {
					endRunningNotification(alarm);
				}
				startNotification();
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
		if (!PENDING_ALARMS.isEmpty()) {
			List<Alarm> pendingAlarms = new ArrayList<>(PENDING_ALARMS.values());
			pendingAlarms.sort(Comparator.comparing(Alarm::getStartTime));
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
	 * A thread handling stored animations within scenes or alarms.
	 */
        public class StoredAnimationThread extends Thread {
                /**
                 * The light that is animated.
                 */
                private final Light mLight;
                /**
                 * The steps for the light.
                 */
                private final List<Step> mSteps;
                /**
                 * The start date of the alarm or scene.
                 */
                private final Date mAlarmDate;
                /**
                 * The alarm type.
                 */
                private final AlarmType mAlarmType;
                /**
                 * The total duration of the alarm.
                 */
                private final long mAlarmDuration;
                /**
                 * An exception callback called in case of SocketException.
                 */
                private AnimationCallback mAnimationCallback = null;

                /**
                 * Create an animation thread.
                 *
                 * @param light        The light.
                 * @param steps        The steps for the light.
                 * @param alarmDate    The start date of the alarm or scene.
                 * @param alarmType    The alarm type.
                 * @param alarmDuration The total duration of the alarm.
                 */
                protected StoredAnimationThread(final Light light, final List<Step> steps, final Date alarmDate,
                                final AlarmType alarmType, final long alarmDuration) {
                        mLight = light;
                        mSteps = steps;
                        mAlarmDate = alarmDate;
                        mAlarmType = alarmType;
                        mAlarmDuration = alarmDuration;
                }

		/**
		 * Set the exception callback called in case of Exception.
		 *
		 * @param callback The callback.
		 * @return The updated animation thread.
		 */
		public StoredAnimationThread setAnimationCallback(final AnimationCallback callback) {
			mAnimationCallback = callback;
			return this;
		}

		@Override
                public void run() {
                        try {
                                LifxAnimationService.stopAnimationForMac(LifxAlarmService.this, mLight.getTargetAddress());
                                mLight.endAnimation(false);
                                int n = 0;
                                while (!isInterrupted() && (mAlarmType == AlarmType.CYCLIC || n < mSteps.size())) {
                                        Step step = mSteps.get(n % mSteps.size());
                                        long cycleStart = mAlarmDate.getTime() + (n / mSteps.size()) * mAlarmDuration;
                                        long start = cycleStart + step.getDelay();
                                        long waitTime = start - System.currentTimeMillis();
                                        if (waitTime > 0) {
                                                //noinspection BusyWait
                                                Thread.sleep(waitTime);
                                        }

                                        if (n > 0) {
                                                Step previous = mSteps.get((n - 1) % mSteps.size());
                                                if (previous.getStoredColor() instanceof StoredAnimation) {
                                                        LifxAnimationService.stopAnimationForMac(LifxAlarmService.this, mLight.getTargetAddress());
                                                        mLight.endAnimation(false);
                                                }
                                        }

                                        StoredColor storedColor = step.getStoredColor();
                                        if (storedColor instanceof StoredAnimation) {
                                                StoredAnimation storedAnimation = (StoredAnimation) storedColor;
                                                LifxAnimationService.triggerAnimationService(LifxAlarmService.this, mLight,
                                                                storedAnimation.getAnimationData());
                                        }
                                        else {
                                                Color color = storedColor.getColor();
                                                int duration = (int) step.getDuration();
                                                Power power = mLight.getPower();
                                                boolean wasOff = power != null && power.isOff();
                                                if (mLight instanceof MultiZoneLight) {
                                                        MultizoneColors colors = storedColor instanceof StoredMultizoneColors
                                                                        ? ((StoredMultizoneColors) storedColor).getColors()
                                                                        : new MultizoneColors.Fixed(color);
                                                        if (colors.isOff()) {
                                                                mLight.setPower(false, duration, false);
                                                        }
                                                        else if (wasOff) {
                                                                ((MultiZoneLight) mLight).setColors(colors, 0, false);
                                                                mLight.setPower(true, duration, false);
                                                        }
                                                        else {
                                                                ((MultiZoneLight) mLight).setColors(colors, duration, false);
                                                        }
                                                }
                                                else if (mLight instanceof TileChain) {
                                                        TileChainColors colors = storedColor instanceof StoredTileColors
                                                                        ? ((StoredTileColors) storedColor).getColors()
                                                                        : new TileChainColors.Fixed(color);
                                                        if (colors.isOff()) {
                                                                mLight.setPower(false, duration, false);
                                                        }
                                                        else if (wasOff) {
                                                                ((TileChain) mLight).setColors(colors, 0, false);
                                                                mLight.setPower(true, duration, false);
                                                        }
                                                        else {
                                                                ((TileChain) mLight).setColors(colors, duration, false);
                                                        }
                                                }
                                                else {
                                                        if (color.isOff()) {
                                                                mLight.setPower(false, duration, false);
                                                        }
                                                        else if (wasOff) {
                                                                mLight.setColor(color, 0, false);
                                                                mLight.setPower(true, duration, false);
                                                        }
                                                        else {
                                                                mLight.setColor(color, duration, false);
                                                        }
                                                }
                                        }
                                        n++;
                                }

                                if (!isInterrupted()) {
                                        Step lastStep = mSteps.get((n - 1) % mSteps.size());
                                        if (mAlarmType != AlarmType.CYCLIC && mAlarmType != AlarmType.STOP_MANUALLY) {
                                                long end = mAlarmDate.getTime() + lastStep.getDelay() + lastStep.getDuration();
                                                long waitTime = end - System.currentTimeMillis();
                                                if (waitTime > 0) {
                                                        //noinspection BusyWait
                                                        Thread.sleep(waitTime);
                                                }
                                        }
                                        if (mAlarmType != AlarmType.STOP_MANUALLY
                                                        && lastStep.getStoredColor() instanceof StoredAnimation) {
                                                LifxAnimationService.stopAnimationForMac(LifxAlarmService.this, mLight.getTargetAddress());
                                                mLight.endAnimation(false);
                                        }
                                        if (mAnimationCallback != null) {
                                                mAnimationCallback.onAnimationEnd(false);
                                        }
                                }
                        }
                        catch (InterruptedException e) {
                                LifxAnimationService.stopAnimationForMac(LifxAlarmService.this, mLight.getTargetAddress());
                                mLight.endAnimation(true);
                                if (mAnimationCallback != null) {
                                        mAnimationCallback.onAnimationEnd(true);
                                }
                        }
                        catch (IOException e) {
                                if (mAnimationCallback != null) {
                                        mAnimationCallback.onException(e);
                                }
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
	 * A thread handling ringtone animation.
	 */
	public class RingtoneAnimationThread extends BaseAnimationThread {
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

			if (mDefinition.maximizeVolume()) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (audioManager != null) {
					audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
				}
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

		/**
		 * Flag indicating if volume should be maximized.
		 *
		 * @return Flag indicating if volume should be maximized.
		 */
		boolean maximizeVolume();
	}
}
