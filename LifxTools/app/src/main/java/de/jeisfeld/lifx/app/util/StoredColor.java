package de.jeisfeld.lifx.app.util;

import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;

/**
 * Class holding information about a stored color.
 */
public class StoredColor {
	/**
	 * The color.
	 */
	private final Color mColor;
	/**
	 * The deviceId for the color.
	 */
	private final int mDeviceId;
	/**
	 * The name of the color.
	 */
	private final String mName;
	/**
	 * The id for storage.
	 */
	private final int mId;

	/**
	 * Generate a stored color.
	 *
	 * @param id The id for storage
	 * @param color The color
	 * @param deviceId The device id
	 * @param name The name
	 */
	public StoredColor(final int id, final Color color, final int deviceId, final String name) {
		mColor = color;
		mDeviceId = deviceId;
		mName = name;
		mId = id;
	}

	/**
	 * Generate a new stored color without id.
	 *
	 * @param color The color
	 * @param deviceId The device id
	 * @param name The name
	 */
	public StoredColor(final Color color, final int deviceId, final String name) {
		this(-1, color, deviceId, name);
	}

	/**
	 * Generate a new stored color by adding id.
	 *
	 * @param id The id
	 * @param storedColor the base stored color.
	 */
	public StoredColor(final int id, final StoredColor storedColor) {
		this(id, storedColor.getColor(), storedColor.getDeviceId(), storedColor.getName());
	}

	/**
	 * Get the color.
	 *
	 * @return The color.
	 */
	public Color getColor() {
		return mColor;
	}

	/**
	 * Get the device id for the color.
	 *
	 * @return The device id for the color.
	 */
	public int getDeviceId() {
		return mDeviceId;
	}

	/**
	 * Get the light for the color.
	 *
	 * @return The light for the color.
	 */
	public Light getLight() {
		return (Light) DeviceRegistry.getInstance().getDeviceById(getDeviceId());
	}

	/**
	 * Get the name of the color.
	 *
	 * @return The name of the color.
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Get the id for storage.
	 *
	 * @return The id for storage.
	 */
	public int getId() {
		return mId;
	}

	@Override
	public final String toString() {
		return getId() + ": " + getName() + " - " + getColor() + " - " + (getLight() == null ? getDeviceId() : getLight().getLabel());
	}
}
