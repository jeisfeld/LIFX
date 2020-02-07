package de.jeisfeld.lifx.app.ui.home;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import de.jeisfeld.lifx.R;
import de.jeisfeld.lifx.app.ui.home.HomeFragment.NoDeviceCallback;
import de.jeisfeld.lifx.app.util.DeviceRegistry;
import de.jeisfeld.lifx.app.util.DeviceRegistry.DeviceUpdateCallback;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * An adapter for the list of devices in the home fragment.
 */
class DeviceAdapter extends BaseAdapter {
	/**
	 * The list of devices.
	 */
	private final List<Device> mDevices;
	/**
	 * The list of view models.
	 */
	private final List<DeviceViewModel> mViewModels = new ArrayList<>();

	/**
	 * The context.
	 */
	private final Context mContext;
	/**
	 * The lifecycle owner.
	 */
	private final LifecycleOwner mLifeCycleOwner;
	/**
	 * Callback on change of "no device" status.
	 */
	private final NoDeviceCallback mNoDeviceCallback;
	/**
	 * A store for the views.
	 */
	private final List<View> mViews = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param fragment The triggering fragment.
	 * @param callback A calllback called in case of no devices.
	 */
	DeviceAdapter(final Fragment fragment, final NoDeviceCallback callback) {
		super();
		mDevices = DeviceRegistry.getInstance().getDevices();
		mContext = fragment.getContext();
		mLifeCycleOwner = fragment.getViewLifecycleOwner();
		mNoDeviceCallback = callback;
		for (Device device : mDevices) {
			mViewModels.add(new DeviceViewModel(device));
		}

		DeviceRegistry.getInstance().update(new DeviceUpdateCallback() {
			@Override
			public void onDeviceUpdated(final Device device, final boolean isNew, final boolean isMissing) {
				if (isNew) {
					if (mDevices.size() == 0) {
						mNoDeviceCallback.onChange(true);
					}
					addDevice(device);
				}
			}

			@Override
			public void onNoDevicesFound() {
				if (mDevices.size() == 0) {
					mNoDeviceCallback.onChange(false);
				}
			}
		});
	}

	@Override
	public int getCount() {
		return mDevices.size();
	}

	@Override
	public Device getItem(final int position) {
		return mDevices.get(position);
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	@Override
	public synchronized View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view;

		// do not use convertView, as information is stored in the views
		synchronized (mViews) {
			if (mViews.size() > position && mViews.get(position) != null) {
				return mViews.get(position);
			}
		}

		view = LayoutInflater.from(mContext).inflate(R.layout.list_view_home, parent, false);

		final Device device = getItem(position);

		final TextView text = view.findViewById(R.id.textViewHome);
		text.setText(device.getLabel());

		final Button powerButton = view.findViewById(R.id.buttonPower);

		DeviceViewModel model = mViewModels.get(position);
		model.getPower().observe(mLifeCycleOwner, new Observer<Power>() {
			@Override
			public void onChanged(final Power power) {
				if (power == null) {
					powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_undefined));
				}
				else if (power.isOn()) {
					powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_on));
				}
				else {
					powerButton.setBackground(mContext.getDrawable(R.drawable.powerbutton_off));
				}
			}
		});

		powerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				model.togglePower();
			}
		});

		synchronized (mViews) {
			if (mViews.size() > position) {
				mViews.set(position, view);
			}
			else {
				mViews.add(position, view);
			}
		}

		model.checkPower();

		return view;
	}

	/**
	 * Add a device to the list.
	 *
	 * @param device The device to be added.
	 */
	public void addDevice(final Device device) {
		mDevices.add(device);
		mViewModels.add(new DeviceViewModel(device));
		notifyDataSetChanged();
	}
}
