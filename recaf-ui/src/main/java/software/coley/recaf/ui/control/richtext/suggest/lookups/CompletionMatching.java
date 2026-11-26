package software.coley.recaf.ui.control.richtext.suggest.lookups;

import jakarta.annotation.Nonnull;

/**
 * Prefix matching helpers shared by completion providers.
 *
 * @author Matt Coley
 */
public final class CompletionMatching {
	private CompletionMatching() {}

	/**
	 * @param name
	 * 		Name to check for prefix match.
	 * @param partial
	 * 		Partial text to match as a prefix.
	 *
	 * @return {@code true} if the name starts with the partial text, or if the partial text is empty. {@code false} otherwise.
	 */
	public static boolean matchesPrefix(@Nonnull String name, @Nonnull String partial) {
		if (partial.isEmpty())
			return true;
		return name.startsWith(partial);
	}

	/**
	 * @param name
	 * 		Name to check for prefix match.
	 * @param partial
	 * 		Partial text to match as a prefix.
	 *
	 * @return Penalty score for the name based on prefix matching.
	 */
	public static int prefixPenalty(@Nonnull String name, @Nonnull String partial) {
		return name.startsWith(partial) ? 0 : 1;
	}
}
