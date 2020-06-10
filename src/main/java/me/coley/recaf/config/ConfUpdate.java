package me.coley.recaf.config;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import me.coley.recaf.util.LangUtil;

import java.util.concurrent.TimeUnit;

/**
 * Private configuration that are intended for user-access.
 *
 * @author Matt
 */
public class ConfUpdate extends Config {
	/**
	 * Enable update checking.
	 */
	@Conf("update.active")
	public boolean active = true;
	/**
	 * Time since last check.
	 */
	@Conf("update.lastcheck")
	public long lastCheck;
	/**
	 * Frequency of checks.
	 */
	@Conf("update.frequency")
	public Frequency frequency = Frequency.DAILY;

	ConfUpdate() {
		super("update");
	}

	@Override
	public void loadType(FieldWrapper field, Class<?> type, JsonValue value) {
		if (type.equals(Frequency.class)) {
			field.set(Frequency.valueOf(value.asString()));
		}
	}

	@Override
	public void saveType(FieldWrapper field, Class<?> type, Object value, JsonObject json) {
		if (type.equals(Frequency.class)) {
			String name = field.key();
			json.add(name, ((Frequency) value).name());
		}
	}

	@Override
	public boolean supported(Class<?> type) {
		return type.equals(Frequency.class);
	}

	/**
	 * @return {@code true} if the requirements are not met to check for an update.
	 */
	public boolean shouldSkip() {
		return !active || !frequency.check(lastCheck);
	}

	/**
	 * Frequency of updates to check.
	 */
	public enum Frequency {
		ALWAYS(0), DAILY(TimeUnit.DAYS.toMillis(1)), WEEKLY(TimeUnit.DAYS.toMillis(7));

		private final long time;

		Frequency(long time) {
			this.time = time;
		}

		/**
		 * @param lastCheck
		 * 		Last checked time.
		 *
		 * @return {@code true} if the time since the last check is beyond the frequency threshold.
		 */
		public boolean check(long lastCheck) {
			return System.currentTimeMillis() - lastCheck > time;
		}

		@Override
		public String toString() {
			return LangUtil.translate("update.frequency." + name().toLowerCase());
		}
	}
}
