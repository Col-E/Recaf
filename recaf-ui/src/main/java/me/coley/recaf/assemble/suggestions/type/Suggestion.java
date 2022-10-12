package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import me.coley.recaf.assemble.suggestions.SuggestionsResults;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Suggestion entry for {@link SuggestionsResults}
 *
 * @author Matt Coley
 */
public interface Suggestion extends Comparable<Suggestion> {

	@Nonnull
	Node viewAsNode();

	@Nullable
	default Node getGraphic() {
		return null;
	}
}
