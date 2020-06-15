package de.jeisfeld.lifx.app.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A broadcast receiver being informed about Boots.
 */
public class BootReceiver extends BroadcastReceiver {
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		String action = intent.getAction();

		// Re-create all timers after boot or installation.
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			triggerAllTimers();
		}
		if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
			triggerAllTimers();
		}
	}

	/**
	 * Trigger all widget or notification timers.
	 */
	private static void triggerAllTimers() {
		AlarmReceiver.createAllAlarms();
	}
}
