package de.jeisfeld.lifx.app.settings;

import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.os.Logger;

/**
 * Fragment for settings.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
	/**
	 * The filename of the file for export/import preferences.
	 */
	private static final String EXPORT_FILENAME = "MySharedPreferences.exp";

	@Override
	public final void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
		configureExportImportButtons();
	}

	/**
	 * Configure buttons for export/import preferences.
	 */
	private void configureExportImportButtons() {
		Preference exportPreference = findPreference(getString(R.string.key_pref_dummy_export));
		assert exportPreference != null;
		exportPreference.setOnPreferenceClickListener(preference -> {
			File preferencesFile = new File(requireContext().getExternalFilesDir(null), EXPORT_FILENAME);
			if (preferencesFile.exists()) {
				DialogUtil.displayConfirmationMessage(requireActivity(), new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								doExportPreferences(preferencesFile);
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								DialogUtil.displayToast(requireContext(), R.string.toast_settings_export_canceled);
							}
						}, R.string.title_dialog_export, R.string.button_cancel, R.string.button_overwrite, R.string.message_dialog_export_overwrite,
						preferencesFile.getAbsolutePath());
			}
			else {
				doExportPreferences(preferencesFile);
			}
			return true;
		});

		Preference importPreference = findPreference(getString(R.string.key_pref_dummy_import));
		assert importPreference != null;
		importPreference.setOnPreferenceClickListener(preference -> {
			File preferencesFile = new File(requireContext().getExternalFilesDir(null), EXPORT_FILENAME);
			if (preferencesFile.exists()) {
				if (preferencesFile.exists()) {
					DialogUtil.displayConfirmationMessage(requireActivity(), new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog) {
							doImportPreferences(preferencesFile);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
							DialogUtil.displayToast(requireContext(), R.string.toast_settings_import_canceled);
						}
					}, R.string.title_dialog_import, R.string.button_cancel, R.string.button_overwrite, R.string.message_dialog_import_overwrite);
				}
			}
			else {
				DialogUtil.displayConfirmationMessage(requireActivity(), R.string.title_dialog_import, R.string.message_dialog_import_missing_file,
						preferencesFile.getAbsolutePath());
			}
			return true;
		});
	}

	/**
	 * Export the preferences.
	 *
	 * @param preferencesFile The file for export.
	 */
	private void doExportPreferences(final File preferencesFile) {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(preferencesFile);
			fileOutputStream.write(PreferenceUtil.getPreferencesExportByteArray());
			fileOutputStream.close();
			DialogUtil.displayToast(getContext(), R.string.toast_settings_exported, preferencesFile.getAbsolutePath());
		}
		catch (IOException e) {
			Logger.error(e);
		}
	}

	/**
	 * Import the preferences.
	 *
	 * @param preferencesFile The file for import.
	 */
	private void doImportPreferences(final File preferencesFile) {
		try {
			FileInputStream fileInputStream = new FileInputStream(preferencesFile);
			byte[] bytes = new byte[(int) preferencesFile.length()];
			int count = fileInputStream.read(bytes);
			if (count != preferencesFile.length()) {
				throw new RuntimeException("Error when reading preferences file");
			}
			fileInputStream.close();
			PreferenceUtil.importFromByteArray(bytes);
			DialogUtil.displayToast(getContext(), R.string.toast_settings_imported);
		}
		catch (Exception e) {
			Logger.error(e);
			DialogUtil.displayToast(getContext(), R.string.toast_settings_import_failed, e.getMessage());
		}
	}
}
