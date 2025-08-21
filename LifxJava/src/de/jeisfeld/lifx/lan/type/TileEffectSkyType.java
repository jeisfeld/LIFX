package de.jeisfeld.lifx.lan.type;

/**
 * Tile effect sky types.
 */
public enum TileEffectSkyType {
	// JAVADOC:OFF
	SUNRISE,
	SUNSET,
	CLOUDS;
	// JAVADOC:ON

	/**
	 * Get the TileEffectSkyType from its integer value.
	 *
	 * @param tileEffectSkyType the tile effect sky type integer value.
	 * @return The tile effect sky type.
	 */
	public static TileEffectSkyType fromInt(final int tileEffectSkyType) {
		for (TileEffectSkyType skyType : values()) {
			if (skyType.ordinal() == tileEffectSkyType) {
				return skyType;
			}
		}
		return TileEffectSkyType.CLOUDS;
	}
}
