package de.jeisfeld.lifx.app.alarms;

import android.content.Context;

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
import de.jeisfeld.lifx.app.storedcolors.ColorRegistry;
import de.jeisfeld.lifx.app.storedcolors.StoredColor;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.Light;

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
	 * Generate an alarm.
	 *
	 * @param id           The id for storage
	 * @param isActive     The active flag
	 * @param startTime    The alarm start time
	 * @param weekDays     The week days
	 * @param name         The name
	 * @param steps        The steps
	 * @param alarmType    The alarm type.
	 * @param stopSequence The stop sequence.
	 */
	public Alarm(final int id, final boolean isActive, final Date startTime, final Set<Integer> weekDays, final String name, // SUPPRESS_CHECKSTYLE
				 final List<Step> steps, final AlarmType alarmType, final Alarm stopSequence) {
		mId = id;
		mIsActive = isActive;
		mStartTime = startTime;
		mWeekDays = weekDays;
		mName = name;
		mSteps = steps;
		mAlarmType = alarmType;
		mStopSequence = stopSequence;
	}

	/**
	 * Generate a new alarm without id.
	 *
	 * @param isActive     The active flag
	 * @param startTime    The alarm start time
	 * @param weekDays     The week days
	 * @param name         The name
	 * @param steps        The steps
	 * @param alarmType    The alarm type.
	 * @param stopSequence The stop sequence.
	 */
	public Alarm(final boolean isActive, final Date startTime, final Set<Integer> weekDays, final String name, final List<Step> steps,
				 final AlarmType alarmType, final Alarm stopSequence) {
		this(-1, isActive, startTime, weekDays, name, steps, alarmType, stopSequence);
	}

	/**
	 * Generate a new alarm by adding id.
	 *
	 * @param id    The id
	 * @param alarm the base alarm.
	 */
	protected Alarm(final int id, final Alarm alarm) {
		this(id, alarm.isActive(), alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(), alarm.getSteps(),
				alarm.getAlarmType(), alarm.getStopSequence());
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

		List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId);
		mSteps = new ArrayList<>();
		for (Integer stepId : stepIds) {
			if (stepId != null) {
				mSteps.add(new Step(stepId));
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

		List<Step> newSteps = new ArrayList<>();
		Collections.sort(alarm.getSteps());
		for (Step step : alarm.getSteps()) {
			newSteps.add(step.store(alarm.getId()));
		}
		alarm.getSteps().clear();
		alarm.getSteps().addAll(newSteps);

		if (alarm.isActive()) {
			AlarmReceiver.setAlarm(Application.getAppContext(), alarm);
		}
		else {
			AlarmReceiver.cancelAlarm(Application.getAppContext(), alarm.getId());
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
						alarm.getSteps(), alarm.getAlarmType(), null);
				PreferenceUtil.removeIndexedSharedPreference(R.string.key_alarm_stop_sequence_id, alarm.getId());
			}
			else {
				if (stopSequence.getId() < 0) {
					stopSequence = stopSequence.store();
					alarm = new Alarm(alarm.getId(), alarm.isActive(), alarm.getStartTime(), alarm.getWeekDays(), alarm.getName(),
							alarm.getSteps(), alarm.getAlarmType(), stopSequence);
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
			newSteps.add(new Step(step.getDelay(), step.getStoredColorId(), step.getDuration()));
		}
		Alarm newStopSequence = null;
		if (getStopSequence() != null) {
			newStopSequence = getStopSequence().clone(context, context.getString(R.string.alarm_stopsequence_name, newName));
		}
		Alarm alarm = new Alarm(isActive(), getStartTime(), getWeekDays(), newName, newSteps, getAlarmType(), newStopSequence);
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
					context.getString(R.string.alarm_stopsequence_name, newName), stopSequence.getSteps(), AlarmType.STOP_SEQUENCE, null);
		}
		return new Alarm(getId(), isActive(), getStartTime(), getWeekDays(), newName, getSteps(), getAlarmType(), stopSequence);
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
						updatedSteps.add(new Step(step.getId(), step.getDelay() + durationDiff, step.getStoredColorId(), step.getDuration()));
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
						updatedSteps.add(new Step(step.getId(), step.getDelay() + delayDiff,
								step.getStoredColorId(), step.getDuration()));
					}
					else {
						updatedSteps.add(new Step(step.getId(), step.getDelay() + delayDiff, step.getStoredColorId(), step.getDuration()));
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
	}

	/**
	 * Information about the steps for a light.
	 */
	public static class LightSteps {
		/**
		 * The light.
		 */
		private Light mLight;
		/**
		 * The steps for this light.
		 */
		private List<Step> mSteps;

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
		STANDARD(R.drawable.ic_alarmtype_standard),
		/**
		 * Alarm having notification that waits until stopped manually.
		 */
		STOP_MANUALLY(R.drawable.ic_alarmtype_stopmanual),
		/**
		 * Alarm running cyclically until stopped manually.
		 */
		CYCLIC(R.drawable.ic_alarmtype_cyclic),
		/**
		 * Secondary alarm used as stop sequence after stopping an alarm.
		 */
		STOP_SEQUENCE(R.drawable.ic_alarm_stopsequence_off);

		/**
		 * The button resource used for the alarm type.
		 */
		private final int mButtonResource;

		/**
		 * Constructor.
		 *
		 * @param buttonResource The button resource.
		 */
		AlarmType(final int buttonResource) {
			mButtonResource = buttonResource;
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
