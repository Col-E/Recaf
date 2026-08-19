package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import regexodus.Pattern;
import software.coley.recaf.util.RegexUtil;

/**
 * Matcher for regular expressions over strings.
 *
 * @author Matt Coley
 */
public final class RegexMatcher implements Matcher<String> {
	private final Pattern pattern;

	/**
	 * @param regex
	 * 		Regular expression. Invalid expressions never match.
	 */
	public RegexMatcher(@Nonnull String regex) {
		pattern = RegexUtil.pattern(regex);
	}

	/**
	 * @param value
	 * 		String to test.
	 *
	 * @return {@code true} when the string contains a match.
	 */
	@Override
	public boolean matches(@Nullable String value) {
		return value != null
				&& pattern.matcher(value).find();
	}
}
