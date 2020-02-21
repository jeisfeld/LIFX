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
import de.jeisfeld.lifx.lan.type.TileColors;
import de.jeisfeld.lifx.lan.type.TileEffectInfo;
import de.jeisfeld.lifx.os.Logger;

/**
 * Test class for testing LIFX API.
 */
public final class Test {
	// JAVADOC:OFF
	// SYSTEMOUT:OFF
	private static final String MAC_FARBLAMPE = "D0:73:D5:53:DC:A7";
	private static final String MAC_SWLAMPE = "D0:73:D5:56:40:78";
	private static final String MAC_LICHTSTREIFEN = "D0:73:D5:14:88:FC";
	private static final String MAC_FARBLAMPE_PLUS = "D0:73:D5:2F:51:94";
	private static final String MAC_TILE_4 = "D0:73:D5:55:1B:DF";

	private static final Light FARBLAMPE = LifxLan.getInstance().getLightByMac(MAC_FARBLAMPE);
	// private static final Light FARBLAMPE_PLUS = LifxLan.getInstance().getLightByMac(MAC_FARBLAMPE_PLUS);
	private static final Light SWLAMPE = LifxLan.getInstance().getLightByMac(MAC_SWLAMPE);
	private static final MultiZoneLight LICHTSTREIFEN = (MultiZoneLight) LifxLan.getInstance().getLightByMac(MAC_LICHTSTREIFEN);
	private static final TileChain TILE_4 = (TileChain) LifxLan.getInstance().getLightByMac(MAC_TILE_4);

	private static final int ONESECOND = 1000;
	private static final int TWOSECONDS = 2000;
	private static final int FIVESECONDS = 5000;
	private static final int HALFMINUTE = 30000;

	public static void main(final String[] args) throws Exception { // SUPPRESS_CHECKSTYLE
		Logger.setLogDetails(false);
		new Test().test6();
	}

	void test0() throws Exception {
		for (Device device : LifxLan.getInstance().getDevices()) {
			System.out.println(device.getFullInformation());
		}
	}

	void test1() throws Exception { // SUPPRESS_CHECKSTYLE
		Color endColor = Test.FARBLAMPE.getColor();
		Test.FARBLAMPE.cycle(Color.CYCLE_RAINBOW_LOW)
				.setCycleDuration(Test.FIVESECONDS)
				.setStartTransitionTime(Test.ONESECOND)
				.setEndColor(endColor, Test.ONESECOND)
				.setBrightness(1)
				.setCycleCount(2)
				.start();

		Test.FARBLAMPE.waitForAnimationEnd();
	}

	void test2() throws Exception { // SUPPRESS_CHECKSTYLE
		Test.FARBLAMPE.wakeup(Test.HALFMINUTE, null);
		Test.SWLAMPE.wakeup(Test.HALFMINUTE, null);
		Test.LICHTSTREIFEN.wakeup(Test.HALFMINUTE, null);

		Test.FARBLAMPE.waitForAnimationEnd();
		Test.SWLAMPE.waitForAnimationEnd();
		Test.LICHTSTREIFEN.waitForAnimationEnd();
	}

	void test3() throws Exception { // SUPPRESS_CHECKSTYLE
		Random random = new Random();
		Test.FARBLAMPE.animation(new AnimationDefinition() {

			@Override
			public int getDuration(final int n) {
				return random.nextInt(Test.FIVESECONDS);
			}

			@Override
			public Color getColor(final int n) {
				return new Color(random.nextInt(65536), random.nextInt(65536), random.nextInt(65536), 1500 + random.nextInt(7500)); // MAGIC_NUMBER
			}
		})
				.setEndColor(Color.OFF, Test.TWOSECONDS)
				.start();

		Thread.sleep(Test.HALFMINUTE);
		Test.FARBLAMPE.endAnimation(true);
	}

	void test4() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Test.LICHTSTREIFEN.getFullInformation());
		Test.LICHTSTREIFEN.setColors(Test.FIVESECONDS, true, new MultizoneColors.Interpolated(false,
				Color.RED, Color.GREEN, Color.BLUE).withRelativeBrightness(0.2)); // MAGIC_NUMBER
		Test.LICHTSTREIFEN.setPower(true);
		System.out.println(Test.LICHTSTREIFEN.getFullInformation());
	}

	void test5() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Test.LICHTSTREIFEN.getFullInformation());
		Test.LICHTSTREIFEN
				.rollingAnimation(10000, // MAGIC_NUMBER
						new MultizoneColors.Interpolated(true, Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE))
				.setBrightness(0.3) // MAGIC_NUMBER
				.start();
	}

	void test6() throws Exception { // SUPPRESS_CHECKSTYLE
		TILE_4.setColors((byte) 0, 0,
				new TileColors.InterpolatedCorners(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE).withRelativeBrightness(0.3));
		TILE_4.setEffect(new TileEffectInfo.Morph(10000, Color.RED, Color.WHITE));
	}

}
