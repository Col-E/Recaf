package me.coley.recaf.ui.control.code;

import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Adds bracket pair highlighting to a {@link SyntaxArea}.
 * <br>
 * Modified from the RichTextFX bracket highlighting demo.
 *
 * @author Matt Coley
 */
public class BracketSupport {
	private static final Logger logger = Logging.get(BracketSupport.class);
	private static final String MATCH_STYLE = "bracket-match";
	private static final String BRACKET_PAIRS = "(){}[]<>";
	private static final String BRACKET_PAIR_PATTERN = "[()\\{\\}\\[\\]<>]";
	private static final int MAX_SEARCH_DISTANCE = 50_000;
	private final List<BracketUpdateListener> listeners = new ArrayList<>();
	private final Set<BracketPair> highlightedBracketPairs = Collections.synchronizedSortedSet(new TreeSet<>());
	private final Set<BracketPair> bracketPairs = Collections.synchronizedSortedSet(new TreeSet<>());
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

	public void textInserted(PlainTextChange change) {
		// Check if inserted text contains any brackets. If so we will need to recompute the pairs.
		String text = change.getInserted();
		if (RegexUtil.getMatcher(BRACKET_PAIR_PATTERN, text).find()) {
			recalculatePairs();
			return;
		}
		// Update pairs ahead of change
		int insertionStart = change.getPosition();
		int insertionEnd = change.getInsertionEnd();
		int inserted = insertionEnd - insertionStart;
		for (BracketPair pair : snapshot(bracketPairs)) {
			if (pair.getStart() > insertionStart) {
				remove(pair);
				add(new BracketPair(pair.getStart() + inserted, pair.getEnd() + inserted));
			} else if (pair.getEnd() > insertionStart) {
				remove(pair);
				add(new BracketPair(pair.getStart(), pair.getEnd() + inserted));
			}
		}
	}


	public void textRemoved(PlainTextChange change) {
		// Check if removed text contains any brackets. If so we will need to recompute the pairs.
		String text = change.getRemoved();
		if (RegexUtil.getMatcher(BRACKET_PAIR_PATTERN, text).find()) {
			recalculatePairs();
			return;
		}
		// Remove/update affected pairs
		int removalStart = change.getPosition();
		int removalEnd = change.getRemovalEnd();
		int removed = removalEnd - removalStart;
		for (BracketPair pair : snapshot(bracketPairs)) {
			// Pairs that end after the start of the removed text
			if (pair.getEnd() > removalStart) {
				// Remove pairs originating in removed region
				if (pair.getStart() > removalStart && pair.getStart() < removalEnd) {
					remove(pair);
				}
				// Offset items after the removed region
				else if (pair.getStart() < removalStart) {
					remove(pair);
					int newEnd = findMatchingBracket(pair.getStart());
					if (newEnd >= 0) {
						add(new BracketPair(pair.getStart(), newEnd));
					}
				}
				// Offset items after the removed region
				else if (pair.getStart() > removalEnd) {
					remove(pair);
					add(new BracketPair(pair.getStart() - removed, pair.getEnd() - removed));
				}
			}
		}
	}

	private void recalculatePairs() {
		// Clear old pairs
		clear();
		// Rescan document for pairs
		String text = editor.getText();
		for (int i = 0; i < BRACKET_PAIRS.length(); i += 2) {
			char c = BRACKET_PAIRS.charAt(i);
			int startPos = 0;
			while (true) {
				int match = text.indexOf(c, startPos);
				// When found, create a bracket pair of the matched opening bracket character
				// to its closing bracket if one exists.
				if (match >= 0) {
					int other = findMatchingBracket(match);
					if (other > 0) {
						int bracketStart = Math.min(match, other);
						int bracketEnd = Math.max(match, other);
						BracketPair pair = new BracketPair(bracketStart, bracketEnd);
						add(pair);
					}
					// Increment start pos
					startPos = match + 1;
				} else {
					// No matches, loop ends
					break;
				}
			}
		}
	}

	/**
	 * Highlight the matching bracket at the given position.
	 *
	 * @param caret
	 * 		New caret position.
	 */
	private void highlightBracket(int caret) {
		// Clear previously highlighted pair
		clearSelectedBrackets();
		// Only highlight if passed caret position is valid
		if (caret < 0 || caret >= editor.getLength()) {
			return;
		}
		for (BracketPair pair : snapshot(bracketPairs)) {
			if (pair.getStart() == caret || pair.getEnd() == caret ||
					pair.getStart() == caret - 1 || pair.getEnd() == caret - 1) {
				addHighlight(pair);
				highlightedBracketPairs.add(pair);
				break;
			}
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
		// Index must be before the last character in the document
		if (index >= editor.getLength() - 1) {
			return -1;
		}
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
	 * @param paragraph
	 * 		Paragraph index, 0-based.
	 *
	 * @return A bracket pair
	 */
	public BracketPair findBracketOnParagraph(int paragraph) {
		// Ignore out of bounds paragraphs
		if (paragraph < 0 || paragraph >= editor.getParagraphs().size())
			return null;
		// Get first bracket that opens on the paragraph
		int start = editor.position(paragraph, 0).toOffset();
		int end = editor.position(paragraph, editor.getParagraphLength(paragraph)).toOffset();
		for (BracketPair pair : snapshot(bracketPairs)) {
			if (pair.getStart() >= start && pair.getStart() <= end) {
				return pair;
			}
		}
		return null;
	}

	/**
	 * Clear any highlighted bracket pair's highlight.
	 */
	public void clearSelectedBrackets() {
		Iterator<BracketPair> iterator = highlightedBracketPairs.iterator();
		while (iterator.hasNext()) {
			BracketPair pair = iterator.next();
			removeHighlight(pair);
			iterator.remove();
		}
	}

	/**
	 * @param listener
	 * 		Listener to receive updates when a bracket is added or removed.
	 */
	public void addBracketListener(BracketUpdateListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove from receiving updates.
	 *
	 * @return {@code true} when the listener was removed.
	 */
	public boolean removeBracketListener(BracketUpdateListener listener) {
		return listeners.remove(listener);
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


	private void add(BracketPair pair) {
		if (!editor.getText(pair.getStart(), pair.getEnd()).contains("\n")) {
			return;
		}
		bracketPairs.add(pair);
		// Update listeners
		int line = offsetToLine(pair.getStart());
		if (line >= 0) {
			listeners.forEach(listener -> listener.onBracketAdded(line, pair));
		}
	}

	private void remove(BracketPair pair) {
		bracketPairs.remove(pair);
		// Update listeners
		int line = offsetToLine(pair.getStart());
		if (line >= 0) {
			listeners.forEach(listener -> listener.onBracketRemoved(line, pair));
		}
	}

	private void clear() {
		for (BracketPair pair : snapshot(bracketPairs)) {
			remove(pair);
		}
	}

	private int offsetToLine(int offset) {
		return offsetToParagraph(offset) + 1;
	}

	private int offsetToParagraph(int offset) {
		if (offset >= 0 && offset < editor.getLength()) {
			return editor.offsetToPosition(offset, TwoDimensional.Bias.Backward).getMajor();
		}
		return -1;
	}

	// TODO: Use this and other safety checks to allow using this from background thread
	private Set<BracketPair> snapshot(Set<BracketPair> bracketPairs) {
		return bracketPairs; //new TreeSet<>(bracketPairs);
	}
}