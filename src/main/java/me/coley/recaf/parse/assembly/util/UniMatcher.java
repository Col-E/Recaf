package me.coley.recaf.parse.assembly.util;

import java.util.function.Function;

/**
 * Regex wrapper for single-match values.
 *
 * @param <T> Type of value intended to parse.
 *
 * @author Matt
 */
public class UniMatcher<T> extends AbstractMatcher {
	private final Function<String, T> parser;

	public UniMatcher(String patternStr, Function<String, T> parser) {
		super(patternStr);
		this.parser = parser;
	}

	public T get() {
		return parser.apply(getMatcher().group(0));
	}
}
