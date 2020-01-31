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
		FARBLAMPE.setColor(Color.RED.addRgb(Color.GREEN, 0.5));
		FARBLAMPE.setColor(Color.GREEN.addRgb(Color.BLUE, 0.5), 2000, true);
		FARBLAMPE.setColor(Color.BLUE.addRgb(Color.RED, 0.5), 2000, true);
		FARBLAMPE.setColor(Color.RED.addRgb(Color.GREEN, 0.5), 2000, true);

	}

}
