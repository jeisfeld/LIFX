package de.jeisfeld.lifx.app.storedcolors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.MainViewModel;
import de.jeisfeld.lifx.app.home.TileViewModel;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.lan.type.TileColors;
import de.jeisfeld.lifx.lan.type.TileInfo;

/**
 * Class holding information about stored tile colors.
 */
public class StoredTileColors extends StoredColor {
	/**
	 * The colors.
	 */
	private final TileChainColors mColors;

	/**
	 * Generate stored colors.
	 *
	 * @param id       The id for storage
	 * @param colors   The tile chain colors
	 * @param deviceId The device id
	 * @param name     The name
	 */
	public StoredTileColors(final int id, final TileChainColors colors, final int deviceId, final String name) {
		super(id, null, deviceId, name);
		mColors = colors;
	}

	/**
	 * Generate new stored colors without id.
	 *
	 * @param colors   The tile chain colors
	 * @param deviceId The device id
	 * @param name     The name
	 */
	public StoredTileColors(final TileChainColors colors, final int deviceId, final String name) {
		this(-1, colors, deviceId, name);
	}

	/**
	 * Generate new stored colors by adding id.
	 *
	 * @param id           The id
	 * @param storedColors the base stored colors.
	 */
	public StoredTileColors(final int id, final StoredTileColors storedColors) {
		this(id, storedColors.getColors(), storedColors.getDeviceId(), storedColors.getName());
	}

	/**
	 * Retrieve a stored color from storage via id.
	 *
	 * @param colorId The id.
	 */
	protected StoredTileColors(final int colorId) {
		super(colorId);
		StoreType storeType = StoreType.fromOrdinal(
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_color_tilechain_type, colorId, 0));
		TileChainColors tileChainColors;
		if (storeType == StoreType.FIXED) {
			Color color = PreferenceUtil.getIndexedSharedPreferenceColor(R.string.key_color_color, colorId, Color.NONE);
			tileChainColors = new TileChainColors.Fixed(color);
		}
		else {
			List<Color> colors = PreferenceUtil.getIndexedSharedPreferenceColorList(R.string.key_color_colors, colorId);
			switch (storeType) {
			case PERTILE_EXACT:
				List<Integer> tileSizes = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_color_tilechain_sizes, colorId);
				TileColors[] tileColors = new TileColors[tileSizes.size() / 2];
				int colorIndex = 0;
				for (int tileIndex = 0; tileIndex < tileColors.length; tileIndex++) {
					int width = tileSizes.get(2 * tileIndex);
					int height = tileSizes.get(2 * tileIndex + 1);
					if (colors.size() >= colorIndex + width * height) {
						tileColors[tileIndex] = new TileColors.Exact(colors.subList(colorIndex, colorIndex + width * height), width, height);
						colorIndex += width * height;
					}
					else {
						tileColors[tileIndex] = TileColors.OFF;
					}
				}
				TileChain tileChain = (TileChain) DeviceRegistry.getInstance().getDeviceById(getDeviceId()).getDevice();
				tileChainColors = new TileChainColors.PerTile(tileChain, tileColors);
				break;
			default:
				// should not happen
				tileChainColors = TileChainColors.OFF;
			}
		}
		mColors = tileChainColors;
	}

	@Override
	public final StoredTileColors store() {
		StoredTileColors storedColors = this;
		if (getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_color_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_color_max_id, newId);

			List<Integer> colorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
			colorIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, colorIds);
			storedColors = new StoredTileColors(newId, this);
		}

		int colorId = storedColors.getId();
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_device_id, colorId, storedColors.getDeviceId());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_color_name, colorId, storedColors.getName());

		TileChainColors colors = storedColors.getColors();
		if (colors instanceof TileChainColors.Fixed) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_tilechain_type, colorId, StoreType.FIXED.ordinal());
			PreferenceUtil.setIndexedSharedPreferenceColor(R.string.key_color_color, colorId, ((TileChainColors.Fixed) colors).getColor());
		}
		else {
			TileChain tileChain = storedColors.getLight();
			List<TileInfo> tileInfoList = tileChain.getTileInfo();
			List<Integer> tileSizes = new ArrayList<>();
			List<Color> tileChainColors = new ArrayList<>();
			for (TileInfo tileInfo : tileInfoList) {
				tileSizes.add((int) tileInfo.getWidth());
				tileSizes.add((int) tileInfo.getHeight());
				List<Color> tileColors = colors.getTileColors(
						tileInfo.getWidth(), tileInfo.getHeight(), tileInfo.getMinX(), tileInfo.getMinY(), tileInfo.getRotation(),
						tileChain.getTotalWidth(), tileChain.getTotalHeight(), tileInfo.getWidth(), tileInfo.getHeight());
				tileChainColors.addAll(tileColors);
			}
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_color_tilechain_type, colorId, StoreType.PERTILE_EXACT.ordinal());
			PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_color_tilechain_sizes, colorId, tileSizes);
			PreferenceUtil.setIndexedSharedPreferenceColorList(R.string.key_color_colors, colorId, tileChainColors);
		}
		return storedColors;
	}

	/**
	 * Get the colors.
	 *
	 * @return The colors.
	 */
	public TileChainColors getColors() {
		return mColors;
	}

	/**
	 * Get the tile chain for the color.
	 *
	 * @return The tile chain for the color.
	 */
	@Override
	public TileChain getLight() {
		return (TileChain) super.getLight();
	}

	@NonNull
	@Override
	public final String toString() {
		return "[" + getId() + "](" + getName() + ")(" + (getLight() == null ? getDeviceId() : getLight().getLabel() + ")-" + getColors());
	}

	@Override
	public final Drawable getButtonDrawable(final Context context) {
		Drawable baseDrawable = super.getButtonDrawable(context);
		if (!(baseDrawable instanceof GradientDrawable)) {
			return baseDrawable;
		}
		GradientDrawable drawable = (GradientDrawable) baseDrawable;
		TileChainColors colors = getColors();
		if (colors instanceof TileChainColors.Fixed) {
			drawable.setColor(ColorUtil.toAndroidDisplayColor(((TileChainColors.Fixed) colors).getColor()));
		}
		else {
			TileChain tileChain = (TileChain) DeviceRegistry.getInstance().getDeviceById(getDeviceId()).getDevice();
			if (tileChain.getTotalWidth() == 0 || tileChain.getTotalHeight() == 0) {
				drawable.setShape(GradientDrawable.RECTANGLE);
				drawable.setColor(android.graphics.Color.GRAY);
			}
			else {
				return getTileChainDrawable(tileChain, colors);
			}
		}
		return drawable;
	}

	/**
	 * Get the drawable showing tile chain colors.
	 *
	 * @param tileChain       The tile chain.
	 * @param tileChainColors The tile chain colors.
	 * @return The drawable.
	 */
	public static Drawable getTileChainDrawable(final TileChain tileChain, final TileChainColors tileChainColors) {
		Bitmap bitmap = Bitmap.createBitmap(tileChain.getTotalWidth(), tileChain.getTotalHeight(), Config.ARGB_8888);
		for (int y = 0; y < tileChain.getTotalHeight(); y++) {
			for (int x = 0; x < tileChain.getTotalWidth(); x++) {
				bitmap.setPixel(x, tileChain.getTotalHeight() - 1 - y,
						ColorUtil.toAndroidDisplayColor(tileChainColors.getColor(x, y, tileChain.getTotalWidth(), tileChain.getTotalHeight())));
			}
		}
		return new BitmapDrawable(Application.getAppContext().getResources(), bitmap);
	}

	@Override
	protected final void setColor(final int colorDuration, final MainViewModel model) throws IOException {
		getLight().setColors(getColors(), colorDuration, false);
		if (model instanceof TileViewModel) {
			((TileViewModel) model).updateStoredColors(getColors(), 1);
		}
	}

	/**
	 * Flag for determine which kind of TileChainColors is stored.
	 */
	public enum StoreType {
		/**
		 * Fixed.
		 */
		FIXED,
		/**
		 * Exact per tile.
		 */
		PERTILE_EXACT;

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
