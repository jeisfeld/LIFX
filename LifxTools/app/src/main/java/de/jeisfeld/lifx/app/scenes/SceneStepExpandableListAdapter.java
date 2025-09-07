package de.jeisfeld.lifx.app.scenes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.scenes.Scene.LightSteps;
import de.jeisfeld.lifx.app.scenes.Scene.Step;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsDialogFragment.StoreColorType;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.RequestDurationDialogFragment.RequestDurationDialogListener;
import de.jeisfeld.lifx.lan.Light;

/**
 * Adapter for the expandable list of scene steps.
 */
public class SceneStepExpandableListAdapter extends BaseExpandableListAdapter {
        private static final int SECONDS_PER_MINUTE = (int) TimeUnit.MINUTES.toSeconds(1);
        private final SceneConfigurationFragment mFragment;
        private Scene mScene;
        private List<LightSteps> mLightStepsList;
        private Map<Light, Boolean> mInitialExpandingStatus;
        private WeakReference<ExpandableListView> mParent = new WeakReference<>(null);

        protected SceneStepExpandableListAdapter(final SceneConfigurationFragment fragment, final Scene scene,
                                                 final Map<Light, Boolean> initialExpandingStatus) {
                mFragment = fragment;
                mScene = scene;
                mLightStepsList = scene.getLightSteps();
                mInitialExpandingStatus = initialExpandingStatus;
        }

        protected static String getDelayString(final long delay) {
                return delay == 3600000 ? "60:00" : String.format(Locale.getDefault(), "%1$tM:%1$tS", new Date(delay));
        }

        protected Map<Light, Boolean> getExpandingStatus() {
                ExpandableListView parent = mParent.get();
                Map<Light, Boolean> result = new HashMap<>();
                if (parent != null) {
                        for (int groupPosition = 0; groupPosition < getGroupCount(); groupPosition++) {
                                LightSteps lightSteps = getGroup(groupPosition);
                                result.put(lightSteps.getLight(), parent.isGroupExpanded(groupPosition));
                        }
                }
                return result;
        }

        @Override
        public int getGroupCount() {
                return mLightStepsList.size();
        }

        @Override
        public int getChildrenCount(final int groupPosition) {
                return getGroup(groupPosition).getSteps().size();
        }

        @Override
        public LightSteps getGroup(final int groupPosition) {
                return mLightStepsList.get(groupPosition);
        }

        @Override
        public Step getChild(final int groupPosition, final int childPosition) {
                return getGroup(groupPosition).getSteps().get(childPosition);
        }

        @Override
        public long getGroupId(final int groupPosition) {
                return groupPosition;
        }

        @Override
        public long getChildId(final int groupPosition, final int childPosition) {
                return childPosition;
        }

        @Override
        public boolean hasStableIds() {
                return false;
        }

        @Override
        public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent) {
                mParent = new WeakReference<>((ExpandableListView) parent);
                View view = convertView;
                if (convertView == null) {
                        LayoutInflater layoutInflater = (LayoutInflater) mFragment.requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        assert layoutInflater != null;
                        view = layoutInflater.inflate(R.layout.list_view_scene_lights, parent, false);
                }
                if (groupPosition >= getGroupCount()) {
                        return view;
                }

                LightSteps lightSteps = getGroup(groupPosition);
                TextView listTitleTextView = view.findViewById(R.id.textViewDeviceName);
                listTitleTextView.setText(lightSteps.getLight().getLabel());

                boolean isCollapsed = !isExpanded;

                Boolean initialExpandingStatus = mInitialExpandingStatus.get(lightSteps.getLight());
                if (initialExpandingStatus != null) {
                        if (initialExpandingStatus) {
                                ((ExpandableListView) parent).expandGroup(groupPosition);
                                isCollapsed = false;
                        }
                        else {
                                ((ExpandableListView) parent).collapseGroup(groupPosition);
                                isCollapsed = true;
                        }
                        mInitialExpandingStatus.remove(lightSteps.getLight());
                }

                TextView textViewStartTime = view.findViewById(R.id.textViewStartTime);
                TextView textViewEndTime = view.findViewById(R.id.textViewEndTime);
                ImageView imageViewAddStep = view.findViewById(R.id.imageViewAddSceneStep);

                long minDelay = Long.MAX_VALUE;
                long maxEndTime = Long.MIN_VALUE;
                for (Step step : lightSteps.getSteps()) {
                        minDelay = Math.min(minDelay, step.getDelay());
                        maxEndTime = Math.max(maxEndTime, step.getDelay() + step.getDuration());
                }

                if (isCollapsed) {
                        textViewStartTime.setText(getDelayString(minDelay));
                        textViewStartTime.setVisibility(View.VISIBLE);
                        textViewEndTime.setText(getDelayString(maxEndTime));
                        textViewEndTime.setVisibility(View.VISIBLE);
                        imageViewAddStep.setVisibility(View.GONE);
                }
                else {
                        textViewStartTime.setVisibility(View.GONE);
                        textViewEndTime.setVisibility(View.GONE);
                        imageViewAddStep.setVisibility(View.VISIBLE);
                        long finalMaxEndTime = maxEndTime;
                        imageViewAddStep.setOnClickListener(v -> StoredColorsDialogFragment.displayStoredColorsDialog(mFragment.requireActivity(), (int) lightSteps.getLight().getParameter(DeviceRegistry.DEVICE_ID), StoreColorType.ONLYSELECT, true, false,
                                (dialog, storedColor) -> {
                                        mScene.getSteps().add(new Step(finalMaxEndTime, storedColor.getId(), SceneConfigurationFragment.DEFAULT_DURATION));
                                        mScene = SceneRegistry.getInstance().addOrUpdate(mScene);
                                        mInitialExpandingStatus = getExpandingStatus();
                                        notifyDataSetChanged(mScene);
                                }));
                }

                return view;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView,
                                 final ViewGroup parent) {
                final Step originalStep = getChild(groupPosition, childPosition);
                View view = convertView;
                if (convertView == null) {
                        LayoutInflater layoutInflater = (LayoutInflater) mFragment.requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        assert layoutInflater != null;
                        view = layoutInflater.inflate(R.layout.list_view_alarm_steps, parent, false);
                }

                final TextView textViewStartTime = view.findViewById(R.id.textViewStartTime);
                final TextView textViewEndTime = view.findViewById(R.id.textViewEndTime);
                textViewStartTime.setText(getDelayString(originalStep.getDelay()));
                textViewEndTime.setText(getDelayString(originalStep.getDelay() + originalStep.getDuration()));

                final ImageView imageViewStoredColor = view.findViewById(R.id.imageViewStoredColor);
                final TextView textViewStoredColorName = view.findViewById(R.id.textViewStoredColorName);
                imageViewStoredColor.setImageDrawable(originalStep.getStoredColor().getButtonDrawable(mFragment.requireContext()));
                textViewStoredColorName.setText(originalStep.getStoredColor().getName());

                View.OnClickListener changeColorListener = v -> {
                        final Step step = getChild(groupPosition, childPosition);
                        StoredColorsDialogFragment.displayStoredColorsDialog(mFragment.requireActivity(), step.getStoredColor().getDeviceId(), StoreColorType.ONLYSELECT, true, false,
                                (dialog, storedColor) -> {
                                        Step newStep = new Step(step.getId(), step.getDelay(), storedColor.getId(), step.getDuration());
                                        mScene.getSteps().remove(step);
                                        mScene.getSteps().add(newStep);
                                        SceneRegistry.getInstance().addOrUpdate(mScene);
                                        notifyDataSetChanged();
                                });
                };
                imageViewStoredColor.setOnClickListener(changeColorListener);
                textViewStoredColorName.setOnClickListener(changeColorListener);

                textViewStartTime.setOnClickListener(v -> {
                        final Step step = getChild(groupPosition, childPosition);
                        int delaySeconds = (int) (step.getDelay() / TimeUnit.SECONDS.toMillis(1));

                        DialogUtil.displayDurationDialog(mFragment.requireActivity(), new RequestDurationDialogListener() {
                                        @Override
                                        public void onDialogPositiveClick(final DialogFragment dialog, final int minutes, final int seconds) {
                                                Step newStep = step.withDelay(TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds));
                                                mScene.updateDelay(newStep);
                                                SceneRegistry.getInstance().addOrUpdate(mScene);
                                                notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onDialogNegativeClick(final DialogFragment dialog) {
                                        }
                                }, R.string.title_dialog_scene_step_delay, R.string.button_ok, delaySeconds / SECONDS_PER_MINUTE,
                                delaySeconds % SECONDS_PER_MINUTE, R.string.message_dialog_scene_step_delay);
                });

                textViewEndTime.setOnClickListener(v -> {
                        final Step step = getChild(groupPosition, childPosition);
                        int durationSeconds = (int) (step.getDuration() / TimeUnit.SECONDS.toMillis(1));

                        DialogUtil.displayDurationDialog(mFragment.requireActivity(), new RequestDurationDialogListener() {
                                        @Override
                                        public void onDialogPositiveClick(final DialogFragment dialog, final int minutes, final int seconds) {
                                                Step newStep = step.withDuration(TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds));
                                                mScene.updateDuration(newStep);
                                                SceneRegistry.getInstance().addOrUpdate(mScene);
                                                notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onDialogNegativeClick(final DialogFragment dialog) {
                                        }
                                }, R.string.title_dialog_scene_step_duration, R.string.button_ok, durationSeconds / SECONDS_PER_MINUTE,
                                durationSeconds % SECONDS_PER_MINUTE, R.string.message_dialog_scene_step_duration);
                });

                view.findViewById(R.id.imageViewDelete).setOnClickListener(v -> DialogUtil.displayConfirmationMessage(mFragment.requireActivity(), dialog -> {
                        mScene.updateDuration(originalStep.withDuration(0));
                        mScene.removeStep(originalStep.getId());
                        SceneRegistry.getInstance().remove(originalStep, mScene.getId());
                        SceneRegistry.getInstance().addOrUpdate(mScene);
                        mInitialExpandingStatus = getExpandingStatus();
                        notifyDataSetChanged();

                        if (mScene.getSteps().isEmpty()) {
                                SceneRegistry.getInstance().remove(mScene);
                                FragmentActivity activity = mFragment.requireActivity();
                                NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
                                navController.navigateUp();
                        }
                }, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_scene_step));

                return view;
        }

        @Override
        public boolean isChildSelectable(final int groupPosition, final int childPosition) {
                return false;
        }

        @Override
        public final void notifyDataSetChanged() {
                mLightStepsList = mScene.getLightSteps();
                super.notifyDataSetChanged();
        }

        protected void notifyDataSetChanged(final Scene scene) {
                mScene = scene;
                mLightStepsList = scene.getLightSteps();
                super.notifyDataSetChanged();
        }
}

