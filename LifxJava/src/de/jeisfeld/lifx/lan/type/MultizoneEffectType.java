package de.jeisfeld.lifx.lan.type;

/**
 * Multizone effects.
 */
public enum MultizoneEffectType {
	// JAVADOC:OFF
	OFF,
	MOVE,
	UNKNOWN;
	// JAVADOC:ON

	/**
	 * Get the MultizoneEffectType from its integer value.
	 *
	 * @param multizoneEffect the multizone effect integer value.
	 * @return The tile effect.
	 */
	public static MultizoneEffectType fromInt(final int multizoneEffect) {
		for (MultizoneEffectType effect : values()) {
			if (effect.ordinal() == multizoneEffect) {
				return effect;
			}
		}
		return MultizoneEffectType.UNKNOWN;
	}
}
