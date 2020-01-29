package de.jeisfeld.lifx;

import java.util.List;

import de.jeisfeld.lifx.lan.LifxLan;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.Logger;

/**
 * Test class for testing LIFX API.
 */
public class Test {

	public static void main(final String[] args) throws Exception {
		List<Light> lights = new LifxLan(1).getLights();

		for (Light light : lights) {
			Logger.info(light.getFullInformation());
		}
	}

}
