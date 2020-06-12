package de.jeisfeld.lifx.app.storedcolors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;

/**
 * Fragment for management of stored colors.
 */
public class StoredColorsFragment extends Fragment {
	/**
	 * Parameter to pass the deviceId.
	 */
	private static final String PARAM_DEVICE_ID = "deviceId";

	/**
	 * Navigate to this fragment.
	 *
	 * @param fragment The source fragment.
	 * @param deviceId  The device id for which to start the fragment.
	 */
	public static void navigate(final Fragment fragment, final Integer deviceId) {
		FragmentActivity activity = fragment == null ? null : fragment.getActivity();
		if (activity != null) {
			NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
			Bundle bundle = new Bundle();
			if (deviceId != null) {
				bundle.putInt(PARAM_DEVICE_ID, deviceId);
			}
			navController.navigate(R.id.nav_stored_colors, bundle);
		}
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_stored_colors, container, false);
		final RecyclerView recyclerView = root.findViewById(R.id.recyclerViewStoredColors);
		populateRecyclerView(recyclerView);
		return root;
	}

	/**
	 * Populate the recycler view for the stored colors.
	 *
	 * @param recyclerView The recycler view.
	 */
	private void populateRecyclerView(final RecyclerView recyclerView) {
		Integer deviceId = null;
		if (getArguments() != null) {
			deviceId = getArguments().getInt(PARAM_DEVICE_ID, -1);
			if (deviceId == -1) {
				deviceId = null;
			}
		}
		StoredColorsViewAdapter adapter = new StoredColorsViewAdapter(this, recyclerView, deviceId);
		ItemTouchHelper.Callback callback = new StoredColorsItemMoveCallback(adapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		adapter.setStartDragListener(touchHelper::startDrag);
		touchHelper.attachToRecyclerView(recyclerView);

		recyclerView.setAdapter(adapter);
	}
}
