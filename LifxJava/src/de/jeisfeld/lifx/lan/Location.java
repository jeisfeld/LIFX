package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * A location of devices.
 */
public class Location implements Serializable {
	/**
	 * The default serializable version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The location GUID.
	 */
	private final byte[] mLocationId;
	/**
	 * The location label.
	 */
	private final String mLocationLabel;
	/**
	 * The update time.
	 */
	private final Date mUpdateTime;

	/**
	 * Create a Location.
	 *
	 * @param locationId the location GUID.
	 * @param locationLabel the location label.
	 * @param updateTime the last location update time.
	 */
	public Location(final byte[] locationId, final String locationLabel, final Date updateTime) {
		if (locationId == null) {
			mLocationId = TypeUtil.generateGuid();
		}
		else if (locationId.length == 16) { // MAGIC_NUMBER
			mLocationId = locationId;
		}
		else {
			throw new InvalidLocationIdException();
		}
		mLocationLabel = locationLabel;
		mUpdateTime = updateTime;
	}

	/**
	 * Create a Location.
	 *
	 * @param locationId the Location GUID.
	 * @param locationLabel the location label.
	 */
	public Location(final byte[] locationId, final String locationLabel) {
		this(locationId, locationLabel, new Date());
	}

	/**
	 * Create a new Location.
	 *
	 * @param locationLabel the location label.
	 */
	public Location(final String locationLabel) {
		this(null, locationLabel);
	}

	/**
	 * Retrieve the list of devices of this group.
	 *
	 * @return the list of devices found in this group.
	 */
	public List<Device> getDevices() {
		return LifxLan.getInstance().getDevicesByFilter(device -> equals(device.getLocation()));
	}

	/**
	 * Update the location label.
	 *
	 * @param newLocationLabel The new location label.
	 * @throws IOException Connection issues
	 */
	public void updateLabel(final String newLocationLabel) throws IOException {
		Location updatedLocation = new Location(getLocationId(), newLocationLabel);
		for (Device device : getDevices()) {
			device.setLocation(updatedLocation);
		}
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + TypeUtil.toHex(getLocationId(), false) + "] (" + getLocationLabel() + ")";
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(mLocationId);
	}

	@Override
	public final boolean equals(final Object obj) {
		return obj instanceof Location && Arrays.equals(getLocationId(), ((Location) obj).getLocationId());
	}

	/**
	 * Get the location label.
	 *
	 * @return the location label
	 */
	public String getLocationLabel() {
		return mLocationLabel;
	}

	/**
	 * Get the location id.
	 *
	 * @return the location id.
	 */
	public final byte[] getLocationId() {
		return mLocationId;
	}

	/**
	 * Get the updateTime.
	 *
	 * @return the updateTime
	 */
	public final Date getUpdateTime() {
		return mUpdateTime;
	}

	/**
	 * An exception for invalid groupIds.
	 */
	public static class InvalidLocationIdException extends RuntimeException {
		/**
		 * The default serial version UID.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 */
		public InvalidLocationIdException() {
			super("Invalid location id - please use valid GUID of length 16");
		}

	}

}
