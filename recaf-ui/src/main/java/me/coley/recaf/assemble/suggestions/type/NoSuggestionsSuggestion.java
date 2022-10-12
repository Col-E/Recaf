package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.util.Lang;

import javax.annotation.Nonnull;

public class NoSuggestionsSuggestion implements Suggestion {

	@Override
	public int compareTo(@Nonnull Suggestion o) {
		return 0;
	}

	@Nonnull
	@Override
	public Node viewAsNode() {
		return new BoundLabel(Lang.getBinding("assembler.suggestions.none"));
	}
}
