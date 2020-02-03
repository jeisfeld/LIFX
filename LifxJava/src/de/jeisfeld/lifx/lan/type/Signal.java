package de.jeisfeld.lifx.lan.type;

/**
 * Class holding signal information.
 */
public class Signal {
	/**
	 * The signal value.
	 */
	private final int mValue;

	/**
	 * Constructor.
	 *
	 * @param signal The signal level.
	 */
	public Signal(final float signal) {
		mValue = (int) Math.round(10 * Math.log10(signal)); // MAGIC_NUMBER
	}

	/**
	 * Get the signal strength.
	 *
	 * @return the signal strength.
	 */
	public int getValue() {
		return mValue;
	}

	/**
	 * Get a text representing the signal strength.
	 *
	 * @return The signal text.
	 */
	public String getValueText() {
		if (mValue < 0 || mValue == 200) { // MAGIC_NUMBER
			// The value is wifi rssi
			if (mValue == 200) { // MAGIC_NUMBER
				return "No signal";
			}
			else if (mValue <= -80) { // MAGIC_NUMBER
				return "Very bad signal";
			}
			else if (mValue <= -70) { // MAGIC_NUMBER
				return "Somewhat bad signal";
			}
			else if (mValue < -60) { // MAGIC_NUMBER
				return "Alright signal";
			}
			else {
				return "Good signal";
			}
		}
		else {
			// The value is signal to noise ratio
			if (mValue == 4 || mValue == 5) { // MAGIC_NUMBER
				return "Very bad signal";
			}
			else if (mValue >= 7 && mValue <= 11) { // MAGIC_NUMBER
				return "Somewhat bad signal";
			}
			else if (mValue >= 12 && mValue <= 16) { // MAGIC_NUMBER
				return "Alright signal";
			}
			else if (mValue > 16) { // MAGIC_NUMBER
				return "Good signal";
			}
			else {
				return "No signal";
			}
		}
	}

	@Override
	public final String toString() {
		return getValueText() + " (" + getValue() + ")";
	}

}
