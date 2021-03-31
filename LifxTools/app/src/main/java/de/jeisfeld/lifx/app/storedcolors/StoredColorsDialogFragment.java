package de.jeisfeld.lifx.app.storedcolors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;

/**
 * Dialog for selecting stored colors or storing the current color.
 */
public class StoredColorsDialogFragment extends DialogFragment {
	/**
	 * Parameter to pass the deviceId to the DialogFragment.
	 */
	private static final String PARAM_DEVICE_ID = "deviceId";
	/**
	 * Parameter to pass information if it is only select or storage of color or storage of animation.
	 */
	private static final String PARAM_STORE_COLOR_TYPE = "storeColorType";
	/**
	 * Parameter to pass information if only selection is supported.
	 */
	private static final String PARAM_INCLUDE_OFF = "includeOff";
	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";

	/**
	 * Display a dialog for storing a color or displaying a stored color.
	 *
	 * @param activity       the current activity
	 * @param deviceId       the device id.
	 * @param storeColorType the flag indicating if it is only select or storage of color or storage of animation.
	 * @param includeOff     the flag indicating if option "OFF" should be displayed.
	 * @param listener       The listener waiting for the response
	 */
	public static void displayStoredColorsDialog(final FragmentActivity activity,
												 final int deviceId,
												 final StoreColorType storeColorType,
												 final boolean includeOff,
												 final StoredColorsDialogListener listener) {
		Bundle bundle = new Bundle();
		StoredColorsDialogFragment fragment = new StoredColorsDialogFragment();
		fragment.setListener(listener);
		bundle.putInt(PARAM_DEVICE_ID, deviceId);
		bundle.putSerializable(PARAM_STORE_COLOR_TYPE, storeColorType);
		bundle.putBoolean(PARAM_INCLUDE_OFF, includeOff);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<StoredColorsDialogListener> mListener = null;

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
		final StoreColorType storeColorType = (StoreColorType) getArguments().getSerializable(PARAM_STORE_COLOR_TYPE);
		final boolean includeOff = getArguments().getBoolean(PARAM_INCLUDE_OFF);

		// Listeners cannot retain functionality when automatically recreated.
		// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
		boolean preventRecreation = false;
		if (savedInstanceState != null) {
			preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
		}
		if (preventRecreation) {
			dismiss();
		}

		final View view = View.inflate(requireActivity(), R.layout.dialog_stored_colors, null);
		view.findViewById(R.id.dialog_title_save).setVisibility(storeColorType == StoreColorType.ONLYSELECT ? View.GONE : View.VISIBLE);
		view.findViewById(R.id.messageTextSaveName).setVisibility(storeColorType == StoreColorType.ONLYSELECT ? View.GONE : View.VISIBLE);
		view.findViewById(R.id.editTextSaveName).setVisibility(storeColorType == StoreColorType.ONLYSELECT ? View.GONE : View.VISIBLE);
		if (storeColorType == StoreColorType.ANIMATION) {
			((TextView) view.findViewById(R.id.dialog_title_save)).setText(R.string.title_dialog_save_animation);
			((TextView) view.findViewById(R.id.messageTextSaveName)).setText(R.string.message_dialog_save_animation_name);
		}

		List<StoredColor> storedColors = ColorRegistry.getInstance().getStoredColors(deviceId);
		if (includeOff) {
			storedColors.add(0, StoredColor.fromDeviceOff(deviceId));
		}

		if (storedColors.size() > 0 && getContext() != null) {
			view.findViewById(R.id.gridViewStoredColors).setVisibility(View.VISIBLE);
			view.findViewById(R.id.dialog_title_select).setVisibility(View.VISIBLE);

			ArrayAdapter<StoredColor> adapter = new ArrayAdapter<StoredColor>(getContext(), R.layout.grid_entry_select_color, storedColors) {
				@Override
				public View getView(final int position, final View view, @NonNull final ViewGroup parent) {
					final View newView;
					if (view == null) {
						newView = View.inflate(requireActivity(), R.layout.grid_entry_select_color, null);
					}
					else {
						newView = view;
					}

					final StoredColor storedColor = storedColors.get(position);
					((TextView) newView.findViewById(R.id.textViewColorName)).setText(storedColor.getName());
					ImageView imageView = newView.findViewById(R.id.imageViewApplyColor);
					imageView.setImageDrawable(storedColor.getButtonDrawable(getContext()));
					imageView.setOnClickListener(v -> {
						StoredColorsDialogListener listener = mListener == null ? null : mListener.getValue();
						if (listener != null) {
							listener.onStoredColorClick(StoredColorsDialogFragment.this, storedColor);
							if (storeColorType == StoreColorType.ONLYSELECT) {
								dismiss();
							}
						}
					});

					return newView;
				}
			};
			((GridView) view.findViewById(R.id.gridViewStoredColors)).setAdapter(adapter);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setView(view);
		if (storeColorType != StoreColorType.ONLYSELECT) {
			builder.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
				// Send the positive button event back to the host activity
				if (mListener != null && mListener.getValue() != null) {
					mListener.getValue().onDialogNegativeClick(StoredColorsDialogFragment.this);
				}
			});
			builder.setPositiveButton(R.string.button_save, (dialog, id) -> {
				// Send the negative button event back to the host activity
				if (mListener != null && mListener.getValue() != null) {
					mListener.getValue().onDialogPositiveClick(StoredColorsDialogFragment.this,
							((EditText) view.findViewById(R.id.editTextSaveName)).getText().toString());
				}
			});
		}
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
		 * @param text   the text returned from the input.
		 */
		default void onDialogPositiveClick(DialogFragment dialog, String text) {
			// do nothing
		}

		/**
		 * Callback method for negative click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 */
		default void onDialogNegativeClick(DialogFragment dialog) {
			// do nothing
		}

		/**
		 * Callback method for click on a stored color.
		 *
		 * @param dialog      the confirmation dialog fragment.
		 * @param storedColor The stored color.
		 */
		void onStoredColorClick(DialogFragment dialog, StoredColor storedColor);
	}

	/**
	 * The type of color storing.
	 */
	public enum StoreColorType {
		/**
		 * No storing, only select color.
		 */
		ONLYSELECT,
		/**
		 * Allow to store color.
		 */
		COLOR,
		/**
		 * Allow to store animation.
		 */
		ANIMATION
	}
}
