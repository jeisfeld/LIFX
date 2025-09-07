package de.jeisfeld.lifx.app.scenes;

import android.content.Context;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * Registry holding information about scenes.
 */
public final class SceneRegistry {
	/**
	 * Singleton instance.
	 */
	private static SceneRegistry mInstance = null;
	/**
	 * The scenes.
	 */
	private final SparseArray<Scene> mScenes = new SparseArray<>();

	/**
	 * Create the registry and retrieve stored entries.
	 */
	private SceneRegistry() {
		List<Integer> sceneIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_scene_ids);
		for (int sceneId : sceneIds) {
			mScenes.put(sceneId, new Scene(sceneId));
		}
	}

	/**
	 * Get the singleton instance.
	 *
	 * @return The instance
	 */
	public static SceneRegistry getInstance() {
		if (mInstance == null) {
			mInstance = new SceneRegistry();
		}
		return mInstance;
	}

	/**
	 * Get list of scenes.
	 *
	 * @return The list of scenes
	 */
	public List<Scene> getScenes() {
		List<Scene> result = new ArrayList<>();
		for (int sceneId : PreferenceUtil.getSharedPreferenceIntList(R.string.key_scene_ids)) {
			Scene scene = mScenes.get(sceneId);
			if (scene != null) {
				result.add(scene);
			}
		}
		return result;
	}

	/**
	 * Get a single scene.
	 *
	 * @param id The id
	 * @return The scene
	 */
	public Scene getScene(final int id) {
		Scene scene = mScenes.get(id);
		if (scene == null) {
			scene = new Scene(id);
			mScenes.put(id, scene);
		}
		return scene;
	}

	/**
	 * Get a new automatic scene name.
	 *
	 * @param context The context
	 * @return The scene name
	 */
	public String getNewSceneName(final Context context) {
		List<String> existingNames = getScenes().stream().map(Scene::getName).collect(Collectors.toList());
		String name = null;
		int count = 1;
		while (name == null) {
			name = context.getResources().getString(R.string.default_scene_name, count);
			if (existingNames.contains(name)) {
				name = null;
				count++;
			}
		}
		return name;
	}

	/**
	 * Add or update a scene in local store.
	 *
	 * @param scene The scene
	 * @return The stored scene
	 */
	Scene addOrUpdate(final Scene scene) {
		Scene newScene = scene.store();
		mScenes.put(newScene.getId(), newScene);
		return newScene;
	}

	/**
	 * Remove a scene.
	 *
	 * @param scene The scene
	 */
	void remove(final Scene scene) {
		int sceneId = scene.getId();
		mScenes.remove(sceneId);
		List<Integer> sceneIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_scene_ids);
		sceneIds.remove((Integer) sceneId);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_scene_ids, sceneIds);

		for (Scene.Step step : scene.getSteps()) {
			remove(step, sceneId);
		}
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_scene_name, sceneId);
	}

	/**
	 * Remove a scene step.
	 *
	 * @param step    The step
	 * @param sceneId The scene id
	 */
	void remove(final Scene.Step step, final int sceneId) {
		int stepId = step.getId();
		List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_scene_step_ids, sceneId);
		stepIds.remove((Integer) stepId);
		PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_scene_step_ids, sceneId, stepIds);

		PreferenceUtil.removeIndexedSharedPreference(R.string.key_scene_step_delay, stepId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_scene_step_stored_color_id, stepId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_scene_step_duration, stepId);
	}
}

