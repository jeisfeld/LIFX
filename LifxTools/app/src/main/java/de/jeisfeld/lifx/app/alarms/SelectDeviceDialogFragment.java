package de.jeisfeld.lifx.app.alarms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.lan.Device;

/**
 * Dialog for selecting a device.
 */
public class SelectDeviceDialogFragment extends DialogFragment {
	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";

	/**
	 * Display a dialog for storing a color or displaying a stored color.
	 *
	 * @param activity the current activity
	 * @param listener The listener waiting for the response
	 */
	public static void displaySelectDeviceDialog(final FragmentActivity activity,
												 final SelectDeviceDialogListener listener) {
		SelectDeviceDialogFragment fragment = new SelectDeviceDialogFragment();
		fragment.setListener(listener);
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
		// Listeners cannot retain functionality when automatically recreated.
		// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
		boolean preventRecreation = false;
		if (savedInstanceState != null) {
			preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
		}
		if (preventRecreation) {
			dismiss();
		}

		final View view = View.inflate(requireActivity(), R.layout.dialog_select_device, null);
		List<Device> devices = DeviceRegistry.getInstance().getDevices(true);
		if (devices.size() > 0 && getContext() != null) {
			ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(getContext(), R.layout.list_view_select_device, devices) {
				@Override
				public View getView(final int position, final View view, @NonNull final ViewGroup parent) {
					final View newView;
					if (view == null) {
						newView = View.inflate(requireActivity(), R.layout.list_view_select_device, null);
					}
					else {
						newView = view;
					}
					Device device = devices.get(position);
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

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view);
		return builder.create();
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
	public interface SelectDeviceDialogListener {
		/**
		 * Callback method for selection of a device.
		 *
		 * @param device the selected device.
		 */
		void onDeviceSelected(Device device);
	}
}
