package de.jeisfeld.lifx;

import java.util.Random;

import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;
import de.jeisfeld.lifx.lan.type.TileChainColors;
import de.jeisfeld.lifx.os.Logger;

/**
 * Test class for testing LIFX API.
 */
public final class Test {
	// JAVADOC:OFF
	// SYSTEMOUT:OFF
	private static final String MAC_FARBLAMPE = "D0:73:D5:53:DC:A7";
	private static final String MAC_SWLAMPE = "D0:73:D5:56:40:78";
	private static final String MAC_Z1 = "D0:73:D5:14:88:FC";
	private static final String MAC_Z2 = "D0:73:D5:41:46:4B";
	@SuppressWarnings("unused")
	private static final String MAC_FARBLAMPE_PLUS = "D0:73:D5:2F:51:94";
	private static final String MAC_TILE_4 = "D0:73:D5:55:1B:DF";

	private static final Light FARBLAMPE = LifxLan.getInstance().getLightByMac(MAC_FARBLAMPE);
	// private static final Light FARBLAMPE_PLUS = LifxLan.getInstance().getLightByMac(MAC_FARBLAMPE_PLUS);
	private static final Light SWLAMPE = LifxLan.getInstance().getLightByMac(MAC_SWLAMPE);
	@SuppressWarnings("unused")
	private static final MultiZoneLight Z1 = (MultiZoneLight) LifxLan.getInstance().getLightByMac(MAC_Z1);
	private static final MultiZoneLight Z2 = (MultiZoneLight) LifxLan.getInstance().getLightByMac(MAC_Z2);
	private static final TileChain TILE_4 = (TileChain) LifxLan.getInstance().getLightByMac(MAC_TILE_4);

	private static final int ONESECOND = 1000;
	private static final int TWOSECONDS = 2000;
	private static final int FIVESECONDS = 5000;
	private static final int HALFMINUTE = 30000;

	public static void main(final String[] args) throws Exception { // SUPPRESS_CHECKSTYLE
		Logger.setLogDetails(false);
		new Test().test4();
	}

	void test0() throws Exception {
		for (Device device : LifxLan.getInstance().getDevices()) {
			System.out.println(device.getFullInformation());
		}
	}

	void test1() throws Exception { // SUPPRESS_CHECKSTYLE
		Color endColor = FARBLAMPE.getColor();
		FARBLAMPE.cycle(Color.CYCLE_RAINBOW_LOW)
				.setCycleDuration(FIVESECONDS)
				.setStartTransitionTime(ONESECOND)
				.setEndColor(endColor, ONESECOND)
				.setBrightness(1)
				.setCycleCount(2)
				.start();

		FARBLAMPE.waitForAnimationEnd();
	}

	void test2() throws Exception { // SUPPRESS_CHECKSTYLE
		FARBLAMPE.wakeup(HALFMINUTE, null);
		SWLAMPE.wakeup(HALFMINUTE, null);
		Z2.wakeup(HALFMINUTE, null);

		FARBLAMPE.waitForAnimationEnd();
		SWLAMPE.waitForAnimationEnd();
		Z2.waitForAnimationEnd();
	}

	void test3() throws Exception { // SUPPRESS_CHECKSTYLE
		Random random = new Random();
		FARBLAMPE.animation(new AnimationDefinition() {
			@Override
			public int getDuration(final int n) {
				return random.nextInt(FIVESECONDS);
			}

			@Override
			public Color getColor(final int n) {
				return new Color(random.nextInt(65536), random.nextInt(65536), random.nextInt(65536), 1500 + random.nextInt(7500)); // MAGIC_NUMBER
			}
		})
				.setEndColor(Color.OFF, TWOSECONDS)
				.start();

		Thread.sleep(HALFMINUTE);
		FARBLAMPE.endAnimation(true);
	}

	void test4() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Z2.getFullInformation());
		Z2.setColors(new MultizoneColors.Fixed(Color.RED).combine(new MultizoneColors.Fixed(Color.GREEN), 0.5) // MAGIC_NUMBER
				.combine(new MultizoneColors.Fixed(Color.BLUE), 0.75), FIVESECONDS, true); // MAGIC_NUMBER
		Logger.setLogDetails(true);
		Z2.setPower(true);
		Logger.setLogDetails(false);
		System.out.println(Z2.getFullInformation());
	}

	void test5() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Z2.getFullInformation());
		Z2
				.rollingAnimation(10000, // MAGIC_NUMBER
						new MultizoneColors.Interpolated(true, Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE))
				.setBrightness(0.3) // MAGIC_NUMBER
				.start();
	}

	void test6() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Z2.getFullInformation());
		// Z2.setEffect(MultizoneEffectInfo.OFF);
		Z2.setEffect(new MultizoneEffectInfo.Move(ONESECOND, false));

	}

	void test7() throws Exception { // SUPPRESS_CHECKSTYLE
		double xCenter = (TILE_4.getTotalWidth() - 1) / 2.0;
		double yCenter = (TILE_4.getTotalHeight() - 1) / 2.0;
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			final int j = i;
			TILE_4.setColors(new TileChainColors() {
				/**
				 * The default serializable version id.
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Color getColor(final int x, final int y, final int width, final int height) {
					double distance = Math.sqrt((x - xCenter) * (x - xCenter) + (y - yCenter) * (y - yCenter));
					return new Color((int) (1024 * (5 * distance - j)), -1, 10000, 4000); // MAGIC_NUMBER
				}
			}, 0, false);
		}

	}

	void test8() throws Exception { // SUPPRESS_CHECKSTYLE
		TILE_4.setColors(new TileChainColors.InterpolatedCorners(Color.RED, Color.GREEN, Color.GREEN, Color.RED)
				.withRelativeBrightness(0.01), 0, false); // MAGIC_NUMBER

		// TILE_4.setEffect(new TileEffectInfo.Morph(10000, Color.RED, Color.WHITE));
	}
}
