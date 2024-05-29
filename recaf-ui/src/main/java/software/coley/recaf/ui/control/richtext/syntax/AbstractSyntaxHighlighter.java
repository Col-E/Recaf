package software.coley.recaf.ui.control.richtext.syntax;

import jakarta.annotation.Nonnull;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
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

	private static final StyleSpans<Collection<String>> EMPTY_SPANS = new StyleSpansBuilder<Collection<String>>()
			.add(Collections.emptyList(), 0)
			.create();
	private static final int DEFAULT_MAX_SPANS = 25_000;
	private final int maxSpans;

	/**
	 * New base highlighter with default span limit
	 */
	protected AbstractSyntaxHighlighter() {
		this(DEFAULT_MAX_SPANS);
	}

	/**
	 * New base highlighter with the given span limit
	 *
	 * @param maxSpans
	 * 		Max span count to allow in output.
	 */
	protected AbstractSyntaxHighlighter(int maxSpans) {
		this.maxSpans = maxSpans;
	}

	@Nonnull
	@Override
	public final StyleSpans<Collection<String>> createStyleSpans(@Nonnull String text, int start, int end) {
		StyleSpans<Collection<String>> spans = createStyleSpansImpl(text, start, end);

		// Prevent bogus inputs from hanging the application by emitting stupidly complex span collections.
		int spanCount = spans.getSpanCount();
		if (spanCount > maxSpans) {
			logger.warn("Skipping syntax computation, input yielded {} spans, max is {}", spanCount, maxSpans);
			return EMPTY_SPANS;
		}

		return spans;
	}

	@Nonnull
	protected abstract StyleSpans<Collection<String>> createStyleSpansImpl(@Nonnull String text, int start, int end);
}
