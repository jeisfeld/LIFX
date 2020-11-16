package de.jeisfeld.lifx.app.animation;

import android.annotation.SuppressLint;
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
import java.util.HashMap;
import java.util.Map;

import androidx.core.app.NotificationCompat;
import de.jeisfeld.lifx.app.MainActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.HomeFragment;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationCallback;
import de.jeisfeld.lifx.lan.Light.BaseAnimationThread;
import de.jeisfeld.lifx.os.Logger;

/**
 * A service handling LIFX animations in the background.
 */
public class LifxAnimationService extends Service {
	/**
	 * The request code for the main notification.
	 */
	private static final int REQUEST_CODE = -2;
	/**
	 * The id for the service.
	 */
	private static final int SERVICE_ID = 2;
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
	 * Map from MACs to Lights for all lights with running animations.
	 */
	private static final Map<String, Light> ANIMATED_LIGHTS = new HashMap<>();
	/**
	 * Map from MACs to Light labels for all lights with running animations.
	 */
	private static final Map<String, String> ANIMATED_LIGHT_LABELS = new HashMap<>();
	/**
	 * Map from MACs to animation data for all lights with running animations.
	 */
	private static final Map<String, AnimationData> ANIMATED_LIGHT_DATA = new HashMap<>();

	@Override
	public final void onCreate() {
		super.onCreate();
		createNotificationChannel();
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (intent == null) {
			return START_REDELIVER_INTENT;
		}

		final String mac = intent.getStringExtra(EXTRA_DEVICE_MAC);
		final String label = intent.getStringExtra(EXTRA_DEVICE_LABEL);
		final AnimationData animationData = AnimationData.fromIntent(intent);

		assert animationData != null;
		assert mac != null;
		assert label != null;
		ANIMATED_LIGHT_LABELS.put(mac, label);
		Logger.info("LifxAnimationService start (" + ANIMATED_LIGHT_LABELS.size() + ") (" + mac + "," + label + ")");

		startNotification();

		getDeviceAnimationThread(mac, animationData).start();

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
						ANIMATED_LIGHT_DATA.put(mac, animationData);
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
							if (!animationData.isRunning()) {
								try {
									animationData.getNativeAnimationDefinition(light).startAnimation();
								}
								catch (IOException e) {
									updateOnEndAnimation(light.getTargetAddress(), wakeLock);
								}
							}
							while (true) {
								try {
									//noinspection BusyWait
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
									return;
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
	 * Get a wakelock for a device and acquire it.
	 *
	 * @param device The device.
	 * @return The wakelock.
	 */
	@SuppressLint("WakelockTimeout")
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
				REQUEST_CODE, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_animation))
				.setContentText(getString(R.string.notification_text_animation_running, getAnimatedDevicesString()))
				.setSmallIcon(R.drawable.ic_notification_icon_logo)
				.setContentIntent(pendingIntent)
				.build();
		startForeground(SERVICE_ID, notification);
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
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		HomeFragment.sendBroadcastStopAnimation(this, mac);
		final String label = ANIMATED_LIGHT_LABELS.get(mac);
		synchronized (ANIMATED_LIGHTS) {
			ANIMATED_LIGHTS.remove(mac);
			ANIMATED_LIGHT_LABELS.remove(mac);
			ANIMATED_LIGHT_DATA.remove(mac);
			Logger.info("LifxAnimationService end (" + ANIMATED_LIGHT_LABELS.size() + ") (" + mac + "," + label + ")");
			if (ANIMATED_LIGHTS.size() == 0) {
				Intent serviceIntent = new Intent(this, LifxAnimationService.class);
				stopService(serviceIntent);
			}
			else {
				startNotification();
			}
		}
	}

	/**
	 * Get the animation status for a given MAC.
	 *
	 * @param mac the MAC.
	 * @return the animation status for this MAC.
	 */
	public static AnimationStatus getAnimationStatus(final String mac) {
		AnimationData animationData = ANIMATED_LIGHT_DATA.get(mac);
		Light light = ANIMATED_LIGHTS.get(mac);
		if (animationData == null || light == null) {
			return AnimationStatus.OFF;
		}
		else {
			return animationData.hasNativeImplementation(light) ? AnimationStatus.NATIVE : AnimationStatus.CUSTOM;
		}
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
		return builder.toString();
	}

	/**
	 * The animation status of a light.
	 */
	public enum AnimationStatus {
		/**
		 * No animation.
		 */
		OFF,
		/**
		 * Native animation.
		 */
		NATIVE,
		/**
		 * Custom animation.
		 */
		CUSTOM
	}

}
