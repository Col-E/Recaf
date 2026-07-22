package software.coley.recaf.ui.control.richtext.highlight;

import jakarta.annotation.Nonnull;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;
import software.coley.recaf.util.IntRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SelectedWordHighlighting}.
 */
class SelectedWordHighlightingTest {
	private static final String MATCH_STYLE = "selected-word-match";
	private static final String TEXT = """
			class Example {
				void test() {
					foo();
					foobar();
					foo = foo + 1;
				}
			}
			""";

	@Test
	void returnsOriginalSpansWhenNoMatchesAreTracked() {
		// No matches are tracked when the user has not selected a word, or the caret is not on a word.
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		StyleSpans<Collection<String>> spans = StyleSpans.singleton(Collections.emptyList(), TEXT.length());
		assertSame(spans, highlighting.apply(0, spans));
	}

	@Test
	void refreshReturnsTrueOnlyWhenMatchesChange() {
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		int caret = TEXT.indexOf("foo();") + 1;

		// Refreshing with the same text and caret position should return true the first time, but false the second time.
		assertTrue(highlighting.refresh(TEXT, "", caret));
		assertFalse(highlighting.refresh(TEXT, "", caret));
		assertTrue(highlighting.refresh(TEXT, "foo()", caret));
		assertFalse(highlighting.refresh(TEXT, "foo()", caret));
	}

	@Test
	void refreshReportsOnlyOldAndNewMatchRanges() {
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		int fooCall = TEXT.indexOf("foo();");
		int fooAssignment = TEXT.indexOf("foo =");
		int fooAddition = TEXT.indexOf("foo +");
		int foobar = TEXT.indexOf("foobar");

		// Refresh with a caret on the first "foo" should report all 3 matches of "foo"
		assertEquals(List.of(
				new IntRange(fooCall, fooCall + 3),
				new IntRange(fooAssignment, fooAssignment + 3),
				new IntRange(fooAddition, fooAddition + 3)
		), highlighting.refreshAndGetAffectedRanges(TEXT, "", fooCall + 1));

		// Refresh with a caret on the 'foo()' should report no matches, since the caret is not on a word.
		assertEquals(Collections.emptyList(), highlighting.refreshAndGetAffectedRanges(TEXT, "", fooCall + 1));

		// Refresh with a caret on "foobar" should report the old "foo" matches, and the new "foobar" match.
		assertEquals(List.of(
				new IntRange(fooCall, fooCall + 3),
				new IntRange(foobar, foobar + 6),
				new IntRange(fooAssignment, fooAssignment + 3),
				new IntRange(fooAddition, fooAddition + 3)
		), highlighting.refreshAndGetAffectedRanges(TEXT, "", foobar + 1));
	}

	@Test
	void updateReportsPreviouslyStyledRanges() {
		String text = "fizz foo buzz bar";
		int foo = text.indexOf("foo");
		int bar = text.indexOf("bar");

		// Baseline update with "foo" selected, should report the single match of "foo".
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		highlighting.updateMatches(text, "foo", foo);

		// Update with "bar" selected, should report the old "foo" match, and the new "bar" match.
		assertEquals(List.of(
				new IntRange(foo, foo + 3),
				new IntRange(bar, bar + 3)
		), highlighting.updateMatchesAndGetAffectedRanges(text, "bar", bar));
	}

	@Test
	void appliesStyleToWholeWordMatchesOnly() {
		// Baseline update with a caret on the first "foo" should report all 3 matches of "foo",
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		highlighting.updateMatches(TEXT, "", TEXT.indexOf("foo();") + 1);

		// Apply the highlighting, and verify that only the whole-word matches are styled.
		StyleSpans<Collection<String>> spans = StyleSpans.singleton(List.of("base"), TEXT.length());
		assertEquals(List.of(
				span(TEXT.indexOf("foo();"), "base"),
				span(3, "base", MATCH_STYLE),
				span(TEXT.indexOf("foo =") - TEXT.indexOf("foo();") - 3, "base"),
				span(3, "base", MATCH_STYLE),
				span(TEXT.indexOf("foo +") - TEXT.indexOf("foo =") - 3, "base"),
				span(3, "base", MATCH_STYLE),
				span(TEXT.length() - TEXT.indexOf("foo +") - 3, "base")
		), describe(highlighting.apply(0, spans)));
	}

	@Test
	void skipsBlacklistedSelectedWords() {
		// Simple single-keyword skip test.
		String text = "class Example { class Other {} }";
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting(Set.of("class"));

		// No results for blacklisted words, and no highlighting should be applied.
		assertEquals(Collections.emptyList(), highlighting.refreshAndGetAffectedRanges(text, "class", 0));
		assertSameSpansWithoutHighlights(highlighting, text);

		// Non-blacklisted words should still be highlighted.
		int example = text.indexOf("Example");
		assertEquals(List.of(
				new IntRange(example, example + 7),
				new IntRange(text.lastIndexOf("Example"), text.lastIndexOf("Example") + 7)
		), highlighting.refreshAndGetAffectedRanges(text, "Example", example));
	}

	@Test
	void appliesStyleWithOffsetSpans() {
		String text = "a(); ab(); a = a + 1;";
		int from = text.indexOf("ab();");
		int length = text.length() - from;

		// Baseline update with a caret on the first "a" should report all 3 matches of "a".
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		highlighting.updateMatches(text, "", text.indexOf("a ="));

		// Apply the highlighting, and verify that only the whole-word matches are styled.
		StyleSpans<Collection<String>> spans = StyleSpans.singleton(Collections.emptyList(), length);
		assertEquals(List.of(
				span(text.indexOf("a =") - from),
				span(1, MATCH_STYLE),
				span(text.indexOf("a +") - text.indexOf("a =") - 1),
				span(1, MATCH_STYLE),
				span(text.length() - text.indexOf("a +") - 1)
		), describe(highlighting.apply(from, spans)));
	}

	@Test
	void removesExistingMatchStyleOnly() {
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting();
		StyleSpans<Collection<String>> spans = StyleSpans.singleton(List.of("base", MATCH_STYLE), 3);

		// Removing the selected word style should only remove the selected word match style, and preserve other styles.
		assertEquals(List.of(span(3, "base")), describe(highlighting.removeHighlightStyle(spans)));
	}

	@Test
	void skipsHighlightingWhenGlobalMatchLimitIsExceeded() {
		String text = "a a a";
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting(2, 25_000);

		// If the global match limit is exceeded, then highlighting should not be applied.
		assertEquals(Collections.emptyList(), highlighting.refreshAndGetAffectedRanges(text, "a", 0));
		assertSameSpansWithoutHighlights(highlighting, text);
	}

	@Test
	void skipsHighlightingWhenLineMatchLimitIsExceeded() {
		String text = "a a\na";
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting(25_000, 1);

		// If the per-line limit is exceeded, then highlighting should not be applied.
		assertEquals(Collections.emptyList(), highlighting.refreshAndGetAffectedRanges(text, "a", 0));
		assertSameSpansWithoutHighlights(highlighting, text);
	}

	@Test
	void allowsHighlightingWhenPerLineLimitIsNotExceeded() {
		String text = "a\na\na";
		SelectedWordHighlighting highlighting = new SelectedWordHighlighting(25_000, 1);

		// If the per-line limit is not exceeded, then highlighting should be applied.
		assertEquals(List.of(
				new IntRange(0, 1),
				new IntRange(2, 3),
				new IntRange(4, 5)
		), highlighting.refreshAndGetAffectedRanges(text, "a", 0));
	}

	/**
	 * @param spans
	 * 		Spans to map.
	 *
	 * @return Simplified span models.
	 */
	@Nonnull
	private static List<SpanInfo> describe(@Nonnull StyleSpans<Collection<String>> spans) {
		List<SpanInfo> descriptions = new ArrayList<>();
		for (StyleSpan<Collection<String>> span : spans)
			descriptions.add(new SpanInfo(span.getLength(), List.copyOf(span.getStyle())));
		return descriptions;
	}

	/**
	 * Validates that the given highlighting instance does not apply any highlighting to the given text.
	 *
	 * @param highlighting
	 * 		Instance to test.
	 * @param text
	 * 		Text to apply highlighting to.
	 */
	private static void assertSameSpansWithoutHighlights(@Nonnull SelectedWordHighlighting highlighting, @Nonnull String text) {
		// Should be same reference as input spans, since no highlighting was applied.
		StyleSpans<Collection<String>> spans = StyleSpans.singleton(Collections.emptyList(), text.length());
		assertSame(spans, highlighting.apply(0, spans));
	}

	/**
	 * @param length
	 * 		Length of the span.
	 * @param styles
	 * 		Styles applied to the span.
	 *
	 * @return Span info for the given length and styles.
	 */
	@Nonnull
	private static SpanInfo span(int length, String... styles) {
		return new SpanInfo(length, List.of(styles));
	}

	/** Simple record for span length and styles */
	private record SpanInfo(int length, List<String> styles) {}
}
