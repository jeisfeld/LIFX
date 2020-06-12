package de.jeisfeld.lifx.app;

import com.google.android.material.navigation.NavigationView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

/**
 * The main activity of the app.
 */
public class MainActivity extends AppCompatActivity {
	/**
	 * The navigation bar configuration.
	 */
	private AppBarConfiguration mAppBarConfiguration;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		NavigationView navigationView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_manage_devices, R.id.nav_stored_colors,
				R.id.nav_alarms, R.id.nav_settings)
				.setDrawerLayout(drawer)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
		NavigationUI.setupWithNavController(navigationView, navController);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			Intent intent = getIntent();
			finish();
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public final boolean onSupportNavigateUp() {
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		return NavigationUI.navigateUp(navController, mAppBarConfiguration)
				|| super.onSupportNavigateUp();
	}

	@Override
	protected final void attachBaseContext(final Context newBase) {
		super.attachBaseContext(Application.createContextWrapperForLocale(newBase));
	}
}
