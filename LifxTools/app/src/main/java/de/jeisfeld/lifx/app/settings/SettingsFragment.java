package de.jeisfeld.lifx.app.settings;

import android.os.Bundle;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

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
	 * The filename prefix of the file for export/import preferences.
	 */
	private static final String EXPORT_FILENAME_PREFIX = "LifxPreferences-";
	/**
	 * The date format used within export file names.
	 */
	private static final String EXPORT_FILENAME_DATEFORMAT = "yyyyMMdd-HHmmss";
	/**
	 * The filename suffix of the file for export/import preferences.
	 */
	private static final String EXPORT_FILENAME_SUFFIX = ".exp";

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
			File preferencesFile = getExportFile();
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
			File[] preferencesFiles = getImportFiles();
			if (preferencesFiles == null || preferencesFiles.length == 0) {
				DialogUtil.displayConfirmationMessage(requireActivity(), R.string.title_dialog_import, R.string.message_dialog_import_missing_file,
						requireContext().getExternalFilesDir(null));
			}
			else if (preferencesFiles.length == 1) {
				doImportPreferencesAfterConfirmation(preferencesFiles[0]);
			}
			else {
				Arrays.sort(preferencesFiles, (o1, o2) -> o2.getName().compareTo(o1.getName()));
				SelectFileDialogFragment.displaySelectImportFileDialog(requireActivity(),
						this::doImportPreferencesAfterConfirmation,
						R.string.title_dialog_select_date_for_import,
						this::getDisplayName,
						preferencesFiles);
			}

			return true;
		});
	}

	/**
	 * Get the file for export.
	 *
	 * @return The file for export.
	 */
	private File getExportFile() {
		return new File(requireContext().getExternalFilesDir(null),
				EXPORT_FILENAME_PREFIX + DateFormat.format(EXPORT_FILENAME_DATEFORMAT, new Date()) + EXPORT_FILENAME_SUFFIX);
	}

	/**
	 * Get the files for import.
	 *
	 * @return The files for import.
	 */
	private File[] getImportFiles() {
		return Objects.requireNonNull(requireContext().getExternalFilesDir(null))
				.listFiles((dir, name) -> name.startsWith(EXPORT_FILENAME_PREFIX) && name.endsWith(EXPORT_FILENAME_SUFFIX));
	}

	/**
	 * Get the display name of an import file.
	 *
	 * @param importFile The import file.
	 * @return The display name.
	 */
	private String getDisplayName(final File importFile) {
		String name = importFile.getName();
		try {
			name = name.substring(0, name.lastIndexOf(EXPORT_FILENAME_SUFFIX));
			name = name.substring(EXPORT_FILENAME_PREFIX.length());
			name = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMddHHmmss"),
					new SimpleDateFormat(EXPORT_FILENAME_DATEFORMAT, Locale.getDefault()).parse(name)).toString();
		}
		catch (Exception e) {
			// ignore
		}
		return name;
	}

	/**
	 * Import preferences from file, after first confirming overwrite.
	 *
	 * @param preferencesFile The file for import.
	 */
	private void doImportPreferencesAfterConfirmation(final File preferencesFile) {
		DialogUtil.displayConfirmationMessage(requireActivity(), new ConfirmDialogListener() {
			@Override
			public void onDialogPositiveClick(final DialogFragment dialog) {
				try {
					FileInputStream fileInputStream = new FileInputStream(preferencesFile);
					byte[] bytes = new byte[(int) preferencesFile.length()];
					int count = fileInputStream.read(bytes);
					if (count != preferencesFile.length()) {
						fileInputStream.close();
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

			@Override
			public void onDialogNegativeClick(final DialogFragment dialog) {
				DialogUtil.displayToast(requireContext(), R.string.toast_settings_import_canceled);
			}
		}, R.string.title_dialog_import, R.string.button_cancel, R.string.button_overwrite, R.string.message_dialog_import_overwrite);
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
}
