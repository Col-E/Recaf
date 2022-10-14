package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import me.coley.recaf.assemble.suggestions.SuggestionsResults;
import me.coley.recaf.ui.control.VirtualizedContextMenu;
import me.coley.recaf.util.EscapeUtil;
import me.darknet.assembler.parser.Group;
import org.fxmisc.richtext.StyledTextArea;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Suggestion impl for items represented as strings.
 *
 * @author Amejonah
 * @see InfoSuggestion
 * @see MemberInfoSuggestion
 * @see StringMatchSuggestion
 */
public class StringSuggestion implements Suggestion {
	protected final String suggestedText;

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
	void onAction(VirtualizedContextMenu.SelectionActionEvent<Suggestion> e, SuggestionsResults results,
				  int position, Group suggestionGroup, Area area) {
		String input = results.getInput();
		// We should replace any matched text if possible.
		// If the user has done something like 'class.` and we suggest starting from the '.' then we just append.
		boolean replaceMode = input.length() > 1;
		if (replaceMode) {
			int groupStart = suggestionGroup.start().getStart() + 1;
			int groupEnd = suggestionGroup.start().getEnd() + 1;
			// We don't want to replace the whole group. Just the newly suggested bits.
			// So find where the suggestion begins, the replace text from there.
			String existingTestInGroup = area.getText(groupStart, groupEnd);
			int suggestStartInExisting = Math.max(0, existingTestInGroup.lastIndexOf(input));
			int replaceStart = groupStart + suggestStartInExisting;
			area.replaceText(replaceStart, groupEnd,
					EscapeUtil.escape(suggestedText));
			area.moveTo(replaceStart + suggestedText.length());
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
