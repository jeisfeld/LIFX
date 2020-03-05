package de.jeisfeld.lifx.app.ui.storedcolors;

import java.util.List;

import javax.annotation.Nonnull;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.ui.home.LightViewModel;
import de.jeisfeld.lifx.app.ui.home.MultizoneViewModel;
import de.jeisfeld.lifx.app.ui.home.TileViewModel;
import de.jeisfeld.lifx.app.util.ColorRegistry;
import de.jeisfeld.lifx.app.util.StoredColor;
import de.jeisfeld.lifx.app.util.StoredMultizoneColors;
import de.jeisfeld.lifx.app.util.StoredTileColors;

/**
 * Dialog for selecting stored colors or storing the current color.
 */
public class StoredColorsDialogFragment extends DialogFragment {
	/**
	 * Parameter to pass the deviceId to the DialogFragment.
	 */
	private static final String PARAM_DEVICE_ID = "deviceId";
	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity the current activity
	 * @param model the light model.
	 * @param deviceId the device id.
	 * @param listener The listener waiting for the response
	 */
	public static void displayStoredColorsDialog(final FragmentActivity activity,
			final LightViewModel model,
			final int deviceId,
			final StoredColorsDialogListener listener) {
		Bundle bundle = new Bundle();
		StoredColorsDialogFragment fragment = new StoredColorsDialogFragment();
		fragment.setListener(listener);
		fragment.setModel(model);
		bundle.putInt(PARAM_DEVICE_ID, deviceId);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<StoredColorsDialogListener> mListener = null;
	/**
	 * The light view model.
	 */
	private MutableLiveData<LightViewModel> mModel = null;

	/**
	 * Set the model.
	 *
	 * @param model The model.
	 */
	public final void setModel(final LightViewModel model) {
		mModel = new MutableLiveData<>(model);
	}

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final StoredColorsDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	@Override
	@Nonnull
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		assert getArguments() != null;
		final int deviceId = getArguments().getInt(PARAM_DEVICE_ID);

		// Listeners cannot retain functionality when automatically recreated.
		// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
		boolean preventRecreation = false;
		if (savedInstanceState != null) {
			preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
		}
		if (preventRecreation) {
			dismiss();
		}

		final LayoutInflater inflater = requireActivity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.dialog_stored_colors, null);
		List<StoredColor> storedColors = ColorRegistry.getInstance().getStoredColors(deviceId);
		if (storedColors.size() > 0 && getContext() != null) {
			view.findViewById(R.id.gridViewStoredColors).setVisibility(View.VISIBLE);
			view.findViewById(R.id.dialog_title_select).setVisibility(View.VISIBLE);

			ArrayAdapter<StoredColor> adapter = new ArrayAdapter<StoredColor>(getContext(), R.layout.grid_entry_select_color, storedColors) {
				@Override
				public View getView(final int position, final View view, final ViewGroup parent) {
					final View newView;
					if (view == null) {
						newView = inflater.inflate(R.layout.grid_entry_select_color, null);
					}
					else {
						newView = view;
					}

					final StoredColor storedColor = storedColors.get(position);
					((TextView) newView.findViewById(R.id.textViewColorName)).setText(storedColor.getName());
					ImageView imageView = newView.findViewById(R.id.imageViewApplyColor);
					imageView.setImageDrawable(StoredColorsViewAdapter.getButtonDrawable(getContext(), storedColor));
					imageView.setOnClickListener(v -> {
						LightViewModel model = mModel.getValue();
						if (model == null) {
							return;
						}
						StoredColorsDialogListener listener = mListener == null ? null : mListener.getValue();
						if (storedColor instanceof StoredMultizoneColors && model instanceof MultizoneViewModel) {
							((MultizoneViewModel) model).updateColors(((StoredMultizoneColors) storedColor).getColors());
						}
						else if (storedColor instanceof StoredTileColors && model instanceof TileViewModel) {
							((TileViewModel) model).updateColors(((StoredTileColors) storedColor).getColors());
						}
						else {
							model.updateColor(storedColor.getColor());
						}
						if (listener != null) {
							listener.onStoredColorClick(storedColor);
						}
					});

					return newView;
				}
			};
			((GridView) view.findViewById(R.id.gridViewStoredColors)).setAdapter(adapter);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view) //
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogNegativeClick(StoredColorsDialogFragment.this);
					}
				}) //
				.setPositiveButton(R.string.button_save, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogPositiveClick(StoredColorsDialogFragment.this,
								((EditText) view.findViewById(R.id.editTextSaveName)).getText().toString());
					}
				});
		return builder.create();
	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(StoredColorsDialogFragment.this);
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
	public interface StoredColorsDialogListener {
		/**
		 * Callback method for positive click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 * @param text the text returned from the input.
		 */
		void onDialogPositiveClick(DialogFragment dialog, String text);

		/**
		 * Callback method for negative click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 */
		void onDialogNegativeClick(DialogFragment dialog);

		/**
		 * Callback method for click on a stored color.
		 *
		 * @param storedColor The stored color.
		 */
		void onStoredColorClick(StoredColor storedColor);
	}
}
