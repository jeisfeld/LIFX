package de.jeisfeld.lifx.app.alarms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
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
	 * The alarm frequency.
	 */
	private final Frequency mFrequency;
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
	 * @param startTime The alarm start time
	 * @param frequency The alarm frequency
	 * @param name      The name
	 * @param steps     The steps
	 */
	public Alarm(final int id, final Date startTime, final Frequency frequency, final String name, final List<Step> steps) {
		mId = id;
		mStartTime = startTime;
		mFrequency = frequency;
		mName = name;
		mSteps = steps;
	}

	/**
	 * Generate a new alarm without id.
	 *
	 * @param startTime The alarm start time
	 * @param frequency The alarm frequency
	 * @param name      The name
	 * @param steps     The steps
	 */
	public Alarm(final Date startTime, final Frequency frequency, final String name, final List<Step> steps) {
		this(-1, startTime, frequency, name, steps);
	}

	/**
	 * Generate a new alarm by adding id.
	 *
	 * @param id    The id
	 * @param alarm the base alarm.
	 */
	public Alarm(final int id, final Alarm alarm) {
		this(id, alarm.getStartTime(), alarm.getFrequency(), alarm.getName(), alarm.getSteps());
	}

	/**
	 * Retrieve an alarm from storage via id.
	 *
	 * @param alarmId The id.
	 */
	protected Alarm(final int alarmId) {
		mId = alarmId;
		mStartTime = new Date(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_start_time, alarmId, 0));
		mFrequency = Frequency.fromOrdinal(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_frequency, alarmId, 0));
		mName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_alarm_name, alarmId);

		List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, alarmId);
		mSteps = new ArrayList<>();
		for (Integer stepId : stepIds) {
			if (stepId != null) {
				mSteps.add(new Step(alarmId, stepId));
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
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_start_time, alarm.getId(), alarm.getStartTime().getTime());
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_alarm_frequency, alarm.getId(), alarm.getFrequency().ordinal());
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_alarm_name, alarm.getId(), alarm.getName());
		List<Step> newSteps = new ArrayList<>();
		for (Step step : alarm.getSteps()) {
			newSteps.add(step.store());
		}
		alarm.getSteps().clear();
		alarm.getSteps().addAll(newSteps);
		return alarm;
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
	 * Get the alarm frequency.
	 *
	 * @return The alarm frequency.
	 */
	public Frequency getFrequency() {
		return mFrequency;
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
		return "[" + getId() + "](" + getName() + ")(" + getStartTime() + ")(" + getFrequency() + ")(" + getSteps() + ")";
	}

	/**
	 * An alarm step.
	 */
	public class Step {
		/**
		 * The id for storage.
		 */
		private final int mId;
		/**
		 * The step delay.
		 */
		private final long mDelay;
		/**
		 * The deviceId.
		 */
		private final int mDeviceId;
		/**
		 * The color.
		 */
		private final Color mColor;
		/**
		 * The alarm steps.
		 */
		private final long mDuration;
		/**
		 * A reference to the alarm.
		 */
		private final int mAlarmId;

		/**
		 * Generate an alarm step.
		 *
		 * @param id       The id for storage
		 * @param alarmId  The corresponding alarm id.
		 * @param delay    the delay
		 * @param deviceId the deviceId
		 * @param color    the color
		 * @param duration the duration
		 */
		public Step(final int id, final int alarmId, final long delay, final int deviceId, final Color color, final long duration) {
			mId = id;
			mAlarmId = alarmId;
			mDelay = delay;
			mDeviceId = deviceId;
			mColor = color;
			mDuration = duration;
		}

		/**
		 * Generate a new alarm step without id.
		 *
		 * @param alarmId  The corresponding alarm id.
		 * @param delay    the delay
		 * @param deviceId the deviceId
		 * @param color    the color
		 * @param duration the duration
		 */
		public Step(final int alarmId, final long delay, final int deviceId, final Color color, final long duration) {
			this(-1, alarmId, delay, deviceId, color, duration);
		}

		/**
		 * Generate a new alarm step by adding id.
		 *
		 * @param id   The id
		 * @param step the base step.
		 */
		public Step(final int id, final Step step) {
			this(id, step.getAlarmId(), step.getDelay(), step.getDeviceId(), step.getColor(), step.getDuration());
		}

		/**
		 * Retrieve an alarm step from storage via id.
		 *
		 * @param alarmId The id of the alarm.
		 * @param stepId  The id of the alarm step.
		 */
		protected Step(final int alarmId, final int stepId) {
			mId = stepId;
			mAlarmId = alarmId;
			mDelay = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_step_delay, stepId, 0);
			mDeviceId = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_alarm_step_device_id, stepId, -1);
			mColor = PreferenceUtil.getIndexedSharedPreferenceColor(R.string.key_alarm_step_color, stepId, Color.OFF);
			mDuration = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_alarm_step_duration, stepId, 0);
		}

		/**
		 * Store this alarm step.
		 *
		 * @return the stored alarm step.
		 */
		public Step store() {
			Step step = this;

			if (getId() < 0) {
				int newId = PreferenceUtil.getSharedPreferenceInt(R.string.key_alarm_step_max_id, 0) + 1;
				PreferenceUtil.setSharedPreferenceInt(R.string.key_alarm_step_max_id, newId);

				List<Integer> stepIds = PreferenceUtil.getIndexedSharedPreferenceIntList(R.string.key_alarm_step_ids, getAlarmId());
				stepIds.add(newId);
				PreferenceUtil.setIndexedSharedPreferenceIntList(R.string.key_alarm_ids, getAlarmId(), stepIds);
				step = new Step(newId, this);
			}
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_step_delay, step.getId(), step.getDelay());
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_alarm_step_device_id, step.getId(), step.getDeviceId());
			PreferenceUtil.setIndexedSharedPreferenceColor(R.string.key_alarm_step_color, step.getId(), step.getColor());
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_alarm_step_duration, step.getId(), step.getDuration());
			return step;
		}

		/**
		 * Get the alarm id.
		 *
		 * @return The alarm id.
		 */
		public int getAlarmId() {
			return mAlarmId;
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
		 * Get the device id.
		 *
		 * @return The device id.
		 */
		public int getDeviceId() {
			return mDeviceId;
		}

		/**
		 * Get the light for the alarm step.
		 *
		 * @return The light for the alarm step.
		 */
		public Light getLight() {
			return (Light) DeviceRegistry.getInstance().getDeviceById(getDeviceId());
		}

		/**
		 * Get the color.
		 *
		 * @return The color.
		 */
		public Color getColor() {
			return mColor;
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
			return "[" + getId() + "](" + getName() + ")(" + getStartTime() + ")(" + getFrequency() + ")(" + getSteps() + ")";
		}
	}

	/**
	 * Alarm frequencies.
	 */
	public enum Frequency {
		/**
		 * Alarm is off.
		 */
		OFF,
		/**
		 * One time alarm.
		 */
		ONE_TIME,
		/**
		 * Weekly alarm.
		 */
		WEEKLY,
		/**
		 * Daily alarm.
		 */
		DAILY;

		/**
		 * Get alarm frequency from its ordinal value.
		 *
		 * @param ordinal The ordinal value.
		 * @return The direction.
		 */
		protected static Frequency fromOrdinal(final int ordinal) {
			for (Frequency frequency : values()) {
				if (ordinal == frequency.ordinal()) {
					return frequency;
				}
			}
			return Frequency.OFF;
		}
	}

}
