package de.jeisfeld.lifx.app.alarms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoredColorsDialogListener;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter;
import de.jeisfeld.lifx.app.util.DialogUtil;

/**
 * Fragment for configuration of an alarm.
 */
public class AlarmStepConfigurationFragment extends Fragment {
	/**
	 * Parameter to pass the alarmId.
	 */
	private static final String PARAM_ALARM_ID = "alarmId";
	/**
	 * Parameter to pass the stepId.
	 */
	private static final String PARAM_STEP_ID = "stepId";
	/**
	 * The list of values of the seekbars.
	 */
	private static final int[] DELAYS = {0, 5, 10, 15, 20, 30, 45, 60, 75, 90, 105, 120, 150, 180, 210, 240, 270,
			300, 360, 420, 480, 540, 600, 720, 900, 1200, 1800, 2700, 3600};
	/**
	 * The start time.
	 */
	private long mStartTime = 0;
	/**
	 * The end time.
	 */
	private long mEndTime = 0;
	/**
	 * The stored color.
	 */
	private StoredColor mStoredColor = null;

	/**
	 * Navigate to this fragment.
	 *
	 * @param activity The source activity.
	 * @param alarmId  The alarm id for which to start the fragment.
	 * @param stepId   The step id for which to start the fragment.
	 */
	public static void navigate(final FragmentActivity activity, final int alarmId, final Integer stepId) {
		if (activity != null) {
			NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
			Bundle bundle = new Bundle();
			bundle.putInt(AlarmStepConfigurationFragment.PARAM_ALARM_ID, alarmId);
			if (stepId != null) {
				bundle.putInt(AlarmStepConfigurationFragment.PARAM_STEP_ID, stepId);
			}
			navController.navigate(R.id.nav_alarm_step_configuration, bundle);
		}
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		assert getArguments() != null;
		View root = inflater.inflate(R.layout.fragment_alarm_step_configuration, container, false);
		final int alarmId = getArguments().getInt(PARAM_ALARM_ID, -1);
		final int stepId = getArguments().getInt(PARAM_STEP_ID, -1);

		final ImageView imageViewStoredColor = root.findViewById(R.id.imageViewStoredColor);
		final TextView textViewColorName = root.findViewById(R.id.textViewColorName);
		final TextView textViewDeviceName = root.findViewById(R.id.textViewDeviceName);
		final SeekBar seekBarAlarmStepStart = root.findViewById(R.id.seekBarAlarmStepStart);
		seekBarAlarmStepStart.setMax(DELAYS.length - 1);
		final TextView textViewAlarmStepStart = root.findViewById(R.id.textViewAlarmStepStart);
		final SeekBar seekBarAlarmStepEnd = root.findViewById(R.id.seekBarAlarmStepEnd);
		seekBarAlarmStepEnd.setMax(DELAYS.length - 1);
		final TextView textViewAlarmStepEnd = root.findViewById(R.id.textViewAlarmStepEnd);

		final Step step;
		if (stepId >= 0) {
			step = new Step(stepId);

			mStartTime = step.getDelay();
			seekBarAlarmStepStart.setProgress(delayValueToProgress(mStartTime));
			mEndTime = mStartTime + step.getDuration();
			seekBarAlarmStepEnd.setProgress(delayValueToProgress(mEndTime));

			mStoredColor = step.getStoredColor();
			textViewColorName.setText(mStoredColor.getName());
			textViewDeviceName.setText(mStoredColor.getLight().getLabel());
			imageViewStoredColor.setImageDrawable(StoredColorsViewAdapter.getButtonDrawable(requireContext(), mStoredColor));
		}

		textViewAlarmStepStart.setText(getDelayString(mStartTime));
		textViewAlarmStepEnd.setText(getDelayString(mEndTime));

		seekBarAlarmStepStart.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				mStartTime = delayProgressToValue(progress);
				textViewAlarmStepStart.setText(getDelayString(mStartTime));
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}
		});

		seekBarAlarmStepEnd.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				mEndTime = delayProgressToValue(progress);
				textViewAlarmStepEnd.setText(getDelayString(mEndTime));
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				// do nothing
			}
		});

		OnClickListener selectStoredColorListener = v -> SelectDeviceDialogFragment.displaySelectDeviceDialog(requireActivity(), device ->
				StoredColorsDialogFragment.displayStoredColorsDialog(requireActivity(), (int) device.getParameter(DeviceRegistry.DEVICE_ID), true,
						new StoredColorsDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
								// do nothing
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}

							@Override
							public void onStoredColorClick(final StoredColor storedColor) {
								mStoredColor = storedColor;
								textViewColorName.setText(storedColor.getName());
								textViewDeviceName.setText(storedColor.getLight().getLabel());
								imageViewStoredColor.setImageDrawable(StoredColorsViewAdapter.getButtonDrawable(requireContext(), storedColor));
							}
						}), new ArrayList<>(ColorRegistry.getInstance().getLightsWithStoredColors()));

		imageViewStoredColor.setOnClickListener(selectStoredColorListener);
		textViewColorName.setOnClickListener(selectStoredColorListener);
		textViewDeviceName.setOnClickListener(selectStoredColorListener);

		root.findViewById(R.id.buttonCancel).setOnClickListener(v -> {
			NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
			navController.navigateUp();
		});

		root.findViewById(R.id.buttonSave).setOnClickListener(v -> {
			if (mStoredColor == null) {
				DialogUtil.displayToast(requireContext(), R.string.toast_did_not_save_no_color);
				return;
			}
			Step newStep = new Step(stepId, mStartTime, mStoredColor.getId(), mEndTime - mStartTime);
			Alarm alarm = new Alarm(alarmId);
			alarm.putStep(newStep);
			AlarmRegistry.getInstance().addOrUpdate(alarm);

			NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
			navController.navigateUp();
		});

		return root;
	}

	/**
	 * Convert progress bar value to delay value.
	 *
	 * @param progress The progress bar value
	 * @return The delay value (in millis).
	 */
	private static long delayProgressToValue(final int progress) {
		return DELAYS[progress] * 1000; // MAGIC_NUMBER
	}

	/**
	 * Convert delay value (in millis) to progress bar value.
	 *
	 * @param delay The delay value
	 * @return The progress bar value
	 */
	private static int delayValueToProgress(final long delay) {
		long delaySec = delay / 1000; // MAGIC_NUMBER
		for (int progress = 0; progress < DELAYS.length; progress++) {
			if (delaySec <= DELAYS[progress]) {
				return progress;
			}
		}
		return DELAYS.length - 1;
	}

	/**
	 * Get String representation of a delay.
	 *
	 * @param delay The delay.
	 * @return The String representation.
	 */
	protected static String getDelayString(final long delay) {
		return delay == 3600000 ? "60:00" : String.format(Locale.getDefault(), "%1$tM:%1$tS", new Date(delay)); // MAGIC_NUMBER
	}
}
