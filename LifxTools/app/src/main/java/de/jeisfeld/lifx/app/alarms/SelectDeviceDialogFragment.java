package de.jeisfeld.lifx.app.alarms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.lan.Device;

/**
 * Dialog for selecting a device.
 */
public class SelectDeviceDialogFragment extends DialogFragment {
	/**
	 * Parameter to pass the list of devices to the fragment.
	 */
	private static final String PARAM_LIST_OF_DEVICES = "listOfDevices";

	/**
	 * Display a dialog for selecting a device.
	 *
	 * @param activity the current activity
	 * @param listener The listener waiting for the response
	 * @param devices  The list of devices to be shown.
	 */
	public static void displaySelectDeviceDialog(final FragmentActivity activity,
												 final SelectDeviceDialogListener listener,
												 final ArrayList<Device> devices) {
		SelectDeviceDialogFragment fragment = new SelectDeviceDialogFragment();
		fragment.setListener(listener);
		Bundle args = new Bundle();
		args.putSerializable(PARAM_LIST_OF_DEVICES, devices);
		fragment.setArguments(args);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<SelectDeviceDialogListener> mListener = null;

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final SelectDeviceDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	@Override
	@Nonnull
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		final View view = View.inflate(requireActivity(), R.layout.dialog_select_device, null);
		assert getArguments() != null;
		@SuppressWarnings("unchecked")
		List<Device> lights = (List<Device>) getArguments().getSerializable(PARAM_LIST_OF_DEVICES);
		if (lights != null && lights.size() > 0 && getContext() != null) {
			ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(getContext(), R.layout.list_view_select_device, lights) {
				@Override
				public View getView(final int position, final View view, @NonNull final ViewGroup parent) {
					final View newView;
					if (view == null) {
						newView = View.inflate(requireActivity(), R.layout.list_view_select_device, null);
					}
					else {
						newView = view;
					}
					Device device = lights.get(position);
					TextView textViewDeviceName = newView.findViewById(R.id.textViewDeviceName);
					textViewDeviceName.setText(device.getLabel());
					newView.setOnClickListener(v -> {
						SelectDeviceDialogListener listener = mListener == null ? null : mListener.getValue();
						if (listener != null) {
							listener.onDeviceSelected(device);
							dismiss();
						}
					});
					return newView;
				}
			};
			((ListView) view.findViewById(R.id.listViewSelectDevice)).setAdapter(adapter);
		}
		else {
			((TextView) view.findViewById(R.id.textViewNoDeviceAvailable)).setVisibility(View.VISIBLE);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view);
		return builder.create();
	}

	@Override
	public final void onPause() {
		super.onPause();
		// this dialog does not support onPause as it has serialization issues in colors.
		dismiss();
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface SelectDeviceDialogListener {
		/**
		 * Callback method for selection of a device.
		 *
		 * @param device the selected device.
		 */
		void onDeviceSelected(Device device);
	}
}
