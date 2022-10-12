package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import javax.annotation.Nonnull;

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

}
