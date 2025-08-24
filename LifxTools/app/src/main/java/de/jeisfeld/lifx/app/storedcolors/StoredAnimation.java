package de.jeisfeld.lifx.app.storedcolors;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.animation.AnimationData;
import de.jeisfeld.lifx.app.animation.LifxAnimationService;
import de.jeisfeld.lifx.app.home.LightViewModel;
import de.jeisfeld.lifx.app.home.MainViewModel;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * Class holding information about a stored animation.
 */
public class StoredAnimation extends StoredColor {
	/**
	 * The animation data.
	 */
	private final AnimationData mAnimationData;
	/**
	 * The relative brightness.
	 */
	private final double mRelativeBrightness;

	/**
	 * Generate stored colors.
	 *
	 * @param id                 The id for storage
	 * @param animationData      The animation data
	 * @param relativeBrightness The relative brightness
	 * @param deviceId           The device id
	 * @param name               The name
	 */
	public StoredAnimation(final int id, final AnimationData animationData, final double relativeBrightness, final int deviceId, final String name) {
		super(id, null, deviceId, name);
		mAnimationData = animationData;
		mRelativeBrightness = relativeBrightness;
	}

	/**
	 * Generate new stored colors without id.
	 *
	 * @param animationData      The animation data
	 * @param relativeBrightness The relative brightness
	 * @param deviceId           The device id
	 * @param name               The name
	 */
	public StoredAnimation(final AnimationData animationData, final double relativeBrightness, final int deviceId, final String name) {
		this(-1, animationData, relativeBrightness, deviceId, name);
	}

	/**
	 * Generate new stored colors by adding id.
	 *
	 * @param id              The id
	 * @param storedAnimation the base stored animation.
	 */
	public StoredAnimation(final int id, final StoredAnimation storedAnimation) {
		this(id, storedAnimation.getAnimationData(), storedAnimation.getRelativeBrightness(),
				storedAnimation.getDeviceId(), storedAnimation.getName());
	}

	/**
	 * Retrieve a stored color from storage via id.
	 *
	 * @param colorId The id.
	 */
	protected StoredAnimation(final int colorId) {
		super(colorId);
		mAnimationData = AnimationData.fromStoredAnimation(colorId);
		mRelativeBrightness = PreferenceUtil.getIndexedSharedPreferenceDouble(R.string.key_animation_relative_brightness, colorId, 1);
	}

	@Override
	public final StoredAnimation store() {
		StoredAnimation storedAnimation = this;
		if (getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_color_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_color_max_id, newId);

			List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
			colorIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, colorIds);
			storedAnimation = new StoredAnimation(newId, this);
		}

		int colorId = storedAnimation.getId();
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_device_id, colorId, storedAnimation.getDeviceId());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_color_name, colorId, storedAnimation.getName());
		PreferenceUtil.setIndexedSharedPreferenceDouble(R.string.key_animation_relative_brightness, colorId, mRelativeBrightness);

		mAnimationData.store(colorId);
		return storedAnimation;
	}

	/**
	 * Get the animation data.
	 *
	 * @return The animation data.
	 */
	public AnimationData getAnimationData() {
		return mAnimationData;
	}

	/**
	 * Get the relative brightness.
	 *
	 * @return The relative brightness.
	 */
	private double getRelativeBrightness() {
		return mRelativeBrightness;
	}

	@NonNull
	@Override
	public final String toString() {
		return "[" + getId() + "](" + getName() + ")(" + (getLight() == null ? getDeviceId() : getLight().getLabel() + ")-" + getAnimationData());
	}

	@Override
	public final Drawable getButtonDrawable(final Context context) {
		Drawable animationDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_toggle_animation_play, context.getTheme());
		return new LayerDrawable(
				new Drawable[]{getAnimationData().getBaseButtonDrawable(context, getLight(), getRelativeBrightness()), animationDrawable});
	}

        @Override
        protected final void setColor(final int colorDuration, final MainViewModel model) {
                if (model instanceof LightViewModel) {
                        // Ensure that the desired brightness is stored and applied before
                        // the animation starts.  Updating the selected brightness first
                        // allows the model's startAnimation call to pick up the new value
                        // and transmit the full color information if necessary.
                        ((LightViewModel) model).updateSelectedBrightness(mRelativeBrightness);
                        ((LightViewModel) model).startAnimation(mAnimationData);
                }
                else {
                        PreferenceUtil.setIndexedSharedPreferenceDouble(R.string.key_device_selected_brightness, getDeviceId(), getRelativeBrightness());
                        LifxAnimationService.triggerAnimationService(Application.getAppContext(), getLight(), mAnimationData);
                }
        }

}
