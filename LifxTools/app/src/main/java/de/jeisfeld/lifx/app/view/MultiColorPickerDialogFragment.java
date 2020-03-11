package de.jeisfeld.lifx.app.view;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Dialog for handling a picked image for a tile chain.
 */
public class MultiColorPickerDialogFragment extends DialogFragment {
	/**
	 * The number of multizone color pickers.
	 */
	public static final int MULTIZONE_PICKER_COUNT = 6;
	/**
	 * The index of the cyclic flag in the flag array.
	 */
	public static final int CYCLIC_FLAG_INDEX = MULTIZONE_PICKER_COUNT;

	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";
	/**
	 * Parameter to pass the initial colors to the DialogFragment.
	 */
	private static final String PARAM_INITIAL_COLORS = "initialColors";
	/**
	 * Parameter to pass the cyclic flag.
	 */
	private static final String PARAM_CYCLIC = "isCyclic";
	/**
	 * The list of colors.
	 */
	private final Color[] mColors = new Color[MULTIZONE_PICKER_COUNT];
	/**
	 * The button to switch between cyclic and non-cyclic.
	 */
	private ToggleButton mToggleButtonCyclic;
	/**
	 * The flags indicating which color pickers are active.
	 */
	private final boolean[] mFlags = new boolean[MULTIZONE_PICKER_COUNT + 1];

	/**
	 * Display a dialog for handling a picked image for a tile chain.
	 *
	 * @param activity the current activity
	 * @param initialColors The initial colors.
	 * @param isCyclic status of cyclic button
	 * @param listener The listener waiting for the response
	 */
	public static void displayMultiColorPickerDialog(final FragmentActivity activity,
			final ArrayList<Color> initialColors,
			final boolean isCyclic,
			final MultiColorPickerDialogListener listener) {
		Bundle bundle = new Bundle();
		MultiColorPickerDialogFragment fragment = new MultiColorPickerDialogFragment();
		fragment.setListener(listener);

		bundle.putSerializable(PARAM_INITIAL_COLORS, initialColors);
		bundle.putBoolean(PARAM_CYCLIC, isCyclic);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<MultiColorPickerDialogListener> mListener = null;

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final MultiColorPickerDialogListener listener) {
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
		@SuppressWarnings("unchecked")
		ArrayList<Color> initialColors = (ArrayList<Color>) getArguments().getSerializable(PARAM_INITIAL_COLORS);
		mFlags[CYCLIC_FLAG_INDEX] = getArguments().getBoolean(PARAM_CYCLIC);

		if (initialColors == null) {
			initialColors = new ArrayList<>();
		}
		for (int i = 0; i < MULTIZONE_PICKER_COUNT && i < initialColors.size(); i++) {
			mColors[i] = initialColors.get(i);
			mFlags[i] = mColors[i] != null;
		}

		final View view = View.inflate(requireActivity(), R.layout.dialog_multi_colorpicker, null);
		mToggleButtonCyclic = view.findViewById(R.id.toggleButtonCyclic);

		mToggleButtonCyclic.setChecked(mFlags[CYCLIC_FLAG_INDEX]);

		mToggleButtonCyclic.setOnCheckedChangeListener((buttonView, isChecked) -> {
			mFlags[CYCLIC_FLAG_INDEX] = isChecked;
			updateColorsOnListener();
		});

		prepareMultiColorPickerView(view, R.id.colorPicker1, mColors[0], 0);
		prepareMultiColorPickerView(view, R.id.colorPicker2, mColors[1], 1);
		prepareMultiColorPickerView(view, R.id.colorPicker3, mColors[2], 2);
		prepareMultiColorPickerView(view, R.id.colorPicker4, mColors[3], 3); // MAGIC_NUMBER
		prepareMultiColorPickerView(view, R.id.colorPicker5, mColors[4], 4); // MAGIC_NUMBER
		prepareMultiColorPickerView(view, R.id.colorPicker6, mColors[5], 5); // MAGIC_NUMBER

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view) //
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogNegativeClick(MultiColorPickerDialogFragment.this);
					}
				}) //
				.setPositiveButton(R.string.button_ok, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogPositiveClick(MultiColorPickerDialogFragment.this, getSelectedColors(),
								mToggleButtonCyclic.isChecked(), mFlags);
					}
				});
		return builder.create();
	}

	/**
	 * Get the currently selected colors.
	 *
	 * @return The corrently selected colors.
	 */
	private ArrayList<Color> getSelectedColors() {
		ArrayList<Color> result = new ArrayList<>();
		for (int i = 0; i < MULTIZONE_PICKER_COUNT; i++) {
			if (mFlags[i]) {
				result.add(mColors[i]);
			}
		}
		return result;
	}

	/**
	 * Update the colors on the listener.
	 */
	private void updateColorsOnListener() {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onColorUpdate(getSelectedColors(), mToggleButtonCyclic.isChecked(), mFlags);
		}
	}

	/**
	 * Prepare a single color picker within the multicolor picker.
	 *
	 * @param dialogView The dialog view.
	 * @param parentViewId The parent view of the single color picker.
	 * @param initialColor The initial color.
	 * @param index the index of the color picker.
	 */
	private void prepareMultiColorPickerView(final View dialogView, final int parentViewId, final Color initialColor, final int index) {
		final View parentView = dialogView.findViewById(parentViewId);
		final ColorPickerView colorPickerView = parentView.findViewById(R.id.ColorPickerView);
		ColorPickerDialog.prepareColorPickerView(parentView, colorPickerView);

		if (initialColor != null) {
			colorPickerView.getViewTreeObserver()
					.addOnGlobalLayoutListener(() -> ColorPickerDialog.updateColorPickerFromLight(colorPickerView, initialColor));
		}

		colorPickerView.setColorListener((ColorListener) (color, fromUser) -> {
			if (fromUser) {
				mColors[index] = ColorUtil.convertAndroidColorToColor(color, Color.WHITE_TEMPERATURE, true);
				updateColorsOnListener();
			}
		});

		parentView.findViewById(R.id.buttonClose).setOnClickListener(v -> {
			parentView.findViewById(R.id.ColorPickerView).setVisibility(View.INVISIBLE);
			parentView.findViewById(R.id.BrightnessSlideBar).setVisibility(View.INVISIBLE);
			parentView.findViewById(R.id.buttonClose).setVisibility(View.INVISIBLE);
			parentView.findViewById(R.id.buttonOpen).setVisibility(View.VISIBLE);
			mFlags[index] = false;
			updateColorsOnListener();
		});

		parentView.findViewById(R.id.buttonOpen).setOnClickListener(v -> {
			parentView.findViewById(R.id.ColorPickerView).setVisibility(View.VISIBLE);
			parentView.findViewById(R.id.BrightnessSlideBar).setVisibility(View.VISIBLE);
			parentView.findViewById(R.id.buttonClose).setVisibility(View.VISIBLE);
			parentView.findViewById(R.id.buttonOpen).setVisibility(View.INVISIBLE);
			mFlags[index] = true;
			mColors[index] = Color.OFF;
			ColorPickerDialog.updateColorPickerFromLight(colorPickerView, Color.OFF);
			updateColorsOnListener();
		});

		if (!mFlags[index]) {
			parentView.findViewById(R.id.buttonClose).performClick();
		}

	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(MultiColorPickerDialogFragment.this);
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

	/**
	 * A callback handler for the dialog.
	 */
	public interface MultiColorPickerDialogListener {
		/**
		 * Callback method for update of the image.
		 *
		 * @param colors The selected colors.
		 * @param isCyclic flag indicating if colors should be considered as cyclic.
		 * @param flags flags indicating selection of color pickers.
		 */
		void onColorUpdate(ArrayList<Color> colors, boolean isCyclic, boolean[] flags);

		/**
		 * Callback method for positive click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 * @param colors the selected colors.
		 * @param isCyclic flag indicating if colors should be considered as cyclic.
		 * @param flags flags indicating selection of color pickers.
		 */
		void onDialogPositiveClick(DialogFragment dialog, ArrayList<Color> colors, boolean isCyclic, boolean[] flags);

		/**
		 * Callback method for negative click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 */
		void onDialogNegativeClick(DialogFragment dialog);
	}
}
