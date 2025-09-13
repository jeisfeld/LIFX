package de.jeisfeld.lifx.app.animation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TableRow;

import javax.annotation.Nonnull;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.animation.ColorCycleAnimationDialogFragment;
import de.jeisfeld.lifx.app.animation.ColorCycleAnimationDialogFragment.ColorCycleAnimationDialogListener;
import de.jeisfeld.lifx.app.home.MultizoneViewModel;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.lan.animation.MultizoneMoveDefinition;

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
	 * @param model the multizone view model.
	 * @param listener The listener waiting for the response
	 */
	public static void displayMultizoneAnimationDialog(final FragmentActivity activity, final MultizoneViewModel model,
			final MultizoneAnimationDialogListener listener) {
		Bundle bundle = new Bundle();
		MultizoneAnimationDialogFragment fragment = new MultizoneAnimationDialogFragment();
		fragment.setListener(listener);
		fragment.setModel(model);
		fragment.setArguments(bundle);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());

		// Update the stored color of the model, as this is the baseline for the animation.
		model.checkColor();
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<MultizoneAnimationDialogListener> mListener = new MutableLiveData<>();
	/**
	 * The model.
	 */
	private MutableLiveData<MultizoneViewModel> mModel = new MutableLiveData<>();

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final MultizoneAnimationDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	/**
	 * Set the model.
	 *
	 * @param model the model.
	 */
	public final void setModel(final MultizoneViewModel model) {
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

                final View view = View.inflate(requireActivity(), R.layout.dialog_multizone_animation, null);
                final Spinner spinnerType = view.findViewById(R.id.spinnerType);
                final EditText editTextDuration = view.findViewById(R.id.editTextDuration);
                final EditText editTextStretch = view.findViewById(R.id.editTextStretch);
                final TableRow tableRowDuration = view.findViewById(R.id.tableRowDuration);
                final TableRow tableRowStretch = view.findViewById(R.id.tableRowStretch);

                spinnerType.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(final AdapterView<?> parent, final View selectedView, final int position,
                                                                 final long id) {
                                if (position >= MultizoneMoveDefinition.Direction.values().length) {
                                        tableRowDuration.setVisibility(View.GONE);
                                        tableRowStretch.setVisibility(View.GONE);
                                }
                                else {
                                        tableRowDuration.setVisibility(View.VISIBLE);
                                        tableRowStretch.setVisibility(View.VISIBLE);
                                }
                        }

                        @Override
                        public void onNothingSelected(final AdapterView<?> parent) {
                                // do nothing
                        }
                });

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
                                        if (mListener != null && mListener.getValue() != null && mModel != null
                                                        && mModel.getValue() != null) {
                                                int type = spinnerType.getSelectedItemPosition();
                                                if (type >= MultizoneMoveDefinition.Direction.values().length) {
                                                        if (mModel.getValue().getLight() != null
                                                                        && mModel.getValue().getLight().getParameter(DeviceRegistry.DEVICE_ID) != null) {
                                                                int deviceId = (int) mModel.getValue().getLight().getParameter(DeviceRegistry.DEVICE_ID);
                                                                FragmentActivity activity = getActivity();
                                                                if (activity != null) {
                                                                        ColorCycleAnimationDialogFragment.displayColorCycleAnimationDialog(
                                                                                        activity, mModel.getValue(), deviceId,
                                                                                        new ColorCycleAnimationDialogListener() {
                                                                                                @Override
                                                                                                public void onDialogPositiveClick(final DialogFragment dialogFragment,
                                                                                                                final AnimationData animationData) {
                                                                                                        mListener.getValue().onDialogPositiveClick(dialogFragment, animationData);
                                                                                                }

                                                                                                @Override
                                                                                                public void onDialogNegativeClick(final DialogFragment dialogFragment) {
                                                                                                        mListener.getValue().onDialogNegativeClick(dialogFragment);
                                                                                                }
                                                                                        });
                                                                }
                                                        }
                                                }
                                                else {
                                                        int duration;
                                                        try {
                                                                duration = (int) (Double.parseDouble(editTextDuration.getText().toString()) * 1000); // MAGIC_NUMBER
                                                        }
                                                        catch (Exception e) {
                                                                duration = 10000; // MAGIC_NUMBER
                                                        }
                                                        double stretch;
                                                        try {
                                                                stretch = Math.max(0.1, Double.parseDouble(editTextStretch.getText().toString())); // MAGIC_NUMBER
                                                        }
                                                        catch (Exception e) {
                                                                stretch = 1;
                                                        }

                                                        MultizoneMoveDefinition.Direction direction =
                                                                        MultizoneMoveDefinition.Direction.fromOrdinal(type);

                                                        mListener.getValue().onDialogPositiveClick(MultizoneAnimationDialogFragment.this,
                                                                        new MultizoneMove(duration, stretch, direction,
                                                                                        mModel.getValue().getColors().getValue(), false));
                                                }
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
			outState.putBoolean(PREVENT_RECREATION, true);
		}
		super.onSaveInstanceState(outState);
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface MultizoneAnimationDialogListener {
		/**
		 * Callback method for positive click from the confirmation dialog.
		 *
		 * @param dialog The confirmation dialog fragment.
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
