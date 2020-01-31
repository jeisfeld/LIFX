package de.jeisfeld.lifx;

import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;
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
		System.out.println(FARBLAMPE.getFullInformation());
		FARBLAMPE.setColor(Color.RED, 2000, true);
		FARBLAMPE.setColor(Color.GOLD.withBrightness(0.1), 5000, true);

	}

}
