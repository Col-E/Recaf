package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.VirtualizedContextMenu;
import me.coley.recaf.ui.util.Lang;
import me.darknet.assembler.parser.Group;
import org.fxmisc.richtext.StyledTextArea;

import javax.annotation.Nonnull;
import java.util.Collection;

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

	@Override
	public <Area extends StyledTextArea<Collection<String>, Collection<String>>>
	void onAction(VirtualizedContextMenu.SelectionActionEvent<Suggestion> e, int position, Group suggestionGroup, Area area) {}
}
