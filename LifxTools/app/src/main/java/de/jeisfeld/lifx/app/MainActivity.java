package de.jeisfeld.lifx.app;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings.Panel;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.AppBarConfiguration.Builder;
import androidx.navigation.ui.NavigationUI;
import de.jeisfeld.lifx.app.alarms.AlarmReceiver;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.app.util.LocalNetworkPermission;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * The main activity of the app.
 */
public class MainActivity extends AppCompatActivity {
	/**
	 * The request code used to query for notification permission.
	 */
	protected static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1;
	/**
	 * The request code used to query for local network permission.
	 */
	private static final int REQUEST_CODE_LOCAL_NETWORK = 2;
	/**
	 * The resource key for the navigation page.
	 */
	private static final String EXTRA_NAVIGATION_PAGE = "de.jeisfeld.lifx.app.NAVIGATION_PAGE";
	/**
	 * The saved instance state for network reuest pending
	 */
	private static final String STATE_LOCAL_NETWORK_REQUEST_PENDING = "localNetworkRequestPending";
	/**
	 * The navigation bar configuration.
	 */
	private AppBarConfiguration mAppBarConfiguration;
	/**
	 * Flag if waiting for local network access.
	 */
	private boolean mWaitingForLocalNetwork;
	/**
	 * Flag if local network access request is pending.
	 */
	private boolean mLocalNetworkRequestPending;

	/**
	 * Static helper method to create an intent for the activity.
	 *
	 * @param context          The context creating the intent.
	 * @param navigationPageId The navigation page id.
	 * @return the intent.
	 */
	public static Intent createIntent(final Context context, final int navigationPageId) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_NAVIGATION_PAGE, navigationPageId);
		return intent;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		// Do not restore fragments that access lights before permission is available.
		super.onCreate(LocalNetworkPermission.isGranted(this) ? savedInstanceState : null);
		if (!LocalNetworkPermission.isGranted(this)) {
			mWaitingForLocalNetwork = true;
			if (savedInstanceState != null) {
				mLocalNetworkRequestPending = savedInstanceState.getBoolean(STATE_LOCAL_NETWORK_REQUEST_PENDING);
			}
			return;
		}
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		AppCompatDelegate.setDefaultNightMode(
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_night_mode, R.string.pref_default_night_mode));
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		NavigationView navigationView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		mAppBarConfiguration = new Builder(R.id.nav_home, R.id.nav_manage_devices, R.id.nav_stored_colors,
				R.id.nav_alarms, R.id.nav_scenes, R.id.nav_settings)
				.setOpenableLayout(drawer)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
		NavigationUI.setupWithNavController(navigationView, navController);

		int navigationPageId = getIntent().getIntExtra(EXTRA_NAVIGATION_PAGE, -1);
		if (navigationPageId >= 0) {
			navController.popBackStack();
			navController.navigate(navigationPageId);
		}

		if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATION_PERMISSION);
			}
		}

		WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null && !wifiManager.isWifiEnabled()) {
			if (VERSION.SDK_INT >= VERSION_CODES.Q) {
				startActivity(new Intent(Panel.ACTION_WIFI));
			}
			else {
				wifiManager.setWifiEnabled(true);
			}
		}
	}

	/** Explain LAN access using the standard confirmation dialog before requesting it. */
	private void showLocalNetworkPermissionDialog() {
		DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
			@Override
			public void onDialogPositiveClick(final DialogFragment dialog) {
				mLocalNetworkRequestPending = true;
				ActivityCompat.requestPermissions(MainActivity.this,
						new String[]{LocalNetworkPermission.PERMISSION}, REQUEST_CODE_LOCAL_NETWORK);
			}

			@Override
			public void onDialogNegativeClick(final DialogFragment dialog) {
				finish();
			}
		}, R.string.local_network_permission_retry, R.string.button_cancel,
				R.string.button_ok, R.string.local_network_permission_explanation);
	}

	@Override
	protected final void onPostResume() {
		super.onPostResume();
		if (mWaitingForLocalNetwork && !mLocalNetworkRequestPending) {
			// DialogUtil clears listeners when state is saved, so rebuild the confirmation on resume.
			ConfirmDialogFragment dialog = (ConfirmDialogFragment) getSupportFragmentManager()
					.findFragmentByTag(ConfirmDialogFragment.class.toString());
			if (dialog != null) {
				dialog.dismissNow();
			}
			showLocalNetworkPermissionDialog();
		}
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		outState.putBoolean(STATE_LOCAL_NETWORK_REQUEST_PENDING, mLocalNetworkRequestPending);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected final void onResume() {
		super.onResume();
		if (mWaitingForLocalNetwork && LocalNetworkPermission.isGranted(this)) {
			mWaitingForLocalNetwork = false;
			AlarmReceiver.createAllAlarms();
			recreate();
		}
		else if (!mWaitingForLocalNetwork && !LocalNetworkPermission.isGranted(this)) {
			recreate();
		}
	}

	@Override
	public final void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_CODE_LOCAL_NETWORK) {
			mLocalNetworkRequestPending = false;
			if (LocalNetworkPermission.isGranted(this)) {
				mWaitingForLocalNetwork = false;
				AlarmReceiver.createAllAlarms();
			}
			// Recreate the confirmation with a live listener, or initialize the permitted UI.
			recreate();
		}
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		if (mWaitingForLocalNetwork) {
			return false;
		}
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			finish();
			startActivity(new Intent(this, MainActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public final boolean onSupportNavigateUp() {
		if (mWaitingForLocalNetwork) {
			return false;
		}
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		return NavigationUI.navigateUp(navController, mAppBarConfiguration)
				|| super.onSupportNavigateUp();
	}

	@Override
	protected final void attachBaseContext(final Context newBase) {
		super.attachBaseContext(Application.createContextWrapperForLocale(newBase));
	}
}
