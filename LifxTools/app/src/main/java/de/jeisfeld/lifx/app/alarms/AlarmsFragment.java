package de.jeisfeld.lifx.app.alarms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;

/**
 * Fragment for management of alarms.
 */
public class AlarmsFragment extends Fragment {
	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_alarms, container, false);
		final RecyclerView recyclerView = root.findViewById(R.id.recyclerViewAlarms);
		populateRecyclerView(recyclerView);
		return root;
	}

	/**
	 * Populate the recycler view for the alarms.
	 *
	 * @param recyclerView The recycler view.
	 */
	private void populateRecyclerView(final RecyclerView recyclerView) {
		AlarmsViewAdapter adapter = new AlarmsViewAdapter(this, recyclerView);
		ItemTouchHelper.Callback callback = new AlarmsItemMoveCallback(adapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		adapter.setStartDragListener(touchHelper::startDrag);
		touchHelper.attachToRecyclerView(recyclerView);

		recyclerView.setAdapter(adapter);
	}
}
