package de.jeisfeld.lifx.app.ui.storedcolors;

import java.util.List;

import javax.annotation.Nonnull;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
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
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.ui.storedcolors.StoredColorsViewAdapter.SetColorTask;
import de.jeisfeld.lifx.app.util.ColorRegistry;
import de.jeisfeld.lifx.app.util.StoredColor;

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
	 * @param deviceId the device id.
	 * @param listener The listener waiting for the response
	 */
	public static void displayStoredColorsDialog(final FragmentActivity activity,
			final int deviceId,
			final StoredColorsDialogListener listener) {
		Bundle bundle = new Bundle();
		bundle.putInt(PARAM_DEVICE_ID, deviceId);
		StoredColorsDialogFragment fragment = new StoredColorsDialogFragment();
		fragment.setListener(listener);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private StoredColorsDialogListener mListener = null;

	public final void setListener(final StoredColorsDialogListener listener) {
		mListener = listener;
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
			mListener = null;
			dismiss();
		}

		final LayoutInflater inflater = requireActivity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.dialog_stored_colors, null);
		List<StoredColor> storedColors = ColorRegistry.getInstance().getStoredColors(deviceId);
		if (storedColors.size() > 0) {
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
					imageView.setOnClickListener(v -> new SetColorTask(getContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, storedColor));

					return newView;
				}
			};
			((GridView) view.findViewById(R.id.gridViewStoredColors)).setAdapter(adapter);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view) //
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null) {
						mListener.onDialogNegativeClick(StoredColorsDialogFragment.this);
					}
				}) //
				.setPositiveButton(R.string.button_save, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null) {
						mListener.onDialogPositiveClick(StoredColorsDialogFragment.this,
								((EditText) view.findViewById(R.id.editTextSaveName)).getText().toString());
					}
				});
		return builder.create();
	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null) {
			mListener.onDialogNegativeClick(StoredColorsDialogFragment.this);
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
	}
}
