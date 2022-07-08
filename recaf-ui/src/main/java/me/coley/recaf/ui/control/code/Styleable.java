package me.coley.recaf.ui.control.code;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides lookup for existing CSS rules in a text component.
 *
 * @author Matt Coley
 * @see SyntaxArea
 * @see SyntaxFlow
 */
public interface Styleable {
	/**
	 * @param pos
	 * 		Position in the text.
	 *
	 * @return CSS rules at the given position.
	 */
	Collection<String> getStyleAtPosition(int pos);

	/**
	 * Called to reset the style.
	 *
	 * @return Future for tracking completion of style computations.
	 */
	CompletableFuture<Void> onClearStyle();

	/**
	 * @param start
	 * 		Start offset in the text to apply the given style.
	 * @param sections
	 * 		List of style sections.
	 *
	 * 	@return Future for tracking completion of style computations.
	 */
	CompletableFuture<Void> onApplyStyle(int start, List<LanguageStyler.Section> sections);
}
