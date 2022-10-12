package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;

public class StringMatchSuggestion extends StringSuggestion {
	@Nullable
	private final String input;
	@Nullable
	private final BitSet matchedChars;

	/**
	 * @param input
	 * 		Input typed when auto-completing
	 * @param suggestedText
	 * 		Suggestion text.
	 * @param matchedChars
	 * 		Chars which match the search.
	 */
	public StringMatchSuggestion(@Nullable String input, String suggestedText, @Nullable BitSet matchedChars) {
		super(suggestedText);
		this.input = input;
		this.matchedChars = matchedChars;
	}

	/**
	 * @param suggestedText
	 * 		Suggestion text.
	 * @param input
	 * 		Input typed when auto-completing
	 */
	public StringMatchSuggestion(String input, String suggestedText) {
		this(input, suggestedText, null);
	}


	@Nullable
	public BitSet getMatchedChars() {
		return matchedChars;
	}

	@Nonnull
	@Override
	public Node viewAsNode() {
		// If there is no match to begin with, use the full suggestion
		Node text;
		if (input == null) {
			text = new Label(suggestedText);
		} else if (matchedChars != null) {
			final TextFlow textFlow = new TextFlow();
			text = textFlow;
			boolean highlight = false;
			String temp = "";
			for (int i = 0; i < suggestedText.length(); i++) {
				if (matchedChars.get(i) != highlight) {
					if (!temp.isEmpty()) {
						if (!highlight) textFlow.getChildren().add(new javafx.scene.control.Label(temp));
						else {
							Text highlightedText = new Text(temp);
							highlightedText.setFill(Color.DODGERBLUE);
							textFlow.getChildren().add(highlightedText);
						}
						temp = "";
					}
					highlight = !highlight;
				}
				temp += suggestedText.charAt(i);
			}
			if (!temp.isEmpty()) {
				if (!highlight) textFlow.getChildren().add(new javafx.scene.control.Label(temp));
				else {
					Text highlightedText = new Text(temp);
					highlightedText.setFill(Color.DODGERBLUE);
					textFlow.getChildren().add(highlightedText);
				}
			}
		} else {
			// Put a blue highlight on found match for the completion
			Text inputText = new Text(input);
			inputText.setFill(Color.rgb(0, 175, 255));
			final int endIndex = suggestedText.indexOf(input);
			text = endIndex == -1 ? new Label(suggestedText) :
					endIndex == 0 ?
							new TextFlow(
									inputText,
									new Label(suggestedText.substring(input.length()))
							) :
							new TextFlow(
									new Label(suggestedText.substring(0, endIndex)),
									inputText,
									new Label(suggestedText.substring(endIndex + input.length()))
							);
		}
		// Populate with icon if possible
		Node graphic = getGraphic();
		if (graphic == null) return text;
		HBox box = new HBox(graphic, text);
		box.setSpacing(10);
		return box;
	}
}
