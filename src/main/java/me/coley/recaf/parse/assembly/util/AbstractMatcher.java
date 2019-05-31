package me.coley.recaf.parse.assembly.util;

import jregex.Matcher;
import jregex.Pattern;

/**
 * Basic abstract regex wrapper.
 *
 * @param <T> Type of value intended to parse.
 *
 * @author Matt
 */
public abstract class AbstractMatcher {
	/**
	 * Pattern to use for finding regions representing the type T.
	 */
	private final Matcher m;

	public AbstractMatcher(String patternStr) {
		m = new Pattern(patternStr).matcher();
	}

	/**
	 * Run matcher on input text.
	 *
	 * @param text
	 * 		Input text.
	 *
	 * @return Success of match.
	 */
	public boolean run(String text) {
		m.setTarget(text);
		return m.find();
	}

	public Matcher getMatcher() {
		return m;
	}
}