package de.jeisfeld.lifx.lan.type;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Possible products.
 */
public enum Product {
	// JAVADOC:OFF
	LIFX_ORIGINAL_1000(1, "LIFX Original 1000", true, true, false, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_COLOR_650(3, "LIFX Color 650", true, true, false, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_WHITE_800_LOW(10, "LIFX White 800 (Low Voltage)", true, false, false, false, false, 2700, 6500, false, false, 0, false, false),
	LIFX_WHITE_800_HIGH(11, "LIFX White 800 (High Voltage)", true, false, false, false, false, 2700, 6500, false, false, 0, false, false),
	LIFX_COLOR_1000(15, "LIFX Color 1000", true, true, false, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_WHITE_900_BR30_L(18, "LIFX White 900 BR30 (Low Voltage)", true, false, false, false, false, 2700, 6500, false, false, 0, false, false),
	LIFX_WHITE_900_BR30_H(19, "LIFX White 900 BR30 (High Voltage)", true, false, false, false, false, 2700, 6500, false, false, 0, false, false),
	LIFX_COLOR_1000_BR30(20, "LIFX Color 1000 BR30", true, true, false, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_COLOR_1000_2(22, "LIFX Color 1000", true, true, false, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_A19(27, "LIFX A19", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30(28, "LIFX BR30", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A19_NIGHT_VISION(29, "LIFX A19 Night Vision", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_NIGHT_VISION(30, "LIFX BR30 Night Vision", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_Z(31, "LIFX Z", true, true, false, false, true, 2500, 9000, false, false, 0, false, false),
	LIFX_Z_2(32, "LIFX Z 2", true, true, false, false, true, 1500, 9000, false, false, 1532997580, false, false),
	LIFX_DOWNLIGHT(36, "LIFX Downlight", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_2(37, "LIFX Downlight", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BEAM(38, "LIFX Beam", true, true, false, false, true, 1500, 9000, false, false, 1532997580, false, false),
	LIFX_DOWNLIGHT_WHITE_TO_WARM(39, "LIFX Downlight White to Warm", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_3(40, "LIFX Downlight", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A19_2(43, "LIFX A19", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_2(44, "LIFX BR30", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_PLUS__A19_2(45, "LIFX+ A19", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_PLUS_BR30_2(46, "LIFX+ BR30", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI(49, "LIFX Mini", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI_WHITE_TO_WARM(50, "LIFX Mini White to Warm", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI_WHITE(51, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_GU10(52, "LIFX GU10", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_GU10_2(53, "LIFX GU10", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_TILE(55, "LIFX Tile", true, true, false, true, false, 2500, 9000, true, false, 0, false, false),
	LIFX_CANDLE(57, "LIFX Candle", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI_COLOR(59, "LIFX Mini Color", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI_WHITE_TO_WARM_2(60, "LIFX Mini White to Warm", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI_WHITE_2(61, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_A19_3(62, "LIFX A19", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_3(63, "LIFX BR30", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_PLUS__A19_3(64, "LIFX+ A19", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_PLUS_BR30_3(65, "LIFX+ BR30", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_MINI_WHITE_3(66, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_CANDLE_2(68, "LIFX Candle", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_SWITCH(70, "LIFX Switch", false, false, false, false, false, 0, 0, false, false, 0, true, false),
	LIFX_SWITCH_2(71, "LIFX Switch", false, false, false, false, false, 0, 0, false, false, 0, true, false),
	LIFX_CANDLE_WHITE_TO_WARM(81, "LIFX Candle White to Warm", true, false, false, true, false, 2200, 6500, false, false, 0, false, false),
	LIFX_FILAMENT_CLEAR(82, "LIFX Filament Clear", true, false, false, false, false, 2100, 2100, false, false, 0, false, false),
	LIFX_FILAMENT_AMBER(85, "LIFX Filament Amber", true, false, false, false, false, 2000, 2000, false, false, 0, false, false),
	LIFX_MINI_WHITE_4(87, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_MINI_WHITE_5(88, "LIFX Mini White", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_SWITCH_3(89, "LIFX Switch", false, false, false, false, false, 0, 0, false, false, 0, true, false),
	LIFX_CLEAN(90, "LIFX Clean", true, true, false, false, false, 1500, 9000, false, true, 0, false, false),
	LIFX_COLOR(91, "LIFX Color", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_COLOR_2(92, "LIFX Color", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A19_US(93, "LIFX A19 US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_4(94, "LIFX BR30", true, true, false, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_WHITE_TO_WARM_2(96, "LIFX Candle White to Warm", true, false, false, true, false, 2200, 6500, false, false, 0, false, false),
	LIFX_A19_4(97, "LIFX A19", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_5(98, "LIFX BR30", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CLEAN_2(99, "LIFX Clean", true, true, false, false, false, 1500, 9000, false, true, 0, false, false),
	LIFX_FILAMENT_CLEAR_2(100, "LIFX Filament Clear", true, false, false, false, false, 2100, 2100, false, false, 0, false, false),
	LIFX_FILAMENT_AMBER_2(101, "LIFX Filament Amber", true, false, false, false, false, 2000, 2000, false, false, 0, false, false),
	LIFX_A19_NIGHT_VISION_2(109, "LIFX A19 Night Vision", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_NIGHT_VISION_2(110, "LIFX BR30 Night Vision", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A19_NIGHT_VISION_3(111, "LIFX A19 Night Vision", true, true, true, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_NIGHT_VISION_INTL(112, "LIFX BR30 Night Vision Intl", true, true, true, false, false, 2500, 9000, false, false, 0, false, false),
	LIFX_MINI_WW_US(113, "LIFX Mini WW US", true, false, false, false, false, 2200, 6500, false, false, 0, false, false),
	LIFX_MINI_WW_INTL(114, "LIFX Mini WW Intl", true, false, false, false, false, 2200, 6500, false, false, 0, false, false),
	LIFX_SWITCH_4(115, "LIFX Switch", false, false, false, false, false, 0, 0, false, false, 0, true, false),
	LIFX_SWITCH_5(116, "LIFX Switch", false, false, false, false, false, 0, 0, false, false, 0, true, false),
	LIFX_Z_US(117, "LIFX Z US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_Z_INTL(118, "LIFX Z Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_BEAM_US(119, "LIFX Beam US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_BEAM_INTL(120, "LIFX Beam Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_DOWNLIGHT_INTL(121, "LIFX Downlight Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_US(122, "LIFX Downlight US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_COLOR_US(123, "LIFX Color US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_COLOR_INTL(124, "LIFX Color Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_WHITE_TO_WARM_US(125, "LIFX White to Warm US", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_WHITE_TO_WARM_INTL(126, "LIFX White to Warm Intl", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_WHITE_US(127, "LIFX White US", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_WHITE_INTL(128, "LIFX White Intl", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_COLOR_US_2(129, "LIFX Color US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_COLOR_INTL_2(130, "LIFX Color Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_WHITE_TO_WARM_US_2(131, "LIFX White to Warm US", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_WHITE_TO_WARM_INTL_2(132, "LIFX White to Warm Intl", true, false, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_WHITE_US_2(133, "LIFX White US", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_WHITE_INTL_2(134, "LIFX White Intl", true, false, false, false, false, 2700, 2700, false, false, 0, false, false),
	LIFX_GU10_COLOR_US(135, "LIFX GU10 Color US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_GU10_COLOR_INTL(136, "LIFX GU10 Color Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOR_US(137, "LIFX Candle Color US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOR_INTL(138, "LIFX Candle Color Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_NEON_US(141, "LIFX Neon US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_NEON_INTL(142, "LIFX Neon Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_STRING_US(143, "LIFX String US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_STRING_INTL(144, "LIFX String Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_OUTDOOR_NEON_US(161, "LIFX Outdoor Neon US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_OUTDOOR_NEON_INTL(162, "LIFX Outdoor Neon Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_A19_US_2(163, "LIFX A19 US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_US(164, "LIFX BR30 US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A19_INTL(165, "LIFX A19 Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_BR30_INTL(166, "LIFX BR30 Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_4(167, "LIFX Downlight", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_5(168, "LIFX Downlight", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A21_1600LM_US(169, "LIFX A21 1600lm US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_A21_1600LM_INTL(170, "LIFX A21 1600lm Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_ROUND_SPOT_US(171, "LIFX Round Spot US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_ROUND_PATH_US(173, "LIFX Round Path US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_SQUARE_PATH_US(174, "LIFX Square Path US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_PAR38_US(175, "LIFX PAR38 US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CEILING_US(176, "LIFX Ceiling US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CEILING_INTL(177, "LIFX Ceiling Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_US_2(178, "LIFX Downlight US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_US_3(179, "LIFX Downlight US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_US_4(180, "LIFX Downlight US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_COLOR_US_3(181, "LIFX Color US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_COLOUR_INTL(182, "LIFX Colour Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOR_US_2(185, "LIFX Candle Color US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOUR_INTL(186, "LIFX Candle Colour Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOR_US_3(187, "LIFX Candle Color US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOUR_INTL_2(188, "LIFX Candle Colour Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CEILING_13X26_US(201, "LIFX Ceiling 13x26\" US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CEILING_13X26_INTL(202, "LIFX Ceiling 13x26\" Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_STRING_US_2(203, "LIFX String US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_STRING_INTL_2(204, "LIFX String Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_INDOOR_NEON_US(205, "LIFX Indoor Neon US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_INDOOR_NEON_INTL(206, "LIFX Indoor Neon Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_PERMANENT_OUTDOOR_US(213, "LIFX Permanent Outdoor US", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_PERMANENT_OUTDOOR_INTL(214, "LIFX Permanent Outdoor Intl", true, true, false, false, true, 1500, 9000, false, false, 0, false, true),
	LIFX_CANDLE_COLOR_US_4(215, "LIFX Candle Color US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_CANDLE_COLOUR_INTL_3(216, "LIFX Candle Colour Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_TUBE_US(217, "LIFX Tube US", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_TUBE_INTL(218, "LIFX Tube Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_LUNA_US(219, "LIFX Luna US", true, true, false, true, false, 1500, 9000, false, false, 0, true, false),
	LIFX_LUNA_INTL(220, "LIFX Luna Intl", true, true, false, true, false, 1500, 9000, false, false, 0, true, false),
	LIFX_ROUND_SPOT_INTL(221, "LIFX Round Spot Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_ROUND_PATH_INTL(222, "LIFX Round Path Intl", true, true, false, true, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_US_5(223, "LIFX Downlight US", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_DOWNLIGHT_INTL_2(224, "LIFX Downlight Intl", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),
	LIFX_PAR38_INTL(225, "LIFX PAR38 INTL", true, true, false, false, false, 1500, 9000, false, false, 0, false, false),

	// JAVADOC:ON
	/**
	 * Unknown product.
	 */
	UNKNOWN(0, "Unknown Product", true, true, false, false, false, 0, 0, false, false, 0, false, false);

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
	 * Flag if this is a HEV light.
	 */
	private final boolean mIsHev;
	/**
	 * Build time for which extended API can be used.
	 */
	private final long mExtApiBuildTime;
	/**
	 * Flag if the device has buttons.
	 */
	private final boolean mButtons;
	/**
	 * Flag if the device supports extended Multizone API.
	 */
	private final boolean mExtendedMultizone;

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
	 * @param pid             The product id.
	 * @param name            The product name.
	 * @param isLight         Flag if this is a light.
	 * @param hasColor        Flag if this is a colored light.
	 * @param hasInfrared     Flag if this light has infrared.
	 * @param isMatrix        Flag if this is a matrix light.
	 * @param isMultizone     Flag if this is a multizone light.
	 * @param minTemperature  The min color temperature.
	 * @param maxTemperature  The max color temperature.
	 * @param isChain         Flag if this is a chain light.
	 * @param isHev           Flag if this is a HEV light.
	 * @param extApiBuildTime Build time for which extended API can be used.
	 */
	Product(final int pid, final String name, final boolean isLight, final boolean hasColor, final boolean hasInfrared, // SUPPRESS_CHECKSTYLE
			final boolean isMatrix, final boolean isMultizone, final int minTemperature, final int maxTemperature, final boolean isChain,
			final boolean isHev, final long extApiBuildTime, final boolean buttons, final boolean extendedMultizone) {
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
		mIsHev = isHev;
		mExtApiBuildTime = extApiBuildTime;
		mButtons = buttons;
		mExtendedMultizone = extendedMultizone;
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
	 * @return true if this is a light.
	 */
	public final boolean isLight() {
		return mIsLight;
	}

	/**
	 * Get flag if this is a colored light.
	 *
	 * @return true if this is a colored light.
	 */
	public final boolean hasColor() {
		return mHasColor;
	}

	/**
	 * Get flag if this has infrared.
	 *
	 * @return true if this has infrared.
	 */
	public final boolean hasInfrared() {
		return mHasInfrared;
	}

	/**
	 * Get flag if this is a matrix light.
	 *
	 * @return true if this is a matrix light.
	 */
	public final boolean isMatrix() {
		return mIsMatrix;
	}

	/**
	 * Get flag if this is a multizone light.
	 *
	 * @return true if this is a multizone light.
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
	 * @return true if this is a chain light.
	 */
	public final boolean isChain() {
		return mIsChain;
	}

	/**
	 * Get flag if this is a HEV light.
	 *
	 * @return true if this is a HEV light.
	 */
	public final boolean isHev() {
		return mIsHev;
	}

	/**
	 * Get flag if the device has buttons.
	 *
	 * @return true if the device has buttons.
	 */
	public final boolean hasButtons() {
		return mButtons;
	}

	/**
	 * Check if the device has extended API.
	 *
	 * @param buildDate The firmware build date.
	 * @return true if there is extended API available.
	 */
	public final boolean hasExtendedApi(final Date buildDate) {
		return mExtApiBuildTime > 0 ? buildDate.getTime() / 1000 >= mExtApiBuildTime : mExtendedMultizone;
	}

	@Override
	public String toString() {
		return getName() + " (" + getId() + ")";
	}

}