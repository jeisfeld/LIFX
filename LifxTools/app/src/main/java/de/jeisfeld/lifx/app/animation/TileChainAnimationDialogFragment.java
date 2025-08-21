package de.jeisfeld.lifx.app.animation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.skydoves.colorpickerview.ColorPickerView;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nonnull;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.TileViewModel;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.view.ColorPickerDialog;
import de.jeisfeld.lifx.app.view.ColorPickerDialog.Builder;
import de.jeisfeld.lifx.app.view.MultiColorPickerDialogFragment;
import de.jeisfeld.lifx.app.view.MultiColorPickerDialogFragment.MultiColorPickerDialogListener;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.animation.TileChainWaveDefinition;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Dialog for setting up a multizone animation.
 */
public class TileChainAnimationDialogFragment extends DialogFragment {
	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";
	/**
	 * The selected colors.
	 */
	private ArrayList<Color> mColors = new ArrayList<>();

	/**
	 * Display a dialog for setting up a multizone animation.
	 *
	 * @param activity the current activity
	 * @param model    the tile view model.
	 * @param listener The listener waiting for the response
	 */
	public static void displayTileChainAnimationDialog(final FragmentActivity activity, final TileViewModel model,
													   final TileChainAnimationDialogListener listener) {
		Bundle bundle = new Bundle();
		TileChainAnimationDialogFragment fragment = new TileChainAnimationDialogFragment();
		fragment.setListener(listener);
		fragment.setModel(model);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<TileChainAnimationDialogListener> mListener = new MutableLiveData<>();
	/**
	 * The model.
	 */
	private MutableLiveData<TileViewModel> mModel = new MutableLiveData<>();

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final TileChainAnimationDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	/**
	 * Set the model.
	 *
	 * @param model the model.
	 */
	public final void setModel(final TileViewModel model) {
		mModel = new MutableLiveData<>(model);
	}

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
		}

		final View parentView = View.inflate(requireActivity(), R.layout.dialog_tilechain_animation, null);
		final Spinner spinnerAnimationType = parentView.findViewById(R.id.spinnerAnimationType);
		final EditText editTextDuration = parentView.findViewById(R.id.editTextDuration);
		final EditText editTextRadius = parentView.findViewById(R.id.editTextRadius);
		final Spinner spinnerDirection = parentView.findViewById(R.id.spinnerDirection);
		final Spinner spinnerForm = parentView.findViewById(R.id.spinnerForm);
		final EditText editTextColorRegex = parentView.findViewById(R.id.editTextColorRegex);
		final CheckBox checkBoxAdjustBrightness = parentView.findViewById(R.id.checkboxAdjustBrightness);
		final EditText editTextCloudSaturation = parentView.findViewById(R.id.editTextCloudSaturation);
		final ImageView imageViewColors = parentView.findViewById(R.id.imageViewColors);

		if (mModel != null && mModel.getValue() != null && mModel.getValue().getLight() != null
				&& mModel.getValue().getLight().getProduct().isChain()) {
			ArrayList<String> items = new ArrayList<>(Arrays.asList(
					getResources().getStringArray(R.array.values_tilechain_animation_type)));
			if (items.size() > TileChainAnimationType.CLOUDS.ordinal()) {
				items.remove(TileChainAnimationType.CLOUDS.ordinal());
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(),
					android.R.layout.simple_spinner_item, items);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinnerAnimationType.setAdapter(adapter);
		}

		prepareSpinnerListener(parentView, spinnerAnimationType);

		mColors.add(Color.RED);
		mColors.add(Color.GREEN);
		mColors.add(Color.BLUE);
		imageViewColors.setImageDrawable(ColorUtil.getButtonDrawable(getContext(), mColors));

		imageViewColors.setOnClickListener(v -> {
			if (getActivity() == null) {
				return;
			}
			TileChainAnimationType selectedType = TileChainAnimationType.fromOrdinal(spinnerAnimationType.getSelectedItemPosition());
			if (selectedType == TileChainAnimationType.CLOUDS) {
				Color initialColor = mColors.isEmpty() ? Color.CYAN : mColors.get(0);
				Builder builder = new Builder(getContext(), R.layout.dialog_colorpicker);
				ColorPickerView colorPickerView = builder.getColorPickerView();
				colorPickerView.getViewTreeObserver().addOnGlobalLayoutListener(
						() -> ColorPickerDialog.updateColorPickerFromLight(colorPickerView, initialColor));
				builder.setColorListener((color, fromUser) -> {
					if (fromUser) {
						mColors = new ArrayList<>();
						mColors.add(ColorUtil.convertAndroidColorToColor(color, Color.WHITE_TEMPERATURE, true));
						imageViewColors.setImageDrawable(ColorUtil.getButtonDrawable(getContext(), mColors));
					}
				}).show();
			}
			else {
				MultiColorPickerDialogFragment.displayMultiColorPickerDialog(getActivity(), mColors, null, new MultiColorPickerDialogListener() {
					@Override
					public void onColorUpdate(final ArrayList<Color> colors, final boolean isCyclic, final boolean[] flags) {
						// do nothing
					}

					@Override
					public void onDialogPositiveClick(final DialogFragment dialog, final ArrayList<Color> colors, final boolean isCyclic,
													  final boolean[] flags) {
						mColors = colors;
						imageViewColors.setImageDrawable(ColorUtil.getButtonDrawable(getContext(), mColors));
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				});
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.title_dialog_animation)
				.setView(parentView)
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogNegativeClick(TileChainAnimationDialogFragment.this);
					}
				})
				.setPositiveButton(R.string.button_start, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null && mListener.getValue() != null && mModel != null // BOOLEAN_EXPRESSION_COMPLEXITY
							&& mModel.getValue() != null && mModel.getValue().getLight() != null) {
						TileChain light = mModel.getValue().getLight();
						TileChainAnimationType animationType = TileChainAnimationType.fromOrdinal(spinnerAnimationType.getSelectedItemPosition());

						int duration;
						try {
							duration = (int) (Double.parseDouble(editTextDuration.getText().toString()) * 1000); // MAGIC_NUMBER
						}
						catch (Exception e) {
							duration = 10000; // MAGIC_NUMBER
						}

						switch (animationType) {
						case IMAGE_TRANSITION:
							String colorRegex = editTextColorRegex.getText().toString();
							boolean adjustBrightness = checkBoxAdjustBrightness.isChecked();

							mListener.getValue().onDialogPositiveClick(TileChainAnimationDialogFragment.this,
									new TileChainImageTransition(duration, colorRegex, adjustBrightness));
							break;
						case FLAME:
							mListener.getValue().onDialogPositiveClick(TileChainAnimationDialogFragment.this,
									new TileChainFlame(duration, false));
							break;
						case MORPH:
							mListener.getValue().onDialogPositiveClick(TileChainAnimationDialogFragment.this,
									new TileChainMorph(duration, mColors, false));
							break;
						case CLOUDS:
							int cloudSaturation;
							try {
								cloudSaturation = Integer.parseInt(editTextCloudSaturation.getText().toString());
							}
							catch (Exception e) {
								cloudSaturation = 50;
							}
							cloudSaturation = Math.max(0, Math.min(255, cloudSaturation));
							mListener.getValue().onDialogPositiveClick(TileChainAnimationDialogFragment.this,
									new TileChainClouds(duration * 5, cloudSaturation, mColors, false));
							break;
						case WAVE:
						default:
							double lightRadius =
									Math.sqrt(light.getTotalHeight() * light.getTotalHeight() + light.getTotalWidth() * light.getTotalWidth()) / 2;

							double radius;
							try {
								radius = Math.max(1, Double.parseDouble(editTextRadius.getText().toString()) * lightRadius);
							}
							catch (Exception e) {
								radius = lightRadius;
							}

							final TileChainWaveDefinition.Direction direction =
									TileChainWaveDefinition.Direction.fromOrdinal(spinnerDirection.getSelectedItemPosition());

							final TileChainWaveDefinition.Form form =
									TileChainWaveDefinition.Form.fromOrdinal(spinnerForm.getSelectedItemPosition());

							mListener.getValue().onDialogPositiveClick(TileChainAnimationDialogFragment.this,
									new TileChainWave(duration, radius, direction, form, mColors));
							break;
						}
					}
				});
		return builder.create();
	}

	/**
	 * Prepare the listener for the animation type spinner.
	 *
	 * @param parentView           The dialog parent view.
	 * @param spinnerAnimationType The spinner.
	 */
	private void prepareSpinnerListener(final View parentView, final Spinner spinnerAnimationType) {
		spinnerAnimationType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View selectedView, final int position, final long id) {
				TileChainAnimationType animationType = TileChainAnimationType.fromOrdinal(position);
				switch (animationType) {
				case IMAGE_TRANSITION:
					parentView.findViewById(R.id.tableRowRadius).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowDirection).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowForm).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowColors).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowColorRegex).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowAdjustBrightness).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowCloudSaturation).setVisibility(View.GONE);
					break;
				case FLAME:
					parentView.findViewById(R.id.tableRowRadius).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowDirection).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowForm).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowColors).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowColorRegex).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowAdjustBrightness).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowCloudSaturation).setVisibility(View.GONE);
					break;
				case MORPH:
					parentView.findViewById(R.id.tableRowRadius).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowDirection).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowForm).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowColors).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowColorRegex).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowAdjustBrightness).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowCloudSaturation).setVisibility(View.GONE);
					break;
				case CLOUDS:
					parentView.findViewById(R.id.tableRowRadius).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowDirection).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowForm).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowColors).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowColorRegex).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowAdjustBrightness).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowCloudSaturation).setVisibility(View.VISIBLE);
					mColors.clear();
					mColors.add(Color.CYAN);
					ImageView imageViewColors = parentView.findViewById(R.id.imageViewColors);
					imageViewColors.setImageDrawable(ColorUtil.getButtonDrawable(getContext(), mColors));
					break;
				case WAVE:
				default:
					parentView.findViewById(R.id.tableRowRadius).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowDirection).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowForm).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowColors).setVisibility(View.VISIBLE);
					parentView.findViewById(R.id.tableRowColorRegex).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowAdjustBrightness).setVisibility(View.GONE);
					parentView.findViewById(R.id.tableRowCloudSaturation).setVisibility(View.GONE);
					break;
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// do nothing
			}
		});
	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(TileChainAnimationDialogFragment.this);
		}
		super.onCancel(dialogInterface);
	}

	@Override
	public final void onSaveInstanceState(@Nonnull final Bundle outState) {
		if (mListener != null) {
			// Typically cannot serialize the listener due to its reference to the activity.
			outState.putBoolean(PREVENT_RECREATION, true);
		}
		super.onSaveInstanceState(outState);
	}

	/**
	 * The direction of the animation.
	 */
	public enum TileChainAnimationType {
		/**
		 * Wave.
		 */
		WAVE,
		/**
		 * Image transition.
		 */
		IMAGE_TRANSITION,
		/**
		 * Flame.
		 */
		FLAME,
		/**
		 * Morph.
		 */
		MORPH,
		/**
		 * Clouds.
		 */
		CLOUDS;

		/**
		 * Get TileChainAnimationType from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The TileChainAnimationType.
		 */
		private static TileChainAnimationType fromOrdinal(final int ordinal) {
			for (TileChainAnimationType animationType : values()) {
				if (ordinal == animationType.ordinal()) {
					return animationType;
				}
			}
			return WAVE;
		}
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface TileChainAnimationDialogListener {
		/**
		 * Callback method for positive click from the confirmation dialog.
		 *
		 * @param dialog        The confirmation dialog fragment.
		 * @param animationData The animation data.
		 */
		void onDialogPositiveClick(DialogFragment dialog, AnimationData animationData);

		/**
		 * Callback method for negative click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 */
		void onDialogNegativeClick(DialogFragment dialog);
	}
}
