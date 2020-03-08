package de.jeisfeld.lifx.app.animation;

import javax.annotation.Nonnull;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;

/**
 * Dialog for setting up a multizone animation.
 */
public class MultizoneAnimationDialogFragment extends DialogFragment {
	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";

	/**
	 * Display a dialog for setting up a multizone animation.
	 *
	 * @param activity the current activity
	 * @param listener The listener waiting for the response
	 */
	public static void displayMultizoneAnimationDialog(final FragmentActivity activity,
			final MultizoneAnimationDialogListener listener) {
		Bundle bundle = new Bundle();
		MultizoneAnimationDialogFragment fragment = new MultizoneAnimationDialogFragment();
		fragment.setListener(listener);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<MultizoneAnimationDialogListener> mListener = null;

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final MultizoneAnimationDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
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

		final View view = View.inflate(requireActivity(), R.layout.dialog_multizone_animation, null);
		final EditText editTextDuration = view.findViewById(R.id.editTextDuration);
		final Spinner spinnerDirection = view.findViewById(R.id.spinnerDiration);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.title_dialog_animation)
				.setView(view)
				.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
					// Send the positive button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						mListener.getValue().onDialogNegativeClick(MultizoneAnimationDialogFragment.this);
					}
				})
				.setPositiveButton(R.string.button_start, (dialog, id) -> {
					// Send the negative button event back to the host activity
					if (mListener != null && mListener.getValue() != null) {
						int duration;
						try {
							duration = (int) (Double.parseDouble(editTextDuration.getText().toString()) * 1000); // MAGIC_NUMBER
						}
						catch (Exception e) {
							duration = 10000; // MAGIC_NUMBER
						}
						Direction direction = Direction.fromOrdinal(spinnerDirection.getSelectedItemPosition());

						mListener.getValue().onDialogPositiveClick(MultizoneAnimationDialogFragment.this, duration, direction);
					}
				});
		return builder.create();
	}

	@Override
	public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
		if (mListener != null && mListener.getValue() != null) {
			mListener.getValue().onDialogNegativeClick(MultizoneAnimationDialogFragment.this);
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
	 * The direction of the animation.
	 */
	public enum Direction {
		/**
		 * Forward movement.
		 */
		FORWARD,
		/**
		 * Backward movement.
		 */
		BACKWARD;

		/**
		 * Get Direction from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The direction.
		 */
		private static Direction fromOrdinal(final int ordinal) {
			for (Direction direction : values()) {
				if (ordinal == direction.ordinal()) {
					return direction;
				}
			}
			return FORWARD;
		}
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface MultizoneAnimationDialogListener {
		/**
		 * Callback method for positive click from the confirmation dialog.
		 *
		 * @param dialog The confirmation dialog fragment.
		 * @param duration The duration of the animation.
		 * @param direction The direction of the animation.
		 */
		void onDialogPositiveClick(DialogFragment dialog, int duration, Direction direction);

		/**
		 * Callback method for negative click from the confirmation dialog.
		 *
		 * @param dialog the confirmation dialog fragment.
		 */
		void onDialogNegativeClick(DialogFragment dialog);
	}
}
