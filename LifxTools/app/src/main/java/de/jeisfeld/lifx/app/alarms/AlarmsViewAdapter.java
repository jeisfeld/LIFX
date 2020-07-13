package de.jeisfeld.lifx.app.alarms;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DialogUtil;
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
	 * @param fragment     the calling fragment.
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
		holder.mAlarm = mAlarms.get(position);
		holder.mCheckBoxActive.setChecked(holder.mAlarm.isActive());
		holder.mTextViewAlarmName.setText(holder.mAlarm.getName());
		holder.mTextViewStartTime.setText(String.format(Locale.getDefault(), "%1$tH:%1$tM", holder.mAlarm.getStartTime()));
		holder.mLayoutWeekDays.setVisibility(holder.mAlarm.isActive() ? View.VISIBLE : View.GONE);
		holder.mToggleButtonMonday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.MONDAY));
		holder.mToggleButtonTuesday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.TUESDAY));
		holder.mToggleButtonWednesday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.WEDNESDAY));
		holder.mToggleButtonThursday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.THURSDAY));
		holder.mToggleButtonFriday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.FRIDAY));
		holder.mToggleButtonSaturday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.SATURDAY));
		holder.mToggleButtonSunday.setChecked(holder.mAlarm.getWeekDays().contains(Calendar.SUNDAY));

		holder.mDragHandle.setOnTouchListener((view, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mStartDragListener.requestDrag(holder);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				view.performClick();
			}
			return false;
		});

		holder.mCheckBoxActive.setOnClickListener(v -> {
			saveAlarm(holder);
			holder.mLayoutWeekDays.setVisibility(holder.mAlarm.isActive() ? View.VISIBLE : View.GONE);
		});

		holder.mTextViewAlarmName.setOnClickListener(v -> AlarmConfigurationFragment.navigate(mFragment.get(), holder.mAlarm.getId()));

		holder.mTextViewStartTime.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			FragmentActivity activity = fragment == null ? null : fragment.getActivity();
			if (activity != null) {
				final Calendar calendar = Calendar.getInstance();
				calendar.setTime(holder.mAlarm.getStartTime());
				TimePickerDialog mTimePicker = new TimePickerDialog(activity,
						(timePicker, selectedHour, selectedMinute) -> {
							holder.mTextViewStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
							Alarm newAlarm = new Alarm(holder.mAlarm.getId(), holder.mCheckBoxActive.isChecked(),
									Alarm.getDate(selectedHour, selectedMinute), holder.mAlarm.getWeekDays(), holder.mAlarm.getName(),
									holder.mAlarm.getSteps(), holder.mAlarm.getAlarmType(), holder.mAlarm.getStopSequence(),
									holder.mAlarm.isMaximizeVolume());
							holder.mAlarm = AlarmRegistry.getInstance().addOrUpdate(newAlarm);
						},
						calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
				mTimePicker.setTitle(R.string.title_dialog_alarm_time);
				mTimePicker.show();
			}
		});

		holder.mToggleButtonMonday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));
		holder.mToggleButtonTuesday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));
		holder.mToggleButtonWednesday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));
		holder.mToggleButtonThursday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));
		holder.mToggleButtonFriday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));
		holder.mToggleButtonSaturday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));
		holder.mToggleButtonSunday.setOnCheckedChangeListener((buttonView, isChecked) -> saveAlarm(holder));

		holder.mDeleteButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			FragmentActivity activity = fragment == null ? null : fragment.getActivity();
			if (activity != null) {
				DialogUtil.displayConfirmationMessage(activity, dialog -> {
					AlarmReceiver.cancelAlarm(activity, holder.mAlarm.getId());
					AlarmRegistry.getInstance().remove(holder.mAlarm);
					mAlarms.remove(position);
					mAlarmIds.remove(position);
					notifyItemRemoved(position);
					notifyItemRangeChanged(position, mAlarms.size() - position);
				}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_alarm, holder.mAlarm.getName());
			}
		});
	}

	/**
	 * Save the alarm.
	 *
	 * @param holder the view holder.
	 */
	private void saveAlarm(final MyViewHolder holder) {
		Date startTime = holder.mAlarm.getStartTime();
		if (holder.mAlarm.getWeekDays().size() == 0) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(startTime);
			startTime = Alarm.getDate(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
		}

		Alarm newAlarm = new Alarm(holder.mAlarm.getId(), holder.mCheckBoxActive.isChecked(), startTime, holder.getSelectecWeekDays(),
				holder.mTextViewAlarmName.getText().toString(), holder.mAlarm.getSteps(), holder.mAlarm.getAlarmType(),
				holder.mAlarm.getStopSequence(), holder.mAlarm.isMaximizeVolume());
		holder.mAlarm = AlarmRegistry.getInstance().addOrUpdate(newAlarm);
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
		 * The alarm.
		 */
		private Alarm mAlarm;
		/**
		 * The whole item.
		 */
		private final View mRowView;
		/**
		 * The active flag.
		 */
		private final CheckBox mCheckBoxActive;
		/**
		 * The alarm name.
		 */
		private final TextView mTextViewAlarmName;
		/**
		 * The start time.
		 */
		private final TextView mTextViewStartTime;
		/**
		 * The image view.
		 */
		private final ImageView mDragHandle;
		/**
		 * The delete button.
		 */
		private final ImageView mDeleteButton;
		/**
		 * The layout containing buttons for weekdays.
		 */
		private final LinearLayout mLayoutWeekDays;
		/**
		 * The toggle button for Monday.
		 */
		private final ToggleButton mToggleButtonMonday;
		/**
		 * The toggle button for Tuesday.
		 */
		private final ToggleButton mToggleButtonTuesday;
		/**
		 * The toggle button for Wednesday.
		 */
		private final ToggleButton mToggleButtonWednesday;
		/**
		 * The toggle button for Thursday.
		 */
		private final ToggleButton mToggleButtonThursday;
		/**
		 * The toggle button for Friday.
		 */
		private final ToggleButton mToggleButtonFriday;
		/**
		 * The toggle button for Saturday.
		 */
		private final ToggleButton mToggleButtonSaturday;
		/**
		 * The toggle button for Sunday.
		 */
		private final ToggleButton mToggleButtonSunday;

		/**
		 * Constructor.
		 *
		 * @param itemView The item view.
		 */
		public MyViewHolder(final View itemView) {
			super(itemView);
			mRowView = itemView;
			mCheckBoxActive = itemView.findViewById(R.id.checkBoxActive);
			mTextViewAlarmName = itemView.findViewById(R.id.textViewAlarmName);
			mTextViewStartTime = itemView.findViewById(R.id.textViewStartTime);
			mDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
			mDeleteButton = itemView.findViewById(R.id.imageViewDelete);
			mLayoutWeekDays = itemView.findViewById(R.id.layoutWeekDays);
			mToggleButtonMonday = itemView.findViewById(R.id.toggleButtonMonday);
			mToggleButtonTuesday = itemView.findViewById(R.id.toggleButtonTuesday);
			mToggleButtonWednesday = itemView.findViewById(R.id.toggleButtonWednesday);
			mToggleButtonThursday = itemView.findViewById(R.id.toggleButtonThursday);
			mToggleButtonFriday = itemView.findViewById(R.id.toggleButtonFriday);
			mToggleButtonSaturday = itemView.findViewById(R.id.toggleButtonSaturday);
			mToggleButtonSunday = itemView.findViewById(R.id.toggleButtonSunday);
		}


		/**
		 * Get the selected weekdays.
		 *
		 * @return The selected weekdays.
		 */
		private Set<Integer> getSelectecWeekDays() {
			Set<Integer> weekDays = new HashSet<>();
			if (mToggleButtonMonday.isChecked()) {
				weekDays.add(Calendar.MONDAY);
			}
			if (mToggleButtonTuesday.isChecked()) {
				weekDays.add(Calendar.TUESDAY);
			}
			if (mToggleButtonWednesday.isChecked()) {
				weekDays.add(Calendar.WEDNESDAY);
			}
			if (mToggleButtonThursday.isChecked()) {
				weekDays.add(Calendar.THURSDAY);
			}
			if (mToggleButtonFriday.isChecked()) {
				weekDays.add(Calendar.FRIDAY);
			}
			if (mToggleButtonSaturday.isChecked()) {
				weekDays.add(Calendar.SATURDAY);
			}
			if (mToggleButtonSunday.isChecked()) {
				weekDays.add(Calendar.SUNDAY);
			}
			return weekDays;
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
