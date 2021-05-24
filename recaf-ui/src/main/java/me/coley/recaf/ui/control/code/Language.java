package me.coley.recaf.ui.control.code;

import java.util.*;

/**
 * A collection of rules that match against a language's feature set and themes to apply distinct
 * styles to each of these rules.
 *
 * @author Geoff Hayward
 * @author Matt Coley
 */
public class Language {
	private final List<Rule> rules;
	private final String name;
	private final boolean wrap;

	/**
	 * @param name
	 * 		Identifier.
	 * @param rules
	 * 		Rules for matching against language features.
	 * @param wrap
	 * 		Should text wrapping be enabled.
	 */
	public Language(String name, List<Rule> rules, boolean wrap) {
		if(name == null)
			throw new IllegalArgumentException("Language name must not be null");
		if(rules == null)
			throw new IllegalArgumentException("Language rule list must not be null");
		this.name = name;
		this.rules = rules;
		this.wrap = wrap;
	}

	/**
	 * @return Identifier.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Rules for matching against language features.
	 */
	public List<Rule> getRules() {
		return rules;
	}

	/**
	 * @return Should text wrapping be enabled.
	 */
	public boolean doWrap() {
		return wrap;
	}


	@Override
	public String toString() {
		return "Language[" + name + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Language language = (Language) o;
		return wrap == language.wrap &&
				rules.equals(language.rules) &&
				name.equals(language.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(rules, name, wrap);
	}
}
