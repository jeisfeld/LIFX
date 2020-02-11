package de.jeisfeld.lifx.app.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import androidx.preference.PreferenceManager;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.Application;

/**
 * Utility class for handling the shared preferences.
 */
public final class PreferenceUtil {

	/**
	 * Hide default constructor.
	 */
	private PreferenceUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve the default shared preferences of the application.
	 *
	 * @return the default shared preferences.
	 */
	private static SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(Application.getAppContext());
	}

	/**
	 * Retrieve a String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static String getSharedPreferenceString(final int preferenceId) {
		return PreferenceUtil.getSharedPreferences().getString(Application.getAppContext().getString(preferenceId), null);
	}

	/**
	 * Retrieve a String shared preference, setting a default value if the preference is not set.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultId the String key of the default value.
	 * @return the corresponding preference value.
	 */
	public static String getSharedPreferenceString(final int preferenceId, final int defaultId) {
		String result = PreferenceUtil.getSharedPreferenceString(preferenceId);
		if (result == null) {
			result = Application.getAppContext().getString(defaultId);
			PreferenceUtil.setSharedPreferenceString(preferenceId, result);
		}
		return result;
	}

	/**
	 * Set a String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param s the target value of the preference.
	 */
	public static void setSharedPreferenceString(final int preferenceId, final String s) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putString(Application.getAppContext().getString(preferenceId), s);
		editor.apply();
	}

	/**
	 * Retrieve a boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static boolean getSharedPreferenceBoolean(final int preferenceId) {
		return PreferenceUtil.getSharedPreferences().getBoolean(Application.getAppContext().getString(preferenceId), false);
	}

	/**
	 * Set a Boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param b the target value of the preference.
	 */
	public static void setSharedPreferenceBoolean(final int preferenceId, final boolean b) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putBoolean(Application.getAppContext().getString(preferenceId), b);
		editor.apply();
	}

	/**
	 * Retrieve an integer shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static int getSharedPreferenceInt(final int preferenceId, final int defaultValue) {
		return PreferenceUtil.getSharedPreferences().getInt(Application.getAppContext().getString(preferenceId), defaultValue);
	}

	/**
	 * Set an integer shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i the target value of the preference.
	 */
	public static void setSharedPreferenceInt(final int preferenceId, final int i) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putInt(Application.getAppContext().getString(preferenceId), i);
		editor.apply();
	}

	/**
	 * Increment a counter shared preference, and return the new value.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the new value.
	 */
	public static int incrementCounter(final int preferenceId) {
		int newValue = PreferenceUtil.getSharedPreferenceInt(preferenceId, 0) + 1;
		PreferenceUtil.setSharedPreferenceInt(preferenceId, newValue);
		return newValue;
	}

	/**
	 * Retrieve an integer from a shared preference string.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultId the String key of the default value. If not existing, value -1 is returned.
	 * @return the corresponding preference value.
	 */
	public static int getSharedPreferenceIntString(final int preferenceId, final Integer defaultId) {
		String resultString;

		if (defaultId == null) {
			resultString = PreferenceUtil.getSharedPreferenceString(preferenceId);
		}
		else {
			resultString = PreferenceUtil.getSharedPreferenceString(preferenceId, defaultId);
		}
		if (resultString == null || resultString.length() == 0) {
			return -1;
		}
		try {
			return Integer.parseInt(resultString);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Set a string shared preference from an integer.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i the target value of the preference.
	 */
	public static void setSharedPreferenceIntString(final int preferenceId, final int i) {
		PreferenceUtil.setSharedPreferenceString(preferenceId, Integer.toString(i));
	}

	/**
	 * Retrieve a long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static long getSharedPreferenceLong(final int preferenceId, final long defaultValue) {
		return PreferenceUtil.getSharedPreferences().getLong(Application.getAppContext().getString(preferenceId), defaultValue);
	}

	/**
	 * Set a long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i the target value of the preference.
	 */
	public static void setSharedPreferenceLong(final int preferenceId, final long i) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putLong(Application.getAppContext().getString(preferenceId), i);
		editor.apply();
	}

	/**
	 * Get a String set shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static Set<String> getSharedPreferenceStringSet(final int preferenceId) {
		return PreferenceUtil.getSharedPreferences().getStringSet(Application.getAppContext().getString(preferenceId), new HashSet<String>());
	}

	/**
	 * Retrieve a long from a shared preference string.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultId the String key of the default value. If not existing, value -1 is returned.
	 * @return the corresponding preference value.
	 */
	public static long getSharedPreferenceLongString(final int preferenceId, final Integer defaultId) {
		String resultString;

		if (defaultId == null) {
			resultString = PreferenceUtil.getSharedPreferenceString(preferenceId);
		}
		else {
			resultString = PreferenceUtil.getSharedPreferenceString(preferenceId, defaultId);
		}
		if (resultString == null || resultString.length() == 0) {
			return -1;
		}
		try {
			return Long.parseLong(resultString);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Set a string shared preference from a long.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i the target value of the preference.
	 */
	public static void setSharedPreferenceLongString(final int preferenceId, final long i) {
		PreferenceUtil.setSharedPreferenceString(preferenceId, Long.toString(i));
	}

	/**
	 * Retrieve a String List shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static ArrayList<String> getSharedPreferenceStringList(final int preferenceId) {
		String restoreString = PreferenceUtil.getSharedPreferenceString(preferenceId);
		if (restoreString == null || restoreString.length() == 0) {
			return new ArrayList<>();
		}

		String[] folderArray = restoreString.split("\\r?\\n");
		return new ArrayList<>(Arrays.asList(folderArray));
	}

	/**
	 * Set a String List shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param stringList the target value of the preference.
	 */
	public static void setSharedPreferenceStringList(final int preferenceId, final List<String> stringList) {
		if (stringList == null || stringList.size() == 0) {
			PreferenceUtil.removeSharedPreference(preferenceId);
		}
		else {
			StringBuilder saveStringBuffer = new StringBuilder();
			for (String string : stringList) {
				if (saveStringBuffer.length() > 0) {
					saveStringBuffer.append("\n");
				}
				saveStringBuffer.append(string);
			}
			PreferenceUtil.setSharedPreferenceString(preferenceId, saveStringBuffer.toString());
		}
	}

	/**
	 * Retrieve an Integer List shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static ArrayList<Integer> getSharedPreferenceIntList(final int preferenceId) {
		List<String> stringList = PreferenceUtil.getSharedPreferenceStringList(preferenceId);
		ArrayList<Integer> deviceIds = new ArrayList<>();
		for (String idString : stringList) {
			deviceIds.add(Integer.valueOf(idString));
		}
		return deviceIds;
	}

	/**
	 * Set an Integer List shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param intList the target value of the preference.
	 */
	public static void setSharedPreferenceIntList(final int preferenceId, final List<Integer> intList) {
		List<String> stringList = new ArrayList<>();
		for (int id : intList) {
			stringList.add(Integer.toString(id));
		}
		PreferenceUtil.setSharedPreferenceStringList(preferenceId, stringList);
	}

	/**
	 * Remove a shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 */
	public static void removeSharedPreference(final int preferenceId) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.remove(Application.getAppContext().getString(preferenceId));
		editor.apply();
	}

	/**
	 * Get an indexed preference key that allows to store a shared preference with index.
	 *
	 * @param preferenceId The base preference id
	 * @param index The index
	 * @return The indexed preference key.
	 */
	private static String getIndexedPreferenceKey(final int preferenceId, final Object index) {
		return Application.getAppContext().getString(preferenceId) + "[" + index + "]";
	}

	/**
	 * Retrieve an indexed String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @return the corresponding preference value.
	 */
	public static String getIndexedSharedPreferenceString(final int preferenceId, final Object index) {
		return PreferenceUtil.getSharedPreferences().getString(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), null);
	}

	/**
	 * Set an indexed String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param s the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceString(final int preferenceId, final Object index, final String s) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putString(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), s);
		editor.apply();
	}

	/**
	 * Retrieve an indexed boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static boolean getIndexedSharedPreferenceBoolean(final int preferenceId, final Object index, final boolean defaultValue) {
		return PreferenceUtil.getSharedPreferences().getBoolean(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param b the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceBoolean(final int preferenceId, final Object index, final boolean b) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putBoolean(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), b);
		editor.apply();
	}

	/**
	 * Retrieve an indexed int shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static int getIndexedSharedPreferenceInt(final int preferenceId, final Object index, final int defaultValue) {
		return PreferenceUtil.getSharedPreferences().getInt(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed int shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param i the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceInt(final int preferenceId, final Object index, final int i) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putInt(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), i);
		editor.apply();
	}

	/**
	 * Retrieve an indexed long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static long getIndexedSharedPreferenceLong(final int preferenceId, final Object index, final long defaultValue) {
		return PreferenceUtil.getSharedPreferences().getLong(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param i the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceLong(final int preferenceId, final Object index, final long i) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.putLong(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index), i);
		editor.apply();
	}

	/**
	 * Retrieve a String List indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @return the corresponding preference value.
	 */
	public static ArrayList<String> getIndexedSharedPreferenceStringList(final int preferenceId, final Object index) {
		String restoreString = PreferenceUtil.getIndexedSharedPreferenceString(preferenceId, index);
		if (restoreString == null || restoreString.length() == 0) {
			return new ArrayList<>();
		}

		String[] folderArray = restoreString.split("\\r?\\n");
		return new ArrayList<>(Arrays.asList(folderArray));
	}

	/**
	 * Set a String List indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @param stringList the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceStringList(final int preferenceId, final Object index, final List<String> stringList) {
		if (stringList == null || stringList.size() == 0) {
			PreferenceUtil.removeIndexedSharedPreference(preferenceId, index);
		}
		else {
			StringBuilder saveStringBuffer = new StringBuilder();
			for (String string : stringList) {
				if (saveStringBuffer.length() > 0) {
					saveStringBuffer.append("\n");
				}
				saveStringBuffer.append(string);
			}
			PreferenceUtil.setIndexedSharedPreferenceString(preferenceId, index, saveStringBuffer.toString());
		}
	}

	/**
	 * Remove an indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 */
	public static void removeIndexedSharedPreference(final int preferenceId, final Object index) {
		Editor editor = PreferenceUtil.getSharedPreferences().edit();
		editor.remove(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index));
		editor.apply();
	}

	/**
	 * Check the existence of an indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index The index
	 * @return True if the preference exists.
	 */
	public static boolean hasIndexedSharedPreference(final int preferenceId, final Object index) {
		return PreferenceUtil.getSharedPreferences().contains(PreferenceUtil.getIndexedPreferenceKey(preferenceId, index));
	}

}
