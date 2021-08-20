package de.jeisfeld.lifx.app.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import javax.annotation.Nonnull;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.DeviceAdapter;
import de.jeisfeld.lifx.app.home.TileViewModel;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Dialog for handling a picked image for a tile chain.
 */
public class TuneTilesDialogFragment extends DialogFragment {
	/**
	 * Parameter to pass the colors to the DialogFragment.
	 */
	private static final String PARAM_COLORS = "colors";
	/**
	 * Parameter to pass the picked bitmap to the DialogFragment.
	 */
	private static final String PARAM_INITIAL_BRIGHTNESS = "initialBrightness";

	/**
	 * Display a dialog for handling a picked image for a tile chain.
	 *
	 * @param activity the current activity
	 * @param model the light model.
	 * @param listener The listener waiting for the response
	 */
	public static void displayPickedImageDialog(final FragmentActivity activity,
			final TileViewModel model,
			final TuneTilesDialogListener listener) {
		TuneTilesDialogFragment fragment = new TuneTilesDialogFragment();
		fragment.setListener(listener);
		fragment.setModel(model);

		TileChainColors colors = model.getColors().getValue();
		if (colors == null) {
			return;
		}
		Double initialBrightness = model.getRelativeBrightness().getValue();
		if (initialBrightness == null) {
			initialBrightness = 1.0;
		}

		Bundle bundle = new Bundle();
		bundle.putSerializable(PARAM_COLORS, colors);
		bundle.putDouble(PARAM_INITIAL_BRIGHTNESS, initialBrightness);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<TuneTilesDialogListener> mListener = null;
	/**
	 * The tile view model.
	 */
	private MutableLiveData<TileViewModel> mModel = null;
	/**
	 * The current colors.
	 */
	private TileChainColors mCurrentColors = null;
	/**
	 * The base colors.
	 */
	private TileChainColors mBaseColors = null;
	/**
	 * The initial colors.
	 */
	private TileChainColors mInitialColors = null;

	/**
	 * Set the model.
	 *
	 * @param model The model.
	 */
	public final void setModel(final TileViewModel model) {
		mModel = new MutableLiveData<>(model);
	}

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final TuneTilesDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	@Nonnull
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		assert getArguments() != null;
		mBaseColors = (TileChainColors) getArguments().getSerializable(PARAM_COLORS);
		double initialBrightness = getArguments().getDouble(PARAM_INITIAL_BRIGHTNESS);
		mInitialColors = mBaseColors.withRelativeBrightness(initialBrightness);
		mCurrentColors = mInitialColors;

		TileViewModel model = mModel.getValue();
		if (model == null) {
			dismiss();
			return super.onCreateDialog(savedInstanceState);
		}

		final View view = View.inflate(requireActivity(), R.layout.dialog_tile_tune, null);

		final SeekBar seekBarBrightness = view.findViewById(R.id.seekBarBrightness);
		final SeekBar seekBarContrast = view.findViewById(R.id.seekBarContrast);
		final SeekBar seekBarSaturation = view.findViewById(R.id.seekBarSaturation);
		final SeekBar seekBarHue = view.findViewById(R.id.seekBarHue);
		final SeekBar seekBarColorTemperature = view.findViewById(R.id.seekBarColorTemperature);
		seekBarBrightness.setProgress((int) (initialBrightness * seekBarBrightness.getMax()));

		final OnBrightnessContrastChangeListener seekBarListener = new OnBrightnessContrastChangeListener(seekBarBrightness, seekBarContrast,
				seekBarSaturation, seekBarHue, seekBarColorTemperature);
		seekBarBrightness.setOnSeekBarChangeListener(seekBarListener);
		seekBarContrast.setOnSeekBarChangeListener(seekBarListener);
		seekBarSaturation.setOnSeekBarChangeListener(seekBarListener);
		seekBarHue.setOnSeekBarChangeListener(seekBarListener);
		seekBarColorTemperature.setOnSeekBarChangeListener(seekBarListener);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.title_dialog_image)
				.setView(view) //
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogNegativeClick(TuneTilesDialogFragment.this, mInitialColors);
					}
				}) //
				.setPositiveButton(R.string.button_ok, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogPositiveClick(TuneTilesDialogFragment.this, mCurrentColors);
					}
				});
		return builder.create();
	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(TuneTilesDialogFragment.this, mInitialColors);
		}
		super.onCancel(dialogInterface);
	}

	@Override
	public final void onPause() {
		super.onPause();
		// this dialog does not support onPause as it has serialization issues in colors.
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(TuneTilesDialogFragment.this, mInitialColors);
		}
		dismiss();
	}

	/**
	 * The seekbar listener applied when changing brightness or contrast.
	 */
	private final class OnBrightnessContrastChangeListener implements OnSeekBarChangeListener {
		/**
		 * The brightness SeekBar.
		 */
		private final SeekBar mSeekBarBrightness;
		/**
		 * The contrast SeekBar.
		 */
		private final SeekBar mSeekBarContrast;
		/**
		 * The saturation SeekBar.
		 */
		private final SeekBar mSeekBarSaturation;
		/**
		 * The hue SeekBar.
		 */
		private final SeekBar mSeekBarHue;
		/**
		 * The color temperature SeekBar.
		 */
		private final SeekBar mSeekBarColorTemperature;

		/**
		 * Constructor.
		 *
		 * @param seekBarBrightness The brightness seekbar.
		 * @param seekBarContrast The contrast seekbar.
		 * @param seekBarSaturation The saturation seekbar.
		 * @param seekBarHue The hue seekbar.
		 * @param seekBarColorTemperature The color temperature seekbar.
		 */
		private OnBrightnessContrastChangeListener(final SeekBar seekBarBrightness, final SeekBar seekBarContrast, final SeekBar seekBarSaturation,
				final SeekBar seekBarHue, final SeekBar seekBarColorTemperature) {
			mSeekBarBrightness = seekBarBrightness;
			mSeekBarContrast = seekBarContrast;
			mSeekBarSaturation = seekBarSaturation;
			mSeekBarHue = seekBarHue;
			mSeekBarColorTemperature = seekBarColorTemperature;
		}

		@Override
		public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
			if (fromUser) {
				update();
			}
		}

		@Override
		public void onStartTrackingTouch(final SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(final SeekBar seekBar) {
			update();
		}

		/**
		 * Update the view and the tile chain from the seekbar status.
		 */
		private void update() {
			short hueDiff = (short) (256 * (mSeekBarHue.getProgress() - 128)); // MAGIC_NUMBER
			double saturationDiff = ((double) mSeekBarSaturation.getProgress() * 2 - mSeekBarSaturation.getMax()) / mSeekBarSaturation.getMax();
			double brightness = (double) mSeekBarBrightness.getProgress() / mSeekBarBrightness.getMax();
			double contrast = (double) mSeekBarContrast.getProgress() / mSeekBarContrast.getMax();
			double contrastFactor = (2 * contrast - 1) / (1.01 - contrast) / 2; // MAGIC_NUMBER
			short colorTemperature = DeviceAdapter.progressBarToColorTemperature(mSeekBarColorTemperature.getProgress());

			mCurrentColors = new TileChainColors() {
				/**
				 * The default serial version id.
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Color getColor(final int x, final int y, final int width, final int height) {
					if (mBaseColors == null) {
						return null;
					}
					Color baseColor = mBaseColors.getColor(x, y, width, height);
					double oldBrightness = TypeUtil.toDouble(baseColor.getBrightness());
					return new Color(baseColor.getHue() + hueDiff,
							TypeUtil.toShort(TypeUtil.toDouble(baseColor.getSaturation()) + saturationDiff),
							TypeUtil.toShort(contrast <= 0.5 ? brightness * (2 * contrast * (oldBrightness - 1) + 1) // MAGIC_NUMBER
									: oldBrightness * brightness + (oldBrightness + brightness - 1) * contrastFactor),
							colorTemperature);
				}
			};

			TileViewModel model = mModel.getValue();
			if (model != null) {
				TuneTilesDialogListener listener = mListener.getValue();
				if (listener != null) {
					listener.onImageUpdate(mCurrentColors);
				}
			}
		}
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface TuneTilesDialogListener {
		/**
		 * Callback method for update of the image.
		 *
		 * @param colors The selected colors.
		 */
		void onImageUpdate(TileChainColors colors);

		/**
		 * Callback method for positive click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 * @param colors the selected colors.
		 */
		void onDialogPositiveClick(DialogFragment dialog, TileChainColors colors);

		/**
		 * Callback method for negative click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 * @param initialColors the initial colors.
		 */
		void onDialogNegativeClick(DialogFragment dialog, TileChainColors initialColors);
	}
}
