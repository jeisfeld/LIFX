package de.jeisfeld.lifx.lan;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * A group of devices.
 */
public class Group {
	/**
	 * The group GUID.
	 */
	private final byte[] mGroupId;
	/**
	 * The group label.
	 */
	private final String mGroupLabel;
	/**
	 * The update time.
	 */
	private final Date mUpdateTime;

	/**
	 * Create a Group. This constructor should be taken only if groupId is well known.
	 *
	 * @param groupId the group GUID.
	 * @param groupLabel the group label.
	 * @param updateTime the last group update time.
	 */
	public Group(final byte[] groupId, final String groupLabel, final Date updateTime) {
		if (groupId == null) {
			mGroupId = TypeUtil.generateGuid();
		}
		else if (groupId.length == 16) { // MAGIC_NUMBER
			mGroupId = groupId;
		}
		else {
			throw new InvalidGroupIdException();
		}
		mGroupLabel = groupLabel;
		mUpdateTime = updateTime;
	}

	/**
	 * Create a Group.
	 *
	 * @param groupId the group GUID.
	 * @param groupLabel the group label.
	 */
	public Group(final byte[] groupId, final String groupLabel) {
		this(groupId, groupLabel, new Date());
	}

	/**
	 * Create a new Group.
	 *
	 * @param groupLabel the group label.
	 */
	public Group(final String groupLabel) {
		this(null, groupLabel);
	}

	/**
	 * Retrieve the list of devices of this group.
	 *
	 * @return the list of devices found in this group.
	 */
	public List<Device> getDevices() {
		return LifxLan.getInstance().getDevicesByFilter(device -> equals(device.getGroup()));
	}

	/**
	 * Update the group label.
	 *
	 * @param newGroupLabel The new group label.
	 * @throws IOException Connection issues
	 */
	public void updateLabel(final String newGroupLabel) throws IOException {
		Group updatedGroup = new Group(getGroupId(), newGroupLabel);
		for (Device device : getDevices()) {
			device.setGroup(updatedGroup);
		}
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + TypeUtil.toHex(getGroupId(), false) + "] (" + getGroupLabel() + ")";
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(mGroupId);
	}

	@Override
	public final boolean equals(final Object obj) {
		return obj != null && obj instanceof Group && Arrays.equals(getGroupId(), ((Group) obj).getGroupId());
	}

	/**
	 * Get the group label.
	 *
	 * @return the group label
	 */
	public String getGroupLabel() {
		return mGroupLabel;
	}

	/**
	 * Get the group id.
	 *
	 * @return the group id.
	 */
	public final byte[] getGroupId() {
		return mGroupId;
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
	public static class InvalidGroupIdException extends RuntimeException {
		/**
		 * The default serial version UID.
		 */
		private static final long serialVersionUID = 1L;

		public InvalidGroupIdException() {
			super("Invalid group id - please use valid GUID of length 16");
		}

	}

}
