package me.coley.recaf.parse.assembly.util;

/**
 * Result of RegexToken match attempt.
 *
 * @author Matt
 */
public class MatchResult {
	private final boolean result;
	private final RegexToken failedToken;

	public MatchResult(boolean result, RegexToken failedToken) {
		this.result = result;
		this.failedToken = failedToken;
	}

	/**
	 * @return {@code true} if all tokens successfully matched.
	 */
	public boolean isSuccess() {
		return result;
	}

	/**
	 * @return The token that caused the match to fail. {@code null} if match was a success.
	 */
	public RegexToken getFailedToken() {
		return failedToken;
	}
}
