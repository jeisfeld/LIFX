package de.jeisfeld.lifx.app.scenes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.LifxAlarmService;

/**
 * Dialog for selecting and executing scenes.
 */
public class ScenesDialogFragment extends DialogFragment {
        /**
         * Show the scenes dialog.
         *
         * @param activity The current activity.
         */
        public static void displayScenesDialog(final FragmentActivity activity) {
                ScenesDialogFragment fragment = new ScenesDialogFragment();
                fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
        }

        @Override
        @NonNull
        public final Dialog onCreateDialog(final Bundle savedInstanceState) {
                List<Scene> scenes = SceneRegistry.getInstance().getScenes();

                View view = View.inflate(requireActivity(), R.layout.dialog_scenes, null);
                GridView gridView = view.findViewById(R.id.gridViewScenes);
                ArrayAdapter<Scene> adapter = new ArrayAdapter<Scene>(requireContext(), R.layout.grid_entry_scene, scenes) {
                        @Override
                        public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                                final View newView = convertView == null
                                                ? View.inflate(requireActivity(), R.layout.grid_entry_scene, null)
                                                : convertView;
                                final Scene scene = scenes.get(position);
                                ((TextView) newView.findViewById(R.id.textViewSceneName)).setText(scene.getName());
                                ImageView imageView = newView.findViewById(R.id.imageViewRunScene);
                                imageView.setOnClickListener(v -> {
                                        LifxAlarmService.triggerAlarmService(requireContext(),
                                                        LifxAlarmService.ACTION_TEST_SCENE, scene.getId(), new Date());
                                        dismiss();
                                });
                                return newView;
                        }
                };
                gridView.setAdapter(adapter);

                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setView(view);
                return builder.create();
        }
}
