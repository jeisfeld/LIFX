package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding data for the display view of a multizone light.
 */
public class MultizoneViewModel extends LightViewModel {
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
	private MultiZoneLight getLight() {
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
	 * Set the colors.
	 *
	 * @param colors the colors to be set.
	 */
	public void updateColors(final MultizoneColors colors) {
		updateStoredColors(colors);

		synchronized (mRunningSetColorTasks) {
			mRunningSetColorTasks.add(new SetMultizoneColorsTask(this, colors));
			if (mRunningSetColorTasks.size() > 2) {
				mRunningSetColorTasks.remove(1);
			}
			if (mRunningSetColorTasks.size() == 1) {
				mRunningSetColorTasks.get(0).execute();
			}
		}
	}

	/**
	 * Get the relative brightness.
	 *
	 * @return The relative brightness.
	 */
	public LiveData<Double> getRelativeBrightness() {
		return mRelativeBrightness;
	}

	@Override
	public final void updateBrightness(final short brightness) {
		MultizoneColors oldColors = mColors.getValue();
		if (oldColors != null) {
			updateColors(oldColors.withRelativeBrightness(TypeUtil.toDouble(brightness)));
		}
	}

	@Override
	public final void checkColor() {
		new CheckMultizoneColorsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Update the stored colors and brightness with the given colors.
	 *
	 * @param colors The given colors.
	 */
	private void updateStoredColors(final MultizoneColors colors) {
		int maxBrightness = 0;
		for (int i = 0; i < getLight().getZoneCount(); i++) {
			maxBrightness = Math.max(maxBrightness, TypeUtil.toUnsignedInt(colors.getColor(i, getLight().getZoneCount()).getBrightness()));
		}
		if (maxBrightness == 0) {
			mRelativeBrightness.postValue(0.0);
			mColors.postValue(colors);
		}
		else {
			double relativeBrightness = maxBrightness / 65535.0; // MAGIC_NUMBER
			mRelativeBrightness.postValue(relativeBrightness);
			mColors.postValue(colors.withRelativeBrightness(1 / relativeBrightness));
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
			List<Color> colors = model.getLight().getColors();
			if (colors == null) {
				return null;
			}

			model.updateStoredColors(new MultizoneColors.Exact(colors));
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
		 * Constructor.
		 *
		 * @param model The underlying model.
		 * @param colors The colors.
		 */
		private SetMultizoneColorsTask(final MultizoneViewModel model, final MultizoneColors colors) {
			mModel = new WeakReference<>(model);
			mColors = colors;
		}

		@Override
		protected MultizoneColors doInBackground(final MultizoneColors... colors) {
			MultizoneViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			try {
				model.getLight().setColors(0, false, mColors);
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
			model.updateStoredColors(mColors);
		}

		@Override
		public void execute() {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		}
	}
}
