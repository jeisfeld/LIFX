package de.jeisfeld.lifx.app.scenes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.SelectDeviceDialogFragment;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoreColorType;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.lan.Light;

/**
 * Fragment for management of scenes.
 */
public class ScenesFragment extends Fragment {
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_scenes, container, false);
		final RecyclerView recyclerView = root.findViewById(R.id.recyclerViewScenes);
		populateRecyclerView(recyclerView);

		root.findViewById(R.id.buttonAddScene).setOnClickListener(v -> createNewScene());

		return root;
	}

	/**
	 * Populate the recycler view.
	 */
	private void populateRecyclerView(final RecyclerView recyclerView) {
		ScenesViewAdapter adapter = new ScenesViewAdapter(this, recyclerView);
		ItemTouchHelper.Callback callback = new ScenesItemMoveCallback(adapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		adapter.setStartDragListener(touchHelper::startDrag);
		touchHelper.attachToRecyclerView(recyclerView);
		recyclerView.setAdapter(adapter);
	}

	private void createNewScene() {
		if (ColorRegistry.getInstance().getLightsWithStoredColors().isEmpty()) {
			DialogUtil.displayConfirmationMessage(requireActivity(), R.string.title_no_stored_colors, R.string.message_no_stored_colors);
			return;
		}

		List<Light> lightsWithStoredColors = ColorRegistry.getInstance().getLightsWithStoredColors();
                SelectDeviceDialogFragment.displaySelectDeviceDialog(requireActivity(), device ->
                                StoredColorsDialogFragment.displayStoredColorsDialog(requireActivity(), (int) device.getParameter(DeviceRegistry.DEVICE_ID), StoreColorType.ONLYSELECT, true, true,
                                                (dialog, storedColor) -> {
							List<Scene.Step> steps = new ArrayList<>();
							steps.add(new Scene.Step(0, storedColor.getId(), SceneConfigurationFragment.DEFAULT_DURATION));
							Scene scene = new Scene(SceneRegistry.getInstance().getNewSceneName(getContext()), steps);
							scene = SceneRegistry.getInstance().addOrUpdate(scene);
							SceneConfigurationFragment.navigate(ScenesFragment.this, scene.getId());
						}), new ArrayList<>(lightsWithStoredColors));
	}
}

