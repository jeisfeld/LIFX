package de.jeisfeld.lifx;

import java.util.Random;

import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Light.AnimationDefinition;
import de.jeisfeld.lifx.lan.MultiZoneLight;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.Logger;

/**
 * Test class for testing LIFX API.
 */
public class Test {
	private static String MAC_FARBLAMPE = "D0:73:D5:53:DC:A7";
	private static String MAC_SWLAMPE = "D0:73:D5:56:40:78";
	private static String MAC_LICHTSTREIFEN = "D0:73:D5:14:88:FC";

	private final Light FARBLAMPE = LifxLan.getInstance().getLightByMac(Test.MAC_FARBLAMPE);
	private final Light SWLAMPE = LifxLan.getInstance().getLightByMac(Test.MAC_SWLAMPE);
	private final MultiZoneLight LICHTSTREIFEN = (MultiZoneLight) LifxLan.getInstance().getLightByMac(Test.MAC_LICHTSTREIFEN);

	public static void main(final String[] args) throws Exception {
		Logger.setLogDetails(false);
		new Test().test0();
	}

	private void test0() throws Exception {
		for (Device device : LifxLan.getInstance().getDevices()) {
			System.out.println(device.getFullInformation());
		}
	}

	private void test1() throws Exception {
		Color endColor = FARBLAMPE.getColor();
		FARBLAMPE.cycle(Color.CYCLE_RAINBOW_LOW)
				.setCycleDuration(5000)
				.setStartTransitionTime(1000)
				.setEndColor(endColor, 1000)
				.setBrightness(1)
				.setCycleCount(2)
				.start();

		FARBLAMPE.waitForAnimationEnd();
	}

	private void test2() throws Exception {
		FARBLAMPE.wakeup(30000, null);
		SWLAMPE.wakeup(30000, null);
		LICHTSTREIFEN.wakeup(30000, null);

		FARBLAMPE.waitForAnimationEnd();
		SWLAMPE.waitForAnimationEnd();
		LICHTSTREIFEN.waitForAnimationEnd();
	}

	private void test3() throws Exception {
		Random random = new Random();
		FARBLAMPE.animation(new AnimationDefinition() {

			@Override
			public int getDuration(final int n) {
				return random.nextInt(5000);
			}

			@Override
			public Color getColor(final int n) {
				return new Color(random.nextInt(65536), random.nextInt(65536), random.nextInt(65536), 1500 + random.nextInt(7500));
			}
		})
				.setEndColor(Color.OFF, 2000)
				.start();

		Thread.sleep(20000);
		FARBLAMPE.endAnimation();

	}

}
