package de.jeisfeld.lifx.app.managedevices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;

/**
 * Fragment for management of devices.
 */
public class ManageDevicesFragment extends Fragment {
	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_manage_devices, container, false);
		final RecyclerView recyclerView = root.findViewById(R.id.recyclerViewManageDevices);
		populateRecyclerView(recyclerView);
		return root;
	}

	/**
	 * Populate the recycler view for the devices.
	 *
	 * @param recyclerView The recycler view.
	 */
	private void populateRecyclerView(final RecyclerView recyclerView) {
		ManageDevicesViewAdapter adapter = new ManageDevicesViewAdapter(this, recyclerView);
		ItemTouchHelper.Callback callback = new ManageDevicesItemMoveCallback(adapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		adapter.setStartDragListener(touchHelper::startDrag);
		touchHelper.attachToRecyclerView(recyclerView);

		recyclerView.setAdapter(adapter);
	}
}
