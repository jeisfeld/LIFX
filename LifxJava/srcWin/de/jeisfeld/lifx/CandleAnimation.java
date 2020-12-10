package de.jeisfeld.lifx;

import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.animation.CandleAnimationDefinition;
import de.jeisfeld.lifx.lan.animation.CandleAnimationDefinition.Background;

/**
 * An animation of a candle on a tile chain.
 */
public final class CandleAnimation {
	/**
	 * The MAC of the tile chain used.
	 */
	private static final String MAC_TILE_4 = "D0:73:D5:55:1B:DF";
	/**
	 * The tile chain used.
	 */
	private static final TileChain TILE_4 = (TileChain) LifxLan.getInstance().getLightByMac(MAC_TILE_4);

	/**
	 * Hidden default constructor.
	 */
	private CandleAnimation() {
		// hide default constructor
	}

	/**
	 * The main class to run the animation.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(final String[] args) throws Exception { // SUPPRESS_CHECKSTYLE
		int candleCount = 1;
		int burndown = 1;
		double brightnessFactor = 1;
		Background background = Background.BLACK;

		if (args.length > 0) {
			candleCount = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			burndown = Integer.parseInt(args[1]);
		}
		if (args.length > 2) {
			brightnessFactor = Double.parseDouble(args[2]);
		}
		if (args.length > 3) { // MAGIC_NUMBER
			background = Background.fromOrdinal(Integer.parseInt(args[3])); // MAGIC_NUMBER
		}

		CandleAnimationDefinition definition = new CandleAnimationDefinition(TILE_4, candleCount, burndown, brightnessFactor, background);
		TILE_4.animation(definition).start();
	}

}
