package de.jeisfeld.lifx.app.alarms;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;
import de.jeisfeld.lifx.lan.type.Color;

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
	 * The alarm type.
	 */
	private final AlarmType mAlarmType;
	/**
	 * The alarm used as stop sequence.
	 */
	private final Alarm mStopSequence;
	/**
	 * Flag indicating if ringtone volume should be maximized.
	 */
	private final boolean mMaximizeVolume;

	/**
	 * Generate an alarm.
	 *
	 * @param id             The id for storage
	 * @param isActive       The active flag
	 * @param startTime      The alarm start time
	 * @param weekDays       The week days
	 * @param name           The name
	 * @param steps          The steps
	 * @param alarmType      The alarm type.
	 * @param stopSequence   The stop sequence.
	 * @param maximizeVolume Flag indicating if ringtone volume should be maximized.
	 */
	public Alarm(final int id, final boolean isActive, final Date startTime, final Set<Integer> weekDays, final String name, // SUPPRESS_CHECKSTYLE
				 final List<Step> steps, final AlarmType alarmType, final Alarm stopSequence, final boolean maximizeVolume) {
		mId = id;
		mIsActive = isActive;
		mStartTime = startTime;
		mWeekDays = weekDays;
		mName = name;
		mSteps = steps;
		mAlarmType = alarmType;
		mStopSequence = stopSequence;
		mMaximizeVolume = maximizeVolume;
	}

	/**
	 * Generate a new alarm without id.
	 *
	 * @param isActive       The active flag
	 * @param startTime      The alarm start time
	 * @param weekDays       The week days
	 * @param name           The name
	 * @param steps          The steps
	 * @param alarmType      The alarm type.
	 * @param stopSequence   The stop sequence.
	 * @param maximizeVolume Flag indicating if ringtone volume should be maximized.
	 */
	public Alarm(final boolean isActive, final Date startTime, final Set<Integer> weekDays, final String name, // SUPPRESS_CHECKSTYLE
				 final List<Step> steps, final AlarmType alarmType, final Alarm stopSequence, final boolean maximizeVolume) {
		this(-1, isActive, startTime, weekDays, name, steps, alarmType, stopSequence, maximizeVolume);
	}

	/**
	 * Generate a new alarm by adding id.
	 *
	 * @param id    The id
	 * @param alarm the base alarm.
	 */
	protected Alarm(final int id, final Alarm alarm) {
		this(id, alarm.isActive(), alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(), alarm.getSteps(),
				alarm.getAlarmType(), alarm.getStopSequence(), alarm.isMaximizeVolume());
	}

	/**
	 * Retrieve an alarm from storage via id.
	 *
	 * @param alarmId The id.
	 */
	public Alarm(final int alarmId) {
		mId = alarmId;
		mIsActive = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_alarm_active, alarmId, false);
		mStartTime = new Date(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_start_time, alarmId, 0));
		mWeekDays = new HashSet<>(PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_week_days, alarmId));
		mName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_alarm_name, alarmId);
		mAlarmType = AlarmType.fromInt(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_type, alarmId, -1));
		mMaximizeVolume = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_alarm_maximize_volume, alarmId, true);

		List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId);
		mSteps = new ArrayList<>();
		for (Integer stepId : stepIds) {
			if (stepId != null) {
				String uriString = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_alarm_step_ringtone_uri, stepId);
				if (uriString == null) {
					mSteps.add(new Step(stepId));
				}
				else {
					String ringtoneName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_alarm_step_ringtone_name, stepId);
					mSteps.add(new RingtoneStep(stepId, Uri.parse(uriString), ringtoneName));
				}
			}
		}

		int stopSequenceId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_stop_sequence_id, alarmId, -1);
		if (stopSequenceId >= 0) {
			mStopSequence = new Alarm(stopSequenceId);
		}
		else {
			mStopSequence = null;
		}
	}

	/**
	 * Store this alarm.
	 *
	 * @return the stored alarm.
	 */
	protected Alarm store() {
		Alarm alarm = this;
		if (getId() < 0) {
			int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_alarm_max_id, 0) + 1;
			PreferenceUtil.setSharedPreferenceInt(R.string.key_alarm_max_id, newId);

			if (alarm.getAlarmType().isPrimary()) {
				List<Integer> alarmIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids);
				alarmIds.add(newId);
				PreferenceUtil.setSharedPreferenceIntList(R.string.key_alarm_ids, alarmIds);
			}

			alarm = new Alarm(newId, this);
		}

		alarm = alarm.storeStopSequence();

		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_alarm_active, alarm.getId(), alarm.isActive());
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_start_time, alarm.getId(), alarm.getStartTime().getTime());
		PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_alarm_week_days, alarm.getId(), new ArrayList<>(alarm.getWeekDays()));
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_alarm_name, alarm.getId(), alarm.getName());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_alarm_type, alarm.getId(), alarm.getAlarmType().ordinal());
		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_alarm_maximize_volume, alarm.getId(), alarm.isMaximizeVolume());

		List<Step> newSteps = new ArrayList<>();
		Collections.sort(alarm.getSteps());
		for (Step step : alarm.getSteps()) {
			newSteps.add(step.store(alarm.getId()));
		}
		alarm.getSteps().clear();
		alarm.getSteps().addAll(newSteps);

		if (alarm.getAlarmType().isPrimary()) {
			if (alarm.isActive()) {
				AlarmReceiver.setAlarm(Application.getAppContext(), alarm);
			}
			else {
				AlarmReceiver.cancelAlarm(Application.getAppContext(), alarm.getId());
			}
		}

		return alarm;
	}

	/**
	 * Store the stop sequence status.
	 *
	 * @return The alarm with updated stop sequence.
	 */
	private Alarm storeStopSequence() {
		Alarm alarm = this;
		if (alarm.getStopSequence() == null) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_stop_sequence_id, alarm.getId());
		}
		else {
			Alarm stopSequence = alarm.getStopSequence();
			if (stopSequence.getSteps().size() == 0) {
				if (stopSequence.getId() >= 0) {
					AlarmRegistry.getInstance().remove(stopSequence);
				}
				alarm = new Alarm(alarm.getId(), alarm.isActive(), alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(),
						alarm.getSteps(), alarm.getAlarmType(), null, alarm.isMaximizeVolume());
				PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_stop_sequence_id, alarm.getId());
			}
			else {
				if (stopSequence.getId() < 0) {
					stopSequence = stopSequence.store();
					alarm = new Alarm(alarm.getId(), alarm.isActive(), alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(),
							alarm.getSteps(), alarm.getAlarmType(), stopSequence, alarm.isMaximizeVolume());
				}
				else {
					stopSequence.store();
				}
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_alarm_stop_sequence_id, alarm.getId(), stopSequence.getId());
			}
		}
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
	 * Get the alarm type.
	 *
	 * @return The alarm type.
	 */
	public AlarmType getAlarmType() {
		return mAlarmType;
	}

	/**
	 * Get the stop sequence.
	 *
	 * @return The stop sequence.
	 */
	public Alarm getStopSequence() {
		return mStopSequence;
	}

	/**
	 * Get the flag if ringtone volume should be maximized.
	 *
	 * @return true if ringtone volume should be maximized.
	 */
	public boolean isMaximizeVolume() {
		return mMaximizeVolume;
	}

	/**
	 * Toggle the maximizeVolume flag.
	 *
	 * @return The alarm with updated flag.
	 */
	protected Alarm toggleMaximizeVolume() {
		Alarm newAlarm = new Alarm(getId(), isActive(), getStartTime(), getWeekDays(), getName(), getSteps(), getAlarmType(),
				getStopSequence(), !isMaximizeVolume());
		AlarmRegistry.getInstance().addOrUpdate(newAlarm);
		return newAlarm;
	}

	/**
	 * Get a list of LightSteps from the total list of steps.
	 *
	 * @return The lightsteps.
	 */
	protected final List<LightSteps> getLightSteps() {
		Collections.sort(getSteps());
		List<LightSteps> result = new ArrayList<>();
		for (Step step : getSteps()) {
			boolean found = false;
			Light light = step.getStoredColor().getLight();
			for (LightSteps lightSteps : result) {
				if (lightSteps.getLight().equals(light)) {
					found = true;
					lightSteps.getSteps().add(step);
				}
			}
			if (!found) {
				List<Step> newSteps = new ArrayList<>();
				newSteps.add(step);
				result.add(new LightSteps(light, newSteps));
			}
		}
		return result;
	}

	/**
	 * Remove a step from the alarm.
	 *
	 * @param stepId The id of the step to be removed.
	 */
	public void removeStep(final int stepId) {
		getSteps().remove(getSteps().stream().filter(step -> step.getId() == stepId).findFirst().orElse(null));
	}

	/**
	 * Get a clone of the alarm.
	 *
	 * @param context The context.
	 * @param newName the name of the cloned alarm.
	 * @return A clone of the alarm.
	 */
	public Alarm clone(final Context context, final String newName) {
		List<Step> newSteps = new ArrayList<>();
		for (Step step : getSteps()) {
			if (step instanceof RingtoneStep) {
				newSteps.add(new RingtoneStep(step.getDelay(), ((RingtoneStep) step).getRingtoneUri(), step.getDuration()));
			}
			else {
				newSteps.add(new Step(step.getDelay(), step.getStoredColorId(), step.getDuration()));
			}
		}
		Alarm newStopSequence = null;
		if (getStopSequence() != null) {
			newStopSequence = getStopSequence().clone(context, context.getString(R.string.alarm_stopsequence_name, newName));
		}
		Alarm alarm = new Alarm(isActive(), getStartTime(), getWeekDays(), newName, newSteps, getAlarmType(), newStopSequence, isMaximizeVolume());
		return AlarmRegistry.getInstance().addOrUpdate(alarm);
	}

	/**
	 * Update the alarm with changed name.
	 *
	 * @param context the context.
	 * @param newName The new name.
	 * @return The updated alarm.
	 */
	protected Alarm withChangedName(final Context context, final String newName) {
		Alarm stopSequence = getStopSequence();
		if (stopSequence != null) {
			stopSequence = new Alarm(stopSequence.getId(), stopSequence.isActive(), stopSequence.getStartTime(), stopSequence.getWeekDays(),
					context.getString(R.string.alarm_stopsequence_name, newName), stopSequence.getSteps(), AlarmType.STOP_SEQUENCE,
					null, stopSequence.isMaximizeVolume());
		}
		return new Alarm(getId(), isActive(), getStartTime(), getWeekDays(), newName, getSteps(), getAlarmType(), stopSequence, isMaximizeVolume());
	}

	/**
	 * Get the id for storage.
	 *
	 * @return The id for storage.
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Get the total duration of the alarm.
	 *
	 * @return The total duration of the alarm.
	 */
	public long getDuration() {
		long duration = 0;
		for (Step step : getSteps()) {
			duration = Math.max(duration, step.getDelay() + step.getDuration());
		}
		return duration;
	}

	/**
	 * Get the parent alarm for stop sequences.
	 *
	 * @return The parent alarm.
	 */
	public Alarm getParent() {
		for (int parentAlarmId : PreferenceUtil.getSharedPreferenceIntList(R.string.key_alarm_ids)) {
			if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_stop_sequence_id, parentAlarmId, -1) == getId()) {
				return new Alarm(parentAlarmId);
			}
		}
		return null;
	}

	@NonNull
	@Override
	public final String toString() {
		return "[" + getId() + "](" + isActive() + ")(" + getName() + ")(" + String.format(Locale.getDefault(), "%1$tH:%1$tM", getStartTime())
				+ ")(" + getWeekDays().size() + ")(" + getSteps();
	}

	/**
	 * Create a date out of hour and minute, resulting in the next future date with this time.
	 *
	 * @param hour   The hour
	 * @param minute The minute
	 * @return The date
	 */
	public static Date getDate(final int hour, final int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		Calendar now = Calendar.getInstance();
		now.add(Calendar.SECOND, 2 * AlarmReceiver.EARLY_START_SECONDS); // put one-time alarm at least 10 seconds in future
		if (calendar.before(now)) {
			calendar.add(Calendar.DATE, 1);
		}
		return calendar.getTime();
	}

	/**
	 * Update the duration of a step, shifting other steps accordingly.
	 *
	 * @param updatedStep The updated step.
	 */
	protected void updateDuration(final Step updatedStep) {
		List<Step> updatedSteps = new ArrayList<>();
		for (LightSteps lightSteps : getLightSteps()) {
			if (updatedStep.getStoredColor().getLight().equals(lightSteps.getLight())) {
				boolean afterUpdatedStep = false;
				long durationDiff = 0;
				for (Step step : lightSteps.getSteps()) {
					if (step.getId() == updatedStep.getId()) {
						durationDiff = updatedStep.getDuration() - step.getDuration();
						updatedSteps.add(updatedStep);
						afterUpdatedStep = true;
					}
					else if (afterUpdatedStep) {
						updatedSteps.add(step.withDelay(step.getDelay() + durationDiff));
					}
					else {
						updatedSteps.add(step);
					}
				}
			}
			else {
				updatedSteps.addAll(lightSteps.getSteps());
			}
		}
		getSteps().clear();
		getSteps().addAll(updatedSteps);
		Collections.sort(getSteps());
	}

	/**
	 * Update the delay of a step, shifting other steps accordingly.
	 *
	 * @param updatedStep The updated step.
	 */
	protected void updateDelay(final Step updatedStep) {
		List<Step> updatedSteps = new ArrayList<>();
		for (LightSteps lightSteps : getLightSteps()) {
			if (updatedStep.getStoredColor().getLight().equals(lightSteps.getLight())) {
				boolean afterUpdatedStep = false;
				long delayDiff = 0;
				for (Step step : lightSteps.getSteps()) {
					if (step.getId() == updatedStep.getId()) {
						if (afterUpdatedStep) {
							delayDiff -= step.getDuration();
						}
						else {
							updatedSteps.add(updatedStep);
							afterUpdatedStep = true;
							delayDiff = updatedStep.getDelay() - step.getDelay();
						}
					}
					else if (!afterUpdatedStep && step.getDelay() + step.getDuration() > updatedStep.getDelay()) {
						updatedSteps.add(updatedStep);
						afterUpdatedStep = true;
						delayDiff = Math.max(0, updatedStep.getDelay() + updatedStep.getDuration() - step.getDelay());
						updatedSteps.add(step.withDelay(step.getDelay() + delayDiff));
					}
					else {
						updatedSteps.add(step.withDelay(step.getDelay() + delayDiff));
					}
				}
			}
			else {
				updatedSteps.addAll(lightSteps.getSteps());
			}
		}
		getSteps().clear();
		getSteps().addAll(updatedSteps);
		Collections.sort(getSteps());
	}

	/**
	 * An alarm step.
	 */
	public static class Step implements Comparable<Step> {
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
		 * Retrieve an alarm step from storage via id.
		 *
		 * @param stepId The id of the alarm step.
		 */
		protected Step(final int stepId) {
			mId = stepId;
			mDelay = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_step_delay, stepId, 0);
			mStoredColorId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_step_stored_color_id, stepId, 0);
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
				step = new Step(newId, getDelay(), getStoredColorId(), getDuration());
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

		@Override
		public final int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (mDelay ^ (mDelay >>> 32)); // MAGIC_NUMBER
			result = prime * result + (int) (mDuration ^ (mDuration >>> 32)); // MAGIC_NUMBER
			result = prime * result + mId;
			result = prime * result + mStoredColorId;
			return result;
		}

		@Override
		public final boolean equals(final Object obj) {
			if (!(obj instanceof Step)) {
				return false;
			}
			Step other = (Step) obj;
			return mDelay == other.mDelay && mDuration == other.mDuration && mId == other.mId && mStoredColorId == other.mStoredColorId;
		}

		@Override
		public final int compareTo(final Step other) {
			if (getDelay() == other.getDelay()) {
				if (getDuration() == other.getDuration()) {
					return getStoredColor().getLight().getLabel().compareTo(other.getStoredColor().getLight().getLabel());
				}
				else {
					return Long.compare(getDuration(), other.getDuration());
				}
			}
			else {
				return Long.compare(getDelay(), other.getDelay());
			}
		}

		/**
		 * Update the duration of the step.
		 *
		 * @param duration The new duration.
		 * @return The updated step.
		 */
		protected Step withDuration(final long duration) {
			return new Step(getId(), getDelay(), getStoredColorId(), duration);
		}

		/**
		 * Update the delay of the step.
		 *
		 * @param delay The new delay.
		 * @return The updated step.
		 */
		protected Step withDelay(final long delay) {
			return new Step(getId(), delay, getStoredColorId(), getDuration());
		}
	}

	/**
	 * An alarm step representing a ringtone instead of a color.
	 */
	public static class RingtoneStep extends Step {
		/**
		 * The ringtone Uri.
		 */
		private final Uri mRingtoneUri;
		/**
		 * The ringtone name.
		 */
		private String mRingtoneName = null;

		/**
		 * Generate a ringtone alarm step.
		 *
		 * @param id          The id for storage
		 * @param delay       the delay
		 * @param ringtoneUri The ringtone Uri.
		 * @param duration    the duration
		 */
		public RingtoneStep(final int id, final long delay, final Uri ringtoneUri, final long duration) {
			super(id, delay, 0, duration);
			mRingtoneUri = ringtoneUri;
		}

		/**
		 * Generate a ringtone alarm step without id.
		 *
		 * @param delay       the delay
		 * @param ringtoneUri The ringtone Uri.
		 * @param duration    the duration
		 */
		public RingtoneStep(final long delay, final Uri ringtoneUri, final long duration) {
			super(delay, 0, duration);
			mRingtoneUri = ringtoneUri;
		}

		/**
		 * Retrieve a ringtone step from storage via id.
		 *
		 * @param stepId       The id of the alarm step.
		 * @param ringtoneUri  The ringtone Uri.
		 * @param ringtoneName The ringtone name.
		 */
		protected RingtoneStep(final int stepId, final Uri ringtoneUri, final String ringtoneName) {
			super(stepId);
			mRingtoneUri = ringtoneUri;
			mRingtoneName = ringtoneName;
		}

		/**
		 * Get the ringtone Uri.
		 *
		 * @return The ringtone Uri.
		 */
		public Uri getRingtoneUri() {
			return mRingtoneUri;
		}

		/**
		 * Get the ringtone.
		 *
		 * @param context The context.
		 * @return The ringtone.
		 */
		public Ringtone getRingtone(final Context context) {
			Ringtone ringtone = RingtoneManager.getRingtone(context, getRingtoneUri());
			ringtone.setAudioAttributes(new Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
			if (VERSION.SDK_INT >= VERSION_CODES.P) {
				ringtone.setLooping(true);
				ringtone.setVolume(1);
			}
			return ringtone;
		}

		@Override
		public final Step store(final int alarmId) {
			Step step = super.store(alarmId);
			RingtoneStep newStep = new RingtoneStep(step.getId(), step.getDelay(), getRingtoneUri(), step.getDuration());
			PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_alarm_step_ringtone_uri, step.getId(), newStep.getRingtoneUri().toString());
			if (mRingtoneName == null) {
				mRingtoneName = getRingtone(Application.getAppContext()).getTitle(Application.getAppContext());
			}
			PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_alarm_step_ringtone_name, step.getId(), mRingtoneName);
			return newStep;
		}

		@Override
		public final StoredColor getStoredColor() {
			if (mRingtoneName == null) {
				try {
					mRingtoneName = getRingtone(Application.getAppContext()).getTitle(Application.getAppContext());
				}
				catch (Exception e) {
					// ignore
				}
			}
			return new StoredColor(0, Color.OFF, 0, mRingtoneName == null ? "(ERROR)" : mRingtoneName) {
				@Override
				public Light getLight() {
					return DeviceRegistry.getInstance().getRingtoneDummyLight();
				}
			};
		}

		@Override
		protected final RingtoneStep withDuration(final long duration) {
			return new RingtoneStep(getId(), getDelay(), getRingtoneUri(), duration);
		}

		@Override
		protected final RingtoneStep withDelay(final long delay) {
			return new RingtoneStep(getId(), delay, getRingtoneUri(), getDuration());
		}
	}

	/**
	 * Information about the steps for a light.
	 */
	public static class LightSteps {
		/**
		 * The light.
		 */
		private final Light mLight;
		/**
		 * The steps for this light.
		 */
		private final List<Step> mSteps;

		/**
		 * Constructor.
		 *
		 * @param light The light
		 * @param steps The steps for this light
		 */
		public LightSteps(final Light light, final List<Step> steps) {
			mLight = light;
			mSteps = steps;
		}

		/**
		 * Get the light.
		 *
		 * @return The light.
		 */
		public Light getLight() {
			return mLight;
		}

		/**
		 * Get the steps.
		 *
		 * @return The steps.
		 */
		public List<Step> getSteps() {
			return mSteps;
		}
	}

	/**
	 * The alarm types.
	 */
	public enum AlarmType {
		/**
		 * Standard alarm. Runs once and stops on the end.
		 */
		STANDARD(R.drawable.ic_alarmtype_standard, R.string.toast_alarmtype_standard),
		/**
		 * Alarm having notification that waits until stopped manually.
		 */
		STOP_MANUALLY(R.drawable.ic_alarmtype_stopmanual, R.string.toast_alarmtype_stopmanual),
		/**
		 * Alarm running cyclically until stopped manually.
		 */
		CYCLIC(R.drawable.ic_alarmtype_cyclic, R.string.toast_alarmtype_cyclic),
		/**
		 * Secondary alarm used as stop sequence after stopping an alarm.
		 */
		STOP_SEQUENCE(R.drawable.ic_alarm_stopsequence_off, R.string.toast_alarmtype_stopsequence);

		/**
		 * The button resource used for the alarm type.
		 */
		private final int mButtonResource;
		/**
		 * The toast resource used for the alarm type.
		 */
		private final int mToastResource;

		/**
		 * Constructor.
		 *
		 * @param buttonResource The button resource.
		 * @param toastResource  The toat resource.
		 */
		AlarmType(final int buttonResource, final int toastResource) {
			mButtonResource = buttonResource;
			mToastResource = toastResource;
		}

		/**
		 * Get the button resource.
		 *
		 * @return The button resource.
		 */
		public int getButtonResource() {
			return mButtonResource;
		}

		/**
		 * Get the toast resource.
		 *
		 * @return The toast resource.
		 */
		public int getToastResource() {
			return mToastResource;
		}

		/**
		 * Get the Alarm Type from its integer value.
		 *
		 * @param alarmType the alarm type integer value.
		 * @return The tile effect.
		 */
		public static AlarmType fromInt(final int alarmType) {
			for (AlarmType effect : values()) {
				if (effect.ordinal() == alarmType) {
					return effect;
				}
			}
			return AlarmType.STANDARD;
		}

		/**
		 * Get the next primary alarm type.
		 *
		 * @return The next primary alarm type.
		 */
		public AlarmType getNext() {
			AlarmType result = this;
			do {
				result = fromInt((result.ordinal() + 1) % values().length);
			}
			while (!result.isPrimary());
			return result;
		}

		/**
		 * Check if this alarm is primary alarm (and to be listed in list of alarms).
		 *
		 * @return True for primary alarms.
		 */
		public boolean isPrimary() {
			return this != STOP_SEQUENCE;
		}

	}

}
