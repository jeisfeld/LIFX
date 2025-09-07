package de.jeisfeld.lifx.app.scenes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.LifxAlarmService;
import de.jeisfeld.lifx.app.alarms.SelectDeviceDialogFragment;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoreColorType;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.lifx.lan.Light;

/** Fragment for configuration of a scene. */
public class SceneConfigurationFragment extends Fragment {
        /** Parameter key. */
        private static final String PARAM_SCENE_ID = "sceneId";
        /** Default duration. */
        protected static final long DEFAULT_DURATION = 10000;

        private Scene mScene;
        private SceneStepExpandableListAdapter mAdapter;
        private Map<Light, Boolean> mInitialExpandingStatus = new HashMap<>();

        /** Navigate to this fragment. */
        public static void navigate(final Fragment fragment, final Integer sceneId) {
                FragmentActivity activity = fragment == null ? null : fragment.getActivity();
                if (activity != null) {
                        NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
                        Bundle bundle = new Bundle();
                        if (sceneId != null) {
                                bundle.putInt(PARAM_SCENE_ID, sceneId);
                        }
                        navController.navigate(R.id.nav_scene_configuration, bundle);
                }
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
                View root = inflater.inflate(R.layout.fragment_scene_configuration, container, false);
                final ExpandableListView listViewSceneSteps = root.findViewById(R.id.listViewSceneSteps);

                final int sceneId = getArguments() == null ? -1 : getArguments().getInt(PARAM_SCENE_ID, -1);
                mScene = fillFromSceneId(root, mScene == null ? sceneId : mScene.getId());

                for (Scene.LightSteps lightSteps : mScene.getLightSteps()) {
                        mInitialExpandingStatus.putIfAbsent(lightSteps.getLight(), true);
                }
                mAdapter = new SceneStepExpandableListAdapter(this, mScene, mInitialExpandingStatus);
                listViewSceneSteps.setAdapter(mAdapter);
                mInitialExpandingStatus = new HashMap<>();

                root.findViewById(R.id.imageViewAddSceneLight).setOnClickListener(v -> {
                        List<Light> lightsWithStoredColors = ColorRegistry.getInstance().getLightsWithStoredColors();
                        lightsWithStoredColors.removeAll(mScene.getLightSteps().stream().map(Scene.LightSteps::getLight).collect(java.util.stream.Collectors.toSet()));
                        SelectDeviceDialogFragment.displaySelectDeviceDialog(requireActivity(), device ->
                                        StoredColorsDialogFragment.displayStoredColorsDialog(requireActivity(), (int) device.getParameter(DeviceRegistry.DEVICE_ID), StoreColorType.ONLYSELECT, true, false,
                                                        (dialog, storedColor) -> {
                                                                mScene.getSteps().add(new Scene.Step(0, storedColor.getId(), DEFAULT_DURATION));
                                                                mScene = SceneRegistry.getInstance().addOrUpdate(mScene);
                                                                mAdapter.notifyDataSetChanged(mScene);
                                                        }), new ArrayList<>(lightsWithStoredColors));
                });

                root.findViewById(R.id.imageViewTestScene).setOnClickListener(v ->
                        LifxAlarmService.triggerAlarmService(getContext(), LifxAlarmService.ACTION_TEST_SCENE, mScene.getId(), new Date()));

                root.findViewById(R.id.imageViewDeleteScene).setOnClickListener(v -> DialogUtil.displayConfirmationMessage(requireActivity(), dialog -> {
                        SceneRegistry.getInstance().remove(mScene);
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                        navController.navigateUp();
                }, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_scene, mScene.getName()));

                return root;
        }

        private Scene fillFromSceneId(final View root, final int sceneId) {
                Scene scene = SceneRegistry.getInstance().getScene(sceneId);
                final TextView textViewSceneName = root.findViewById(R.id.textViewSceneName);
                textViewSceneName.setText(scene.getName());
                textViewSceneName.setOnClickListener(v -> DialogUtil.displayInputDialog(requireActivity(), new RequestInputDialogListener() {
                        @Override
                        public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
                                if (text == null || text.trim().isEmpty()) {
                                        DialogUtil.displayConfirmationMessage(requireActivity(), R.string.title_did_not_save_empty_name, R.string.message_did_not_save_empty_name);
                                }
                                else {
                                        textViewSceneName.setText(text.trim());
                                        mScene = mScene.withChangedName(getContext(), text.trim());
                                        mAdapter.notifyDataSetChanged(mScene);
                                }
                        }

                        @Override
                        public void onDialogNegativeClick(final DialogFragment dialog) {
                                // do nothing
                        }
                }, R.string.title_dialog_change_scene_name, R.string.button_rename, scene.getName(), R.string.message_dialog_new_scene_name));
                return scene;
        }
}

