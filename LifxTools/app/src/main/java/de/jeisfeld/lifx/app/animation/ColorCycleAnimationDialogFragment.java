package de.jeisfeld.lifx.app.animation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.LightViewModel;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoreColorType;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoredColorsDialogListener;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.RequestDurationDialogFragment.RequestDurationDialogListener;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Dialog for setting up a color cycle animation using start and end times per step.
 */
public class ColorCycleAnimationDialogFragment extends DialogFragment {
    /** Default duration for new steps. */
    private static final int DEFAULT_STEP_DURATION = 10000;

    /** Instance state flag indicating if a dialog should not be recreated after orientation change. */
    private static final String PREVENT_RECREATION = "preventRecreation";

    /** Listener for dialog results. */
    private MutableLiveData<ColorCycleAnimationDialogListener> mListener = new MutableLiveData<>();
    /** The model. */
    private MutableLiveData<LightViewModel> mModel = new MutableLiveData<>();
    /** The device id. */
    private int mDeviceId;

    /** Durations of the steps. */
    private final ArrayList<Integer> mDurations = new ArrayList<>();
    /** Stored colors of the steps. */
    private final ArrayList<StoredColor> mStoredColors = new ArrayList<>();

    /**
     * Display a dialog for setting up a color cycle animation.
     *
     * @param activity the current activity
     * @param model    the light view model
     * @param deviceId the device id
     * @param listener The listener waiting for the response
     */
    public static void displayColorCycleAnimationDialog(final FragmentActivity activity, final LightViewModel model,
                    final int deviceId, final ColorCycleAnimationDialogListener listener) {
        Bundle bundle = new Bundle();
        ColorCycleAnimationDialogFragment fragment = new ColorCycleAnimationDialogFragment();
        fragment.setListener(listener);
        fragment.setModel(model);
        fragment.setDeviceId(deviceId);
        fragment.setArguments(bundle);
        fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());

        // Update stored color baseline for the animation.
        model.checkColor();
    }

    /** Set the listener. */
    public final void setListener(final ColorCycleAnimationDialogListener listener) {
        mListener = new MutableLiveData<>(listener);
    }

    /** Set the model. */
    public final void setModel(final LightViewModel model) {
        mModel = new MutableLiveData<>(model);
    }

    /** Set the device id. */
    public final void setDeviceId(final int deviceId) {
        mDeviceId = deviceId;
    }

    @Override
    @Nonnull
    public final Dialog onCreateDialog(final Bundle savedInstanceState) {
        boolean preventRecreation = false;
        if (savedInstanceState != null) {
            preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
        }
        if (preventRecreation) {
            dismiss();
        }

        final View view = View.inflate(requireActivity(), R.layout.dialog_color_cycle_animation, null);
        final ListView listViewSteps = view.findViewById(R.id.listViewColorCycleSteps);
        final StepAdapter adapter = new StepAdapter();
        listViewSteps.setAdapter(adapter);

        Button buttonAdd = view.findViewById(R.id.buttonAddStep);
        buttonAdd.setOnClickListener(v -> {
            FragmentActivity activity = getActivity();
            if (activity != null && mDeviceId != 0) {
                StoredColorsDialogFragment.displayStoredColorsDialog(activity, mDeviceId, StoreColorType.ONLYSELECT,
                        false, false, new StoredColorsDialogListener() {
                            @Override
                            public void onStoredColorClick(final DialogFragment dialog, final StoredColor storedColor) {
                                mStoredColors.add(storedColor);
                                mDurations.add(DEFAULT_STEP_DURATION);
                                adapter.notifyDataSetChanged();
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
                        ArrayList<Color> colors = new ArrayList<>();
                        for (StoredColor sc : mStoredColors) {
                            colors.add(sc.getColor());
                        }
                        mListener.getValue().onDialogPositiveClick(ColorCycleAnimationDialogFragment.this,
                                new ColorCycle(mDurations, colors));
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
            outState.putBoolean(PREVENT_RECREATION, true);
        }
        super.onSaveInstanceState(outState);
    }

    /** Adapter for displaying and editing steps. */
    private class StepAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mStoredColors.size();
        }

        @Override
        public Object getItem(final int position) {
            return position;
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = View.inflate(parent.getContext(), R.layout.list_view_alarm_steps, null);
            }

            final int start = getStart(position);
            final int end = start + mDurations.get(position);

            TextView textViewStart = view.findViewById(R.id.textViewStartTime);
            TextView textViewEnd = view.findViewById(R.id.textViewEndTime);
            textViewStart.setText(getDelayString(start));
            textViewEnd.setText(getDelayString(end));

            ImageView imageViewColor = view.findViewById(R.id.imageViewStoredColor);
            TextView textViewColorName = view.findViewById(R.id.textViewStoredColorName);
            StoredColor storedColor = mStoredColors.get(position);
            imageViewColor.setImageDrawable(storedColor.getButtonDrawable(requireContext()));
            textViewColorName.setText(storedColor.getName());

            View.OnClickListener changeColorListener = v -> {
                if (mDeviceId != 0) {
                    StoredColorsDialogFragment.displayStoredColorsDialog(requireActivity(), mDeviceId,
                            StoreColorType.ONLYSELECT, true, true, new StoredColorsDialogListener() {
                                @Override
                                public void onStoredColorClick(final DialogFragment dialog, final StoredColor newColor) {
                                    mStoredColors.set(position, newColor);
                                    notifyDataSetChanged();
                                }
                            });
                }
            };
            imageViewColor.setOnClickListener(changeColorListener);
            textViewColorName.setOnClickListener(changeColorListener);

            if (position > 0) {
                textViewStart.setOnClickListener(v -> {
                    final int delaySeconds = start / (int) TimeUnit.SECONDS.toMillis(1);
                    DialogUtil.displayDurationDialog(requireActivity(), new RequestDurationDialogListener() {
                        @Override
                        public void onDialogPositiveClick(final DialogFragment dialog, final int minutes,
                                final int seconds) {
                            int newStart = (int) (TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds));
                            int diff = newStart - start;
                            mDurations.set(position - 1, mDurations.get(position - 1) + diff);
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onDialogNegativeClick(final DialogFragment dialog) {
                        }
                    }, R.string.title_dialog_scene_step_delay, R.string.button_ok,
                            delaySeconds / 60, delaySeconds % 60, R.string.message_dialog_scene_step_delay);
                });
            }

            textViewEnd.setOnClickListener(v -> {
                final int endSeconds = end / (int) TimeUnit.SECONDS.toMillis(1);
                DialogUtil.displayDurationDialog(requireActivity(), new RequestDurationDialogListener() {
                    @Override
                    public void onDialogPositiveClick(final DialogFragment dialog, final int minutes, final int seconds) {
                        int newEnd = (int) (TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds));
                        mDurations.set(position, newEnd - start);
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onDialogNegativeClick(final DialogFragment dialog) {
                    }
                }, R.string.title_dialog_scene_step_duration, R.string.button_ok,
                        endSeconds / 60, endSeconds % 60, R.string.message_dialog_scene_step_duration);
            });

            view.findViewById(R.id.imageViewDelete).setOnClickListener(v -> {
                mDurations.remove(position);
                mStoredColors.remove(position);
                notifyDataSetChanged();
            });

            return view;
        }

        private int getStart(final int position) {
            int start = 0;
            for (int i = 0; i < position; i++) {
                start += mDurations.get(i);
            }
            return start;
        }

        private String getDelayString(final long delay) {
            return delay == 3600000 ? "60:00" : String.format(Locale.getDefault(), "%1$tM:%1$tS", new Date(delay));
        }
    }

    /** Callback interface for the dialog. */
    public interface ColorCycleAnimationDialogListener {
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

