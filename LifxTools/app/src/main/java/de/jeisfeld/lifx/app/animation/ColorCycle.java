package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.animation.CycleAnimationDefinition;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Animation cycling through a list of colors.
 */
public class ColorCycle extends AnimationData {
    /**
     * The default serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Duration for each color step.
     */
    private final int mStepDuration;
    /**
     * Colors to cycle through.
     */
    private final ArrayList<Color> mColors;

    /**
     * Constructor.
     *
     * @param stepDuration duration for each color step
     * @param colors       list of colors to cycle
     */
    public ColorCycle(final int stepDuration, final ArrayList<Color> colors) {
        mStepDuration = stepDuration;
        mColors = colors;
    }

    @Override
    public final void addToIntent(final Intent serviceIntent) {
        super.addToIntent(serviceIntent);
        serviceIntent.putExtra(EXTRA_ANIMATION_DURATION, mStepDuration);
        serviceIntent.putExtra(EXTRA_COLOR_LIST, mColors);
    }

    @Override
    public final void store(final int colorId) {
        super.store(colorId);
        PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_animation_duration, colorId, mStepDuration);
        PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId, mColors);
    }

    @Override
    protected final AnimationType getType() {
        return AnimationType.COLOR_CYCLE;
    }

    @Override
    protected final AnimationDefinition getAnimationDefinition(final Light light) {
        Color[] colors = new Color[mColors.size()];
        double brightness = getSelectedBrightness(light);
        for (int i = 0; i < mColors.size(); i++) {
            colors[i] = mColors.get(i).withRelativeBrightness(brightness);
        }
        return new CycleAnimationDefinition(mStepDuration, 0, colors);
    }

    @Override
    public final Drawable getBaseButtonDrawable(final Context context, final Light light, final double relativeBrightness) {
        ArrayList<Color> colors = new ArrayList<>();
        for (Color c : mColors) {
            colors.add(c.withRelativeBrightness(relativeBrightness));
        }
        return ColorUtil.getButtonDrawable(context, colors);
    }

    @Override
    public final boolean isValid() {
        return mColors != null && !mColors.isEmpty() && mStepDuration > 0;
    }
}
