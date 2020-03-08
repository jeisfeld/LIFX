package de.jeisfeld.lifx.app.view;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.TileViewModel;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.ImageUtil;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;

/**
 * Dialog for handling a picked image for a tile chain.
 */
public class PickedImageDialogFragment extends DialogFragment {
	/**
	 * Parameter to pass the picked bitmap to the DialogFragment.
	 */
	private static final String PARAM_BITMAP = "bitmap";
	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";
	/**
	 * The scale factor when clicking on the image.
	 */
	private static final float SCALE_FACTOR = 0.9f;
	/**
	 * The overhead size taken when prescaling the image before setting contrast and saturation.
	 */
	private static final int PRESCALE_OVERHEAD_FACTOR = 4;

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity the current activity
	 * @param model    the light model.
	 * @param bitmap   the picked image.
	 * @param listener The listener waiting for the response
	 */
	public static void displayPickedImageDialog(final FragmentActivity activity,
												final TileViewModel model,
												final Bitmap bitmap,
												final PickedImageDialogListener listener) {
		Bundle bundle = new Bundle();
		PickedImageDialogFragment fragment = new PickedImageDialogFragment();
		fragment.setListener(listener);
		fragment.setModel(model);

		bundle.putParcelable(PARAM_BITMAP, bitmap); // MAGIC_NUMBER
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<PickedImageDialogListener> mListener = null;
	/**
	 * The tile view model.
	 */
	private MutableLiveData<TileViewModel> mModel = null;
	/**
	 * The picked bitmap.
	 */
	private Bitmap mBitmap = null;
	/**
	 * The scaled bitmap.
	 */
	private Bitmap mScaledBitmap = null;
	/**
	 * The partial bitmaps.
	 */
	private final List<PartialBitmap> mPartialBitmaps = new ArrayList<>();
	/**
	 * The current colors.
	 */
	private TileChainColors mCurrentColors = null;
	/**
	 * The width of the tile chain.
	 */
	private int mTileWidth;
	/**
	 * The height of the tile chain.
	 */
	private int mTileHeight;
	/**
	 * The toggle button for filtering.
	 */
	private ToggleButton mFilterButton;

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
	public final void setListener(final PickedImageDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	@Nonnull
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		// Listeners cannot retain functionality when automatically recreated.
		// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
		boolean preventRecreation = false;
		if (savedInstanceState != null) {
			preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
		}
		if (preventRecreation) {
			dismiss();
			return super.onCreateDialog(savedInstanceState);
		}

		assert getArguments() != null;
		mBitmap = getArguments().getParcelable(PARAM_BITMAP);
		mScaledBitmap = mBitmap;

		TileViewModel model = mModel.getValue();
		if (model == null) {
			dismiss();
			return super.onCreateDialog(savedInstanceState);
		}
		mTileWidth = model.getLight().getTotalWidth();
		mTileHeight = model.getLight().getTotalHeight();

		PartialBitmap initialPartialBitmap = new PartialBitmap(mBitmap, 0, 1, 0, 1);
		mPartialBitmaps.add(initialPartialBitmap);

		final View view = View.inflate(requireActivity(), R.layout.dialog_picked_image, null);
		final ImageView imageView = view.findViewById(R.id.imageViewPickedImage);
		final Button backButton = view.findViewById(R.id.buttonBack);
		mFilterButton = view.findViewById(R.id.toggleButtonFilter);

		// Only partial downscaling, so that final downscaling is done with final brightness/contrast/saturation
		mScaledBitmap = initialPartialBitmap.getPrescaledBitmap();

		final SeekBar seekBarBrightness = view.findViewById(R.id.seekBarBrightness);
		final SeekBar seekBarContrast = view.findViewById(R.id.seekBarContrast);
		final SeekBar seekBarSaturation = view.findViewById(R.id.seekBarSaturation);
		final OnBrightnessContrastChangeListener seekBarListener = new OnBrightnessContrastChangeListener(seekBarBrightness, seekBarContrast,
				seekBarSaturation, imageView);
		seekBarBrightness.setOnSeekBarChangeListener(seekBarListener);
		seekBarContrast.setOnSeekBarChangeListener(seekBarListener);
		seekBarSaturation.setOnSeekBarChangeListener(seekBarListener);
		seekBarListener.update();

		imageView.setOnTouchListener((v, event) -> {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				PartialBitmap currentParitalBitmap = mPartialBitmaps.get(mPartialBitmaps.size() - 1);
				PartialBitmap newPartialBitmap = currentParitalBitmap.zoomedPartialBitmap(
						event.getX() / imageView.getWidth(), event.getY() / imageView.getHeight());
				mScaledBitmap = newPartialBitmap.getPrescaledBitmap();
				mPartialBitmaps.add(newPartialBitmap);
				seekBarListener.update();
				backButton.setVisibility(View.VISIBLE);
				return true;
			case MotionEvent.ACTION_UP:
				v.performClick();
				return true;
			default:
				break;
			}
			return false;
		});

		backButton.setOnClickListener(v -> {
			if (mPartialBitmaps.size() < 2) {
				return;
			}
			PartialBitmap previousPartialBitmap = mPartialBitmaps.get(mPartialBitmaps.size() - 2);
			mPartialBitmaps.remove(mPartialBitmaps.size() - 1);
			mScaledBitmap = previousPartialBitmap.getPrescaledBitmap();
			seekBarListener.update();
			if (mPartialBitmaps.size() < 2) {
				backButton.setVisibility(View.GONE);
			}
		});

		mFilterButton.setOnClickListener(v -> {
			PartialBitmap currentParitalBitmap = mPartialBitmaps.get(mPartialBitmaps.size() - 1);
			mScaledBitmap = currentParitalBitmap.getPrescaledBitmap();
			seekBarListener.update();
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view) //
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogNegativeClick(PickedImageDialogFragment.this);
					}
				}) //
				.setPositiveButton(R.string.button_ok, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogPositiveClick(PickedImageDialogFragment.this, mCurrentColors);
					}
				});
		return builder.create();
	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(PickedImageDialogFragment.this);
		}
		super.onCancel(dialogInterface);
	}

	@Override
	public final void onSaveInstanceState(@Nonnull final Bundle outState) {
		if (mListener != null) {
			// Typically cannot serialize the listener due to its reference to the activity.
			mListener = null;
			outState.putBoolean(PREVENT_RECREATION, true);
		}
		super.onSaveInstanceState(outState);
	}

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
		 * The image view.
		 */
		private final ImageView mImageView;

		/**
		 * Constructor.
		 *
		 * @param seekBarBrightness The brightness seekbar.
		 * @param seekBarContrast   The contrast seekbar.
		 * @param seekBarSaturation The saturation seekbar.
		 * @param imageView         The image view.
		 */
		private OnBrightnessContrastChangeListener(final SeekBar seekBarBrightness, final SeekBar seekBarContrast, final SeekBar seekBarSaturation,
												   final ImageView imageView) {
			mSeekBarBrightness = seekBarBrightness;
			mSeekBarContrast = seekBarContrast;
			mSeekBarSaturation = seekBarSaturation;
			mImageView = imageView;
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
			int seekbarContrast = mSeekBarContrast.getProgress();
			int brightness = mSeekBarBrightness.getProgress();
			int seekbarSaturation = mSeekBarSaturation.getProgress();
			float saturation = 4f / 3 / (1f - (seekbarSaturation / 50f - 1f) * 0.98f) - 1f / 3; // MAGIC_NUMBER
			float contrast = 2f / (1f - (float) (Math.asin(seekbarContrast / 50f - 1f) * 2 / Math.PI) * 0.98f) - 1f; // MAGIC_NUMBER

			Bitmap bitmap = ImageUtil.changeBitmapColors(mScaledBitmap, contrast, brightness / 100f - 1f, saturation); // MAGIC_NUMBER

			mImageView.setImageBitmap(bitmap);

			TileViewModel model = mModel.getValue();
			if (model != null) {
				final Bitmap rescaledBitmap = ImageUtil.createScaledBitmap(bitmap, mTileWidth, mTileHeight, mFilterButton.isChecked());

				mImageView.setImageBitmap(rescaledBitmap);

				mCurrentColors = new TileChainColors() {
					@Override
					public Color getColor(final int x, final int y, final int width, final int height) {
						return ColorUtil.convertAndroidColorToColor(rescaledBitmap.getPixel(x, height - 1 - y), Color.WHITE_TEMPERATURE, true);
					}
				};
				PickedImageDialogListener listener = mListener.getValue();
				if (listener != null) {
					listener.onImageUpdate(mCurrentColors);
				}
			}
		}
	}

	/**
	 * Class storing information about a part of the bitmap.
	 */
	private final class PartialBitmap {
		/**
		 * The full bitmap.
		 */
		private final Bitmap mFullBitmap;
		/**
		 * The partial bitmap.
		 */
		private final Bitmap mPartialBitmap;
		/**
		 * The min x coordinate of the partial bitmap in the full bitmap (0..1).
		 */
		private final float mMinX;
		/**
		 * The max x coordinate of the partial bitmap in the full bitmap (0..1).
		 */
		private final float mMaxX;
		/**
		 * The min y coordinate of the partial bitmap in the full bitmap (0..1).
		 */
		private final float mMinY;
		/**
		 * The max y coordinate of the partial bitmap in the full bitmap (0..1).
		 */
		private final float mMaxY;

		/**
		 * Constructor.
		 *
		 * @param fullBitmap The full bitmap.
		 * @param minX       The min x coordinate of the partial bitmap in the full bitmap (0..1)
		 * @param maxX       The max x coordinate of the partial bitmap in the full bitmap (0..1)
		 * @param minY       The min y coordinate of the partial bitmap in the full bitmap (0..1)
		 * @param maxY       The max y coordinate of the partial bitmap in the full bitmap (0..1)
		 */
		private PartialBitmap(final Bitmap fullBitmap, final float minX, final float maxX, final float minY, final float maxY) {
			mFullBitmap = fullBitmap;
			mMinX = minX;
			mMaxX = maxX;
			mMinY = minY;
			mMaxY = maxY;

			int x0 = (int) Math.max(0, Math.floor(mMinX * fullBitmap.getWidth()));
			int y0 = (int) Math.max(0, Math.floor(mMinY * fullBitmap.getHeight()));
			int x1 = (int) Math.min(fullBitmap.getWidth(), Math.ceil(mMaxX * fullBitmap.getWidth()));
			int y1 = (int) Math.min(fullBitmap.getHeight(), Math.ceil(mMaxY * fullBitmap.getHeight()));

			mPartialBitmap = Bitmap.createBitmap(fullBitmap, x0, y0, x1 - x0, y1 - y0);
		}

		/**
		 * Get a scaled bitmap from this partial bitmap.
		 *
		 * @param width  The target width.
		 * @param height The target height.
		 * @return The bitmap scaled to these dimensions.
		 */
		private Bitmap getScaledBitmap(final int width, final int height) {
			return ImageUtil.createScaledBitmap(mPartialBitmap, width, height, mFilterButton.isChecked());
		}

		/**
		 * Get a prescaled bitmap for the tile.
		 *
		 * @return The bitmap scaled to these dimensions.
		 */
		private Bitmap getPrescaledBitmap() {
			return getScaledBitmap(mTileWidth * PRESCALE_OVERHEAD_FACTOR, mTileHeight * PRESCALE_OVERHEAD_FACTOR);
		}

		/**
		 * Get a zoomed partial bitmap from this partial bitmap.
		 *
		 * @param x The x coordinate of the zoom center in range 0..1.
		 * @param y The y coordinate of the zoom center in range 0..1.
		 * @return The zoomed partial bitmap.
		 */
		private PartialBitmap zoomedPartialBitmap(final float x, final float y) {
			float xStartFactor = (mMaxX - mMinX) * mFullBitmap.getWidth() / mTileWidth;
			float yStartFactor = (mMaxY - mMinY) * mFullBitmap.getHeight() / mTileHeight;
			float xScaleFactor;
			float yScaleFactor;
			if (xStartFactor > yStartFactor) {
				xScaleFactor = SCALE_FACTOR;
				yScaleFactor = Math.min(1, xScaleFactor * xStartFactor / yStartFactor);
			}
			else {
				yScaleFactor = SCALE_FACTOR;
				xScaleFactor = Math.min(1, yScaleFactor * yStartFactor / xStartFactor);
			}

			float centerX = mMinX + x * (mMaxX - mMinX);
			float centerY = mMinY + y * (mMaxY - mMinY);
			float newWidth = xScaleFactor * (mMaxX - mMinX);
			float newHeight = yScaleFactor * (mMaxY - mMinY);
			float newMinX = centerX - newWidth / 2;
			float newMaxX = centerX + newWidth / 2;
			float newMinY = centerY - newHeight / 2;
			float newMaxY = centerY + newHeight / 2;
			if (newMinX < 0) {
				newMinX = 0;
				newMaxX = newWidth;
			}
			if (newMaxX > 1) {
				newMaxX = 1;
				newMinX = 1 - newWidth;
			}
			if (newMinY < 0) {
				newMinY = 0;
				newMaxY = newHeight;
			}
			if (newMaxY > 1) {
				newMaxY = 1;
				newMinY = 1 - newHeight;
			}
			return new PartialBitmap(mFullBitmap, newMinX, newMaxX, newMinY, newMaxY);
		}
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface PickedImageDialogListener {
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
		 */
		void onDialogNegativeClick(DialogFragment dialog);
	}
}
