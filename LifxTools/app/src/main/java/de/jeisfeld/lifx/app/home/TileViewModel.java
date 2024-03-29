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
import de.jeisfeld.lifx.app.animation.LifxAnimationService;
import de.jeisfeld.lifx.app.animation.LifxAnimationService.AnimationStatus;
import de.jeisfeld.lifx.app.animation.TileChainFlame;
import de.jeisfeld.lifx.app.animation.TileChainMorph;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileChainColors.Fixed;
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
	 * @param light   The light.
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
	public final void updateStoredColor(final Color color) {
		super.updateStoredColor(color);
		updateStoredColors(new TileChainColors.Fixed(color), 1);
	}

	@Override
	public final void updateColor(final Color color, final boolean isImmediate) {
		updateStoredColors(new TileChainColors.Fixed(color), 1);
		super.updateColor(color, isImmediate);
	}

	@Override
	public final void updateSelectedBrightness(final double brightness) {
		super.updateSelectedBrightness(brightness);
		mRelativeBrightness.postValue(brightness);
	}

	/**
	 * Update the main color for color pickers.
	 *
	 * @param color The color for update.
	 */
	private void updateStoredMainColor(final Color color) {
		if (color != null) {
			super.updateStoredColor(color);
		}
	}

	/**
	 * Set the colors.
	 *
	 * @param colors           the colors to be set.
	 * @param brightnessFactor the brightness factor.
	 * @param isImmediate      Flag indicating if the change should be immediate.
	 * @param stopAnimation    Flag indicating if animation should be stopped.
	 */
	public void updateColors(final TileChainColors colors, final double brightnessFactor, final boolean isImmediate, final boolean stopAnimation) {
		if (stopAnimation) {
			stopAnimationOrAlarm();
		}
		synchronized (mRunningSetColorTasks) {
			if (colors == null) {
				mRelativeBrightness.postValue(brightnessFactor);
				mRunningSetColorTasks.add(new SetTileChainColorsTask(this, brightnessFactor, isImmediate));
			}
			else {
				updateStoredColors(colors, brightnessFactor);
				mRunningSetColorTasks.add(new SetTileChainColorsTask(this, colors.withRelativeBrightness(brightnessFactor), isImmediate));
			}
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
			if (LifxAnimationService.getAnimationStatus(getLight().getTargetAddress()) == AnimationStatus.NATIVE) {
				updateColors(null, brightness, true, false);
			}
			else {
				updateColors(oldColors, brightness, true, false);
			}
		}
	}

	@Override
	public final void checkColor() {
		new CheckTileChainColorsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected final boolean isRefreshColorsAllowed() {
		// Due to tendency for connectivity issues, check Multizone light only if disconnected or if colors have not yet been initialized.
		return super.isRefreshColorsAllowed()
				&& (mColors.getValue() == null || (!Power.ON.equals(mPower.getValue()) && !Power.OFF.equals(mPower.getValue())));
	}

	/**
	 * Update the stored colors and brightness with the given colors.
	 *
	 * @param colors           The given colors.
	 * @param brightnessFactor the brightness factor.
	 */
	public void updateStoredColors(final TileChainColors colors, final double brightnessFactor) {
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
		if (colors instanceof TileChainColors.Fixed) {
			super.updateStoredColor(((Fixed) colors).getColor().withRelativeBrightness(brightnessFactor));
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
		@SuppressWarnings("deprecation")
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

			model.updateStoredMainColor(model.getLight().getColor());
			model.updateStoredColors(colors, 1);

			// Check animation status
			AnimationStatus animationStatus = LifxAnimationService.getAnimationStatus(model.getLight().getTargetAddress());
			if (animationStatus != AnimationStatus.CUSTOM) {
				TileEffectInfo effectInfo = model.getLight().getEffectInfo();
				if (effectInfo != null) {
					AnimationData animationData;
					switch (effectInfo.getType()) {
					case FLAME:
						if (animationStatus == AnimationStatus.OFF) {
							animationData = new TileChainFlame(effectInfo.getSpeed(), true);
							model.startAnimation(animationData);
						}
						break;
					case MORPH:
						if (animationStatus == AnimationStatus.OFF) {
							animationData = new TileChainMorph(effectInfo.getSpeed(), effectInfo.getPaletteColors(), true);
							model.startAnimation(animationData);
						}
						break;
					case OFF:
						if (animationStatus == AnimationStatus.NATIVE) {
							model.stopAnimation();
						}
						break;
					default:
						break;
					}
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
		 * The brightness to be set.
		 */
		private final Double mBrightness;
		/**
		 * Flag indicating if the change should be immediate.
		 */
		private final boolean mIsImmediate;

		/**
		 * Constructor.
		 *
		 * @param model       The underlying model.
		 * @param colors      The colors.
		 * @param isImmediate Flag indicating if the change should be immediate.
		 */
		@SuppressWarnings("deprecation")
		private SetTileChainColorsTask(final TileViewModel model, final TileChainColors colors, final boolean isImmediate) {
			mModel = new WeakReference<>(model);
			mColors = colors;
			mBrightness = null;
			mIsImmediate = isImmediate;
		}

		@SuppressWarnings("deprecation")
		private SetTileChainColorsTask(final TileViewModel model, final double brightness, final boolean isImmediate) {
			mModel = new WeakReference<>(model);
			mColors = null;
			mBrightness = brightness;
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
				if (mColors == null) {
					model.getLight().setBrightness(mBrightness);
				}
				else {
					if (model.mPower.getValue() != null && model.mPower.getValue().isOff() && isAutoOn()) {
						model.getLight().setColors(mColors, 0, false);
						model.getLight().setPower(true, colorDuration, false);
					}
					else {
						model.getLight().setColors(mColors, colorDuration, false);
					}
				}
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
			if (isAutoOn()) {
				model.updatePowerButton(Power.ON);
			}
		}

		@Override
		public void execute() {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		}
	}

}
