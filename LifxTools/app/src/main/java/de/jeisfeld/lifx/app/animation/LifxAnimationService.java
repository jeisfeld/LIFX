package de.jeisfeld.lifx.app.animation;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.jeisfeld.lifx.app.MainActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationCallback;
import de.jeisfeld.lifx.lan.Light.BaseAnimationThread;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.os.Logger;

/**
 * A service handling LIFX animations in the background.
 */
public class LifxAnimationService extends Service {
	/**
	 * The id of the notification channel.
	 */
	public static final String CHANNEL_ID = "LifxAnimationChannel";
	/**
	 * Key for the device MAC within the intent.
	 */
	public static final String EXTRA_DEVICE_MAC = "de.jeisfeld.lifx.DEVICE_MAC";
	/**
	 * Key for the device Label within the intent.
	 */
	public static final String EXTRA_DEVICE_LABEL = "de.jeisfeld.lifx.DEVICE_LABEL";
	/**
	 * Key for the alarm id within the intent.
	 */
	public static final String EXTRA_ALARM_ID = "de.jeisfeld.lifx.ALARM_ID";
	/**
	 * Key for the alarm id within the intent.
	 */
	public static final String EXTRA_ALARM_TIME = "de.jeisfeld.lifx.ALARM_TIME";
	/**
	 * The intent of the broadcast for stopping an animation.
	 */
	public static final String EXTRA_ANIMATION_STOP_INTENT = "de.jeisfeld.lifx.ANIMATION_STOP_INTENT";
	/**
	 * Key for the broadcast data giving MAC of stopped animation.
	 */
	public static final String EXTRA_ANIMATION_STOP_MAC = "de.jeisfeld.lifx.ANIMATION_STOP_MAC";
	/**
	 * The retry count for alarms.
	 */
	private static final int ALARM_RETRY_COUNT = 3;
	/**
	 * Map from MACs to Lights for all lights with running animations.
	 */
	private static final Map<String, Light> ANIMATED_LIGHTS = new HashMap<>();
	/**
	 * Map from MACs to Light labels for all lights with running animations.
	 */
	private static final Map<String, String> ANIMATED_LIGHT_LABELS = new HashMap<>();
	/**
	 * Map from alarm Ids to Light labels for all lights with running animations.
	 */
	private static final List<Alarm> ANIMATED_ALARMS = new ArrayList<>();
	/**
	 * The local broadcast manager.
	 */
	private LocalBroadcastManager mBroadcastManager;

	@Override
	public final void onCreate() {
		super.onCreate();
		createNotificationChannel();
		mBroadcastManager = LocalBroadcastManager.getInstance(this);
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		final int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);

		if (alarmId >= 0) {
			final Date alarmDate = (Date) intent.getSerializableExtra(EXTRA_ALARM_TIME);
			Alarm alarm = new Alarm(alarmId);

			synchronized (ANIMATED_LIGHTS) {
				ANIMATED_ALARMS.add(alarm);
			}

			startNotification();
			getAlarmAnimationThread(alarm, alarmDate).start();
		}
		else {
			final String mac = intent.getStringExtra(EXTRA_DEVICE_MAC);
			final String label = intent.getStringExtra(EXTRA_DEVICE_LABEL);
			final AnimationData animationData = AnimationData.fromIntent(intent);

			assert animationData != null;
			assert mac != null;
			assert label != null;
			ANIMATED_LIGHT_LABELS.put(mac, label);

			startNotification();

			getDeviceAnimationThread(mac, animationData).start();
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
	 * Get a device animation thread.
	 *
	 * @param mac           The device MAC.
	 * @param animationData The animation data.
	 * @return The animation thread.
	 */
	private Thread getDeviceAnimationThread(final String mac, final AnimationData animationData) {
		return new Thread() {
			@Override
			public void run() {
				if (!animationData.isValid()) {
					updateOnEndAnimation(mac, null);
					return;
				}
				Light tmpLight = ANIMATED_LIGHTS.get(mac);
				if (tmpLight == null) {
					tmpLight = DeviceRegistry.getInstance().getLightByMac(mac);
					if (tmpLight == null) {
						tmpLight = LifxLan.getInstance().getLightByMac(mac);
					}
					if (tmpLight == null || tmpLight.getTargetAddress() == null || !mac.equals(tmpLight.getTargetAddress())) {
						updateOnEndAnimation(mac, null);
						return;
					}
					synchronized (ANIMATED_LIGHTS) {
						ANIMATED_LIGHTS.put(mac, tmpLight);
					}
				}
				else {
					tmpLight.endAnimation(false);
				}
				final Light light = tmpLight;
				final WakeLock wakeLock = acquireWakelock(light);

				if (animationData.hasNativeImplementation(light)) {
					new BaseAnimationThread(light) {
						@Override
						public void run() {
							try {
								animationData.getNativeAnimationDefinition(light).startAnimation();
							}
							catch (IOException e) {
								updateOnEndAnimation(light.getTargetAddress(), wakeLock);
							}
							while (true) {
								try {
									Thread.sleep(60000); // MAGIC_NUMBER
								}
								catch (InterruptedException e) {
									try {
										animationData.getNativeAnimationDefinition(light).stopAnimation();
									}
									catch (IOException ex) {
										// ignore
									}
									updateOnEndAnimation(light.getTargetAddress(), wakeLock);
								}
							}
						}
					}.start();
				}
				else {
					light.animation(animationData.getAnimationDefinition(light))
							.setAnimationCallback(new AnimationCallback() {
								@Override
								public void onException(final IOException e) {
									updateOnEndAnimation(light.getTargetAddress(), wakeLock);
								}

								@Override
								public void onAnimationEnd(final boolean isInterrupted) {
									updateOnEndAnimation(light.getTargetAddress(), wakeLock);
								}
							})
							.start();
				}
			}
		};
	}

	/**
	 * Get an alarm animation thread.
	 *
	 * @param alarm     the alarm
	 * @param alarmDate the alarm date
	 * @return The animation thread
	 */
	private Thread getAlarmAnimationThread(final Alarm alarm, final Date alarmDate) {
		final List<Step> alarmSteps = new ArrayList<>(alarm.getSteps());
		Collections.sort(alarmSteps);

		return new Thread() {
			@Override
			public void run() {
				Set<Light> lights = new HashSet<>();
				for (Step step : alarmSteps) {
					lights.add(step.getStoredColor().getLight());
				}

				final WakeLock wakeLock = acquireWakelock(alarm);

				for (Light light : lights) {
					light.endAnimation(false);
				}
				for (Light light : lights) {
					Power power = light.getPower();
					if (power != null && power.isOff()) {
						int count = 0;
						boolean success = false;
						while (!success && count < ALARM_RETRY_COUNT) {
							try {
								light.setColor(Color.OFF);
								light.setPower(true);
								success = true;
							}
							catch (IOException e) {
								Logger.error(e);
								count++;
							}
						}
					}
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

							StoredColor storedColor = step.getStoredColor();

							int count = 0;
							boolean success = false;
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
				if (waitTime > 0) {
					try {
						Thread.sleep(waitTime);
					}
					catch (InterruptedException e) {
						// ignore
					}
				}

				for (Thread thread : actionThreads) {
					thread.start();
				}

				for (Thread thread : actionThreads) {
					try {
						thread.join();
					}
					catch (InterruptedException e) {
						// ignore
					}
				}

				updateOnEndAnimation(alarm, wakeLock);
			}
		};
	}

	/**
	 * Get a wakelock for a device and acquire it.
	 *
	 * @param device The device.
	 * @return The wakelock.
	 */
	private WakeLock acquireWakelock(final Device device) {
		if (PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_wakelock, true)) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			assert powerManager != null;
			WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.jeisfeld.lifx." + device.getTargetAddress());
			wakelock.acquire();
			return wakelock;
		}
		else {
			return null;
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
			wakelock.acquire();
			return wakelock;
		}
		else {
			return null;
		}
	}

	/**
	 * Send broadcast informing about the end of an animation.
	 *
	 * @param mac The MAC for which the animation ended.
	 */
	public void sendBroadcastStopAnimation(final String mac) {
		Intent intent = new Intent(EXTRA_ANIMATION_STOP_INTENT);
		intent.putExtra(EXTRA_ANIMATION_STOP_MAC, mac);
		mBroadcastManager.sendBroadcast(intent);
	}

	/**
	 * Create the channel for service animation notifications.
	 */
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel animationChannel = new NotificationChannel(
					CHANNEL_ID, getString(R.string.notification_channel_animation), NotificationManager.IMPORTANCE_DEFAULT);
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
				.setContentTitle(getString(R.string.notification_title_animation))
				.setContentText(getString(R.string.notification_text_animation_running, getAnimatedDevicesString()))
				.setSmallIcon(R.drawable.ic_notification_icon_logo)
				.setContentIntent(pendingIntent)
				.build();
		startForeground(1, notification);
	}

	/**
	 * Stop the animation from UI for a given MAC.
	 *
	 * @param context the context.
	 * @param mac     The MAC
	 */
	public static void stopAnimationForMac(final Context context, final String mac) {
		Light light = ANIMATED_LIGHTS.get(mac);
		if (light != null) {
			light.endAnimation(false);
		}
	}

	/**
	 * Update the service after the animation has ended.
	 *
	 * @param mac      the MAC.
	 * @param wakeLock The wakelock on that light.
	 */
	private void updateOnEndAnimation(final String mac, final WakeLock wakeLock) {
		if (wakeLock != null) {
			wakeLock.release();
		}
		sendBroadcastStopAnimation(mac);
		synchronized (ANIMATED_LIGHTS) {
			ANIMATED_LIGHTS.remove(mac);
			ANIMATED_LIGHT_LABELS.remove(mac);
			if (ANIMATED_ALARMS.size() == 0 && ANIMATED_LIGHTS.size() == 0) {
				Intent serviceIntent = new Intent(this, LifxAnimationService.class);
				stopService(serviceIntent);
			}
			else {
				startNotification();
			}
		}
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
		synchronized (ANIMATED_LIGHTS) {
			ANIMATED_ALARMS.remove(alarm);
			if (ANIMATED_ALARMS.size() == 0 && ANIMATED_LIGHTS.size() == 0) {
				Intent serviceIntent = new Intent(this, LifxAnimationService.class);
				stopService(serviceIntent);
			}
			else {
				startNotification();
			}
		}
	}

	/**
	 * Check if there is a running animation for a given MAC.
	 *
	 * @param mac the MAC.
	 * @return true if there is a running animation for this MAC.
	 */
	public static boolean hasRunningAnimation(final String mac) {
		return ANIMATED_LIGHTS.containsKey(mac);
	}

	/**
	 * Get a display String for all animated devices.
	 *
	 * @return a display String for all animated devices.
	 */
	public static String getAnimatedDevicesString() {
		StringBuilder builder = new StringBuilder();
		for (String label : ANIMATED_LIGHT_LABELS.values()) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(label);
		}
		for (Alarm alarm : ANIMATED_ALARMS) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(alarm.getName());
		}
		return builder.toString();
	}

}
