package de.jeisfeld.lifx.lan.type;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Possible products.
 */
public enum Product {
	// JAVADOC:OFF
	ORIGINAL_1000(1, "Original 1000", true, true, false, false, false, 2500, 9000, false, 0),
	COLOR_650(3, "Color 650", true, true, false, false, false, 2500, 9000, false, 0),
	WHITE_800_LOW(10, "White 800 (Low Voltage)", true, false, false, false, false, 2700, 6500, false, 0),
	WHITE_800_HIGH(11, "White 800 (High Voltage)", true, false, false, false, false, 2700, 6500, false, 0),
	WHITE_900_BR30(18, "White 900 BR30 (Low Voltage)", true, false, false, false, false, 2700, 6500, false, 0),
	COLOR_1000_BR30(20, "Color 1000 BR30", true, true, false, false, false, 2500, 9000, false, 0),
	COLOR_1000(22, "Color 1000", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_A19(27, "LIFX A19", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_BR30(28, "LIFX BR30", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_PLUS__A19(29, "LIFX+ A19", true, true, true, false, false, 2500, 9000, false, 0),
	LIFX_PLUS_BR30(30, "LIFX+ BR30", true, true, true, false, false, 2500, 9000, false, 0),
	LIFX_Z(31, "LIFX Z", true, true, false, false, true, 2500, 9000, false, 0),
	LIFX_Z_2(32, "LIFX Z 2", true, true, false, false, true, 2500, 9000, false, 1532997580),
	LIFX_DOWNLIGHT(36, "LIFX Downlight", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_DOWNLIGHT_2(37, "LIFX Downlight", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_BEAM(38, "LIFX Beam", true, true, false, false, true, 2500, 9000, false, 1532997580),
	LIFX_A19_2(43, "LIFX A19", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_BR30_2(44, "LIFX BR30", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_PLUS__A19_2(45, "LIFX+ A19", true, true, true, false, false, 2500, 9000, false, 0),
	LIFX_PLUS_BR30_2(46, "LIFX+ BR30", true, true, true, false, false, 2500, 9000, false, 0),
	LIFX_MINI(49, "LIFX Mini", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_MINI_WARM_TO_WHITE(50, "LIFX Mini Warm to White", true, false, false, false, false, 1500, 4000, false, 0),
	LIFX_MINI_WHITE(51, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, 0),
	LIFX_GU10(52, "LIFX GU10", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_TILE(55, "LIFX Tile", true, true, false, true, false, 2500, 9000, true, 0),
	LIFX_CANDLE(57, "LIFX Candle", true, true, false, true, false, 2500, 9000, false, 0),
	LIFX_MINI_COLOR(59, "LIFX Mini Color", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_MINI_WARM_TO_WHITE_2(60, "LIFX Mini Warm to White", true, false, false, false, false, 1500, 4000, false, 0),
	LIFX_MINI_WHITE_2(61, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, 0),
	LIFX_A19_3(62, "LIFX A19", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_BR30_3(63, "LIFX BR30", true, true, false, false, false, 2500, 9000, false, 0),
	LIFX_PLUS__A19_3(64, "LIFX+ A19", true, true, true, false, false, 2500, 9000, false, 0),
	LIFX_PLUS_BR30_3(65, "LIFX+ BR30", true, true, true, false, false, 2500, 9000, false, 0),
	LIFX_CANDLE_2(68, "LIFX Candle", true, true, false, true, false, 2500, 9000, false, 0),
	LIFX_CANDLE_WARM_TO_WHITE(81, "LIFX Candle Warm to White", true, false, false, true, false, 2500, 6500, false, 0),
	LIFX_FILAMENT(82, "LIFX Filament", true, false, false, false, false, 2000, 2000, false, 0),

	// JAVADOC:ON
	/**
	 * Unknown product.
	 */
	UNKNOWN(0, "Unknown Product", false, false, false, false, false, 0, 0, false, 0);

	/**
	 * The product id.
	 */
	private final int mPid;
	/**
	 * The product name.
	 */
	private final String mName;
	/**
	 * Flag if this is a light.
	 */
	private final boolean mIsLight;
	/**
	 * Flag if this is a colored light.
	 */
	private final boolean mHasColor;
	/**
	 * Flag if this light has infrared.
	 */
	private final boolean mHasInfrared;
	/**
	 * Flag if this is a matrix light.
	 */
	private final boolean mIsMatrix;
	/**
	 * Flag if this is a multizone light.
	 */
	private final boolean mIsMultizone;
	/**
	 * The min color temperature.
	 */
	private final int mMinTemperature;
	/**
	 * The max color temperature.
	 */
	private final int mMaxTemperature;
	/**
	 * Flag if this is a chain light.
	 */
	private final boolean mIsChain;
	/**
	 * Build time for which extended API can be used.
	 */
	private final long mExtApiBuildTime;

	/**
	 * Map from id to product.
	 */
	private static final Map<Integer, Product> PRODUCT_MAP = new LinkedHashMap<>();

	static {
		for (Product product : Product.values()) {
			Product.PRODUCT_MAP.put(product.getId(), product);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param pid The product id.
	 * @param name The product name.
	 * @param isLight Flag if this is a light.
	 * @param hasColor Flag if this is a colored light.
	 * @param hasInfrared Flag if this light has infrared.
	 * @param isMatrix Flag if this is a matrix light.
	 * @param isMultizone Flag if this is a multizone light.
	 * @param minTemperature The min color temperature.
	 * @param maxTemperature The max color temperature.
	 * @param isChain Flag if this is a chain light.
	 * @param extApiBuildTime Build time for which extended API can be used.
	 */
	Product(final int pid, final String name, final boolean isLight, final boolean hasColor, final boolean hasInfrared, // SUPPRESS_CHECKSTYLE
			final boolean isMatrix, final boolean isMultizone, final int minTemperature, final int maxTemperature, final boolean isChain,
			final long extApiBuildTime) {
		mPid = pid;
		mName = name;
		mIsLight = isLight;
		mHasColor = hasColor;
		mHasInfrared = hasInfrared;
		mIsMatrix = isMatrix;
		mIsMultizone = isMultizone;
		mMinTemperature = minTemperature;
		mMaxTemperature = maxTemperature;
		mIsChain = isChain;
		mExtApiBuildTime = extApiBuildTime;
	}

	/**
	 * Get a product from its id.
	 *
	 * @param id The id
	 * @return The product.
	 */
	public static Product fromId(final int id) {
		Product product = Product.PRODUCT_MAP.get(id);
		return product == null ? UNKNOWN : product;
	}

	/**
	 * Get the product id.
	 *
	 * @return the product id
	 */
	public final int getId() {
		return mPid;
	}

	/**
	 * Get the product name.
	 *
	 * @return the product name
	 */
	public final String getName() {
		return mName;
	}

	/**
	 * Get flag if this is a light.
	 *
	 * @return flag if this is a light.
	 */
	public final boolean isLight() {
		return mIsLight;
	}

	/**
	 * Get flag if this is a colored light.
	 *
	 * @return flag if this is a colored light.
	 */
	public final boolean hasColor() {
		return mHasColor;
	}

	/**
	 * Get flag if this has infrared.
	 *
	 * @return flag if this has infrared.
	 */
	public final boolean hasInfrared() {
		return mHasInfrared;
	}

	/**
	 * Get flag if this is a matrix light.
	 *
	 * @return flag if this is a matrix light.
	 */
	public final boolean isMatrix() {
		return mIsMatrix;
	}

	/**
	 * Get flag if this is a multizone light.
	 *
	 * @return flag if this is a multizone light.
	 */
	public final boolean isMultizone() {
		return mIsMultizone;
	}

	/**
	 * Get the min color temperature.
	 *
	 * @return the min color temperature
	 */
	public final int getMinTemperature() {
		return mMinTemperature;
	}

	/**
	 * Get the max color temperature.
	 *
	 * @return the max color temperature
	 */
	public final int getMaxTemperature() {
		return mMaxTemperature;
	}

	/**
	 * Get flag if this is a chain light.
	 *
	 * @return flag if this is a chain light.
	 */
	public final boolean isChain() {
		return mIsChain;
	}

	/**
	 * Check if the device has extended API.
	 *
	 * @param buildDate The firmware build date.
	 * @return true if there is extended API available.
	 */
	public final boolean hasExtendedApi(final Date buildDate) {
		return buildDate.getTime() / 1000 >= mExtApiBuildTime; // MAGIC_NUMBER
	}

	@Override
	public String toString() {
		return getName() + " (" + getId() + ")";
	}

}
