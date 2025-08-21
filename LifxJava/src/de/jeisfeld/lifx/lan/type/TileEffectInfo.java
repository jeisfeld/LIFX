package de.jeisfeld.lifx.lan.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Class holding information about tile effect.
 */
public class TileEffectInfo {
	/**
	 * The number of parameters.
	 */
	public static final int TILE_EFFECT_PARAMETER_COUNT = 32;
	/**
	 * Empty parameter set.
	 */
	private static final byte[] EMPTY_PARAMETERS = new byte[TILE_EFFECT_PARAMETER_COUNT];
	/**
	 * The instance id used by these classes.
	 */
	private static final int INSTANCE_ID = 99;
	/**
	 * Tile effect info for no effect.
	 */
	public static final TileEffectInfo OFF = new TileEffectInfo.Off();

	/**
	 * The instance id.
	 */
	private final int mInstanceId;
	/**
	 * The effect type.
	 */
	private final TileEffectType mType;
	/**
	 * The effect speed.
	 */
	private final int mSpeed;
	/**
	 * The effect parameters.
	 */
	private final byte[] mParameters;
	/**
	 * The palette colors.
	 */
	private final List<Color> mPaletteColors;

	/**
	 * Constructor.
	 *
	 * @param instanceId    The instance id.
	 * @param type          The effect type.
	 * @param speed         The effect speed.
	 * @param parameters    The effect parameters.
	 * @param paletteColors The palette colors.
	 */
	public TileEffectInfo(final int instanceId, final TileEffectType type, final int speed, final byte[] parameters,
						  final List<Color> paletteColors) {
		mInstanceId = instanceId;
		mType = type;
		mSpeed = speed;
		mParameters = parameters;
		mPaletteColors = paletteColors;
	}

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder("Tile Effect[");
		result.append("InstanceId=").append(TypeUtil.toUnsignedString(mInstanceId)).append(", ");
		result.append("EffectType=").append(mType.name()).append(", ");
		result.append("Speed=").append(TypeUtil.toUnsignedString(mSpeed)).append(", ");

		StringBuilder parameterString = new StringBuilder("[");
		for (byte parameter : mParameters) {
			parameterString.append(TypeUtil.toUnsignedString(parameter)).append(", ");
		}
		parameterString.replace(parameterString.length() - 2, parameterString.length(), "]");
		result.append("Parameters=").append(parameterString).append(", ");

		result.append("Colors=").append(mPaletteColors.toString());
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
	public final TileEffectType getType() {
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
	public final byte[] getParameters() {
		return mParameters;
	}

	/**
	 * Get the palette colors.
	 *
	 * @return the palette colors
	 */
	public final List<Color> getPaletteColors() {
		return mPaletteColors;
	}

	/**
	 * Tile effect info for no effect.
	 */
	public static class Off extends TileEffectInfo {
		/**
		 * Create no-effect info.
		 */
		public Off() {
			super(INSTANCE_ID, TileEffectType.OFF, 0, EMPTY_PARAMETERS, new ArrayList<Color>());
		}
	}

	/**
	 * Tile effect info for Flame.
	 */
	public static class Flame extends TileEffectInfo {
		/**
		 * Create Flame info.
		 *
		 * @param speed The speed
		 */
		public Flame(final int speed) {
			super(INSTANCE_ID, TileEffectType.FLAME, speed, EMPTY_PARAMETERS, new ArrayList<Color>());
		}
	}

	/**
	 * Tile effect info for Morph effect.
	 */
	public static class Morph extends TileEffectInfo {
		/**
		 * Create Morph info.
		 *
		 * @param speed  The speed
		 * @param colors The palette colors.
		 */
		public Morph(final int speed, final Color... colors) {
			super(INSTANCE_ID, TileEffectType.MORPH, speed, EMPTY_PARAMETERS, Arrays.asList(colors));
		}
	}

	/**
	 * Tile effect info for Sky.
	 */
	public static class Sky extends TileEffectInfo {
		/**
		 * Create Flame info.
		 *
		 * @param speed              The speed
		 * @param skyType            The sky type
		 * @param cloudSaturationMin The minimum cloud saturation
		 * @param colors             The palette colors.
		 */
		public Sky(final int speed, final TileEffectSkyType skyType, final byte cloudSaturationMin, final Color... colors) {
			super(INSTANCE_ID, TileEffectType.SKY, speed,
					TileEffectInfo.getSkyParameters(skyType, cloudSaturationMin),
					Arrays.asList(colors));
		}
	}

	/**
	 * Get sky parameters for request.
	 *
	 * @param skyType            The sky type
	 * @param cloudSaturationMin The minimum cloud saturation
	 * @return The sky parameters
	 */
	private static byte[] getSkyParameters(final TileEffectSkyType skyType, final byte cloudSaturationMin) {
		byte[] skyParameters = new byte[TILE_EFFECT_PARAMETER_COUNT];
		skyParameters[0] = (byte) skyType.ordinal();
		skyParameters[4] = cloudSaturationMin;
		return skyParameters;
	}
}
