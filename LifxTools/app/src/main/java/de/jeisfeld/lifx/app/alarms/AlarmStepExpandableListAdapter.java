package de.jeisfeld.lifx.app.alarms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.Alarm.LightSteps;
import de.jeisfeld.lifx.app.alarms.Alarm.Step;
import de.jeisfeld.lifx.app.storedcolors.StoredColorsViewAdapter;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.lan.Light;

/**
 * Adapter for the expandable list of alarm steps.
 */
public class AlarmStepExpandableListAdapter extends BaseExpandableListAdapter {
	/**
	 * The context.
	 */
	private FragmentActivity mActivity;
	/**
	 * The alarm.
	 */
	private Alarm mAlarm;
	/**
	 * The list of lightSteps.
	 */
	private List<LightSteps> mLightStepsList;
	/**
	 * The initial expanding status.
	 */
	private Map<Light, Boolean> mInitialExpandingStatus;
	/**
	 * Reference to the parent view.
	 */
	private WeakReference<ExpandableListView> mParent = new WeakReference<>(null);

	/**
	 * Constructor.
	 *
	 * @param activity               The activity.
	 * @param alarm                  The alarm (if already saved)
	 * @param initialExpandingStatus The initial expanding status
	 */
	protected AlarmStepExpandableListAdapter(final FragmentActivity activity, final Alarm alarm,
											 final Map<Light, Boolean> initialExpandingStatus) {
		mActivity = activity;
		mAlarm = alarm;
		mLightStepsList = alarm.getLightSteps();
		mInitialExpandingStatus = initialExpandingStatus;
	}

	/**
	 * Get the expanding status of the lights.
	 *
	 * @return The expanding status of the lights.
	 */
	protected Map<Light, Boolean> getExpandingStatus() {
		ExpandableListView parent = mParent.get();
		Map<Light, Boolean> result = new HashMap<>();
		if (parent != null) {
			for (int groupPosition = 0; groupPosition < getGroupCount(); groupPosition++) {
				LightSteps lightSteps = getGroup(groupPosition);
				result.put(lightSteps.getLight(), parent.isGroupExpanded(groupPosition));
			}
		}
		return result;
	}

	@Override
	public final int getGroupCount() {
		return mLightStepsList.size();
	}

	@Override
	public final int getChildrenCount(final int groupPosition) {
		return getGroup(groupPosition).getSteps().size();
	}

	@Override
	public final LightSteps getGroup(final int groupPosition) {
		return mLightStepsList.get(groupPosition);
	}

	@Override
	public final Step getChild(final int groupPosition, final int childPosition) {
		return getGroup(groupPosition).getSteps().get(childPosition);
	}

	@Override
	public final long getGroupId(final int groupPosition) {
		return groupPosition;
	}

	@Override
	public final long getChildId(final int groupPosition, final int childPosition) {
		return childPosition;
	}

	@Override
	public final boolean hasStableIds() {
		return false;
	}

	@Override
	public final View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent) {
		mParent = new WeakReference<>((ExpandableListView) parent);
		LightSteps lightSteps = getGroup(groupPosition);
		View view = convertView;
		if (convertView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			assert layoutInflater != null;
			view = layoutInflater.inflate(R.layout.list_view_alarm_lights, parent, false);
		}

		TextView listTitleTextView = (TextView) view.findViewById(R.id.textViewDeviceName);
		listTitleTextView.setText(lightSteps.getLight().getLabel());

		// workaround for maintaining expending status after reordering lights due to adding light
		Boolean initialExpandingStatus = mInitialExpandingStatus.get(lightSteps.getLight());
		if (mInitialExpandingStatus != null && initialExpandingStatus != null) {
			if (initialExpandingStatus) {
				((ExpandableListView) parent).expandGroup(groupPosition);
			}
			else {
				((ExpandableListView) parent).collapseGroup(groupPosition);
			}
			mInitialExpandingStatus.remove(lightSteps.getLight());
		}

		return view;
	}

	@Override
	public final View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView,
								   final ViewGroup parent) {
		Step step = getChild(groupPosition, childPosition);
		View view = convertView;
		if (convertView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			assert layoutInflater != null;
			view = layoutInflater.inflate(R.layout.list_view_alarm_steps, parent, false);
		}

		((ImageView) view.findViewById(R.id.imageViewStoredColor))
				.setImageDrawable(StoredColorsViewAdapter.getButtonDrawable(mActivity, step.getStoredColor()));
		((TextView) view.findViewById(R.id.textViewStartTime)).setText(AlarmStepConfigurationFragment.getDelayString(step.getDelay()));
		((TextView) view.findViewById(R.id.textViewEndTime)).setText(
				AlarmStepConfigurationFragment.getDelayString(step.getDelay() + step.getDuration()));
		((TextView) view.findViewById(R.id.textViewStoredColorName)).setText(step.getStoredColor().getName());

		view.findViewById(R.id.imageViewDelete).setOnClickListener(v ->
				DialogUtil.displayConfirmationMessage(mActivity, new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						if (mAlarm != null) {
							mAlarm.removeStep(step);
						}
						AlarmRegistry.getInstance().remove(step, mAlarm.getId());
						getGroup(groupPosition).getSteps().remove(step);
						if (getGroup(groupPosition).getSteps().size() == 0) {
							mLightStepsList.remove(groupPosition);
						}
						notifyDataSetChanged();
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_alarm_step));

		view.setOnClickListener(v -> AlarmStepConfigurationFragment.navigate(mActivity, mAlarm.getId(), step.getId()));

		return view;
	}

	@Override
	public final boolean isChildSelectable(final int groupPosition, final int childPosition) {
		return false;
	}
}
