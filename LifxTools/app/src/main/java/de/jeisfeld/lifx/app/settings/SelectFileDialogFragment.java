package de.jeisfeld.lifx.app.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DialogUtil;

/**
 * Dialog for selecting a file.
 */
public class SelectFileDialogFragment extends DialogFragment {
	/**
	 * Parameter to pass the resource for the dialog title.
	 */
	private static final String PARAM_DIALOG_TITLE = "dialogTitle";
	/**
	 * Parameter to pass the list of files to the fragment.
	 */
	private static final String PARAM_LIST_OF_FILES = "listOfFiles";
	/**
	 * Parameter to pass the display name selector.
	 */
	private static final String PARAM_DISPLAY_NAME_SELECTOR = "displayNameSelector";

	/**
	 * Display a dialog for selecting a file from a list of files.
	 *
	 * @param activity      the current activity
	 * @param listener      The listener waiting for the response
	 * @param titleResource A resource for the dialog title.
	 * @param selector      The selector for the display name of files.
	 * @param files         The list of files to be shown.
	 */
	public static void displaySelectImportFileDialog(final FragmentActivity activity,
													 final SelectFileDialogListener listener,
													 final int titleResource,
													 final FileDisplayNameSelector selector,
													 final File[] files) {
		SelectFileDialogFragment fragment = new SelectFileDialogFragment();
		fragment.setListener(listener);
		Bundle args = new Bundle();
		args.putInt(PARAM_DIALOG_TITLE, titleResource);
		args.putSerializable(PARAM_LIST_OF_FILES, new ArrayList<>(Arrays.asList(files)));
		args.putSerializable(PARAM_DISPLAY_NAME_SELECTOR, selector);
		fragment.setArguments(args);
		fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * The listener called when the dialog is ended.
	 */
	private MutableLiveData<SelectFileDialogListener> mListener = null;

	/**
	 * Set the listener.
	 *
	 * @param listener The listener.
	 */
	public final void setListener(final SelectFileDialogListener listener) {
		mListener = new MutableLiveData<>(listener);
	}

	@Override
	@Nonnull
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		final View view = View.inflate(requireActivity(), R.layout.dialog_select_file, null);
		assert getArguments() != null;

		@SuppressWarnings("unchecked") final List<File> files = (List<File>) getArguments().getSerializable(PARAM_LIST_OF_FILES);
		final FileDisplayNameSelector selector = (FileDisplayNameSelector) getArguments().getSerializable(PARAM_DISPLAY_NAME_SELECTOR);
		assert selector != null;
		final int titleResource = getArguments().getInt(PARAM_DIALOG_TITLE);
		assert files != null;

		((TextView) view.findViewById(R.id.dialog_title_select)).setText(titleResource);

		ArrayAdapter<File> adapter = new ArrayAdapter<File>(requireContext(), R.layout.list_view_select_file, files) {
			@Override
			public View getView(final int position, final View view, @NonNull final ViewGroup parent) {
				final View newView;
				if (view == null) {
					newView = View.inflate(requireActivity(), R.layout.list_view_select_file, null);
				}
				else {
					newView = view;
				}
				File file = files.get(position);
				TextView textViewFileName = newView.findViewById(R.id.textViewFileName);
				textViewFileName.setText(selector.getDisplayName(file));
				newView.setOnClickListener(v -> {
					SelectFileDialogListener listener = mListener == null ? null : mListener.getValue();
					if (listener != null) {
						listener.onFileSelected(file);
						dismiss();
					}
				});

				((ImageView) newView.findViewById(R.id.imageViewDelete)).setOnClickListener((OnClickListener) v ->
						DialogUtil.displayConfirmationMessage(requireActivity(), dialog -> {
									//noinspection ResultOfMethodCallIgnored
									file.delete();
									files.remove(file);
									notifyDataSetChanged();
								}, R.string.title_dialog_delete_stored_settings, R.string.button_cancel, R.string.button_delete,
								R.string.message_dialog_delete_stored_settings, selector.getDisplayName(file)));

				return newView;
			}
		};
		((ListView) view.findViewById(R.id.listViewSelectFile)).setAdapter(adapter);

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
	 * Interface for determining the display name of a file.
	 */
	public interface FileDisplayNameSelector extends Serializable {
		/**
		 * Get the display name of a file.
		 *
		 * @param file The file.
		 * @return The display name.
		 */
		String getDisplayName(File file);
	}

	/**
	 * A callback handler for the dialog.
	 */
	public interface SelectFileDialogListener {
		/**
		 * Callback method for selection of a file.
		 *
		 * @param file the selected file.
		 */
		void onFileSelected(File file);
	}
}
