package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Resolves the completions available for source text at a caret index.
 *
 * @param <T>
 * 		Completion value type.
 *
 * @author Matt Coley
 */
public interface CompletionResolver<T> {
	/**
	 * @param source
	 * 		Full source text.
	 * @param caret
	 * 		Caret index into the source text.
	 *
	 * @return Completions for the caret position. Empty when the caret is out of range or not in a completable position.
	 */
	@Nonnull
	List<T> resolve(@Nonnull String source, int caret);
}
