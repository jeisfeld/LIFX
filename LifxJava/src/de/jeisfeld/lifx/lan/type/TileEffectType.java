package de.jeisfeld.lifx.lan.type;

/**
 * Tile effects.
 */
public enum TileEffectType {
	// JAVADOC:OFF
	OFF,
	UNKNOWN,
	MORPH,
	FLAME;
	// JAVADOC:ON

	/**
	 * Get the TileEffect from its integer value.
	 *
	 * @param tileEffect the tile effect integer value.
	 * @return The tile effect.
	 */
	public static TileEffectType fromInt(final int tileEffect) {
		for (TileEffectType effect : values()) {
			if (effect.ordinal() == tileEffect) {
				return effect;
			}
		}
		return TileEffectType.UNKNOWN;
	}
}
