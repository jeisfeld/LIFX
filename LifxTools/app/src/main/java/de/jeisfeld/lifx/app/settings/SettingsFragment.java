package de.jeisfeld.lifx.app.settings;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import de.jeisfeld.lifx.app.R;

/**
 * Fragment for settings.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
	@Override
	public final void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
	}
}
