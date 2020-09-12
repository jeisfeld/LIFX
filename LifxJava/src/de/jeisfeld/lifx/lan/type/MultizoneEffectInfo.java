package de.jeisfeld.lifx.lan.type;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding information about multizone effect.
 */
public class MultizoneEffectInfo {
	/**
	 * The number of parameters.
	 */
	public static final int MULTIZONE_EFFECT_PARAMETER_COUNT = 8;
	/**
	 * Empty parameter set.
	 */
	private static final int[] EMPTY_PARAMETERS = new int[MULTIZONE_EFFECT_PARAMETER_COUNT];
	/**
	 * Empty parameter set.
	 */
	private static final int[] FORWARD_PARAMETERS = new int[] {1, 1, 0, 0, 0, 0, 0, 0};
	/**
	 * The instance id used by these classes.
	 */
	private static final int INSTANCE_ID = 99;
	/**
	 * Tile effect info for no effect.
	 */
	public static final MultizoneEffectInfo OFF = new MultizoneEffectInfo.Off();

	/**
	 * The instance id.
	 */
	private final int mInstanceId;
	/**
	 * The effect type.
	 */
	private final MultizoneEffectType mType;
	/**
	 * The effect speed.
	 */
	private final int mSpeed;
	/**
	 * The effect parameters.
	 */
	private final int[] mParameters;

	/**
	 * Constructor.
	 *
	 * @param instanceId The instance id.
	 * @param type The effect type.
	 * @param speed The effect speed.
	 * @param parameters The effect parameters.
	 */
	public MultizoneEffectInfo(final int instanceId, final MultizoneEffectType type, final int speed, final int[] parameters) {
		mInstanceId = instanceId;
		mType = type;
		mSpeed = speed;
		mParameters = parameters;
	}

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder("Multizone Effect[");
		result.append("InstanceId=").append(TypeUtil.toUnsignedString(mInstanceId)).append(", ");
		result.append("EffectType=").append(mType.name()).append(", ");
		result.append("Speed=").append(TypeUtil.toUnsignedString(mSpeed)).append(", ");

		StringBuilder parameterString = new StringBuilder("[");
		for (int parameter : mParameters) {
			parameterString.append(TypeUtil.toUnsignedString(parameter)).append(", ");
		}
		parameterString.replace(parameterString.length() - 2, parameterString.length(), "]");
		result.append("Parameters=").append(parameterString.toString()).append(", ");
		return result.toString();
	}

	/**
	 * Get the instance id.
	 *
	 * @return the Instance Id
	 */
	public final int getInstanceId() {
		return mInstanceId;
	}

	/**
	 * Get the effect type.
	 *
	 * @return the effect type.
	 */
	public final MultizoneEffectType getType() {
		return mType;
	}

	/**
	 * Get the effect speed.
	 *
	 * @return the effect speed
	 */
	public final int getSpeed() {
		return mSpeed;
	}

	/**
	 * Get the effect paramters.
	 *
	 * @return the effect parameters
	 */
	public final int[] getParameters() {
		return mParameters;
	}

	/**
	 * Tile effect info for no effect.
	 */
	public static class Off extends MultizoneEffectInfo {
		/**
		 * Create no-effect info.
		 */
		public Off() {
			super(INSTANCE_ID, MultizoneEffectType.OFF, 0, EMPTY_PARAMETERS);
		}
	}

	/**
	 * Tile effect info for Move.
	 */
	public static class Move extends MultizoneEffectInfo {
		/**
		 * Create Move info.
		 *
		 * @param speed The speed
		 * @param isBackward flag for backward move.
		 */
		public Move(final int speed, final boolean isBackward) {
			super(INSTANCE_ID, MultizoneEffectType.MOVE, speed, isBackward ? EMPTY_PARAMETERS : FORWARD_PARAMETERS);
		}
	}
}
