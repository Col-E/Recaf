package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import me.coley.recaf.assemble.suggestions.SuggestionsResults;
import me.coley.recaf.ui.control.VirtualizedContextMenu;
import me.darknet.assembler.parser.Group;
import org.fxmisc.richtext.StyledTextArea;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Suggestion entry for {@link SuggestionsResults}
 *
 * @author Amejonah
 */
public interface Suggestion extends Comparable<Suggestion> {
	@Nonnull
	Node viewAsNode();

	@Nullable
	default Node getGraphic() {
		return null;
	}

	<Area extends StyledTextArea<Collection<String>, Collection<String>>>
	void onAction(VirtualizedContextMenu.SelectionActionEvent<Suggestion> e, SuggestionsResults results,
				  int position, Group suggestionGroup, Area area);
}
