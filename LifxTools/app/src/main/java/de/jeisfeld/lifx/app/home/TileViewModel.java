package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.animation.AnimationData;
import de.jeisfeld.lifx.app.animation.TileChainFlame;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;

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

	/**
	 * Get the effective colors with brightness.
	 *
	 * @return The colors
	 */
	public TileChainColors getColorsWithBrightness() {
		TileChainColors colors = mColors.getValue();
		Double relativeBrightness = mRelativeBrightness.getValue();
		if (colors != null && relativeBrightness != null) {
			return colors.withRelativeBrightness(relativeBrightness);
		}
		else {
			return colors;
		}
	}

	@Override
	protected final void updateColorFromGroup(final Color color) {
		updateStoredColors(new TileChainColors.Fixed(color), 1);
		super.updateColorFromGroup(color);
	}

	@Override
	public final void updateColor(final Color color, final boolean isImmediate) {
		updateStoredColors(new TileChainColors.Fixed(color), 1);
		super.updateColor(color, isImmediate);
	}

	/**
	 * Set the colors.
	 *
	 * @param colors the colors to be set.
	 * @param brightnessFactor the brightness factor.
	 * @param isImmediate Flag indicating if the change should be immediate.
	 */
	public void updateColors(final TileChainColors colors, final double brightnessFactor, final boolean isImmediate) {
		updateStoredColors(colors, brightnessFactor);

		stopAnimationOrAlarm();
		synchronized (mRunningSetColorTasks) {
			mRunningSetColorTasks.add(new SetTileChainColorsTask(this, colors.withRelativeBrightness(brightnessFactor), isImmediate));
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
		TileChainColors oldColors = mColors.getValue();
		if (oldColors != null) {
			updateColors(oldColors, brightness, true);
		}
	}

	@Override
	public final void checkColor() {
		super.checkColor();
		new CheckTileChainColorsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Check if native animation is running on the device.
	 */
	protected final void checkNativeAnimation() {
		if (mAnimationStatus.getValue() != Boolean.TRUE) {
			new CheckTileChainAnimationTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
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

	@Override
	protected final void updateStoredColor(final StoredColor storedColor) {
		if (storedColor instanceof StoredTileColors) {
			updateColors(((StoredTileColors) storedColor).getColors(), 1, false);
		}
		else {
			super.updateStoredColor(storedColor);
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
			if (model.getLight().getTileInfo() == null || model.getLight().getTileInfo().size() == 0
					|| model.getLight().getTileInfo().get(0).getAccelerationX() == 0) {
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
	 * An async task for checking the tile chain animation status.
	 */
	private static final class CheckTileChainAnimationTask extends AsyncTask<String, String, TileChainColors> {
		/**
		 * A weak reference to the underlying model.
		 */
		private final WeakReference<TileViewModel> mModel;

		/**
		 * Constructor.
		 *
		 * @param model The underlying model.
		 */
		private CheckTileChainAnimationTask(final TileViewModel model) {
			mModel = new WeakReference<>(model);
		}

		@Override
		protected TileChainColors doInBackground(final String... strings) {
			TileViewModel model = mModel.get();
			if (model == null) {
				return null;
			}

			TileEffectInfo effectInfo = model.getLight().getEffectInfo();
			if (effectInfo != null) {
				switch (effectInfo.getType()) {
				case FLAME:
					AnimationData animationData = new TileChainFlame(effectInfo.getSpeed(), true);
					model.startAnimation(animationData);
					break;
				case MORPH:
					// TODO
					break;
				default:
					break;
				}
			}
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
		private SetTileChainColorsTask(final TileViewModel model, final TileChainColors colors, final boolean isImmediate) {
			mModel = new WeakReference<>(model);
			mColors = colors;
			mIsImmediate = isImmediate;
		}

		@Override
		protected TileChainColors doInBackground(final TileChainColors... colors) {
			TileViewModel model = mModel.get();
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
