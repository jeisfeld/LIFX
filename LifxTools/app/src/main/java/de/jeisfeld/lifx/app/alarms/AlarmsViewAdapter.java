package de.jeisfeld.lifx.app.alarms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * Adapter for the RecyclerView that allows to sort alarms.
 */
public class AlarmsViewAdapter extends RecyclerView.Adapter<AlarmsViewAdapter.MyViewHolder>
		implements AlarmsItemMoveCallback.ItemTouchHelperContract {
	/**
	 * The list of alarms as view data.
	 */
	private final List<Alarm> mAlarms;
	/**
	 * The listener identifying start of drag.
	 */
	private StartDragListener mStartDragListener;
	/**
	 * The list of alarm ids.
	 */
	private final List<Integer> mAlarmIds;
	/**
	 * A reference to the fragment.
	 */
	private final WeakReference<Fragment> mFragment;

	/**
	 * Constructor.
	 *
	 * @param fragment the calling fragment.
	 * @param recyclerView The recycler view.
	 */
	public AlarmsViewAdapter(final Fragment fragment, final RecyclerView recyclerView) {
		mAlarms = AlarmRegistry.getInstance().getAlarms();
		mAlarmIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids);
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
		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_alarms, parent, false);
		return new MyViewHolder(itemView);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public final void onBindViewHolder(final MyViewHolder holder, final int position) {
		final Alarm alarm = mAlarms.get(position);
		Fragment fragment0 = mFragment.get();
		final Context context = fragment0 == null ? null : fragment0.getContext();
		holder.mName.setText(alarm.getName());

		holder.mDragHandle.setOnTouchListener((view, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mStartDragListener.requestDrag(holder);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				view.performClick();
			}
			return false;
		});

		holder.mDeleteButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			FragmentActivity activity = fragment == null ? null : fragment.getActivity();
			if (activity != null) {
				DialogUtil.displayConfirmationMessage(activity, new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						AlarmRegistry.getInstance().remove(alarm);
						mAlarms.remove(position);
						mAlarmIds.remove(position);
						notifyItemRemoved(position);
						notifyItemRangeChanged(position, mAlarms.size() - position);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				}, null, R.string.button_delete, R.string.message_confirm_delete_alarm, alarm.getName());
			}
		});

		holder.mEditButton.setOnClickListener(v -> {
			// TODO: Enable editing of alarm
		});
	}

	@Override
	public final int getItemCount() {
		return mAlarms.size();
	}

	@Override
	public final void onRowMoved(final int fromPosition, final int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(mAlarms, i, i + 1);
				Collections.swap(mAlarmIds, i, i + 1);
			}
		}
		else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(mAlarms, i, i - 1);
				Collections.swap(mAlarmIds, i, i - 1);
			}
		}
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_alarm_ids, mAlarmIds);
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
		private final TextView mName;
		/**
		 * The image view.
		 */
		private final ImageView mDragHandle;
		/**
		 * The delete button.
		 */
		private final ImageView mDeleteButton;
		/**
		 * The stored colors button.
		 */
		private final ImageView mEditButton;

		/**
		 * Constructor.
		 *
		 * @param itemView The item view.
		 */
		public MyViewHolder(final View itemView) {
			super(itemView);
			mRowView = itemView;
			mName = itemView.findViewById(R.id.textViewAlarmName);
			mEditButton = itemView.findViewById(R.id.imageViewEdit);
			mDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
			mDeleteButton = itemView.findViewById(R.id.imageViewDelete);
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
