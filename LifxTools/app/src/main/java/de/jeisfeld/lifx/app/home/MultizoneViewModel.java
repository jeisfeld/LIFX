package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.app.view.MultiColorPickerDialogFragment;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * Class holding data for the display view of a multizone light.
 */
public class MultizoneViewModel extends LightViewModel {
	/**
	 * Colortemp on zone 0 indicating potential custom setting.
	 */
	private static final short COLORTEMP_FLAG1 = Color.WHITE_TEMPERATURE + 1;
	/**
	 * Colortemp on zone 1 indicating colors set by multipicker.
	 */
	private static final short COLORTEMP_FLAG2_MULTIPICKER = Color.WHITE_TEMPERATURE + 2;
	/**
	 * Colortemp on zone 1 indicating colors set by multipicker.
	 */
	private static final short COLORTEMP_FLAG2_MULTIPICKER_CYCLIC = Color.WHITE_TEMPERATURE + 3; // MAGIC_NUMBER

	/**
	 * The stored Colors of the device.
	 */
	private final MutableLiveData<MultizoneColors> mColors;
	/**
	 * The stored relative brightness of the device.
	 */
	private final MutableLiveData<Double> mRelativeBrightness;

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param multiZoneLight The multiZone light.
	 */
	public MultizoneViewModel(final Context context, final MultiZoneLight multiZoneLight) {
		super(context, multiZoneLight);
		mColors = new MutableLiveData<>();
		mRelativeBrightness = new MutableLiveData<>();
		mRelativeBrightness.setValue(1.0);
	}

	/**
	 * Get the light.
	 *
	 * @return The light.
	 */
	@Override
	protected MultiZoneLight getLight() {
		return (MultiZoneLight) getDevice();
	}

	/**
	 * Get the colors.
	 *
	 * @return The colors.
	 */
	public LiveData<MultizoneColors> getColors() {
		return mColors;
	}

	/**
	 * Get the relative brightness.
	 *
	 * @return The relative brightness.
	 */
	public LiveData<Double> getRelativeBrightness() {
		return mRelativeBrightness;
	}

	/**
	 * Get the effective colors with brightness.
	 *
	 * @return The colors
	 */
	public MultizoneColors getColorsWithBrightness() {
		MultizoneColors colors = mColors.getValue();
		Double relativeBrightness = mRelativeBrightness.getValue();
		if (colors != null && relativeBrightness != null) {
			return colors.withRelativeBrightness(relativeBrightness);
		}
		else {
			return colors;
		}
	}

	@Override
	public final void updateColor(final Color color, final boolean isImmediate) {
		updateStoredColors(new MultizoneColors.Fixed(color), 1);
		super.updateColor(color, isImmediate);
	}

	/**
	 * Set the colors.
	 *
	 * @param colors the colors to be set.
	 * @param brightnessFactor the brightness factor.
	 * @param isImmediate Flag indicating if the change should be immediate.
	 */
	public void updateColors(final MultizoneColors colors, final double brightnessFactor, final boolean isImmediate) {
		updateStoredColors(colors, brightnessFactor);

		stopAnimationOrAlarm();
		synchronized (mRunningSetColorTasks) {
			mRunningSetColorTasks.add(new SetMultizoneColorsTask(this, colors.withRelativeBrightness(brightnessFactor), isImmediate));
			if (mRunningSetColorTasks.size() > 2) {
				mRunningSetColorTasks.remove(1);
			}
			if (mRunningSetColorTasks.size() == 1) {
				mRunningSetColorTasks.get(0).execute();
			}
		}
	}

	@Override
	protected final void doUpdateBrightness(final double brightness) {
		MultizoneColors oldColors = mColors.getValue();
		if (oldColors != null) {
			updateColors(oldColors, brightness, true);
		}
	}

	@Override
	public final void checkColor() {
		super.checkColor();
		new CheckMultizoneColorsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected final boolean isRefreshAllowed() {
		// Due to tendency for connectivity issues, check Multizone light only if disconnected or if colors have not yet been initialized.
		return super.isRefreshAllowed()
				&& (mColors.getValue() == null || (!Power.ON.equals(mPower.getValue()) && !Power.OFF.equals(mPower.getValue())));
	}

	/**
	 * Get MultizoneColors object from the colors read from the device.
	 *
	 * @param colors The colors read.
	 * @return The MultizoneColors object.
	 */
	public static MultizoneColors fromColors(final List<Color> colors) {
		if (colors == null) {
			return null;
		}
		else if (colors.size() < MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT + 2) {
			return new MultizoneColors.Exact(colors);
		}
		else if (colors.get(0).getColorTemperature() == COLORTEMP_FLAG1 && (colors.get(1).getColorTemperature() == COLORTEMP_FLAG2_MULTIPICKER
				|| colors.get(1).getColorTemperature() == COLORTEMP_FLAG2_MULTIPICKER_CYCLIC)) {
			boolean[] flags = new boolean[MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT + 1];
			int flagCount = 0;
			for (int i = 0; i < MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT; i++) {
				flags[i] = colors.get(i + 2).getColorTemperature() == COLORTEMP_FLAG1;
				if (flags[i]) {
					flagCount++;
				}
			}
			// Cyclic flag
			flags[MultiColorPickerDialogFragment.CYCLIC_FLAG_INDEX] = colors.get(1).getColorTemperature() == COLORTEMP_FLAG2_MULTIPICKER_CYCLIC;
			if (flagCount > 0) {
				return new FlaggedMultizoneColors(MultizoneColors.Interpolated.fromColors(flagCount,
						flags[MultiColorPickerDialogFragment.CYCLIC_FLAG_INDEX], colors), flags);
			}
			else {
				return new MultizoneColors.Exact(colors);
			}
		}
		else {
			return new MultizoneColors.Exact(colors);
		}
	}

	/**
	 * Update the stored colors and brightness with the given colors.
	 *
	 * @param colors The given colors.
	 * @param brightnessFactor the brightness factor.
	 */
	private void updateStoredColors(final MultizoneColors colors, final double brightnessFactor) {
		int maxBrightness = colors.getMaxBrightness(getLight().getZoneCount());
		if (maxBrightness == 0) {
			mRelativeBrightness.postValue(0.0);
			mColors.postValue(colors);
		}
		else {
			double relativeBrightness = maxBrightness / 65535.0; // MAGIC_NUMBER
			mRelativeBrightness.postValue(Math.min(1, brightnessFactor * relativeBrightness));
			mColors.postValue(colors.withRelativeBrightness(1 / relativeBrightness));
		}
	}

	@Override
	protected final void updateStoredColor(final StoredColor storedColor) {
		if (storedColor instanceof StoredMultizoneColors) {
			updateColors(((StoredMultizoneColors) storedColor).getColors(), 1, false);
		}
		else {
			super.updateStoredColor(storedColor);
		}
	}

	/**
	 * An async task for checking the multizone colors.
	 */
	private static final class CheckMultizoneColorsTask extends AsyncTask<String, String, MultizoneColors> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<MultizoneViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckMultizoneColorsTask(final MultizoneViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected MultizoneColors doInBackground(final String... strings) {
			MultizoneViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			// Ensure that firmware build time is available.
			model.getLight().getFirmwareBuildTime();
			List<Color> colors = model.getLight().getColors();
			if (colors == null) {
				return null;
			}

			model.updateStoredColors(fromColors(colors), 1);
			return null;
		}
	}

	/**
	 * An async task for setting the multizone colors.
	 */
	private static final class SetMultizoneColorsTask extends AsyncTask<MultizoneColors, String, MultizoneColors> implements AsyncExecutable {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<MultizoneViewModel> mModel;
		/**
		 * The colors to be set.
		 */
		private final MultizoneColors mColors;
		/**
		 * Flag indicating if the change should be immediate.
		 */
		private final boolean mIsImmediate;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 * @param colors The colors.
		 * @param isImmediate Flag indicating if the change should be immediate.
		 */
		private SetMultizoneColorsTask(final MultizoneViewModel model, final MultizoneColors colors, final boolean isImmediate) {
			mModel = new WeakReference<>(model);
			mColors = colors;
			mIsImmediate = isImmediate;
		}

		@Override
		protected MultizoneColors doInBackground(final MultizoneColors... colors) {
			MultizoneViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			try {
				int colorDuration = mIsImmediate ? 0
						: PreferenceUtil.getSharedPreferenceIntString(
								R.string.key_pref_color_duration, R.string.pref_default_color_duration);
				model.getLight().setColors(mColors, colorDuration, false);
				return mColors;
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(final MultizoneColors color) {
			MultizoneViewModel model = mModel.get();
			if (model == null) {
				return;
			}
			synchronized (model.mRunningSetColorTasks) {
				model.mRunningSetColorTasks.remove(this);
				if (model.mRunningSetColorTasks.size() > 0) {
					model.mRunningSetColorTasks.get(0).execute();
				}
			}
		}

		@Override
		public void execute() {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		}
	}

	/**
	 * Multizone colors storing flags.
	 */
	public static final class FlaggedMultizoneColors extends MultizoneColors {
		/**
		 * The default serializable version id.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * A set of custom flags that may be set for custom storage.
		 */
		private final boolean[] mFlags;
		/**
		 * The multizone colors behind this.
		 */
		private final MultizoneColors mMultizoneColors;

		/**
		 * Constructor.
		 *
		 * @param multizoneColors The base multizone colors without flag.
		 */
		private FlaggedMultizoneColors(final MultizoneColors multizoneColors) {
			mMultizoneColors = multizoneColors;
			mFlags = new boolean[MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT + 1];
		}

		/**
		 * Constructor.
		 *
		 * @param multizoneColors The base multizone colors without flag.
		 * @param flags The flags.
		 */
		public FlaggedMultizoneColors(final MultizoneColors multizoneColors, final boolean[] flags) {
			this(multizoneColors);
			setFlags(flags);
		}

		@Override
		public Color getColor(final int zoneIndex, final int zoneCount) {
			Color color = mMultizoneColors.getColor(zoneIndex, zoneCount);
			short colorTemperature;
			if (zoneIndex == 0) {
				colorTemperature = COLORTEMP_FLAG1;
			}
			else if (zoneIndex == 1) {
				colorTemperature = mFlags[MultiColorPickerDialogFragment.CYCLIC_FLAG_INDEX]
						? COLORTEMP_FLAG2_MULTIPICKER_CYCLIC
						: COLORTEMP_FLAG2_MULTIPICKER;
			}
			else if (zoneIndex >= 0 && zoneIndex < MultiColorPickerDialogFragment.MULTIZONE_PICKER_COUNT + 2) {
				colorTemperature = mFlags[zoneIndex - 2] ? COLORTEMP_FLAG1 : Color.WHITE_TEMPERATURE;
			}
			else {
				colorTemperature = Color.WHITE_TEMPERATURE;
			}

			return new Color(color.getHue(), color.getSaturation(), color.getBrightness(), colorTemperature);
		}

		/**
		 * Get the custom flags.
		 *
		 * @return The custom flags.
		 */
		public boolean[] getFlags() {
			return mFlags;
		}

		/**
		 * Get the base MultizoneColors without flags.
		 *
		 * @return The base MultizoneColors.
		 */
		public MultizoneColors getBaseColors() {
			MultizoneColors colors = this;
			while (colors instanceof FlaggedMultizoneColors) {
				colors = ((FlaggedMultizoneColors) colors).mMultizoneColors;
			}
			return colors;
		}

		/**
		 * Set the flags.
		 *
		 * @param flags The flags.
		 */
		private void setFlags(final boolean[] flags) {
			System.arraycopy(flags, 0, mFlags, 0, Math.min(flags.length, mFlags.length));
		}

		/**
		 * Get a string representation of the flags.
		 *
		 * @return A string representation of the flags
		 */
		private String getFlagString() {
			if (mFlags == null) {
				return "";
			}
			StringBuilder stringBuilder = new StringBuilder("[");
			for (boolean flag : mFlags) {
				stringBuilder.append(flag ? "1" : "0");
			}
			stringBuilder.append("]");
			return stringBuilder.toString();
		}

		@Override
		public MultizoneColors withRelativeBrightness(final double brightnessFactor) {
			return new FlaggedMultizoneColors(mMultizoneColors.withRelativeBrightness(brightnessFactor), mFlags);
		}

		@Override
		public String toString() {
			return "Flagged" + mMultizoneColors.toString() + getFlagString();
		}

		@Override
		public MultizoneColors shift(final int shiftCount) {
			return new FlaggedMultizoneColors(mMultizoneColors.shift(shiftCount), mFlags);
		}

		@Override
		public MultizoneColors stretch(final double stretchFactor) {
			return new FlaggedMultizoneColors(mMultizoneColors.stretch(stretchFactor), mFlags);
		}

		@Override
		public MultizoneColors add(final MultizoneColors other, final double quota) {
			return new FlaggedMultizoneColors(mMultizoneColors.add(other, quota), mFlags);
		}

		/**
		 * Get base colors of Interpolation.
		 *
		 * @return The base colors if this is Interpolation.
		 */
		protected List<Color> getInterpolationColors() {
			if (mMultizoneColors instanceof MultizoneColors.Interpolated) {
				return ((Interpolated) mMultizoneColors).getColors();
			}
			else {
				return null;
			}
		}

		/**
		 * Get the interpolation base color for a certain color picker.
		 *
		 * @param index The color picker index.
		 * @return The interpolation base color.
		 */
		protected Color getInterpolationColor(final int index) {
			List<Color> colors = getInterpolationColors();
			if (colors == null) {
				return null;
			}
			int j = 0;
			for (int i = 0; i < mFlags.length; i++) {
				if (mFlags[i]) {
					if (i == index && colors.size() > j) {
						return colors.get(j);
					}
					j++;
				}
			}
			return null;
		}
	}
}
