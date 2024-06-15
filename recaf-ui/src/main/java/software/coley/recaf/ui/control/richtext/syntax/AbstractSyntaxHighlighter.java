package software.coley.recaf.ui.control.richtext.syntax;

import jakarta.annotation.Nonnull;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.util.Collection;
import java.util.Collections;

/**
 * Common base for syntax highlighter implementations.
 *
 * @author Matt Coley
 */
public abstract class AbstractSyntaxHighlighter implements SyntaxHighlighter {
	private static final Logger logger = Logging.get(AbstractSyntaxHighlighter.class);
	private static final int DEFAULT_MAX_SPANS = 400_000;
	private static final int DEFAULT_MAX_SPANS_PER_LINE = 25_000;
	private final int maxSpans;
	private final int maxSpansPerLine;

	/**
	 * New base highlighter with default span limits.
	 */
	protected AbstractSyntaxHighlighter() {
		this(DEFAULT_MAX_SPANS, DEFAULT_MAX_SPANS_PER_LINE);
	}

	/**
	 * New base highlighter with the given span limits.
	 *
	 * @param maxSpans
	 * 		Max span count to allow in output.
	 * @param maxSpansPerLine
	 * 		Max span count per line to allow in output.
	 */
	protected AbstractSyntaxHighlighter(int maxSpans, int maxSpansPerLine) {
		this.maxSpans = maxSpans;
		this.maxSpansPerLine = maxSpansPerLine;
	}

	@Nonnull
	@Override
	@SuppressWarnings("UnnecessaryLocalVariable") // Used for optimization
	public final StyleSpans<Collection<String>> createStyleSpans(@Nonnull String text, int start, int end) {
		StyleSpans<Collection<String>> spans = createStyleSpansImpl(text, start, end);

		// Prevent bogus inputs from hanging the application by emitting stupidly complex span collections.
		// This counts the number of global spans across all lines.
		int spanCount = spans.getSpanCount();
		if (spanCount > maxSpans) {
			logger.warn("Skipping syntax computation, input of {} spans, max is {}", spanCount, maxSpans);
			return StyleSpans.singleton(Collections.emptyList(), start - end);
		}

		// The more problematic "lag" comes when a single line holds many spans.
		// This is because if 100_000 spans are separated across 100_000 lines, rendering 1 span per line is trivial
		// when considering the lines are virtualized vertically. However, because text is not horizontally virtualized
		// 100_000 spans on one line would cause problems.
		int maxSpansPerLineLocal = maxSpansPerLine;
		int currentLine = 0;
		int currentOffset = 0;
		int nextLineOffset = text.indexOf('\n');
		spanCount = 0;
		for (StyleSpan<Collection<String>> span : spans) {
			int spanLength = span.getLength();
			int spanBegin = currentOffset;
			int spanEnd = spanBegin + spanLength;
			spanCount++;

			// When the span goes beyond the next line offset, we scan forward for the next line that contains the span end.
			// In each scan step, we increment the line and clear the span count since we've moved onto a new line.
			while (spanEnd > nextLineOffset) {
				spanCount = 1;
				currentLine++;
				nextLineOffset = text.indexOf('\n', nextLineOffset + 1);
				if (nextLineOffset == -1) nextLineOffset = end;
			}

			// If this line has more spans than allowed, do not yield the results, as that would cause UI slowdowns.
			if (spanCount > maxSpansPerLineLocal) {
				logger.warn("Skipping syntax computation, input of {} spans on line {}, max-per-line is {}", spanCount, currentLine + 1, maxSpans);
				return StyleSpans.singleton(Collections.emptyList(), start - end);
			}

			// Move current offset forward by the size of the span.
			// Areas without syntax are still spans, but with an empty list of style classes.
			currentOffset += spanLength;
		}

		return spans;
	}

	@Nonnull
	protected abstract StyleSpans<Collection<String>> createStyleSpansImpl(@Nonnull String text, int start, int end);
}
