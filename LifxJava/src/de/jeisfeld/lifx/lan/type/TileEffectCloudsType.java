package de.jeisfeld.lifx.lan.type;

/**
 * Tile effect clouds types.
 */
public enum TileEffectCloudsType {
	// JAVADOC:OFF
	SUNRISE,
	SUNSET,
	CLOUDS;
	// JAVADOC:ON

        /**
         * Get the TileEffectCloudsType from its integer value.
         *
         * @param tileEffectCloudsType the tile effect clouds type integer value.
         * @return The tile effect clouds type.
         */
        public static TileEffectCloudsType fromInt(final int tileEffectCloudsType) {
                for (TileEffectCloudsType cloudsType : values()) {
                        if (cloudsType.ordinal() == tileEffectCloudsType) {
                                return cloudsType;
                        }
                }
                return TileEffectCloudsType.CLOUDS;
        }
}
