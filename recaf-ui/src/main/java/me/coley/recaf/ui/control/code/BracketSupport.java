package me.coley.recaf.ui.control.code;

import me.coley.recaf.util.Threads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Adds bracket pair highlighting to a {@link SyntaxArea}.
 * <br>
 * Modified from the RichTextFX bracket highlighting demo.
 *
 * @author Matt Coley
 */
public class BracketSupport {
	private static final String MATCH_STYLE = "bracket-match";
	private static final String BRACKET_PAIRS = "(){}[]<>";
	private static final int MAX_SEARCH_DISTANCE = 50_000;
	private final List<BracketPair> bracketPairs = new ArrayList<>();
	private final SyntaxArea editor;

	/**
	 * @param editor
	 * 		The editor context.
	 */
	public BracketSupport(SyntaxArea editor) {
		this.editor = editor;
		editor.caretPositionProperty()
				.addListener((observable, old, current) -> highlightBracket(current));
	}

	/**
	 * Highlight the matching bracket at the given position.
	 *
	 * @param caret
	 * 		New caret position.
	 */
	private void highlightBracket(int caret) {
		// Clear previously highlighted pair
		clearBrackets();
		// Only highlight if passed caret position is valid
		if (caret < 0 || caret > editor.getLength()) {
			return;
		}
		// Ensure the position is that of a bracket character (handling adjacency)
		if (caret == editor.getLength()) {
			// When adding a character at the end the caret == length(), but we want it to be length() - 1
			// But only if the character is one of the items in the bracket pair
			caret--;
		} else if (BRACKET_PAIRS.contains(editor.getText(caret - 1, caret))) {
			caret--;
		}
		// Search for the other bracket character to close the pair.
		// If found, update the style of the found positions in the document.
		int other = findMatchingBracket(caret);
		if (other >= 0) {
			int start = Math.min(caret, other);
			int end = Math.max(caret, other);
			BracketPair pair = new BracketPair(start, end);
			addHighlight(pair);
			bracketPairs.add(pair);
		}
	}

	/**
	 * Find the matching bracket position.
	 *
	 * @param index
	 * 		Position to start searching from.
	 * 		Should be the position of one of the bracket characters defined in {@link #BRACKET_PAIRS} in the document.
	 *
	 * @return {@code -1} or position of matching bracket.
	 */
	private int findMatchingBracket(int index) {
		char origin = editor.getText(index, index + 1).charAt(0);
		int originPos = BRACKET_PAIRS.indexOf(origin);
		if (originPos < 0)
			return -1;
		// "BRACKET_PAIRS" defined bracket pairs in opening/closing sequences.
		// So +1 will yield the closing bracket for an opening bracket.
		// And -1 will yield the opening bracket for a closing bracket.
		int stepDirection = (originPos % 2 == 0) ? +1 : -1;
		char target = BRACKET_PAIRS.charAt(originPos + stepDirection);
		// Check if the character immediately adjacent to the passed index is the matching bracket
		index += stepDirection;
		if (index < editor.getLength() && editor.getText(index, index + 1).charAt(0) == target) {
			return index;
		}
		// Begin the search in the given direction
		int bracketCount = 1;
		int steps = 0;
		while (index > -1 && index < editor.getLength() && steps < MAX_SEARCH_DISTANCE) {
			char code = editor.getText(index, index + 1).charAt(0);
			// Handle bracket nesting level
			if (code == origin) {
				bracketCount++;
			} else if (code == target) {
				bracketCount--;
			}
			// Check for equality, found match
			if (bracketCount == 0) {
				return index;
			} else {
				steps++;
				index += stepDirection;
			}
		}
		// Not found
		return -1;
	}

	/**
	 * Clear any highlighted bracket pair's highlight.
	 */
	public void clearBrackets() {
		Iterator<BracketPair> iterator = bracketPairs.iterator();
		while (iterator.hasNext()) {
			BracketPair pair = iterator.next();
			removeHighlight(pair);
			iterator.remove();
		}
	}

	/**
	 * @param pair
	 * 		Pair to add highlight for.
	 */
	private void addHighlight(BracketPair pair) {
		modifyStyle(pair.getStart(), Collection::add);
		modifyStyle(pair.getEnd(), Collection::add);
	}

	/**
	 * @param pair
	 * 		Pair to remove highlight for.
	 */
	private void removeHighlight(BracketPair pair) {
		modifyStyle(pair.getStart(), Collection::remove);
		modifyStyle(pair.getEnd(), Collection::remove);
	}

	/**
	 * @param pos
	 * 		Position to modify style of.
	 * @param func
	 * 		Collection function to apply for the {@link #MATCH_STYLE}.
	 * 		Either {@link Collection#add(Object)} or {@link Collection#remove(Object)}}
	 */
	private void modifyStyle(int pos, BiConsumer<Collection<String>, String> func) {
		if (pos < editor.getLength()) {
			String text = editor.getText(pos, pos + 1);
			if (BRACKET_PAIRS.contains(text)) {
				Collection<String> styles = new ArrayList<>(editor.getStyleAtPosition(pos));
				func.accept(styles, MATCH_STYLE);
				// Now, I know in the calling context we're already on the UI thread.
				// But when adding a new character the caret position updates before the UI does.
				// So setting the style would try to set on a range that technically exists but isn't populated.
				// There is no crash in this case, but the style selection won't happen.
				// This all gets resolved if we queue it for the next UI cycle.
				Threads.runFxDelayed(1, () -> {
					if (pos < editor.getLength()) {
						editor.setStyle(pos, pos + 1, styles);
					}
				});
			}
		}
	}
}