package de.jeisfeld.lifx.app.ui.manage;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DeviceRegistry;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;

/**
 * Adapter for the RecyclerView that allows to sort devices.
 */
public class ManageDevicesViewAdapter extends RecyclerView.Adapter<ManageDevicesViewAdapter.MyViewHolder>
		implements ManageDevicesItemMoveCallback.ItemTouchHelperContract {
	/**
	 * The list of devices as view data.
	 */
	private final List<Device> mDevices;
	/**
	 * The listener identifying start of drag.
	 */
	private StartDragListener mStartDragListener;
	/**
	 * The list of device ids.
	 */
	private final List<Integer> mDeviceIds;
	/**
	 * A reference to the fragment.
	 */
	private final WeakReference<Fragment> mFragment;
	/**
	 * A reference to the recycler view.
	 */
	private final WeakReference<RecyclerView> mRecyclerView;

	/**
	 * Constructor.
	 *
	 * @param fragment the calling fragment.
	 * @param recyclerView The recycler view.
	 */
	public ManageDevicesViewAdapter(final Fragment fragment, final RecyclerView recyclerView) {
		mDevices = DeviceRegistry.getInstance().getDevices(false);
		mDeviceIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
		mFragment = new WeakReference<>(fragment);
		mRecyclerView = new WeakReference<>(recyclerView);
	}

	/**
	 * Set the listener identifying start of drag.
	 *
	 * @param startDragListener The listener.
	 */
	public void setStartDragListener(final StartDragListener startDragListener) {
		mStartDragListener = startDragListener;
	}

	@Override
	public final MyViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_manage_devices, parent, false);
		return new MyViewHolder(itemView);
	}

	@Override
	public final void onBindViewHolder(final MyViewHolder holder, final int position) {
		final Device device = mDevices.get(position);
		holder.mTitle.setText(device.getLabel());
		holder.mCheckbox.setChecked(!Boolean.FALSE.equals(device.getParameter(DeviceRegistry.DEVICE_PARAMETER_SHOW)));

		holder.mDragHandle.setOnTouchListener((view, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mStartDragListener.requestDrag(holder);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				view.performClick();
			}
			return false;
		});

		holder.mCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			device.setParameter(DeviceRegistry.DEVICE_PARAMETER_SHOW, isChecked);
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_device_show, device.getParameter(DeviceRegistry.DEVICE_ID), isChecked);
		});

		holder.mDeleteButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			if (fragment != null && fragment.getActivity() != null) {
				DialogUtil.displayConfirmationMessage(fragment.getActivity(), new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						DeviceRegistry.getInstance().remove(device);
						mDevices.remove(position);
						mDeviceIds.remove(position);
						RecyclerView recyclerView = mRecyclerView.get();
						if (recyclerView != null) {
							recyclerView.removeViewAt(position);
						}
						notifyItemRemoved(position);
						notifyItemRangeChanged(position, mDevices.size() - position);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				}, null, R.string.button_delete, R.string.message_confirm_delete_light, device.getLabel());
			}
		});
	}

	@Override
	public final int getItemCount() {
		return mDevices.size();
	}

	@Override
	public final void onRowMoved(final int fromPosition, final int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(mDevices, i, i + 1);
				Collections.swap(mDeviceIds, i, i + 1);
			}
		}
		else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(mDevices, i, i - 1);
				Collections.swap(mDeviceIds, i, i - 1);
			}
		}
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_device_ids, mDeviceIds);
		notifyItemMoved(fromPosition, toPosition);
	}

	@Override
	public final void onRowSelected(final MyViewHolder myViewHolder) {
		myViewHolder.mRowView.setBackgroundColor(Color.LTGRAY);

	}

	@Override
	public final void onRowClear(final MyViewHolder myViewHolder) {
		myViewHolder.mRowView.setBackgroundColor(Color.TRANSPARENT);

	}

	/**
	 * The view holder of the items.
	 */
	public class MyViewHolder extends RecyclerView.ViewHolder {
		/**
		 * The whole item.
		 */
		private final View mRowView;
		/**
		 * The title.
		 */
		private final TextView mTitle;
		/**
		 * The image view.
		 */
		private final ImageView mDragHandle;
		/**
		 * The delete button.
		 */
		private final ImageView mDeleteButton;
		/**
		 * The checkbox.
		 */
		private final CheckBox mCheckbox;

		/**
		 * Constructor.
		 *
		 * @param itemView The item view.
		 */
		public MyViewHolder(final View itemView) {
			super(itemView);
			mRowView = itemView;
			mTitle = itemView.findViewById(R.id.textViewDeviceName);
			mDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
			mDeleteButton = itemView.findViewById(R.id.imageViewDelete);
			mCheckbox = itemView.findViewById(R.id.checkboxSelectLight);
		}
	}

	/**
	 * A listener for starting the drag.
	 */
	public interface StartDragListener {
		/**
		 * Method for starting the drag.
		 *
		 * @param viewHolder The view Holder.
		 */
		void requestDrag(RecyclerView.ViewHolder viewHolder);
	}
}
