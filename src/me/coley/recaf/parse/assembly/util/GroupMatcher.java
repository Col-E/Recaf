package me.coley.recaf.parse.assembly.util;

import java.util.Map;
import java.util.function.Function;

/**
 * Regex wrapper for grouped-match values.
 *
 * @author Matt
 */
public class GroupMatcher extends AbstractMatcher {
	private final Map<String, Function<String, Object>> parsers;

	/**
	 * @param patternStr
	 * 		Pattern to use in matcher.
	 * @param parsers
	 * 		Map of group names to parser functions.
	 */
	public GroupMatcher(String patternStr, Map<String, Function<String, Object>> parsers) {
		super(patternStr);
		this.parsers = parsers;
	}

	public <T> T get(String group) {
		String value = getMatcher().group(group);
		return (T) parsers.get(group).apply(value);
	}
}
