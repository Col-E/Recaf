package me.coley.recaf.ui.control.code;

import java.util.Objects;

/**
 * Language rule that matches against a feature of a language using regex.
 *
 * @author Geoff Hayward
 * @author Matt Coley
 */
public class LanguageRule {
	private final String name;
	private final String pattern;
	private final String backtrackStop;
	private final String backtrackTrigger;

	/**
	 * @param name
	 * 		Identifier.
	 * @param pattern
	 * 		Pattern string to match.
	 */
	public LanguageRule(String name, String pattern) {
		this(name, pattern, null, null);
	}

	/**
	 * @param name
	 * 		Identifier.
	 * @param pattern
	 * 		Pattern string to match.
	 * @param backtrackStop
	 * 		Optional literal text to backtrack to.
	 * @param backtrackTrigger
	 * 		Optional literal text to trigger a backtrack.
	 */
	public LanguageRule(String name, String pattern, String backtrackStop, String backtrackTrigger) {
		if (name == null)
			throw new IllegalArgumentException("Rule name must not be null");
		if (pattern == null)
			throw new IllegalArgumentException("Rule pattern must not be null");
		this.name = name;
		this.pattern = pattern;
		this.backtrackStop = backtrackStop;
		this.backtrackTrigger = backtrackTrigger;
	}

	/**
	 * @return Identifier.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Pattern string to match.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * @return Literal text to backtrack to.
	 * For rules that do not need backtracking, this will be {@code null}.
	 */
	public String getBacktrackStop() {
		return backtrackStop;
	}

	/**
	 * @return Literal text to trigger a backtrack.
	 * For rules that do not need backtracking, this will be {@code null}.
	 */
	public String getBacktrackTrigger() {
		return backtrackTrigger;
	}

	/**
	 * @return {@code true} when the rule has a backtrack trigger, and backtrack ending pattern.
	 */
	public boolean requireBacktracking() {
		return getBacktrackStop() != null && getBacktrackTrigger() != null;
	}

	/**
	 * @return Name as proper regex group title.
	 */
	public String getPatternGroupName() {
		return sterilize(name);
	}

	/**
	 * @param name
	 * 		Original name.
	 *
	 * @return Name stripped of invalid identifier characters. Allows the name to be used as a
	 * regex group name.
	 */
	private static String sterilize(String name) {
		return name.replaceAll("[\\W\\d]+", "").toUpperCase();
	}

	@Override
	public String toString() {
		return "LanguageRule{" +
				"name='" + name + '\'' +
				", pattern='" + pattern + '\'' +
				", backtrack='" + backtrackTrigger + " -> " + backtrackStop + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LanguageRule that = (LanguageRule) o;
		return name.equals(that.name) &&
				pattern.equals(that.pattern) &&
				Objects.equals(backtrackStop, that.backtrackStop) &&
				Objects.equals(backtrackTrigger, that.backtrackTrigger);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, pattern, backtrackStop, backtrackTrigger);
	}
}