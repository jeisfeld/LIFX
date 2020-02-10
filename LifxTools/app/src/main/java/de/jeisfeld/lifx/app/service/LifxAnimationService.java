package de.jeisfeld.lifx.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.core.app.NotificationCompat;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.MainActivity;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.ExceptionCallback;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
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
	 * Key for the notification text within the intent.
	 */
	public static final String EXTRA_NOTIFICATION_TEXT = "de.jeisfeld.lifx.NOTIFICATION_TEXT";
	/**
	 * Key for the device MAC within the intent.
	 */
	public static final String EXTRA_DEVICE_MAC = "de.jeisfeld.lifx.DEVICE_MAC";
	/**
	 * Map from MACs to Lights for all lights with running animations.
	 */
	private static final Map<String, Light> ANIMATED_LIGHTS = new HashMap<>();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String input = intent.getStringExtra(EXTRA_NOTIFICATION_TEXT);
		final String mac = intent.getStringExtra(EXTRA_DEVICE_MAC);

		createNotificationChannel();
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_animation))
				.setContentText(input)
				.setSmallIcon(R.drawable.ic_menu_share)
				.setContentIntent(pendingIntent)
				.build();
		startForeground(1, notification);


		new Thread() {
			public void run() {
				Light light;
				synchronized (ANIMATED_LIGHTS) {
					light = ANIMATED_LIGHTS.get(mac);
				}
				if (light == null) {
					light = LifxLan.getInstance().getLightByMac(mac);
					ANIMATED_LIGHTS.put(mac, light);
				}
				else {
					light.endAnimation();
				}

				if (light instanceof MultiZoneLight) {
					final MultiZoneLight multizoneLight = (MultiZoneLight) light;
					multizoneLight.rollingAnimation(10000, // MAGIC_NUMBER
							new MultizoneColors.Interpolated(true, Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE))
							.setBrightness(0.3) // MAGIC_NUMBER
							.setExceptionCallback(new ExceptionCallback() {
								@Override
								public void onException(final IOException e) {
									endAnimation(multizoneLight);
								}
							})
							.start();
				}
				else {
					// to be implemented later.
				}
			}
		}.start();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Create the channel for service animation notifications.
	 */
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel animationChannel = new NotificationChannel(
					CHANNEL_ID, getString(R.string.notification_channel_animation), NotificationManager.IMPORTANCE_DEFAULT
			);
			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(animationChannel);
		}
	}

	/**
	 * Stop the animation for a given MAC.
	 *
	 * @param context the context.
	 * @param mac     The MAC
	 */
	public static void stopAnimationForMac(final Context context, final String mac) {
		Light light = null;
		synchronized (ANIMATED_LIGHTS) {
			if (ANIMATED_LIGHTS.containsKey(mac)) {
				light = ANIMATED_LIGHTS.get(mac);
				ANIMATED_LIGHTS.remove(mac);
			}
			if (ANIMATED_LIGHTS.size() == 0) {
				Intent serviceIntent = new Intent(context, LifxAnimationService.class);
				context.stopService(serviceIntent);
			}
		}
		if (light != null) {
			light.endAnimation();
		}
	}

	/**
	 * End the animation for a given light.
	 *
	 * @param light The light.
	 */
	private void endAnimation(final Light light) {
		light.endAnimation();
		synchronized (ANIMATED_LIGHTS) {
			ANIMATED_LIGHTS.remove(light.getTargetAddress());
			Logger.log("Animations: " + ANIMATED_LIGHTS);
			if (ANIMATED_LIGHTS.size() == 0) {
				Intent serviceIntent = new Intent(this, LifxAnimationService.class);
				stopService(serviceIntent);
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
		synchronized (ANIMATED_LIGHTS) {
			return ANIMATED_LIGHTS.containsKey(mac);
		}
	}

}
