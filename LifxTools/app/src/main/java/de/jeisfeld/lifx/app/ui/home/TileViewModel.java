package de.jeisfeld.lifx.app.ui.home;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.TileChainColors;

/**
 * Class holding data for the display view of a tile chain.
 */
public class TileViewModel extends LightViewModel {
	/**
	 * The stored Colors of the device.
	 */
	private final MutableLiveData<TileChainColors> mColors;
	/**
	 * The stored relative brightness of the device.
	 */
	private final MutableLiveData<Double> mRelativeBrightness;

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param light The light.
	 */
	public TileViewModel(final Context context, final TileChain light) {
		super(context, light);
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
	public TileChain getLight() {
		return (TileChain) getDevice();
	}

	/**
	 * Get the colors.
	 *
	 * @return The colors.
	 */
	public LiveData<TileChainColors> getColors() {
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

	@Override
	public final void updateColor(final Color color) {
		updateStoredColors(new TileChainColors.Fixed(color), 1);
		super.updateColor(color);
	}

	/**
	 * Set the colors.
	 *
	 * @param colors the colors to be set.
	 * @param brightnessFactor the brightness factor.
	 */
	public void updateColors(final TileChainColors colors, final double brightnessFactor) {
		updateStoredColors(colors, brightnessFactor);

		synchronized (mRunningSetColorTasks) {
			mRunningSetColorTasks.add(new SetTileChainColorsTask(this, colors.withRelativeBrightness(brightnessFactor)));
			if (mRunningSetColorTasks.size() > 2) {
				mRunningSetColorTasks.remove(1);
			}
			if (mRunningSetColorTasks.size() == 1) {
				mRunningSetColorTasks.get(0).execute();
			}
		}
	}

	@Override
	public final void updateBrightness(final double brightness) {
		TileChainColors oldColors = mColors.getValue();
		if (oldColors != null) {
			updateColors(oldColors, brightness);
		}
	}

	@Override
	public final void checkColor() {
		new CheckTileChainColorsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected final boolean isRefreshAllowed() {
		// Due to tendency for connectivity issues, check Multizone light only if disconnected or if colors have not yet been initialized.
		return super.isRefreshAllowed()
				&& (mColors.getValue() == null || (!Power.ON.equals(mPower.getValue()) && !Power.OFF.equals(mPower.getValue())));
	}

	/**
	 * Update the stored colors and brightness with the given colors.
	 *
	 * @param colors The given colors.
	 * @param brightnessFactor the brightness factor.
	 */
	private void updateStoredColors(final TileChainColors colors, final double brightnessFactor) {
		int maxBrightness = colors.getMaxBrightness(getLight());
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

	/**
	 * An async task for checking the tile chain colors.
	 */
	private static final class CheckTileChainColorsTask extends AsyncTask<String, String, TileChainColors> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<TileViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckTileChainColorsTask(final TileViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected TileChainColors doInBackground(final String... strings) {
			TileViewModel model = mModel.get();
			if (model == null) {
				return null;
			}
			if (model.getLight().getTileInfo() == null) {
				model.getLight().refreshTileInfo();
				if (model.getLight().getTileInfo() == null) {
					return null;
				}
			}
			TileChainColors colors = model.getLight().getColors();
			if (colors == null) {
				return null;
			}

			model.updateStoredColors(colors, 1);
			return null;
		}
	}

	/**
	 * An async task for setting the tile chain colors.
	 */
	private static final class SetTileChainColorsTask extends AsyncTask<TileChainColors, String, TileChainColors> implements AsyncExecutable {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<TileViewModel> mModel;
		/**
		 * The colors to be set.
		 */
		private final TileChainColors mColors;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 * @param colors The colors.
		 */
		private SetTileChainColorsTask(final TileViewModel model, final TileChainColors colors) {
			mModel = new WeakReference<>(model);
			mColors = colors;
		}

		@Override
		protected TileChainColors doInBackground(final TileChainColors... colors) {
			TileViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			try {
				model.getLight().setColors(0, mColors);
				return mColors;
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(final TileChainColors color) {
			TileViewModel model = mModel.get();
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

}
