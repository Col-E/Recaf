package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import regexodus.Matcher;
import regexodus.Pattern;
import software.coley.recaf.analytics.logging.Logging;

import java.util.HashMap;
import java.util.Map;

/**
 * Some common regular expression functions and cache for compiled patterns.
 *
 * @author Matt Coley
 */
public class RegexUtil {
	private static final Logger logger = Logging.get(RegexUtil.class);
	private static final Map<String, RegexValidation> FAILED_PATTERNS = new HashMap<>();
	private static final Map<String, ThreadLocalPattern> PATTERNS = new HashMap<>();
	private static final ThreadLocalPattern INVALID_PATTERN = new ThreadLocalPattern(new Pattern("^$"));

	/**
	 * @param pattern
	 * 		Pattern to check.
	 *
	 * @return {@code true} when valid. {@code false} otherwise.
	 */
	@Nonnull
	public static RegexValidation validate(@Nonnull String pattern) {
		// Check prior failed patterns
		RegexValidation result = FAILED_PATTERNS.get(pattern);
		if (result == null) {
			try {
				new Pattern(pattern);
				result = RegexValidation.VALID;
			} catch (Throwable t) {
				// Record new invalid pattern.
				result = new RegexValidation(false, t.getMessage());
				FAILED_PATTERNS.put(pattern, result);
			}
		}
		return result;
	}

	/**
	 * @param pattern
	 * 		Pattern to match.
	 * @param text
	 * 		Some text containing a match for the given pattern.
	 *
	 * @return Text matcher.
	 */
	public static Matcher getMatcher(@Nonnull String pattern, @Nonnull String text) {
		return getPattern(pattern).matcher(text);
	}

	/**
	 * Checks if the entire input matches a pattern.
	 *
	 * @param pattern
	 * 		Pattern to match.
	 * @param input
	 * 		Text to match against.
	 *
	 * @return {@code true} when the input is a full match.
	 */
	public static boolean matches(@Nonnull String pattern, @Nonnull String input) {
		return getMatcher(pattern, input).matches();
	}

	/**
	 * Checks if any portion of the input matches a pattern.
	 *
	 * @param pattern
	 * 		Pattern to match.
	 * @param input
	 * 		Text to match against.
	 *
	 * @return {@code true} when any part of the input matches.
	 */
	public static boolean matchesAny(@Nonnull String pattern, @Nonnull String input) {
		return getMatcher(pattern, input).find();
	}

	/**
	 * Creates new {@link Pattern} or gets it from cache.
	 *
	 * @param regex
	 * 		Regular expression text.
	 *
	 * @return Compiled {@link Pattern} of the regular expression.
	 */
	@Nonnull
	public synchronized static Pattern pattern(@Nonnull String regex) {
		return getPattern(regex).pattern;
	}

	/**
	 * Creates new {@link ThreadLocalPattern} or gets it from cache.
	 *
	 * @param regex
	 * 		Regular expression text.
	 *
	 * @return Compiled {@link ThreadLocalPattern} of the regular expression.
	 */
	@Nonnull
	private synchronized static ThreadLocalPattern getPattern(@Nonnull String regex) {
		return PATTERNS.computeIfAbsent(regex, RegexUtil::generate);
	}

	/**
	 * @param regex
	 * 		Regular expression to compile.
	 *
	 * @return Compiled pattern, or {@link #INVALID_PATTERN} if the pattern is invalid.
	 */
	@Nonnull
	private static ThreadLocalPattern generate(@Nonnull String regex) {
		if (FAILED_PATTERNS.containsKey(regex))
			return INVALID_PATTERN;
		try {
			return new ThreadLocalPattern(new Pattern(regex));
		} catch (Throwable t) {
			FAILED_PATTERNS.put(regex, new RegexValidation(false, t.getMessage()));
			logger.error("Invalid regex pattern", t);
			return INVALID_PATTERN;
		}
	}

	public record RegexValidation(boolean valid, @Nullable String message) {

		public static final RegexValidation VALID = new RegexValidation(true, null);
	}

	private static final class ThreadLocalPattern {
		final ThreadLocal<Matcher> matcher = new ThreadLocal<>();
		final Pattern pattern;

		private ThreadLocalPattern(@Nonnull Pattern pattern) {
			this.pattern = pattern;
		}

		@Nonnull
		Matcher matcher(@Nonnull String input) {
			ThreadLocal<Matcher> tlc = this.matcher;
			Matcher matcher = tlc.get();
			if (matcher == null) {
				matcher = pattern.matcher(input);
				tlc.set(matcher);
			} else {
				matcher.flush();
				matcher.setTarget(input);
			}
			return matcher;
		}
	}
}
