package software.coley.recaf.ui.control.richtext.highlight;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.util.IntRange;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Applies same-word match highlights to editor style spans.
 *
 * @author Matt Coley
 */
public class SelectedWordHighlighting implements EditorComponent {
	private static final Logger logger = Logging.get(SelectedWordHighlighting.class);
	private static final String MATCH_STYLE_CLASS = "selected-word-match";
	private static final int DEFAULT_MAX_MATCHES = 1_000;
	private static final int DEFAULT_MAX_MATCHES_PER_LINE = 50;
	private final int maxMatches;
	private final int maxMatchesPerLine;
	private final Predicate<String> selectionBlacklist;
	private List<IntRange> matchRanges = Collections.emptyList();
	private List<IntRange> styledRanges = Collections.emptyList();
	private Editor editor;
	private CodeArea codeArea;
	private EventStream<Change<Integer>> caretPosEventStream;
	private Consumer<Change<Integer>> caretPosObserver;
	private EventStream<List<PlainTextChange>> textChangeEventStream;
	private Consumer<List<PlainTextChange>> textChangeObserver;
	private ChangeListener<IndexRange> selectionListener;
	private EventHandler<MouseEvent> mousePressedHandler;
	private EventHandler<MouseEvent> mouseReleasedHandler;
	private boolean mouseSelectionInProgress;

	/**
	 * New highlighter with default match limits.
	 */
	public SelectedWordHighlighting() {
		this(DEFAULT_MAX_MATCHES, DEFAULT_MAX_MATCHES_PER_LINE, Collections.emptySet());
	}

	/**
	 * New highlighter with the given match limits.
	 *
	 * @param maxMatches
	 * 		Max match count to allow.
	 * @param maxMatchesPerLine
	 * 		Max match count per line to allow.
	 */
	public SelectedWordHighlighting(int maxMatches, int maxMatchesPerLine) {
		this(maxMatches, maxMatchesPerLine, Collections.emptySet());
	}

	/**
	 * New highlighter with the given selection blacklist and default match limits.
	 *
	 * @param selectionBlacklist
	 * 		Words to exclude from selected-word highlighting.
	 */
	public SelectedWordHighlighting(@Nonnull Collection<String> selectionBlacklist) {
		this(DEFAULT_MAX_MATCHES, DEFAULT_MAX_MATCHES_PER_LINE, selectionBlacklist);
	}

	/**
	 * New highlighter with the given match limits and selection blacklist.
	 *
	 * @param maxMatches
	 * 		Max match count to allow.
	 * @param maxMatchesPerLine
	 * 		Max match count per line to allow.
	 * @param selectionBlacklist
	 * 		Words to exclude from selected-word highlighting.
	 */
	public SelectedWordHighlighting(int maxMatches, int maxMatchesPerLine,
	                               @Nonnull Collection<String> selectionBlacklist) {
		this(maxMatches, maxMatchesPerLine, toBlacklistPredicate(selectionBlacklist));
	}

	/**
	 * New highlighter with the given selection blacklist predicate and default match limits.
	 *
	 * @param selectionBlacklist
	 * 		Predicate returning {@code true} for words to exclude from selected-word highlighting.
	 */
	public SelectedWordHighlighting(@Nonnull Predicate<String> selectionBlacklist) {
		this(DEFAULT_MAX_MATCHES, DEFAULT_MAX_MATCHES_PER_LINE, selectionBlacklist);
	}

	/**
	 * New highlighter with the given match limits and selection blacklist predicate.
	 *
	 * @param maxMatches
	 * 		Max match count to allow.
	 * @param maxMatchesPerLine
	 * 		Max match count per line to allow.
	 * @param selectionBlacklist
	 * 		Predicate returning {@code true} for words to exclude from selected-word highlighting.
	 */
	public SelectedWordHighlighting(int maxMatches, int maxMatchesPerLine,
	                               @Nonnull Predicate<String> selectionBlacklist) {
		this.maxMatches = maxMatches;
		this.maxMatchesPerLine = maxMatchesPerLine;
		this.selectionBlacklist = selectionBlacklist;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		// Skip if already installed on this editor.
		if (this.editor == editor)
			return;

		// Uninstall from any previous editor.
		if (this.editor != null)
			uninstall(this.editor);

		// Record the editor we're installed to.
		this.editor = editor;
		codeArea = editor.getCodeArea();

		// Register listeners to update highlights when the caret position or selection changes.
		caretPosObserver = change -> {
			if (!editor.isTyping() && codeArea.getSelection().getLength() == 0)
				refreshSelectedWordHighlights();
		};
		caretPosEventStream = editor.getCaretPosEventStream();
		caretPosEventStream.addObserver(caretPosObserver);
		selectionListener = (ob, old, cur) -> {
			if (!editor.isTyping())
				refreshSelectedWordHighlights();
		};
		codeArea.selectionProperty().addListener(selectionListener);
		// Restyling while RichTextFX is processing a mouse selection can reset the drag anchor.
		mousePressedHandler = event -> {
			if (event.getButton() == MouseButton.PRIMARY)
				mouseSelectionInProgress = true;
		};
		mouseReleasedHandler = event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				mouseSelectionInProgress = false;
				refreshSelectedWordHighlights();
			}
		};
		codeArea.addEventFilter(MouseEvent.MOUSE_PRESSED, mousePressedHandler);
		codeArea.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedHandler);
		textChangeObserver = this::updateSelectedWordHighlights;
		textChangeEventStream = editor.getTextChangeEventStream()
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.MEDIUM_DELAY_MS));
		textChangeEventStream.addObserver(textChangeObserver);

		// Do an initial refresh to highlight any pre-existing selected word.
		updateMatches(editor.getText(), codeArea.getSelectedText(), codeArea.getCaretPosition());
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		if (this.editor != editor)
			return;

		// Remove any existing match styling from the editor.
		List<IntRange> styledRanges = this.styledRanges;
		matchRanges = Collections.emptyList();
		this.styledRanges = Collections.emptyList();
		restyleSelectedWordHighlights(styledRanges);

		// Unregister listeners.
		if (caretPosEventStream != null && caretPosObserver != null)
			caretPosEventStream.removeObserver(caretPosObserver);
		if (textChangeEventStream != null && textChangeObserver != null)
			textChangeEventStream.removeObserver(textChangeObserver);
		if (codeArea != null && selectionListener != null)
			codeArea.selectionProperty().removeListener(selectionListener);
		if (codeArea != null && mousePressedHandler != null)
			codeArea.removeEventFilter(MouseEvent.MOUSE_PRESSED, mousePressedHandler);
		if (codeArea != null && mouseReleasedHandler != null)
			codeArea.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedHandler);
		caretPosEventStream = null;
		caretPosObserver = null;
		textChangeEventStream = null;
		textChangeObserver = null;
		selectionListener = null;
		mousePressedHandler = null;
		mouseReleasedHandler = null;
		mouseSelectionInProgress = false;

		// Clear out references to the editor.
		codeArea = null;
		this.editor = null;
	}

	/**
	 * @param text
	 * 		Current editor text.
	 * @param selectedText
	 * 		Current selected text.
	 * @param caret
	 * 		Current caret position.
	 *
	 * @return {@code true} when match ranges changed.
	 */
	public boolean refresh(@Nonnull String text, @Nullable String selectedText, int caret) {
		return !refreshAndGetAffectedRanges(text, selectedText, caret).isEmpty();
	}

	/**
	 * @param text
	 * 		Current editor text.
	 * @param selectedText
	 * 		Current selected text.
	 * @param caret
	 * 		Current caret position.
	 *
	 * @return Ranges that either had or now have selected-word match styling.
	 */
	@Nonnull
	public List<IntRange> refreshAndGetAffectedRanges(@Nonnull String text, @Nullable String selectedText, int caret) {
		List<IntRange> updatedRanges = findMatchRanges(text, selectedText, caret);
		if (matchRanges.equals(updatedRanges) && styledRanges.equals(updatedRanges))
			return Collections.emptyList();

		List<IntRange> affectedRanges = mergeRanges(styledRanges, updatedRanges);
		matchRanges = updatedRanges;
		styledRanges = updatedRanges;
		return affectedRanges;
	}

	/**
	 * @param spans
	 * 		Original style spans.
	 *
	 * @return Style spans without the selected-word match styling.
	 */
	@Nonnull
	public StyleSpans<Collection<String>> removeHighlightStyle(@Nonnull StyleSpans<Collection<String>> spans) {
		StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		boolean changed = false;
		for (StyleSpan<Collection<String>> span : spans) {
			Collection<String> style = span.getStyle();
			if (style.contains(MATCH_STYLE_CLASS)) {
				List<String> reducedStyles = new ArrayList<>(style);
				reducedStyles.remove(MATCH_STYLE_CLASS);
				builder.add(reducedStyles, span.getLength());
				changed = true;
			} else {
				builder.add(style, span.getLength());
			}
		}
		if (!changed)
			return spans;
		return builder.create();
	}

	/**
	 * @param text
	 * 		Current editor text.
	 * @param selectedText
	 * 		Current selected text.
	 * @param caret
	 * 		Current caret position.
	 */
	public void updateMatches(@Nonnull String text, @Nullable String selectedText, int caret) {
		matchRanges = findMatchRanges(text, selectedText, caret);
		styledRanges = matchRanges;
	}

	/**
	 * @param text
	 * 		Current editor text.
	 * @param selectedText
	 * 		Current selected text.
	 * @param caret
	 * 		Current caret position.
	 *
	 * @return Ranges that either had or now have selected-word match styling.
	 */
	@Nonnull
	public List<IntRange> updateMatchesAndGetAffectedRanges(@Nonnull String text, @Nullable String selectedText, int caret) {
		return refreshAndGetAffectedRanges(text, selectedText, caret);
	}

	/**
	 * @param from
	 * 		Absolute offset where the spans begin.
	 * @param spans
	 * 		Original style spans.
	 *
	 * @return Style spans with selected-word match styling appended where applicable.
	 */
	@Nonnull
	public StyleSpans<Collection<String>> apply(int from, @Nonnull StyleSpans<Collection<String>> spans) {
		if (matchRanges.isEmpty())
			return spans;

		// Check if we have any overlapping ranges with the given spans.
		// If not, we can skip the work of building new spans.
		int to = from + spans.length();
		List<IntRange> overlappingRanges = new ArrayList<>();
		for (IntRange range : matchRanges) {
			if (range.end() <= from)
				continue;
			if (range.start() >= to)
				break;
			overlappingRanges.add(range);
		}
		if (overlappingRanges.isEmpty())
			return spans;

		// Build new spans with the match style applied to overlapping ranges.
		StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		int absolutePosition = from;
		int rangeIndex = 0;
		for (StyleSpan<Collection<String>> span : spans) {
			int spanStart = absolutePosition;
			int spanEnd = spanStart + span.getLength();
			int cursor = spanStart;

			// Move forward to the first overlapping range that is relevant to this span.
			while (rangeIndex < overlappingRanges.size() && overlappingRanges.get(rangeIndex).end() <= cursor)
				rangeIndex++;

			// For each overlapping range, apply the match style to the overlapping portion of the span.
			int localRangeIndex = rangeIndex;
			while (localRangeIndex < overlappingRanges.size()) {
				IntRange range = overlappingRanges.get(localRangeIndex);
				if (range.start() >= spanEnd)
					break;

				// If the range starts after the cursor, add the original style up to the start of the range.
				int highlightStart = Math.max(cursor, range.start());
				if (cursor < highlightStart)
					builder.add(span.getStyle(), highlightStart - cursor);

				// Add the match style for the overlapping portion of the range.
				int highlightEnd = Math.min(spanEnd, range.end());
				builder.add(withHighlightStyle(span.getStyle()), highlightEnd - highlightStart);
				cursor = highlightEnd;

				if (range.end() <= cursor)
					localRangeIndex++;
				else
					break;
			}

			// Fill any remaining portion of the span with the original style.
			if (cursor < spanEnd)
				builder.add(span.getStyle(), spanEnd - cursor);

			// Update the absolute position and range index for the next span.
			absolutePosition = spanEnd;
			rangeIndex = localRangeIndex;
		}
		return builder.create();
	}

	/**
	 * Restyle any affected ranges due to a change in the selected word or caret position.
	 */
	private void refreshSelectedWordHighlights() {
		if (editor == null || codeArea == null || editor.isTyping() || mouseSelectionInProgress)
			return;

		List<IntRange> affectedRanges = refreshAndGetAffectedRanges(editor.getText(), codeArea.getSelectedText(), codeArea.getCaretPosition());
		restyleSelectedWordHighlights(affectedRanges);
	}

	/**
	 * Restyle any affected ranges due to a change in the selected word or caret position after a text change event.
	 *
	 * @param changes
	 * 		Text changes that may have affected the selected word matches.
	 */
	private void updateSelectedWordHighlights(@Nonnull List<PlainTextChange> changes) {
		if (editor == null || codeArea == null || mouseSelectionInProgress)
			return;

		List<IntRange> affectedRanges = updateMatchesAndGetAffectedRanges(editor.getText(), codeArea.getSelectedText(), codeArea.getCaretPosition());
		affectedRanges = withTextChangeRanges(affectedRanges, changes);
		restyleSelectedWordHighlights(affectedRanges);
	}

	/**
	 * @param ranges
	 * 		Collection of affected ranges to restyle.
	 * @param changes
	 * 		Text changes that may have affected the selected word matches.
	 *
	 * @return Collection of affected ranges to restyle, including any additional ranges that were affected by the text changes.
	 */
	@Nonnull
	private List<IntRange> withTextChangeRanges(@Nonnull List<IntRange> ranges, @Nonnull List<PlainTextChange> changes) {
		if (changes.isEmpty() || editor == null)
			return ranges;

		// Add ranges for the text changes, clamped to the current text length for safety.
		List<IntRange> allRanges = new ArrayList<>(ranges.size() + changes.size());
		allRanges.addAll(ranges);
		int textLength = editor.getTextLength();
		for (PlainTextChange change : changes) {
			int start = Math.max(0, Math.min(change.getPosition(), textLength));
			int end = Math.max(start, Math.min(change.getInsertionEnd(), textLength));

			// Inserted text can inherit the selected-word style from adjacent text.
			// Include it in the patch set so stale match styling is stripped before current matches are applied.
			if (start < end)
				allRanges.add(new IntRange(start, end));
		}
		return allRanges;
	}

	/**
	 * Trigger restyle over the given ranges via {@link Editor#setStyleSpans(int, StyleSpans)}.
	 *
	 * @param ranges
	 * 		Ranges to restyle.
	 */
	private void restyleSelectedWordHighlights(@Nonnull List<IntRange> ranges) {
		if (ranges.isEmpty() || editor == null || codeArea == null)
			return;

		int textLength = editor.getTextLength();
		for (IntRange range : ranges) {
			int start = Math.max(0, Math.min(range.start(), textLength));
			int end = Math.max(start, Math.min(range.end(), textLength));
			if (start == end)
				continue;

			StyleSpans<Collection<String>> spans = codeArea.getStyleSpans(start, end);
			editor.setStyleSpans(start, removeHighlightStyle(spans));
		}
	}

	@Nonnull
	private List<IntRange> findMatchRanges(@Nonnull String text, @Nullable String selectedText, int caret) {
		return findWordMatchRanges(text, selectedText, caret);
	}

	@Nonnull
	private List<IntRange> findWordMatchRanges(@Nonnull String text, @Nullable String selectedText, int caret) {
		String selectedWord = getSelectedWord(text, selectedText, caret);
		if (selectedWord == null || text.isEmpty() || selectionBlacklist.test(selectedWord))
			return Collections.emptyList();

		List<IntRange> matches = new ArrayList<>();
		int limit = text.length() - selectedWord.length();
		int maxMatchesLocal = maxMatches;
		int maxMatchesPerLineLocal = maxMatchesPerLine;
		int currentLine = 0;
		int lineMatchCount = 0;
		int nextLineOffset = text.indexOf('\n');
		if (nextLineOffset == -1)
			nextLineOffset = text.length();

		for (int i = 0; i <= limit; ) {
			int match = text.indexOf(selectedWord, i);
			if (match < 0)
				break;

			int end = match + selectedWord.length();
			if (isWordBoundary(text, match - 1) && isWordBoundary(text, end)) {
				while (match > nextLineOffset) {
					currentLine++;
					lineMatchCount = 0;
					nextLineOffset = text.indexOf('\n', nextLineOffset + 1);
					if (nextLineOffset == -1)
						nextLineOffset = text.length();
				}

				matches.add(new IntRange(match, end));
				lineMatchCount++;

				// Prevent bogus inputs from hanging the application by emitting stupidly complex match collections.
				if (matches.size() > maxMatchesLocal) {
					logger.warn("Skipping selected-word highlighting, max is {}", maxMatchesLocal);
					return Collections.emptyList();
				}

				// As with syntax highlighting, many matches on one long line are more expensive to render than the same
				// count split across many vertically virtualized lines.
				if (lineMatchCount > maxMatchesPerLineLocal) {
					logger.warn("Skipping selected-word highlighting on line {}, max-per-line is {}",
							currentLine + 1, maxMatchesPerLineLocal);
					return Collections.emptyList();
				}
			}
			i = end;
		}
		return matches;
	}

	@Nonnull
	private static Collection<String> withHighlightStyle(@Nonnull Collection<String> existingStyles) {
		if (existingStyles.contains(MATCH_STYLE_CLASS))
			return existingStyles;

		List<String> newStyles = new ArrayList<>(existingStyles.size() + 1);
		newStyles.addAll(existingStyles);
		newStyles.add(MATCH_STYLE_CLASS);
		return newStyles;
	}

	@Nonnull
	private static List<IntRange> mergeRanges(@Nonnull List<IntRange> oldRanges, @Nonnull List<IntRange> newRanges) {
		if (oldRanges.isEmpty())
			return newRanges;
		if (newRanges.isEmpty())
			return oldRanges;

		List<IntRange> allRanges = new ArrayList<>(oldRanges.size() + newRanges.size());
		allRanges.addAll(oldRanges);
		allRanges.addAll(newRanges);
		Collections.sort(allRanges);

		List<IntRange> mergedRanges = new ArrayList<>(allRanges.size());
		IntRange current = allRanges.getFirst();
		for (int i = 1; i < allRanges.size(); i++) {
			IntRange next = allRanges.get(i);
			if (next.start() <= current.end()) {
				current = new IntRange(current.start(), Math.max(current.end(), next.end()));
			} else {
				mergedRanges.add(current);
				current = next;
			}
		}
		mergedRanges.add(current);
		return mergedRanges;
	}

	@Nullable
	private static String getSelectedWord(@Nonnull String text, @Nullable String selectedText, int caret) {
		if (isSelectedWord(selectedText))
			return selectedText;
		if (selectedText != null && !selectedText.isEmpty())
			return null;

		if (text.isEmpty())
			return null;

		int seed = -1;
		if (caret < text.length() && isWordChar(text.charAt(caret))) {
			seed = caret;
		} else if (caret > 0 && isWordChar(text.charAt(caret - 1))) {
			seed = caret - 1;
		}
		if (seed < 0)
			return null;

		int start = seed;
		while (start > 0 && isWordChar(text.charAt(start - 1)))
			start--;

		int end = seed + 1;
		while (end < text.length() && isWordChar(text.charAt(end)))
			end++;

		return text.substring(start, end);
	}

	private static boolean isSelectedWord(@Nullable String text) {
		if (text == null || text.isBlank())
			return false;
		for (int i = 0; i < text.length(); i++) {
			if (!isWordChar(text.charAt(i)))
				return false;
		}
		return true;
	}

	private static boolean isWordBoundary(@Nonnull String text, int index) {
		return index < 0 || index >= text.length() || !isWordChar(text.charAt(index));
	}

	private static boolean isWordChar(char c) {
		return Character.isJavaIdentifierPart(c);
	}

	@Nonnull
	private static Predicate<String> toBlacklistPredicate(@Nonnull Collection<String> selectionBlacklist) {
		Set<String> snapshot = Set.copyOf(selectionBlacklist);
		return snapshot::contains;
	}
}
