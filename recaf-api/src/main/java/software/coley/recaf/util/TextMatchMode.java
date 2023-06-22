package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Match implementations for text.
 *
 * @author Matt coley
 */
public enum TextMatchMode {
	/**
	 * String match via equality.
	 */
	EQUALS((key, text) -> text.equals(key)),
	/**
	 * String match via containment.
	 */
	CONTAINS((key, text) -> text.contains(key)),
	/**
	 * String match via same prefix.
	 */
	STARTS_WITH((key, text) -> text.startsWith(key)),
	/**
	 * String match via same suffix.
	 */
	ENDS_WITH((key, text) -> text.endsWith(key)),
	/**
	 * String match via regular expression matching.
	 */
	REGEX((key, text) -> regmatch(text, key));

	private static final Logger logger = Logging.get(TextMatchMode.class);
	private static final List<TextMatchMode> list = Arrays.asList(values());

	/**
	 * @return Modes as list.
	 */
	@Nonnull
	public static List<TextMatchMode> valuesList() {
		return list;
	}

	private static boolean regmatch(String text, String key) {
		try {
			return RegexUtil.getMatcher(key, text).find();
		} catch (Exception ex) {
			logger.error("Invalid pattern: '{}'", key);
			return false;
		}
	}

	private final BiPredicate<String, String> matcher;

	TextMatchMode(BiPredicate<String, String> matcher) {
		this.matcher = matcher;
	}

	/**
	 * @param key
	 * 		Expected pattern.
	 * @param text
	 * 		Text to test for a match.
	 *
	 * @return {@code true} if the given text matches with the given key.
	 */
	public boolean match(String key, String text) {
		return matcher.test(key, text);
	}
}