package de.jeisfeld.lifx.app.scenes;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;

/**
 * Class holding information about a scene.
 */
public class Scene {
        /** The id for storage. */
        private final int mId;
        /** The scene name. */
        private final String mName;
        /** The scene steps. */
        private final List<Step> mSteps;

        /**
         * Generate a scene.
         *
         * @param id    The id for storage
         * @param name  The name
         * @param steps The steps
         */
        public Scene(final int id, final String name, final List<Step> steps) {
                mId = id;
                mName = name;
                mSteps = steps;
        }

        /**
         * Generate a new scene without id.
         *
         * @param name  The name
         * @param steps The steps
         */
        public Scene(final String name, final List<Step> steps) {
                this(-1, name, steps);
        }

        /**
         * Retrieve a scene from storage via id.
         *
         * @param sceneId The id.
         */
        public Scene(final int sceneId) {
                mId = sceneId;
                mName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_scene_name, sceneId);
                List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_scene_step_ids, sceneId);
                mSteps = new ArrayList<>();
                for (Integer stepId : stepIds) {
                        if (stepId != null) {
                                mSteps.add(new Step(stepId));
                        }
                }
        }

        /**
         * Store this scene.
         *
         * @return the stored scene.
         */
        protected Scene store() {
                Scene scene = this;
                if (scene.getId() < 0) {
                        int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_scene_max_id, 0) + 1;
                        PreferenceUtil.setSharedPreferenceInt(R.string.key_scene_max_id, newId);
                        List<Integer> sceneIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_scene_ids);
                        sceneIds.add(newId);
                        PreferenceUtil.setSharedPreferenceIntList(R.string.key_scene_ids, sceneIds);
                        scene = new Scene(newId, getName(), getSteps());
                }

                PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_scene_name, scene.getId(), scene.getName());

                List<Step> newSteps = new ArrayList<>();
                Collections.sort(scene.getSteps());
                for (Step step : scene.getSteps()) {
                        newSteps.add(step.store(scene.getId()));
                }
                scene.getSteps().clear();
                scene.getSteps().addAll(newSteps);
                return scene;
        }

        /**
         * Get the id for storage.
         *
         * @return The id for storage.
         */
        public int getId() {
                return mId;
        }

        /**
         * Get the name.
         *
         * @return The name.
         */
        public String getName() {
                return mName;
        }

        /**
         * Get the steps.
         *
         * @return The steps.
         */
        public List<Step> getSteps() {
                return mSteps;
        }

        /**
         * Change the scene name.
         *
         * @param context The context
         * @param name    The new name
         * @return The updated scene
         */
        protected Scene withChangedName(final Context context, final String name) {
                Scene newScene = new Scene(getId(), name, getSteps());
                SceneRegistry.getInstance().addOrUpdate(newScene);
                return newScene;
        }

        /**
         * Get a list of LightSteps from the total list of steps.
         *
         * @return The lightsteps.
         */
        protected final List<LightSteps> getLightSteps() {
                Collections.sort(getSteps());
                List<LightSteps> result = new ArrayList<>();
                for (Step step : getSteps()) {
                        boolean found = false;
                        Light light = step.getStoredColor().getLight();
                        for (LightSteps lightSteps : result) {
                                if (lightSteps.getLight().equals(light)) {
                                        found = true;
                                        lightSteps.getSteps().add(step);
                                }
                        }
                        if (!found) {
                                List<Step> newSteps = new ArrayList<>();
                                newSteps.add(step);
                                result.add(new LightSteps(light, newSteps));
                        }
                }
                return result;
        }

        /**
         * Remove a step from the scene.
         *
         * @param stepId The id of the step to remove.
         */
        protected void removeStep(final int stepId) {
                Step remove = null;
                for (Step step : getSteps()) {
                        if (step.getId() == stepId) {
                                remove = step;
                                break;
                        }
                }
                if (remove != null) {
                        getSteps().remove(remove);
                }
        }

        /**
         * Update the duration of a step, shifting other steps accordingly.
         *
         * @param updatedStep The updated step.
         */
        protected void updateDuration(final Step updatedStep) {
                List<Step> updatedSteps = new ArrayList<>();
                for (LightSteps lightSteps : getLightSteps()) {
                        if (updatedStep.getStoredColor().getLight().equals(lightSteps.getLight())) {
                                boolean afterUpdatedStep = false;
                                long durationDiff = 0;
                                for (Step step : lightSteps.getSteps()) {
                                        if (step.getId() == updatedStep.getId()) {
                                                durationDiff = updatedStep.getDuration() - step.getDuration();
                                                updatedSteps.add(updatedStep);
                                                afterUpdatedStep = true;
                                        }
                                        else if (afterUpdatedStep) {
                                                updatedSteps.add(step.withDelay(step.getDelay() + durationDiff));
                                        }
                                        else {
                                                updatedSteps.add(step);
                                        }
                                }
                        }
                        else {
                                updatedSteps.addAll(lightSteps.getSteps());
                        }
                }
                getSteps().clear();
                getSteps().addAll(updatedSteps);
                Collections.sort(getSteps());
        }

        /**
         * Update the delay of a step, shifting other steps accordingly.
         *
         * @param updatedStep The updated step.
         */
        protected void updateDelay(final Step updatedStep) {
                List<Step> updatedSteps = new ArrayList<>();
                for (LightSteps lightSteps : getLightSteps()) {
                        if (updatedStep.getStoredColor().getLight().equals(lightSteps.getLight())) {
                                boolean afterUpdatedStep = false;
                                long delayDiff = 0;
                                for (Step step : lightSteps.getSteps()) {
                                        if (step.getId() == updatedStep.getId()) {
                                                if (afterUpdatedStep) {
                                                        delayDiff -= step.getDuration();
                                                }
                                                else {
                                                        updatedSteps.add(updatedStep);
                                                        afterUpdatedStep = true;
                                                        delayDiff = updatedStep.getDelay() - step.getDelay();
                                                }
                                        }
                                        else if (!afterUpdatedStep && step.getDelay() + step.getDuration() > updatedStep.getDelay()) {
                                                updatedSteps.add(updatedStep);
                                                afterUpdatedStep = true;
                                                delayDiff = Math.max(0, updatedStep.getDelay() + updatedStep.getDuration() - step.getDelay());
                                                updatedSteps.add(step.withDelay(step.getDelay() + delayDiff));
                                        }
                                        else {
                                                updatedSteps.add(step.withDelay(step.getDelay() + delayDiff));
                                        }
                                }
                        }
                        else {
                                updatedSteps.addAll(lightSteps.getSteps());
                        }
                }
                getSteps().clear();
                getSteps().addAll(updatedSteps);
                Collections.sort(getSteps());
        }

        /**
         * Class representing a step of a scene.
         */
        public static class Step implements Comparable<Step> {
                /** The id for storage. */
                private final int mId;
                /** The step delay. */
                private final long mDelay;
                /** The stored color. */
                private final int mStoredColorId;
                /** The step duration. */
                private final long mDuration;

                /**
                 * Generate a step.
                 *
                 * @param id            The id for storage
                 * @param delay         the delay
                 * @param storedColorId The stored color id
                 * @param duration      the duration
                 */
                public Step(final int id, final long delay, final int storedColorId, final long duration) {
                        mId = id;
                        mDelay = delay;
                        mStoredColorId = storedColorId;
                        mDuration = duration;
                }

                /**
                 * Generate a new step without id.
                 *
                 * @param delay         the delay
                 * @param storedColorId The stored color id
                 * @param duration      the duration
                 */
                public Step(final long delay, final int storedColorId, final long duration) {
                        this(-1, delay, storedColorId, duration);
                }

                /**
                 * Retrieve a step from storage via id.
                 *
                 * @param stepId The id
                 */
                protected Step(final int stepId) {
                        mId = stepId;
                        mDelay = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_scene_step_delay, stepId, 0);
                        mStoredColorId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_scene_step_stored_color_id, stepId, 0);
                        mDuration = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_scene_step_duration, stepId, 0);
                }

                /**
                 * Store this step.
                 *
                 * @param sceneId the scene id
                 * @return the stored step
                 */
                public Step store(final int sceneId) {
                        Step step = this;
                        if (getId() < 0) {
                                int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_scene_step_max_id, 0) + 1;
                                PreferenceUtil.setSharedPreferenceInt(R.string.key_scene_step_max_id, newId);
                                step = new Step(newId, getDelay(), getStoredColorId(), getDuration());
                        }

                        List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_scene_step_ids, sceneId);
                        if (!stepIds.contains(step.getId())) {
                                stepIds.add(step.getId());
                                PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_scene_step_ids, sceneId, stepIds);
                        }

                        PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_scene_step_delay, step.getId(), step.getDelay());
                        PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_scene_step_stored_color_id, step.getId(), step.getStoredColorId());
                        PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_scene_step_duration, step.getId(), step.getDuration());
                        return step;
                }

                /** Get the delay. */
                public long getDelay() {
                        return mDelay;
                }

                /** Get the stored color id. */
                public int getStoredColorId() {
                        return mStoredColorId;
                }

                /** Get the stored color. */
                public StoredColor getStoredColor() {
                        return ColorRegistry.getInstance().getStoredColor(getStoredColorId());
                }

                /** Get the duration. */
                public long getDuration() {
                        return mDuration;
                }

                /** Get the id for storage. */
                public int getId() {
                        return mId;
                }

                /** Update the duration. */
                protected Step withDuration(final long duration) {
                        return new Step(getId(), getDelay(), getStoredColorId(), duration);
                }

                /** Update the delay. */
                protected Step withDelay(final long delay) {
                        return new Step(getId(), delay, getStoredColorId(), getDuration());
                }

                @Override
                public int compareTo(final Step other) {
                        if (getDelay() == other.getDelay()) {
                                if (getDuration() == other.getDuration()) {
                                        return getStoredColor().getLight().getLabel().compareTo(other.getStoredColor().getLight().getLabel());
                                }
                                else {
                                        return Long.compare(getDuration(), other.getDuration());
                                }
                        }
                        else {
                                return Long.compare(getDelay(), other.getDelay());
                        }
                }

                @Override
                public boolean equals(final Object obj) {
                        if (!(obj instanceof Step)) {
                                return false;
                        }
                        Step other = (Step) obj;
                        return mDelay == other.mDelay && mDuration == other.mDuration && mId == other.mId && mStoredColorId == other.mStoredColorId;
                }

                @Override
                public int hashCode() {
                        final int prime = 31;
                        int result = 1;
                        result = prime * result + (int) (mDelay ^ (mDelay >>> 32));
                        result = prime * result + (int) (mDuration ^ (mDuration >>> 32));
                        result = prime * result + mId;
                        result = prime * result + mStoredColorId;
                        return result;
                }

                @NonNull
                @Override
                public String toString() {
                        return "[" + getId() + "](" + getDelay() + ")(" + getStoredColor() + ")(" + getDuration() + ")";
                }
        }

        /**
         * Helper class combining light and steps.
         */
        public static class LightSteps {
                /** The light. */
                private final Light mLight;
                /** The steps. */
                private final List<Step> mSteps;

                /**
                 * Constructor.
                 *
                 * @param light The light
                 * @param steps The steps
                 */
                public LightSteps(final Light light, final List<Step> steps) {
                        mLight = light;
                        mSteps = steps;
                }

                /** Get the light. */
                public Light getLight() {
                        return mLight;
                }

                /** Get the steps. */
                public List<Step> getSteps() {
                        return mSteps;
                }
        }
}

