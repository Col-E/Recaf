package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.control.VirtualizedContextMenu;
import me.coley.recaf.util.EscapeUtil;
import me.darknet.assembler.parser.Group;
import org.fxmisc.richtext.StyledTextArea;

import javax.annotation.Nonnull;
import java.util.Collection;

public class StringSuggestion implements Suggestion {

	final String suggestedText;

	/**
	 * @param suggestedText
	 * 		Suggestion text.
	 */
	public StringSuggestion(String suggestedText) {
		this.suggestedText = suggestedText;
	}

	/**
	 * @return Suggestion text.
	 */
	public String getSuggestedText() {
		return suggestedText;
	}

	public int compareTo(StringSuggestion o) {
		return suggestedText.compareTo(o.suggestedText);
	}

	@Override
	public int compareTo(@Nonnull Suggestion o) {
		return o instanceof StringSuggestion ? compareTo((StringSuggestion) o) : 0;
	}

	@Nonnull
	@Override
	public Node viewAsNode() {
		Node graphic = getGraphic();
		if (graphic == null) return new Label(suggestedText);
		HBox box = new HBox(graphic, new Label(suggestedText));
		box.setSpacing(10);
		return box;
	}

	@Override
	public <Area extends StyledTextArea<Collection<String>, Collection<String>>>
	void onAction(VirtualizedContextMenu.SelectionActionEvent<Suggestion> e, int position, Group suggestionGroup, Area area) {
		if (e.getInputEvent() != null && suggestionGroup != null
				&& e.getInputEvent() instanceof KeyEvent
				&& ((KeyEvent) e.getInputEvent()).getCode() == KeyCode.TAB) {
			area.replaceText(suggestionGroup.start().getStart() + 1, suggestionGroup.start().getEnd() + 1,
					EscapeUtil.escape(suggestedText));
			area.moveTo(suggestionGroup.start().getStart() + 1 + suggestedText.length());
		} else {
			int length = 0;
			if (this instanceof StringMatchSuggestion) {
				final StringMatchSuggestion stringMatchSuggestion = (StringMatchSuggestion) this;
				if (stringMatchSuggestion.getInput() != null)
					length = stringMatchSuggestion.getInput().length();
			}
			String insert = EscapeUtil.escape(suggestedText.substring(length));
			final int suggestionPlacement = suggestionGroup != null ? suggestionGroup.start().getEnd() + 1 : position;
			area.insertText(suggestionPlacement, insert);
			area.moveTo(suggestionPlacement + insert.length());
		}
	}
}
