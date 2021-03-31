package de.jeisfeld.lifx.app.storedcolors;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.MainViewModel;
import de.jeisfeld.lifx.app.home.MultizoneViewModel;
import de.jeisfeld.lifx.app.home.MultizoneViewModel.FlaggedMultizoneColors;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter.MultizoneOrientation;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.MultizoneColors.Exact;
import de.jeisfeld.lifx.lan.type.MultizoneColors.Fixed;
import de.jeisfeld.lifx.lan.type.MultizoneColors.Interpolated;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding information about stored multizone colors.
 */
public class StoredMultizoneColors extends StoredColor {
	/**
	 * The color.
	 */
	private final MultizoneColors mColors;

	/**
	 * Generate stored colors.
	 *
	 * @param id       The id for storage
	 * @param colors   The multizone colors
	 * @param deviceId The device id
	 * @param name     The name
	 */
	public StoredMultizoneColors(final int id, final MultizoneColors colors, final int deviceId, final String name) {
		super(id, null, deviceId, name);
		mColors = colors;
	}

	/**
	 * Generate new stored colors without id.
	 *
	 * @param colors   The multizone colors
	 * @param deviceId The device id
	 * @param name     The name
	 */
	public StoredMultizoneColors(final MultizoneColors colors, final int deviceId, final String name) {
		this(-1, colors, deviceId, name);
	}

	/**
	 * Generate new stored colors by adding id.
	 *
	 * @param id           The id
	 * @param storedColors the base stored colors.
	 */
	public StoredMultizoneColors(final int id, final StoredMultizoneColors storedColors) {
		this(id, storedColors.getColors(), storedColors.getDeviceId(), storedColors.getName());
	}

	/**
	 * Retrieve a stored color from storage via id.
	 *
	 * @param colorId The id.
	 */
	public StoredMultizoneColors(final int colorId) {
		super(colorId);
		StoreType storeType = StoreType.fromOrdinal(
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_color_multizone_type, colorId, 0));
		MultizoneColors multzoneColors;

		if (storeType == StoreType.FIXED) {
			Color color = PreferenceUtil.getIndexedSharedPreferenceColor(R.string.key_color_color, colorId, Color.NONE);
			multzoneColors = new MultizoneColors.Fixed(color);
		}
		else {
			List<Color> colors = PreferenceUtil.getIndexedSharedPreferenceColorList(R.string.key_color_colors, colorId);
			switch (storeType) {
			case EXACT:
				multzoneColors = new MultizoneColors.Exact(colors);
				break;
			case INTERPOLATED:
				multzoneColors = new MultizoneColors.Interpolated(false, colors);
				break;
			case INTERPOLATED_CYCLIC:
				multzoneColors = new MultizoneColors.Interpolated(true, colors);
				break;
			default:
				// should not happen
				multzoneColors = MultizoneColors.OFF;
			}
		}

		long flagsLong = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_color_multizone_flags, colorId, -1);
		if (flagsLong == -1) {
			mColors = multzoneColors;
		}
		else {
			mColors = new FlaggedMultizoneColors(multzoneColors, TypeUtil.toBooleanArray(flagsLong));
		}
	}

	@Override
	public final StoredMultizoneColors store() {
		StoredMultizoneColors storedColors = this;
		if (getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_color_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_color_max_id, newId);

			List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
			colorIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, colorIds);
			storedColors = new StoredMultizoneColors(newId, this);
		}

		int colorId = storedColors.getId();
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_device_id, colorId, storedColors.getDeviceId());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_color_name, colorId, storedColors.getName());

		MultizoneColors colors = storedColors.getColors();
		storeMultizoneColors(colorId, colors);
		return storedColors;
	}

	/**
	 * Store multizone color details on a certain color id.
	 *
	 * @param colorId The id.
	 * @param colors  The colors to be stored.
	 */
	public static void storeMultizoneColors(final int colorId, final MultizoneColors colors) {
		MultizoneColors baseColors = colors;
		if (colors instanceof MultizoneViewModel.FlaggedMultizoneColors) {
			long flags = TypeUtil.toLong(((FlaggedMultizoneColors) colors).getFlags());
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_color_multizone_flags, colorId, flags);
			baseColors = ((FlaggedMultizoneColors) colors).getBaseColors();
		}
		if (baseColors instanceof MultizoneColors.Fixed) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_multizone_type, colorId, StoreType.FIXED.ordinal());
			PreferenceUtil.setIndexedSharedPreferenceColor(R.string.key_color_color, colorId, ((Fixed) baseColors).getColor());
		}
		else if (baseColors instanceof MultizoneColors.Exact) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_multizone_type, colorId, StoreType.EXACT.ordinal());
			PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_color_colors, colorId, ((Exact) baseColors).getColors());
		}
		else if (baseColors instanceof MultizoneColors.Interpolated) {
			StoreType storeType = ((Interpolated) baseColors).isCyclic() ? StoreType.INTERPOLATED_CYCLIC : StoreType.INTERPOLATED;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_multizone_type, colorId, storeType.ordinal());
			PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_color_colors, colorId, ((Interpolated) baseColors).getColors());
		}
	}

	/**
	 * Get the colors.
	 *
	 * @return The colors.
	 */
	public MultizoneColors getColors() {
		return mColors;
	}

	/**
	 * Get the multizone light for the color.
	 *
	 * @return The multizone light for the color.
	 */
	@Override
	public MultiZoneLight getLight() {
		return (MultiZoneLight) super.getLight();
	}

	@NonNull
	@Override
	public final String toString() {
		return "[" + getId() + "](" + getName() + ")(" + (getLight() == null ? getDeviceId() : getLight().getLabel() + ")-" + getColors());
	}

	@Override
	public final Drawable getButtonDrawable(final Context context) {
		return getButtonDrawable(context, getColors(), getDeviceId());
	}

	/**
	 * Get a button drawable for Multizone colors.
	 *
	 * @param context  The context.
	 * @param colors   The multizone colors.
	 * @param deviceId The device id.
	 * @return The button drawable.
	 */
	public static Drawable getButtonDrawable(final Context context, final MultizoneColors colors, final int deviceId) {
		GradientDrawable drawable = new GradientDrawable();
		MultizoneColors baseColors = colors;
		if (baseColors instanceof FlaggedMultizoneColors) {
			baseColors = ((FlaggedMultizoneColors) baseColors).getBaseColors();
		}
		if (baseColors instanceof MultizoneColors.Fixed) {
			drawable.setShape(GradientDrawable.OVAL);
			drawable.setColor(ColorUtil.toAndroidDisplayColor(((MultizoneColors.Fixed) baseColors).getColor()));
		}
		else {
			drawable.setShape(GradientDrawable.RECTANGLE);
			if (baseColors instanceof MultizoneColors.Interpolated) {
				drawable = ColorUtil.getButtonDrawable(context, ((MultizoneColors.Interpolated) baseColors).getColors());
			}
			else if (baseColors instanceof MultizoneColors.Exact) {
				drawable.setColors(ColorUtil.toAndroidDisplayColors(((MultizoneColors.Exact) baseColors).getColors()));
			}
			MultizoneOrientation multizoneOrientation = MultizoneOrientation.fromOrdinal(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_multizone_orientation, deviceId, 0));
			drawable.setOrientation(multizoneOrientation.getGradientOrientation());
		}
		return drawable;
	}

	@Override
	protected final void setColor(final int colorDuration, final MainViewModel model) throws IOException {
		getLight().setColors(getColors(), colorDuration, false);
		if (model instanceof MultizoneViewModel) {
			((MultizoneViewModel) model).updateStoredColors(getColors(), 1);
		}
	}

	/**
	 * Flag for determine which kind of MultizoneColors is stored.
	 */
	public enum StoreType {
		/**
		 * Fixed.
		 */
		FIXED,
		/**
		 * Exact.
		 */
		EXACT,
		/**
		 * Non-cyclic Interpolated.
		 */
		INTERPOLATED,
		/**
		 * Cyclic Interpolated.
		 */
		INTERPOLATED_CYCLIC;

		/**
		 * Get a storeType from its ordinal value.
		 *
		 * @param ordinal The ordinal value
		 * @return The storeType.
		 */
		public static StoreType fromOrdinal(final int ordinal) {
			for (StoreType storeType : values()) {
				if (storeType.ordinal() == ordinal) {
					return storeType;
				}
			}
			return FIXED;
		}
	}

}
