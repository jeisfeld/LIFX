package de.jeisfeld.lifx;

import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Power;
import de.jeisfeld.lifx.lan.util.Logger;
import de.jeisfeld.lifx.lan.util.TypeUtil;

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
		long startTime = System.currentTimeMillis();
		FARBLAMPE.setPower(Power.OFF);
		short powerLevel = -1;
		while (powerLevel != 0) {
			powerLevel = FARBLAMPE.getPowerLevel();
			System.out.println((System.currentTimeMillis() - startTime) + " - " + TypeUtil.toUnsignedString(powerLevel));
		}
		FARBLAMPE.setPower(Power.ON);
		while (powerLevel != -1) {
			powerLevel = FARBLAMPE.getPowerLevel();
			System.out.println((System.currentTimeMillis() - startTime) + " - " + TypeUtil.toUnsignedString(powerLevel));
		}
	}

}
