package de.jeisfeld.lifx;

import java.util.Date;
import java.util.Random;

import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.MultizoneEffectInfo;
import de.jeisfeld.lifx.lan.util.TypeUtil;
import de.jeisfeld.lifx.os.Logger;

/**
 * Test class for testing LIFX API.
 */
public final class Test {
	// JAVADOC:OFF
	// SYSTEMOUT:OFF
	private static final String MAC_FARBLAMPE = "D0:73:D5:53:DC:A7";
	@SuppressWarnings("unused")
	private static final String MAC_SWLAMPE = "D0:73:D5:56:40:78";
	@SuppressWarnings("unused")
	private static final String MAC_Z1 = "D0:73:D5:14:88:FC";
	private static final String MAC_Z2 = "D0:73:D5:41:46:4B";
	@SuppressWarnings("unused")
	private static final String MAC_FARBLAMPE_PLUS = "D0:73:D5:2F:51:94";
	@SuppressWarnings("unused")
	private static final String MAC_TILE_4 = "D0:73:D5:55:1B:DF";

	private static final Light FARBLAMPE = LifxLan.getInstance().getLightByMac(MAC_FARBLAMPE);
	// private static final Light FARBLAMPE_PLUS = LifxLan.getInstance().getLightByMac(MAC_FARBLAMPE_PLUS);
	// private static final Light SWLAMPE = LifxLan.getInstance().getLightByMac(MAC_SWLAMPE);
	// private static final MultiZoneLight Z1 = (MultiZoneLight) LifxLan.getInstance().getLightByMac(MAC_Z1);
	private static final MultiZoneLight Z2 = (MultiZoneLight) LifxLan.getInstance().getLightByMac(MAC_Z2);
	// private static final TileChain TILE_4 = (TileChain) LifxLan.getInstance().getLightByMac(MAC_TILE_4);

	private static final int ONESECOND = 1000;
	private static final int TWOSECONDS = 2000;
	private static final int FIVESECONDS = 5000;
	private static final int HALFMINUTE = 30000;

	public static void main(final String[] args) throws Exception { // SUPPRESS_CHECKSTYLE
		Logger.setLogDetails(false);
		new Test().test0();
	}

	void test0() throws Exception {
		for (Device device : LifxLan.getInstance().getDevices()) {
			System.out.println(device.getFullInformation(TypeUtil.INDENT, true));
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
		Z2.wakeup(HALFMINUTE, null);

		FARBLAMPE.waitForAnimationEnd();
		Z2.waitForAnimationEnd();
	}

	void test3() throws Exception { // SUPPRESS_CHECKSTYLE
		Random random = new Random();
		FARBLAMPE.animation(new Light.AnimationDefinition() {
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
		System.out.println(Z2.getFullInformation(TypeUtil.INDENT, true));
		Z2.setColors(new MultizoneColors.Fixed(Color.RED).combine(new MultizoneColors.Fixed(Color.GREEN), 0.5) // MAGIC_NUMBER
				.combine(new MultizoneColors.Fixed(Color.BLUE), 0.75), FIVESECONDS, true); // MAGIC_NUMBER
		Logger.setLogDetails(true);
		Z2.setPower(true);
		Logger.setLogDetails(false);
		System.out.println(Z2.getFullInformation(TypeUtil.INDENT, true));
	}

	void test5() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Z2.getFullInformation(TypeUtil.INDENT, true));
		Z2
				.rollingAnimation(10000, // MAGIC_NUMBER
						new MultizoneColors.Interpolated(true, Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE))
				.setBrightness(0.3) // MAGIC_NUMBER
				.start();
	}

	void test6() throws Exception { // SUPPRESS_CHECKSTYLE
		System.out.println(Z2.getFullInformation(TypeUtil.INDENT, true));
		// Z2.setEffect(MultizoneEffectInfo.OFF);
		Z2.setEffect(new MultizoneEffectInfo.Move(ONESECOND, false));

	}

	void test7() throws Exception { // SUPPRESS_CHECKSTYLE
		FARBLAMPE.animation(new Light.AnimationDefinition() {

			@Override
			public int getDuration(final int n) {
				return 2000; // MAGIC_NUMBER
			}

			@Override
			public Color getColor(final int n) {
				switch (n) {
				case 0:
					return Color.RED;
				case 1:
					return Color.OFF;
				case 2:
					return Color.GREEN;
				case 3: // MAGIC_NUMBER
					return Color.OFF;
				default:
					return null;
				}
			}

			@Override
			public Date getStartTime(final int n) {
				return n == 2 ? new Date(System.currentTimeMillis() + 2000) : null; // MAGIC_NUMBER
			}
		}).start();
	}

	void test8() throws Exception { // SUPPRESS_CHECKSTYLE
		Z2.animation(new MultiZoneLight.AnimationDefinition() {

			@Override
			public int getDuration(final int n) {
				return 2000; // MAGIC_NUMBER
			}

			@Override
			public MultizoneColors getColors(final int n) {
				switch (n) {
				case 0:
					return new MultizoneColors.Interpolated(true, Color.RED, Color.BLUE);
				case 1:
					return MultizoneColors.OFF;
				case 2:
					return new MultizoneColors.Fixed(Color.GREEN);
				case 3: // MAGIC_NUMBER
					return MultizoneColors.OFF;
				default:
					return null;
				}
			}

			@Override
			public Date getStartTime(final int n) {
				return n == 2 ? new Date(System.currentTimeMillis() + 2000) : null; // MAGIC_NUMBER
			}
		}).start();
	}

	void test9() throws Exception { // SUPPRESS_CHECKSTYLE
		// FARBLAMPE.setWaveform(false, Color.GREEN, 0, 1, Waveform.PULSE, 0.5, false);
		// FARBLAMPE.setWaveform(false, null, null, 0.1, null, 0, 1, 0.5, Waveform.PULSE, false);
		FARBLAMPE.setBrightness(0.8); // MAGIC_NUMBER
	}
}
