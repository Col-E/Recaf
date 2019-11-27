package me.coley.recaf.ui.controls.text.model;

/**
 * Language rule that matches against a feature of a language using regex.
 *
 * @author Geoff Hayward
 * @author Matt
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
			throw new IllegalStateException("Rule name must not be null");
		if(pattern == null)
			throw new IllegalStateException("Rule pattern must not be null");
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
}