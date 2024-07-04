package software.coley.recaf.ui.control.richtext.syntax;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.IntRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.*;

/**
 * Various syntax utils, mostly for range computations.
 *
 * @author Matt Coley
 */
public class SyntaxUtil {
	private static final Logger logger = Logging.get(SyntaxUtil.class);

	/**
	 * @param text
	 * 		Full document text.
	 * @param styleSpans
	 * 		Existing style-spans for the full document text.
	 * @param syntaxHighlighter
	 * 		Existing highlighter to use for {@link SyntaxHighlighter#expandRange(String, int, int)}.
	 * 		May be {@code null} to skip this process.
	 * @param change
	 * 		The change made in the editor's text.
	 *
	 * @return Range to restyle in order to fit the changed text.
	 */
	public static IntRange getRangeForRestyle(@Nonnull String text,
											  @Nonnull StyleSpans<Collection<String>> styleSpans,
											  @Nullable SyntaxHighlighter syntaxHighlighter,
											  @Nonnull PlainTextChange change) {
		int textLength = text.length();
		if (textLength == 0) return IntRange.EMPTY;

		// Get range of change
		String inserted = change.getInserted();
		String removed = change.getRemoved();
		int changeStart = change.getPosition();
		int changeEnd;
		if (!inserted.isBlank()) {
			changeEnd = change.getInsertionEnd();
		} else if (!removed.isBlank()) {
			changeEnd = change.getRemovalEnd();
		} else {
			changeEnd = changeStart + Math.abs(change.getNetLength());
		}

		// Expand the range to the boundaries of top-level flattened style-spans.
		// If you change a character in the middle of a multi-line comment, this will make the
		// bounds fit from '/*' to '*/'. This way when we pass the range to the styler it operates on a range
		// that previously matched a top-level rule. In most cases it will make the same match again.
		int styleStart;
		int styleEnd;
		List<IntRange> topLevelSpanRanges = SyntaxUtil.flatten(styleSpans);
		try {
			// Find where the change start and end positions would be inserted into the flattened ranges.
			IntRange startMarker = new IntRange(changeStart - 1, changeStart);
			IntRange endMarker = new IntRange(changeEnd - 1, changeEnd);
			int searchEnd = topLevelSpanRanges.size() - 1;
			int startIndex = abs(Lists.binarySearch(topLevelSpanRanges, startMarker));
			int endIndex = abs(Lists.binarySearch(topLevelSpanRanges, endMarker, startIndex, searchEnd));

			// Normalize binary search results.
			int size = topLevelSpanRanges.size() - 1;
			startIndex = min(size, max(0, startIndex));
			endIndex = min(size, max(0, endIndex));

			// Map to positions in the text.
			styleStart = topLevelSpanRanges.get(startIndex).start();
			styleEnd = topLevelSpanRanges.get(endIndex).end();
		} catch (Exception ex) {
			logger.error("Failed to get span range for change", ex);
			throw new IllegalStateException(ex);
		}

		// Fit to paragraph start/end (newline boundaries)
		styleStart = Math.min(styleStart, text.substring(0, styleStart).lastIndexOf('\n') + 1);
		styleEnd = Math.max(styleEnd, text.indexOf('\n', styleEnd - 1));

		// Allow the syntax highlighter implementation to reshape the range.
		// Building on the example from before with multi-line comments, lets say you delete the last `/` that
		// closes a multi-line comment. The existing range up until here will cut off at the end of the prior range
		// of the multi-line comment. But now that the comment is open, it will continue until the next '*/'.
		//
		// Because of this, we let the syntax highlighter implementations expand the range if they have constructs
		// like multi-line comments.
		if (syntaxHighlighter != null) {
			IntRange expandedRange = syntaxHighlighter.expandRange(text, styleStart, styleEnd);
			styleStart = Math.min(styleStart, expandedRange.start());
			styleEnd = Math.max(styleEnd, expandedRange.end());

			// Fit to paragraph start/end (newline boundaries) again
			styleStart = Math.min(styleStart, text.substring(0, styleStart).lastIndexOf('\n') + 1);
			styleEnd = Math.max(styleEnd, text.indexOf('\n', styleEnd - 1));
		}

		return new IntRange(styleStart, styleEnd);
	}

	/**
	 * The {@link StyleSpans} of RichTextFX are not a tree-like structure. If you have something you wish to style in
	 * the middle of another block, you have to create three blocks instead of having a sub-range in the parent span.
	 * <br>
	 * Having to deal with this layout is kind of annoying when regenerating styles. To deal with that, we flatten cases
	 * as described prior into a single range. If it were ideally represented as a tree, we are stripping all but the
	 * base layer.
	 * <br>
	 * Usage of this can be seen in {@link #getRangeForRestyle(String, StyleSpans, SyntaxHighlighter, PlainTextChange)}.
	 *
	 * @param styleSpans
	 * 		Collection of {@link StyleSpan}.
	 *
	 * @return Flat representation of ranges.
	 */
	public static List<IntRange> flatten(StyleSpans<Collection<String>> styleSpans) {
		// Map the style spans (which only know their own length) into ranges that know their absolute positions.
		List<StyledRange> styledRanges = new ArrayList<>();
		int position = 0;
		for (StyleSpan<Collection<String>> span : styleSpans) {
			int length = span.getLength();
			styledRanges.add(new StyledRange(new IntRange(position, position + length), span.getStyle()));
			position += length;
		}

		// Flatten the ranges together where possible.
		// For example, where each line represents a range, and letters are classes:
		//
		// This sample will be merged into one range of A:
		//
		//  A
		//  AB
		//  A
		int i = 0;
		while (i < styledRanges.size() - 1) {
			StyledRange current = styledRanges.get(i);
			StyledRange next = styledRanges.get(i + 1);
			if (current.canMergeInto(next)) {
				current.merge(next);
				styledRanges.remove(next);
			} else {
				i++;
			}
		}

		// Map back to regular ranges.
		return styledRanges.stream()
				.map(StyledRange::getRange)
				.filter(range -> !range.empty())
				.toList();
	}

	/**
	 * Range mapped to a {@link StyleSpan}.
	 * Contains the collection of style classes of the span.
	 */
	private static class StyledRange {
		private final Collection<String> style;
		private IntRange range;

		/**
		 * @param range
		 * 		Span range.
		 * @param style
		 * 		Span style classes.
		 */
		public StyledRange(@Nonnull IntRange range, @Nonnull Collection<String> style) {
			this.range = range;
			this.style = style;
		}

		/**
		 * @return Span style classes.
		 */
		@Nonnull
		public Collection<String> getStyle() {
			return style;
		}

		/**
		 * @return Span range.
		 */
		@Nonnull
		public IntRange getRange() {
			return range;
		}

		/**
		 * @param other
		 * 		Another range, should be the next range in a containing {@link List}.
		 *
		 * @return {@code true} when the given range contains all the classes of our current range.
		 */
		public boolean canMergeInto(@Nonnull StyledRange other) {
			if (style.isEmpty()) return false;
			return other.style.containsAll(style);
		}

		/**
		 * Adds the length of the given range to our own.
		 * Assumes the ranges are adjacent.
		 *
		 * @param other
		 * 		Other range to merge into this one.
		 */
		public void merge(@Nonnull StyledRange other) {
			range = range.extendForwards(other.range.length());
		}
	}
}
