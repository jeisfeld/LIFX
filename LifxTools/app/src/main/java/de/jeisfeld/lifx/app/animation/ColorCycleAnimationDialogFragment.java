package de.jeisfeld.lifx.app.animation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.LightViewModel;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoreColorType;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoredColorsDialogListener;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Dialog for setting up a color cycle animation.
 */
public class ColorCycleAnimationDialogFragment extends DialogFragment {
    /**
     * Instance state flag indicating if a dialog should not be recreated after orientation change.
     */
    private static final String PREVENT_RECREATION = "preventRecreation";

    /**
     * Display a dialog for setting up a color cycle animation.
     *
     * @param activity the current activity
     * @param model the light view model.
     * @param listener The listener waiting for the response
     */
    public static void displayColorCycleAnimationDialog(final FragmentActivity activity, final LightViewModel model,
                    final ColorCycleAnimationDialogListener listener) {
        Bundle bundle = new Bundle();
        ColorCycleAnimationDialogFragment fragment = new ColorCycleAnimationDialogFragment();
        fragment.setListener(listener);
        fragment.setModel(model);
        fragment.setArguments(bundle);
        fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());

        // Update stored color baseline for the animation.
        model.checkColor();
    }

    /**
     * The listener called when the dialog is ended.
     */
    private MutableLiveData<ColorCycleAnimationDialogListener> mListener = new MutableLiveData<>();
    /**
     * The model.
     */
    private MutableLiveData<LightViewModel> mModel = new MutableLiveData<>();
    /**
     * The selected colors.
     */
    private final ArrayList<Color> mColors = new ArrayList<>();

    /**
     * Set the listener.
     *
     * @param listener The listener.
     */
    public final void setListener(final ColorCycleAnimationDialogListener listener) {
        mListener = new MutableLiveData<>(listener);
    }

    /**
     * Set the model.
     *
     * @param model the model.
     */
    public final void setModel(final LightViewModel model) {
        mModel = new MutableLiveData<>(model);
    }

    @Override
    @Nonnull
    public final Dialog onCreateDialog(final Bundle savedInstanceState) {
        // Listeners cannot retain functionality when automatically recreated.
        boolean preventRecreation = false;
        if (savedInstanceState != null) {
            preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
        }
        if (preventRecreation) {
            dismiss();
        }

        final View view = View.inflate(requireActivity(), R.layout.dialog_color_cycle_animation, null);
        final EditText editTextDuration = view.findViewById(R.id.editTextDuration);
        final LinearLayout layoutColorList = view.findViewById(R.id.layoutColorList);

        view.findViewById(R.id.buttonAddColor).setOnClickListener(v -> {
            FragmentActivity activity = getActivity();
            LightViewModel model = mModel.getValue();
            if (activity != null && model != null && model.getLight() != null
                    && model.getLight().getParameter(DeviceRegistry.DEVICE_ID) != null) {
                int deviceId = (int) model.getLight().getParameter(DeviceRegistry.DEVICE_ID);
                StoredColorsDialogFragment.displayStoredColorsDialog(activity, deviceId, StoreColorType.ONLYSELECT,
                        false, false, new StoredColorsDialogListener() {
                            @Override
                            public void onStoredColorClick(final DialogFragment dialog, final StoredColor storedColor) {
                                mColors.add(storedColor.getColor());
                                ImageView imageView = new ImageView(getContext());
                                imageView.setImageDrawable(storedColor.getButtonDrawable(getContext()));
                                int size = getResources().getDimensionPixelSize(R.dimen.medium_button_size);
                                imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                                layoutColorList.addView(imageView);
                            }
                        });
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_dialog_color_cycle_animation)
                .setView(view)
                .setNegativeButton(R.string.button_cancel, (dialog, id) -> {
                    if (mListener != null && mListener.getValue() != null) {
                        mListener.getValue().onDialogNegativeClick(ColorCycleAnimationDialogFragment.this);
                    }
                })
                .setPositiveButton(R.string.button_start, (dialog, id) -> {
                    if (mListener != null && mListener.getValue() != null) {
                        int duration;
                        try {
                            duration = (int) (Double.parseDouble(editTextDuration.getText().toString()) * 1000);
                        }
                        catch (Exception e) {
                            duration = 1000; // MAGIC_NUMBER
                        }
                        mListener.getValue().onDialogPositiveClick(ColorCycleAnimationDialogFragment.this,
                                new ColorCycle(duration, new ArrayList<>(mColors)));
                    }
                });
        return builder.create();
    }

    @Override
    public final void onCancel(@Nonnull final DialogInterface dialogInterface) {
        if (mListener != null && mListener.getValue() != null) {
            mListener.getValue().onDialogNegativeClick(ColorCycleAnimationDialogFragment.this);
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
    public interface ColorCycleAnimationDialogListener {
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
