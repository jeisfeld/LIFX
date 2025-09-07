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
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.LifxAlarmService;

/**
 * Dialog for selecting a scene.
 */
public class ScenesDialogFragment extends DialogFragment {
        /** Prevent recreation after orientation change. */
        private static final String PREVENT_RECREATION = "preventRecreation";

        /** Display the dialog. */
        public static void displayScenesDialog(final FragmentActivity activity) {
                ScenesDialogFragment fragment = new ScenesDialogFragment();
                fragment.show(activity.getSupportFragmentManager(), fragment.getClass().toString());
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
                boolean preventRecreation = false;
                if (savedInstanceState != null) {
                        preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
                }
                if (preventRecreation) {
                        dismiss();
                }

                final View view = View.inflate(requireActivity(), R.layout.dialog_scenes, null);

                final List<Scene> scenes = SceneRegistry.getInstance().getScenes();
                if (!scenes.isEmpty()) {
                        GridView gridView = view.findViewById(R.id.gridViewScenes);
                        ArrayAdapter<Scene> adapter = new ArrayAdapter<Scene>(requireContext(), R.layout.grid_entry_select_color, scenes) {
                                @Override
                                public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                                        final View newView = convertView == null
                                                        ? View.inflate(requireActivity(), R.layout.grid_entry_select_color, null)
                                                        : convertView;
                                        final Scene scene = scenes.get(position);
                                        ((TextView) newView.findViewById(R.id.textViewColorName)).setText(scene.getName());
                                        ImageView imageView = newView.findViewById(R.id.imageViewApplyColor);
                                        imageView.setImageResource(R.drawable.ic_menu_stored_colors);
                                        imageView.setOnClickListener(v -> {
                                                LifxAlarmService.triggerAlarmService(getContext(),
                                                                LifxAlarmService.ACTION_TEST_SCENE, scene.getId(), new Date());
                                                dismiss();
                                        });
                                        return newView;
                                }
                        };
                        gridView.setAdapter(adapter);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setView(view);
                builder.setNegativeButton(R.string.button_cancel, (dialog, id) -> { });
                return builder.create();
        }

        @Override
        public void onSaveInstanceState(@NonNull final Bundle outState) {
                outState.putBoolean(PREVENT_RECREATION, true);
                super.onSaveInstanceState(outState);
        }
}
