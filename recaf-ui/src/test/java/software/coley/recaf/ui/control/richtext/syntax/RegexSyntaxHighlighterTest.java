package software.coley.recaf.ui.control.richtext.syntax;

import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.util.IntRange;
import software.coley.recaf.util.StringUtil;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.ui.control.richtext.syntax.RegexLanguages.getJavaLanguage;

/**
 * Tests for {@link RegexSyntaxHighlighter}.
 */
class RegexSyntaxHighlighterTest {
	private static final String JAVADOC_PATTERN = "/\\*\\*[\\w\\W]+\\*/";
	private static final RegexSyntaxHighlighter HIGHLIGHTER_JAVA = new RegexSyntaxHighlighter(getJavaLanguage());
	private static final String TEXT_JAVA = """
			{
			/**
			* one
			* @param foo
			*/
			void one(String foo);
							
			/**
			* two
			* @param bar
			*/
			void two(String bar);
							
			/**
			* three
			* @return it does not
			*/
			void three();
			}
			""";
	private static final StyleSpans<Collection<String>> styleSpans =
			HIGHLIGHTER_JAVA.createStyleSpans(TEXT_JAVA, 0, TEXT_JAVA.length());

	@Test
	void testFlatten() {
		List<IntRange> flattened = SyntaxUtil.flatten(styleSpans);
		List<String> flattenedSubstrings = flattened.stream()
				.map(range -> range.sectionOfText(TEXT_JAVA))
				.toList();

		// Should be 13 ranges from flattened spans:
		// 01. {
		// 02. block comment
		// 03. whitespace
		// 04. void
		// 05. one(String foo);
		// 06. block comment
		// 07. whitespace
		// 08. void
		// 09. two(String bar);
		// 10. block comment
		// 11. whitespace
		// 12. void
		// 13. three(); }
		assertEquals(13, flattened.size());
		assertTrue(flattened.size() < styleSpans.getSpanCount(), "Expected less # than base span count");
		assertEquals("{", flattenedSubstrings.get(0).trim());
		assertTrue(flattenedSubstrings.get(1).matches(JAVADOC_PATTERN));
		assertTrue(flattenedSubstrings.get(2).isBlank());
		assertEquals("void", flattenedSubstrings.get(3).trim());
		assertEquals("one(String foo);", flattenedSubstrings.get(4).trim());
		assertTrue(flattenedSubstrings.get(5).matches(JAVADOC_PATTERN));
		assertTrue(flattenedSubstrings.get(6).isBlank());
		assertEquals("void", flattenedSubstrings.get(7).trim());
		assertEquals("two(String bar);", flattenedSubstrings.get(8).trim());
		assertTrue(flattenedSubstrings.get(9).matches(JAVADOC_PATTERN));
		assertTrue(flattenedSubstrings.get(10).isBlank());
		assertEquals("void", flattenedSubstrings.get(11).trim());
		assertEquals("three();\n}", flattenedSubstrings.get(12).trim());
	}

	/**
	 * Change for inserting a space in a javadoc comment.
	 * Should only refresh the range of the javadoc comment, nothing before or after.
	 */
	@Nested
	class Simple_RangeForRestyle {
		@Test
		void testBlock1() {
			// Change for inserting a space
			int blockComment1 = TEXT_JAVA.indexOf("* one");
			int blockComment1Open = TEXT_JAVA.lastIndexOf("/**", blockComment1);
			int blockComment1Close = TEXT_JAVA.indexOf("*/", blockComment1) + 2;
			PlainTextChange change = new PlainTextChange(blockComment1 + 1, "", " ");
			String modifiedText = apply(change);

			// The restyled range should just be the block comment.
			// The change does not break the match, so we do not need to expand.
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.matches(JAVADOC_PATTERN));
			assertEquals(blockComment1Open, restyledRange.start());
			assertEquals(blockComment1Close + change.getNetLength(), restyledRange.end());
		}

		@Test
		void testBlock2() {
			// Change for inserting a space
			int blockComment2 = TEXT_JAVA.indexOf("* two");
			int blockComment2Open = TEXT_JAVA.lastIndexOf("/**", blockComment2);
			int blockComment2Close = TEXT_JAVA.indexOf("*/", blockComment2) + 2;
			PlainTextChange change = new PlainTextChange(blockComment2 + 1, "", " ");
			String modifiedText = apply(change);

			// The restyled range should just be the block comment.
			// The change does not break the match, so we do not need to expand.
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.matches(JAVADOC_PATTERN));
			assertEquals(blockComment2Open, restyledRange.start());
			assertEquals(blockComment2Close + change.getNetLength(), restyledRange.end());
		}

		@Test
		void testBlock3() {
			// Change for inserting a space
			int blockComment3 = TEXT_JAVA.indexOf("* three");
			int blockComment3Open = TEXT_JAVA.lastIndexOf("/**", blockComment3);
			int blockComment3Close = TEXT_JAVA.indexOf("*/", blockComment3) + 2;
			PlainTextChange change = new PlainTextChange(blockComment3 + 1, "", " ");
			String modifiedText = apply(change);

			// The restyled range should just be the block comment.
			// The change does not break the match, so we do not need to expand.
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.matches(JAVADOC_PATTERN));
			assertEquals(blockComment3Open, restyledRange.start());
			assertEquals(blockComment3Close + change.getNetLength(), restyledRange.end());
		}
	}

	/**
	 * Change for removing '/' from block comments.
	 * Should refresh towards the open direction.
	 */
	@Nested
	class BreakOpenBlockComment_RangeForRestyle {
		@Test
		void testBlock1Start() {
			// Change for removing '/'
			int blockComment1 = TEXT_JAVA.indexOf("* one");
			int blockComment1Open = TEXT_JAVA.lastIndexOf("/**", blockComment1);
			int blockComment1Close = TEXT_JAVA.indexOf("*/", blockComment1) + 2;
			PlainTextChange change = new PlainTextChange(blockComment1Open, "/", "");
			String modifiedText = apply(change);

			// The comment is broken, and doesn't flow into any opening '/**' prior.
			// Currently, this will expand backwards to the start since the change is on the edge of these
			// two flattened ranges (0th and 1st).
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertFalse(matchedText.matches(JAVADOC_PATTERN));
			assertEquals(0, restyledRange.start());
			assertEquals(blockComment1Close, restyledRange.end());
		}

		@Test
		void testBlock1End() {
			// Change for removing '/'
			int blockComment1 = TEXT_JAVA.indexOf("* one");
			int blockComment1Open = TEXT_JAVA.lastIndexOf("/**", blockComment1);
			int blockComment1Close = TEXT_JAVA.indexOf("*/", blockComment1) + 2;
			int blockComment2Close = TEXT_JAVA.indexOf("*/", blockComment1Close + 1) + 2;
			PlainTextChange change = new PlainTextChange(blockComment1Close - 1, "/", "");
			String modifiedText = apply(change);

			// The restyled range should expand into block 2, since the end of block 1 was removed.
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.matches(JAVADOC_PATTERN));
			assertEquals(blockComment1Open, restyledRange.start());
			assertEquals(blockComment2Close + change.getNetLength(), restyledRange.end());
		}

		@Test
		void testBlock2Start() {
			// Change for removing '/'
			int blockComment2 = TEXT_JAVA.indexOf("* two");
			int blockComment2Open = TEXT_JAVA.lastIndexOf("/**", blockComment2);
			PlainTextChange change = new PlainTextChange(blockComment2Open, "/", "");
			String modifiedText = apply(change);

			// The restyled range should expand up to the first method declaration, since it is the prior adjacent flattened range
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertFalse(matchedText.matches(JAVADOC_PATTERN));
			assertTrue(matchedText.startsWith("void one("));
		}

		@Test
		void testBlock2End() {
			// Change for removing '/'
			int blockComment2 = TEXT_JAVA.indexOf("* two");
			int blockComment2Open = TEXT_JAVA.lastIndexOf("/**", blockComment2);
			int blockComment2Close = TEXT_JAVA.indexOf("*/", blockComment2) + 2;
			int blockComment3Close = TEXT_JAVA.indexOf("*/", blockComment2Close + 1) + 2;
			PlainTextChange change = new PlainTextChange(blockComment2Close - 1, "/", "");
			String modifiedText = apply(change);

			// The restyled range should expand into block 3, since the end of block 2 was removed.
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.matches(JAVADOC_PATTERN));
			assertEquals(blockComment2Open, restyledRange.start());
			assertEquals(blockComment3Close + change.getNetLength(), restyledRange.end());
		}

		@Test
		void testBlock3Start() {
			// Change for removing '/'
			int blockComment3 = TEXT_JAVA.indexOf("* three");
			int blockComment3Open = TEXT_JAVA.lastIndexOf("/**", blockComment3);
			PlainTextChange change = new PlainTextChange(blockComment3Open, "/", "");
			String modifiedText = apply(change);

			// The restyled range should expand up to the second method declaration, since it is the prior adjacent flattened range
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertFalse(matchedText.matches(JAVADOC_PATTERN));
			assertTrue(matchedText.startsWith("void two("));
		}

		@Test
		void testBlock3End() {
			// Change for removing '/'
			int blockComment3 = TEXT_JAVA.indexOf("* three");
			int blockComment3Open = TEXT_JAVA.lastIndexOf("/**", blockComment3);
			int blockComment3Close = TEXT_JAVA.indexOf("*/", blockComment3) + 2;
			PlainTextChange change = new PlainTextChange(blockComment3Close - 1, "/", "");
			String modifiedText = apply(change);

			// The comment is broken, and doesn't flow into any closing '*/' later.
			// Thus, it should only need to restyle what was previously the block comment.
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, styleSpans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertFalse(matchedText.matches(JAVADOC_PATTERN)); // nothing to close
			assertEquals(blockComment3Open, restyledRange.start());
			assertEquals(blockComment3Close, restyledRange.end()); // Not adding + net like others since target is different
		}
	}

	/**
	 * Change for inserting '/' to a broken block comment.
	 * Should restyle the newly formed block comment range <i>(and some adjacent text, not ideal but not terrible)</i>.
	 */
	@Nested
	class CreateBlockComment_RangeForRestyle {
		@Test
		void testAtStart() {
			String textOpenMissing = """
					{
					**
					* @return foo
					*/
					}
					""";

			// Change for inserting '/' at start
			PlainTextChange change = new PlainTextChange(textOpenMissing.indexOf('*'), "", "/");
			String modifiedText = apply(textOpenMissing, change);

			// Initial spans
			StyleSpans<Collection<String>> spans = HIGHLIGHTER_JAVA.createStyleSpans(textOpenMissing, 0, textOpenMissing.length());

			// Expecting full match of block comment, plus prior unmatched text (range is adjacent)
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, spans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.substring(2).matches(JAVADOC_PATTERN));
			assertEquals(0, restyledRange.start());
			assertEquals(modifiedText.lastIndexOf('/') + 1, restyledRange.end());
		}

		@Test
		void testAtEnd() {
			String textCloseMissing = """
					{
					/**
					* @return foo
					*
					}
					""";

			// Change for inserting '/' at start
			PlainTextChange change = new PlainTextChange(textCloseMissing.lastIndexOf('*') + 1, "", "/");
			String modifiedText = apply(textCloseMissing, change);

			// Initial spans
			StyleSpans<Collection<String>> spans = HIGHLIGHTER_JAVA.createStyleSpans(textCloseMissing, 0, textCloseMissing.length());

			// Expecting full match of block comment, plus following unmatched text (range is adjacent)
			IntRange restyledRange = SyntaxUtil.getRangeForRestyle(modifiedText, spans, HIGHLIGHTER_JAVA, change);
			String matchedText = restyledRange.sectionOfText(modifiedText);
			assertTrue(matchedText.substring(0, matchedText.length() - 2).matches(JAVADOC_PATTERN));
			assertEquals(modifiedText.indexOf('/'), restyledRange.start());
			assertEquals(modifiedText.length() - 1, restyledRange.end());
		}
	}

	private static String apply(PlainTextChange change) {
		return apply(TEXT_JAVA, change);
	}

	private static String apply(String text, PlainTextChange change) {
		text = StringUtil.remove(text, change.getPosition(), change.getRemoved().length());
		text = StringUtil.insert(text, change.getPosition(), change.getInserted());
		return text;
	}
}