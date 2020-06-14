package de.jeisfeld.lifx.app.alarms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * Class holding information about an alarm.
 */
public class Alarm {
	/**
	 * The id for storage.
	 */
	private final int mId;
	/**
	 * The alarm start time.
	 */
	private final Date mStartTime;
	/**
	 * The flag indicating if the alarm is active.
	 */
	private final boolean mIsActive;
	/**
	 * The week days on which the alarm is active.
	 */
	private final Set<Integer> mWeekDays;
	/**
	 * The alarm name.
	 */
	private final String mName;
	/**
	 * The alarm steps.
	 */
	private final List<Step> mSteps;

	/**
	 * Generate an alarm.
	 *
	 * @param id        The id for storage
	 * @param isActive  The active flag
	 * @param startTime The alarm start time
	 * @param weekDays  The week days
	 * @param name      The name
	 * @param steps     The steps
	 */
	public Alarm(final int id, final boolean isActive, final Date startTime, final Set<Integer> weekDays, final String name, final List<Step> steps) {
		mId = id;
		mIsActive = isActive;
		mStartTime = startTime;
		mWeekDays = weekDays;
		mName = name;
		mSteps = steps;
	}

	/**
	 * Generate a new alarm without id.
	 *
	 * @param isActive  The active flag
	 * @param startTime The alarm start time
	 * @param weekDays  The week days
	 * @param name      The name
	 * @param steps     The steps
	 */
	public Alarm(final boolean isActive, final Date startTime, final Set<Integer> weekDays, final String name, final List<Step> steps) {
		this(-1, isActive, startTime, weekDays, name, steps);
	}

	/**
	 * Generate a new alarm by adding id.
	 *
	 * @param id    The id
	 * @param alarm the base alarm.
	 */
	public Alarm(final int id, final Alarm alarm) {
		this(id, alarm.isActive(), alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(), alarm.getSteps());
	}

	/**
	 * Retrieve an alarm from storage via id.
	 *
	 * @param alarmId The id.
	 */
	protected Alarm(final int alarmId) {
		mId = alarmId;
		mIsActive = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_alarm_active, alarmId, false);
		mStartTime = new Date(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_start_time, alarmId, 0));
		mWeekDays = new HashSet<>(PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_week_days, alarmId));
		mName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_alarm_name, alarmId);

		List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId);
		mSteps = new ArrayList<>();
		for (Integer stepId : stepIds) {
			if (stepId != null) {
				mSteps.add(new Step(stepId));
			}
		}
	}

	/**
	 * Store this alarm.
	 *
	 * @return the stored alarm.
	 */
	public Alarm store() {
		Alarm alarm = this;
		if (getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_alarm_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_alarm_max_id, newId);

			List<Integer> alarmIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids);
			alarmIds.add(newId);
			PreferenceUtil.setSharedPreferenceIntList(R.string.key_alarm_ids, alarmIds);
			alarm = new Alarm(newId, this);
		}
		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_alarm_active, alarm.getId(), alarm.isActive());
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_start_time, alarm.getId(), alarm.getStartTime().getTime());
		PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_alarm_week_days, alarm.getId(), new ArrayList<>(alarm.getWeekDays()));
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_alarm_name, alarm.getId(), alarm.getName());

		List<Step> newSteps = new ArrayList<>();
		for (Step step : alarm.getSteps()) {
			newSteps.add(step.store(alarm.getId()));
		}
		alarm.getSteps().clear();
		alarm.getSteps().addAll(newSteps);
		return alarm;
	}

	/**
	 * Get the active flag.
	 *
	 * @return True if the alarm is active.
	 */
	public boolean isActive() {
		return mIsActive;
	}

	/**
	 * Get the start time.
	 *
	 * @return The start time.
	 */
	public Date getStartTime() {
		return mStartTime;
	}

	/**
	 * Get the week days.
	 *
	 * @return The week days.
	 */
	public Set<Integer> getWeekDays() {
		return mWeekDays;
	}

	/**
	 * Get the name of the alarm.
	 *
	 * @return The name of the alarm.
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Get the steps.
	 *
	 * @return The steps.
	 */
	public List<Step> getSteps() {
		return mSteps;
	}

	/**
	 * Remove a step from the alarm.
	 *
	 * @param step The step to be removed.
	 */
	public void removeStep(final Step step) {
		getSteps().remove(step);
	}

	/**
	 * Put a step to the alarm.
	 *
	 * @param step The step to be put.
	 */
	public void putStep(final Step step) {
		List<Step> stepsToRemove = new ArrayList<>();
		for (Step oldStep : getSteps()) {
			if (step.getId() == oldStep.getId()) {
				stepsToRemove.add(oldStep);
			}
		}
		getSteps().removeAll(stepsToRemove);
		getSteps().add(step);
	}

	/**
	 * Get the id for storage.
	 *
	 * @return The id for storage.
	 */
	public int getId() {
		return mId;
	}

	@NonNull
	@Override
	public final String toString() {
		return "[" + getId() + "](" + getName() + ")(" + String.format(Locale.getDefault(), "%1$tH:%1$tM", getStartTime()) + ")" + getSteps();
	}

	/**
	 * An alarm step.
	 */
	public static class Step {
		/**
		 * The id for storage.
		 */
		private final int mId;
		/**
		 * The step delay.
		 */
		private final long mDelay;
		/**
		 * The stored color.
		 */
		private final int mStoredColorId;
		/**
		 * The alarm steps.
		 */
		private final long mDuration;

		/**
		 * Generate an alarm step.
		 *
		 * @param id            The id for storage
		 * @param delay         the delay
		 * @param storedColorId The stored color id.
		 * @param duration      the duration
		 */
		public Step(final int id, final long delay, final int storedColorId, final long duration) {
			mId = id;
			mDelay = delay;
			mStoredColorId = storedColorId;
			mDuration = duration;
		}

		/**
		 * Generate a new alarm step without id.
		 *
		 * @param delay         the delay
		 * @param storedColorId The stored color id.
		 * @param duration      the duration
		 */
		public Step(final long delay, final int storedColorId, final long duration) {
			this(-1, delay, storedColorId, duration);
		}

		/**
		 * Generate a new alarm step by adding id.
		 *
		 * @param id   The id
		 * @param step the base step.
		 */
		public Step(final int id, final Step step) {
			this(id, step.getDelay(), step.getStoredColorId(), step.getDuration());
		}

		/**
		 * Retrieve an alarm step from storage via id.
		 *
		 * @param stepId The id of the alarm step.
		 */
		protected Step(final int stepId) {
			mId = stepId;
			mDelay = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_step_delay, stepId, 0);
			mStoredColorId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_step_stored_color_id, stepId, -1);
			mDuration = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_step_duration, stepId, 0);
		}

		/**
		 * Store this alarm step.
		 *
		 * @param alarmId the alarmId where to store the step.
		 * @return the stored alarm step.
		 */
		public Step store(final int alarmId) {
			Step step = this;

			if (getId() < 0) {
				int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_alarm_step_max_id, 0) + 1;
				PreferenceUtil.setSharedPreferenceInt(R.string.key_alarm_step_max_id, newId);
				step = new Step(newId, this);
			}

			List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId);
			if (!stepIds.contains(step.getId())) {
				stepIds.add(step.getId());
				PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId, stepIds);
			}

			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_step_delay, step.getId(), step.getDelay());
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_alarm_step_stored_color_id, step.getId(), step.getStoredColorId());
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_step_duration, step.getId(), step.getDuration());
			return step;
		}

		/**
		 * Get the delay.
		 *
		 * @return The delay
		 */
		public long getDelay() {
			return mDelay;
		}

		/**
		 * Get the stored color id.
		 *
		 * @return The stored color id.
		 */
		public int getStoredColorId() {
			return mStoredColorId;
		}

		/**
		 * Get the stored color for the alarm step.
		 *
		 * @return The stored color for the alarm step.
		 */
		public StoredColor getStoredColor() {
			return ColorRegistry.getInstance().getStoredColor(getStoredColorId());
		}

		/**
		 * Get the duration.
		 *
		 * @return The duration
		 */
		public long getDuration() {
			return mDuration;
		}

		/**
		 * Get the id for storage.
		 *
		 * @return The id for storage.
		 */
		public int getId() {
			return mId;
		}

		@NonNull
		@Override
		public final String toString() {
			return "[" + getId() + "](" + getDelay() + ")(" + getStoredColor() + ")(" + getDuration() + ")";
		}
	}
}