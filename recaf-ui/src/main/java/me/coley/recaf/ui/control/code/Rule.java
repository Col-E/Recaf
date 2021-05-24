package me.coley.recaf.ui.control.code;

import java.util.Objects;

/**
 * Language rule that matches against a feature of a language using regex.
 *
 * @author Geoff Hayward
 * @author Matt Coley
 */
public class Rule {
	private final String name;
	private final String pattern;

	/**
	 * @param name
	 * 		Identifier.
	 * @param pattern
	 * 		Pattern string to match.
	 */
	public Rule(String name, String pattern) {
		if(name == null)
			throw new IllegalArgumentException("Rule name must not be null");
		if(pattern == null)
			throw new IllegalArgumentException("Rule pattern must not be null");
		this.name = name;
		this.pattern = pattern;
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
		return "Rule{" +
				"name='" + name + '\'' +
				", pattern='" + pattern + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Rule rule = (Rule) o;
		return name.equals(rule.name) &&
				pattern.equals(rule.pattern);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, pattern);
	}
}