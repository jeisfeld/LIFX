package de.jeisfeld.lifx.app.managedevices;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.AlarmRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsFragment;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter.MultizoneOrientation;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Device;
import de.jeisfeld.lifx.lan.MultiZoneLight;

/**
 * Adapter for the RecyclerView that allows to sort devices.
 */
public class ManageDevicesViewAdapter extends RecyclerView.Adapter<ManageDevicesViewAdapter.MyViewHolder>
		implements ManageDevicesItemMoveCallback.ItemTouchHelperContract {
	/**
	 * The list of devices as view data.
	 */
	private final List<DeviceHolder> mDevices;
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
	 * Constructor.
	 *
	 * @param fragment     the calling fragment.
	 * @param recyclerView The recycler view.
	 */
	public ManageDevicesViewAdapter(final Fragment fragment, final RecyclerView recyclerView) {
		mDevices = DeviceRegistry.getInstance().getDevices(false);
		mDeviceIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_device_ids);
		mFragment = new WeakReference<>(fragment);
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

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public final void onBindViewHolder(final MyViewHolder holder, final int position) {
		final DeviceHolder deviceHolder = mDevices.get(position);
		Fragment fragment0 = mFragment.get();
		final Context context = fragment0 == null ? null : fragment0.getContext();
		holder.mTitle.setText(deviceHolder.getLabel());
		holder.mCheckbox.setChecked(!Boolean.FALSE.equals(deviceHolder.isShow()));

		holder.mDragHandle.setOnTouchListener((view, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mStartDragListener.requestDrag(holder);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				view.performClick();
			}
			return false;
		});

		holder.mCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> deviceHolder.setShow(isChecked));

		if (!deviceHolder.isGroup() && AlarmRegistry.getInstance().isUsed(deviceHolder.getDevice())) {
			holder.mDeleteButton.setVisibility(View.INVISIBLE);
		}
		else {
			holder.mDeleteButton.setVisibility(View.VISIBLE);
			holder.mDeleteButton.setOnClickListener(v -> {
				Fragment fragment = mFragment.get();
				FragmentActivity activity = fragment == null ? null : fragment.getActivity();
				if (activity != null) {
					DialogUtil.displayConfirmationMessage(activity, dialog -> {
						DeviceRegistry.getInstance().remove(deviceHolder);
						mDevices.remove(position);
						mDeviceIds.remove(position);
						notifyItemRemoved(position);
						notifyItemRangeChanged(position, mDevices.size() - position);
					}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_light, deviceHolder.getLabel());
				}
			});
		}

		if (deviceHolder.isGroup()) {
			holder.mStoredColorsButton.setVisibility(View.GONE);
		}
		else {
			holder.mStoredColorsButton.setOnClickListener(v -> StoredColorsFragment.navigate(mFragment.get(), deviceHolder.getId()));
		}

		holder.mInfoButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			Activity activity = fragment == null ? null : fragment.getActivity();
			if (activity == null) {
				return;
			}

			View view = LayoutInflater.from(activity).inflate(R.layout.dialog_device_info, null);
			((TextView) view.findViewById(R.id.textViewDeviceName)).setText(
					Html.fromHtml(activity.getString(R.string.label_device_name, deviceHolder.getLabel()), Html.FROM_HTML_MODE_COMPACT));

			TextView textViewDetail = view.findViewById(R.id.textViewDetail);
			textViewDetail.setMovementMethod(new ScrollingMovementMethod());
			if (deviceHolder.isGroup()) {
				StringBuilder text = new StringBuilder(activity.getString(R.string.label_devices_in_group));
				for (Device device : DeviceRegistry.getInstance().getDevices(deviceHolder.getId(), false)) {
					text.append(device.getLabel()).append("<br>");
				}
				textViewDetail.setText(Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT));
			}
			else {
				new GetDeviceInformationTask(textViewDetail).execute(deviceHolder.getDevice());
			}

			new AlertDialog.Builder(activity)
					.setTitle(deviceHolder.isGroup() ? R.string.title_dialog_group_info : R.string.title_dialog_device_info)
					.setView(view)
					.setPositiveButton(R.string.button_ok, (dialog, which) -> {
						// do nothing
					})
					.create()
					.show();
		});

		if (!deviceHolder.isGroup() && deviceHolder.getDevice() instanceof MultiZoneLight && context != null) {
			MultiZoneLight device = (MultiZoneLight) deviceHolder.getDevice();
			holder.mMultizoneOrientationButton.setVisibility(View.VISIBLE);
			MultizoneOrientation orientation0 = (MultizoneOrientation) device.getParameter(DeviceRegistry.DEVICE_PARAMETER_MULTIZONE_ORIENTATION);
			holder.mMultizoneOrientationButton.setImageDrawable(ContextCompat.getDrawable(context, orientation0.getButtonResource()));
			holder.mMultizoneOrientationButton.setOnClickListener(v -> {
				MultizoneOrientation orientation =
						(MultizoneOrientation) device.getParameter(DeviceRegistry.DEVICE_PARAMETER_MULTIZONE_ORIENTATION);

				MultizoneOrientation newOrientation;
				switch (orientation) {
				case LEFT_TO_RIGHT:
					newOrientation = MultizoneOrientation.TOP_TO_BOTTOM;
					break;
				case TOP_TO_BOTTOM:
					newOrientation = MultizoneOrientation.RIGHT_TO_LEFT;
					break;
				case RIGHT_TO_LEFT:
					newOrientation = MultizoneOrientation.BOTTOM_TO_TOP;
					break;
				case BOTTOM_TO_TOP:
				default:
					newOrientation = MultizoneOrientation.LEFT_TO_RIGHT;
					break;
				}

				device.setParameter(DeviceRegistry.DEVICE_PARAMETER_MULTIZONE_ORIENTATION, newOrientation);
				holder.mMultizoneOrientationButton.setImageDrawable(ContextCompat.getDrawable(context, newOrientation.getButtonResource()));
				DeviceRegistry.getInstance().addOrUpdate(device);
			});
		}

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
		 * The info button.
		 */
		private final ImageView mInfoButton;
		/**
		 * The delete button.
		 */
		private final ImageView mDeleteButton;
		/**
		 * The stored colors button.
		 */
		private final ImageView mStoredColorsButton;
		/**
		 * The arrow for multizone orientation.
		 */
		private final ImageView mMultizoneOrientationButton;
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
			mInfoButton = itemView.findViewById(R.id.imageViewInfo);
			mDeleteButton = itemView.findViewById(R.id.imageViewDelete);
			mStoredColorsButton = itemView.findViewById(R.id.imageViewStoredColors);
			mMultizoneOrientationButton = itemView.findViewById(R.id.imageViewMultizoneOrientation);
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

	/**
	 * An async task for Getting device information.
	 */
	private static final class GetDeviceInformationTask extends AsyncTask<Device, String, String> {
		/**
		 * The textView where to display device information.
		 */
		private final WeakReference<TextView> mTextView;

		/**
		 * Constructor.
		 *
		 * @param textView The textView where to display device information.
		 */
		private GetDeviceInformationTask(final TextView textView) {
			mTextView = new WeakReference<>(textView);
		}

		@Override
		protected String doInBackground(final Device... devices) {
			return devices[0].getFullInformation("", false) + "\n";
		}

		@Override
		protected void onPostExecute(final String info) {
			String boldInfo = info.replaceAll("^(.*?):", "<b>$1:</b>")
					.replaceAll("\\n(.*?):", "<br><b>$1:</b>");

			TextView textView = mTextView.get();
			if (textView != null) {
				textView.setText(Html.fromHtml(boldInfo, Html.FROM_HTML_MODE_COMPACT));
			}
		}
	}
}
