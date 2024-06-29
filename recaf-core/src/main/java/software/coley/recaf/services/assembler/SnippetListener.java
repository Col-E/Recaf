package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;

/**
 * Listener for receiving updates when {@link Snippet} entries are added/updated/removed from {@link SnippetManager}.
 *
 * @author Matt Coley
 */
public interface SnippetListener {
	/**
	 * @param snippet
	 * 		Newly added snippet.
	 */
	default void onSnippetAdded(@Nonnull Snippet snippet) {}

	/**
	 * @param old
	 * 		Old snippet instance.
	 * @param current
	 * 		New snippet instance.
	 */
	default void onSnippetModified(@Nonnull Snippet old, @Nonnull Snippet current) {}

	/**
	 * @param snippet
	 * 		Removed snippet.
	 */
	default void onSnippetRemoved(@Nonnull Snippet snippet) {}
}
