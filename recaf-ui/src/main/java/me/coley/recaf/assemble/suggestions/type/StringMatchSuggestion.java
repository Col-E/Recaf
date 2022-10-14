package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import me.coley.recaf.assemble.suggestions.FuzzySuggestionComparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Suggestion impl for matched strings.
 *
 * @author Amejonah
 */
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

	@Nullable
	public String getInput() {
		return input;
	}

	@Override
	public int compareTo(@Nonnull Suggestion o) {
		return o instanceof StringMatchSuggestion ? compareTo(((StringMatchSuggestion) o)) : super.compareTo(o);
	}

	public int compareTo(@Nonnull StringMatchSuggestion o) {
		if (this == o)
			return 0;
		if (matchedChars == null || o.matchedChars == null)
			return super.compareTo(o);
		List<Integer> thisProximity = proximityCounts(matchedChars);
		List<Integer> otherProximity = proximityCounts(o.matchedChars);
		if (thisProximity.isEmpty() && otherProximity.isEmpty()) return 0;
		int result = Boolean.compare(!thisProximity.isEmpty(), !otherProximity.isEmpty());
		if (result != 0)
			return result;
		int thisSum = thisProximity.stream().mapToInt(Integer::intValue).sum();
		int otherSum = otherProximity.stream().mapToInt(Integer::intValue).sum();
		result = Integer.compare(otherSum, thisSum);
		if (result != 0)
			return result;
		// cannot be empty, as we already checked it in above
		// what we could do more is quartil, so we can see which has the most greater "blobs"
		int thisProxMax = thisProximity.stream().mapToInt(Integer::intValue).max().getAsInt();
		int otherProxMax = otherProximity.stream().mapToInt(Integer::intValue).max().getAsInt();
		result = Integer.compare(otherProxMax, thisProxMax);
		if (result != 0)
			return result;
		// fallback fuzzy
		return new FuzzySuggestionComparator(input).compare(getSuggestedText(), o.getSuggestedText());
	}

	private List<Integer> proximityCounts(BitSet matchedChars) {
		if (matchedChars == null || matchedChars.isEmpty())
			return new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		boolean latch = matchedChars.get(0);
		if (latch) counts.add(0);
		for (int i = 1; i < matchedChars.size(); i++) {
			final boolean actualBit = matchedChars.get(i);
			if (latch && actualBit)
				counts.set(counts.size() - 1, counts.get(counts.size() - 1) + 1);
			else if (latch != actualBit) {
				latch = !latch;
				if (latch) counts.add(0);
				else if (counts.get(counts.size() - 1) == 0)
					counts.remove(counts.size() - 1);
			}
		}
		return counts;
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
