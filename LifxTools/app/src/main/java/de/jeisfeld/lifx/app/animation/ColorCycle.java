package de.jeisfeld.lifx.app.animation;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.storedcolors.StoredMultizoneColors;
import de.jeisfeld.lifx.app.storedcolors.StoredTileColors;
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
	 * Stored colors to cycle through.
	 */
	private final ArrayList<StoredColor> mStoredColors;

	/**
	 * Constructor.
	 *
	 * @param durations    list of step durations
	 * @param storedColors list of stored colors to cycle
	 */
	public ColorCycle(final List<Integer> durations, final ArrayList<StoredColor> storedColors) {
		mDurations = new ArrayList<>(durations);
		mStoredColors = storedColors;
	}

	@Override
	public final void addToIntent(final Intent serviceIntent) {
		super.addToIntent(serviceIntent);
		int[] durations = new int[mDurations.size()];
		for (int i = 0; i < mDurations.size(); i++) {
			durations[i] = mDurations.get(i);
		}
		serviceIntent.putExtra(EXTRA_ANIMATION_DURATIONS, durations);
		int[] colorIds = new int[mStoredColors.size()];
		for (int i = 0; i < mStoredColors.size(); i++) {
			colorIds[i] = mStoredColors.get(i).getId();
		}
		serviceIntent.putExtra(EXTRA_STORED_COLOR_IDS, colorIds);
	}

	@Override
	public final void store(final int colorId) {
		super.store(colorId);
		PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_animation_durations_list, colorId, mDurations);
		ArrayList<Long> colorEntries = new ArrayList<>();
		for (StoredColor sc : mStoredColors) {
			colorEntries.add((long) sc.getId());
		}
		PreferenceUtil.setIndexedSharedPreferenceLongList(R.string.key_animation_color_list, colorId, colorEntries);
	}

	@Override
	protected final AnimationType getType() {
		return AnimationType.COLOR_CYCLE;
	}

	@Override
	protected final AnimationDefinition getAnimationDefinition(final Light light) {
                if (light instanceof MultiZoneLight) {
                        return new MultiZoneLight.AnimationDefinition() {
                                @Override
                                public int getDuration(final int n) {
                                        return mDurations.get(n % mDurations.size());
                                }

                                @Override
                                public MultizoneColors getColors(final int n) {
                                        StoredColor sc = mStoredColors.get(n % mStoredColors.size());
                                        if (sc instanceof StoredMultizoneColors) {
                                                return ((StoredMultizoneColors) sc).getColors();
                                        }
                                        Color color = sc.getColor();
                                        return new MultizoneColors.Fixed(color == null ? Color.OFF : color);
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
                                        StoredColor sc = mStoredColors.get(n % mStoredColors.size());
                                        if (sc instanceof StoredTileColors) {
                                                return ((StoredTileColors) sc).getColors();
                                        }
                                        Color color = sc.getColor();
                                        return new TileChainColors.Fixed(color == null ? Color.OFF : color);
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
                                StoredColor sc = mStoredColors.get(n % mStoredColors.size());
                                Color color = sc.getColor();
                                return color == null ? Color.OFF : color;
                        }
                };
	}

	@Override
	public final Drawable getBaseButtonDrawable(final Context context, final Light light, final double relativeBrightness) {
		ArrayList<Color> colors = new ArrayList<>();
		for (StoredColor sc : mStoredColors) {
			Color c = sc.getColor();
			if (c != null) {
				colors.add(c.withRelativeBrightness(relativeBrightness));
			}
		}
		return ColorUtil.getButtonDrawable(context, colors);
	}

	@Override
	public final boolean isValid() {
		if (mStoredColors == null || mDurations == null) {
			return false;
		}
		if (mStoredColors.isEmpty() || mDurations.size() != mStoredColors.size()) {
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

