package de.jeisfeld.lifx;

import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.util.Logger;

/**
 * Test class for testing LIFX API.
 */
public class Test {
	private static String MAC_FARBLAMPE = "D0:73:D5:53:DC:A7";
	private static String MAC_SWLAMPE = "D0:73:D5:56:40:78";

	private final Light FARBLAMPE = LifxLan.getInstance().getLightByMac(Test.MAC_FARBLAMPE);
	// private final Light SWLAMPE = LifxLan.getInstance().getLightByMac(Test.MAC_SWLAMPE);

	public static void main(final String[] args) throws Exception {
		Logger.setLogDetails(false);
		new Test().test();
	}

	private void test() throws Exception {
		// System.out.println(LifxLan.getInstance().getLights());
		// System.out.println(FARBLAMPE.getFullInformation());

		FARBLAMPE.setColor(Color.ORANGE, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.YELLOW, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.GRASS_GREEN, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.GREEN, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.BLUE_GREEN, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.CYAN, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.LIGHT_BLUE, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.BLUE, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.PURPLE, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.MAGENTA, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.PINK, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.RED, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.GOLD, 1000, true);
		System.out.println(FARBLAMPE.getColor());
		FARBLAMPE.setColor(Color.WARM_WHITE, 1000, false);
		System.out.println(FARBLAMPE.getColor());
	}

	private void test1() throws Exception {

		long startTime = System.currentTimeMillis();
		FARBLAMPE.setPower(false, 2000);
		Power power = Power.ON;
		while (!power.isOff()) {
			System.out.println((System.currentTimeMillis() - startTime) + " - " + (power = FARBLAMPE.getPower()) + " - " + FARBLAMPE.getColor());
		}
		FARBLAMPE.setPower(true, 2000);
		while (!power.isOn()) {
			System.out.println((System.currentTimeMillis() - startTime) + " - " + (power = FARBLAMPE.getPower()) + " - " + FARBLAMPE.getColor());
		}
	}

}
