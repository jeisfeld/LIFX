package de.jeisfeld.lifx.app.managedevices;

import android.text.Html;

import androidx.core.text.HtmlCompat;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.Group;

/**
 * A holder for devices or groups.
 */
public class DeviceHolder {
	/**
	 * The device.
	 */
	private final Device mDevice;
	/**
	 * The group.
	 */
	private final Group mGroup;
	/**
	 * The id.
	 */
	private final int mId;

	/**
	 * The flag indicating if the device should be shown.
	 */
	private boolean mIsShow;

	/**
	 * Constructor from device.
	 *
	 * @param device The device.
	 * @param id     The id.
	 * @param isShow The flag indicating if the device should be shown.
	 */
	public DeviceHolder(final Device device, final int id, final boolean isShow) {
		mDevice = device;
		mGroup = null;
		mId = id;
		mIsShow = isShow;
	}

	/**
	 * Constructor from group.
	 *
	 * @param group  The group.
	 * @param id     The id.
	 * @param isShow The flag indicating if the device should be shown.
	 */
	public DeviceHolder(final Group group, final int id, final boolean isShow) {
		mDevice = null;
		mGroup = group;
		mId = id;
		mIsShow = isShow;
	}

	/**
	 * Check if it is group.
	 *
	 * @return True for group.
	 */
	public boolean isGroup() {
		return mGroup != null;
	}

	/**
	 * Get the contained group.
	 *
	 * @return the contained group.
	 */
	public Group getGroup() {
		return mGroup;
	}

	/**
	 * Get the contained device.
	 *
	 * @return the contained device.
	 */
	public Device getDevice() {
		return mDevice;
	}

	/**
	 * Get the device id.
	 *
	 * @return The device id.
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Get the flag if the device should be shown.
	 *
	 * @return The flag if the device should be shown.
	 */
	public boolean isShow() {
		return mIsShow;
	}

	/**
	 * Get the label of the device or group.
	 *
	 * @return The label.
	 */
	public CharSequence getLabel() {
		return isGroup()
				? Html.fromHtml("<b>" + getGroup().getGroupLabel() + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
				: getDevice().getLabel();
	}

	/**
	 * Set the show flag.
	 *
	 * @param show The show flag.
	 */
	protected void setShow(final boolean show) {
		mIsShow = show;
		if (isGroup()) {
			getGroup().setParameter(DeviceRegistry.DEVICE_PARAMETER_SHOW, show);
		}
		else {
			getDevice().setParameter(DeviceRegistry.DEVICE_PARAMETER_SHOW, show);
		}
		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_device_show, getId(), show);
	}
}
