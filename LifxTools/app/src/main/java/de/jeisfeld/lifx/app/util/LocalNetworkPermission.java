package de.jeisfeld.lifx.app.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/** Permission shared by the UI and background LAN service entry points. */
public final class LocalNetworkPermission {
	/** Runtime permission required for LAN sockets when targeting Android 17. */
	@SuppressLint("InlinedApi") // Only requested on API 37+, guarded by isGranted.
	public static final String PERMISSION = android.Manifest.permission.ACCESS_LOCAL_NETWORK;

	private LocalNetworkPermission() {
	}

	/** @return whether LAN communication is permitted on this Android version. */
	public static boolean isGranted(final Context context) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN
				|| ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED;
	}
}
