package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.TileChainColors;

/**
 * Animation cycling through a sequence of colors with individual durations.
 */
public class ColorCycle extends AnimationData {
	/**
	 * The default serial version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Durations for each color step in milliseconds.
	 */
	private final ArrayList<Integer> mDurations;
	/**
	 * Colors to cycle through.
	 */
	private final ArrayList<Color> mColors;

	/**
	 * Constructor.
	 *
	 * @param durations list of step durations
	 * @param colors    list of colors to cycle
	 */
	public ColorCycle(final List<Integer> durations, final ArrayList<Color> colors) {
		mDurations = new ArrayList<>(durations);
		mColors = colors;
	}

	@Override
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		int[] durations = new int[mDurations.size()];
		for (int i = 0; i < mDurations.size(); i++) {
			durations[i] = mDurations.get(i);
		}
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATIONS, durations);
		serviceIntent.putExtra(EXTRA_COLOR_LIST, mColors);
	}

	@Override
	public final void store(final int colorId) {
		super.store(colorId);
		PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_animation_durations_list, colorId, mDurations);
		PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_animation_color_list, colorId, mColors);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.COLOR_CYCLE;
	}

	@Override
       protected final AnimationDefinition getAnimationDefinition(final Light light) {
               final double brightness = getSelectedBrightness(light);
               if (light instanceof MultiZoneLight) {
                       return new MultiZoneLight.AnimationDefinition() {
                               @Override
                               public int getDuration(final int n) {
                                       return mDurations.get(n % mDurations.size());
                               }

                               @Override
                               public MultizoneColors getColors(final int n) {
                                       return new MultizoneColors.Fixed(
                                                       mColors.get(n % mColors.size()).withRelativeBrightness(brightness));
                               }
                       };
               }
               if (light instanceof TileChain) {
                       return new TileChain.AnimationDefinition() {
                               @Override
                               public int getDuration(final int n) {
                                       return mDurations.get(n % mDurations.size());
                               }

                               @Override
                               public TileChainColors getColors(final int n) {
                                       return new TileChainColors.Fixed(
                                                       mColors.get(n % mColors.size()).withRelativeBrightness(brightness));
                               }
                       };
               }
               return new AnimationDefinition() {
                       @Override
                       public int getDuration(final int n) {
                               return mDurations.get(n % mDurations.size());
                       }

                       @Override
                       public Color getColor(final int n) {
                               return mColors.get(n % mColors.size()).withRelativeBrightness(brightness);
                       }
               };
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
		if (mColors == null || mDurations == null) {
			return false;
		}
		if (mColors.isEmpty() || mDurations.size() != mColors.size()) {
			return false;
		}
		for (int d : mDurations) {
			if (d <= 0) {
				return false;
			}
		}
		return true;
	}
}

