package me.coley.recaf.config.impl;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;

/**
 * Options for update policy.
 * 
 * @author Matt
 */
public class ConfUpdate extends Config {
	/**
	 * Enable update checking.
	 */
	@Conf(category = "update", key = "active")
	public boolean active = true;
	/**
	 * Time since last check.
	 */
	@Conf(category = "update", key = "lastcheck")
	public long lastCheck;
	/**
	 * Frequency of checks.
	 */
	@Conf(category = "update", key = "frequency")
	public Frequency frequency = Frequency.DAILY;

	public ConfUpdate() {
		super("rc_update");
		load();
	}

	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		if (type.equals(Frequency.class)) {
			return Json.value(((Frequency) value).name());
		}
		return null;
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		if (type.equals(Frequency.class)) {
			return Frequency.valueOf(value.asString());
		}
		return null;
	}

	/**
	 * @return {@code true} if time since last check is longer than the update
	 *         frequency.
	 */
	public boolean shouldCheck() {
		// check for first-time
		if (lastCheck == 0) {
			lastCheck = System.currentTimeMillis();
			return false;
		}
		// check for valid check interval
		return active && frequency.check(lastCheck);
	}
	
	/**
	 * Static getter.
	 * 
	 * @return ConfDisplay instance.
	 */
	public static ConfUpdate instance() {
		return ConfUpdate.instance(ConfUpdate.class);
	}

	/**
	 * Frequency of updates to check.
	 */
	public enum Frequency {
		ALWAYS(0), DAILY(86_400_000), WEEKLY(7 * 86_400_000);

		private final long time;

		Frequency(long time) {
			this.time = time;
		}

		public boolean check(long lastCheck) {
			return System.currentTimeMillis() - lastCheck > time;
		}
	}
}
