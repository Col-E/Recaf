package software.coley.recaf.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.coley.recaf.util.EscapeUtil.*;

/**
 * Tests for {@link EscapeUtil}
 */
class EscapeUtilTest {
	@Test
	void testCasePairs() {
		escapeUnescape(
				new Case("foobar", "foobar", "foobar", "foobar"),

				// Spaces
				new Case("foo bar", "foo bar", "foo bar", "foo\\u0020bar"),
				new Case("foo\nbar", "foo\\nbar", "foo\\nbar", "foo\\u000Abar"),
				new Case("foo\rbar", "foo\\rbar", "foo\\rbar", "foo\\u000Dbar"),
				new Case("foo\tbar", "foo\\tbar", "foo\\tbar", "foo\\u0009bar"),
				new Case("foo\u2004bar", "foo\u2004bar", "foo\\u2004bar", "foo\\u2004bar"),
				new Case("foo\u2800bar", "foo\u2800bar", "foo\\u2800bar", "foo\\u2800bar"),
				new Case("foo\u3000bar", "foo\u3000bar", "foo\\u3000bar", "foo\\u3000bar"),

				// Slashes
				new Case("foo/bar", "foo/bar", "foo/bar", "foo/bar"),
				new Case("foo\\bar", "foo\\\\bar", "foo\\\\bar", "foo\\\\bar"),

				// Quotes
				new Case("foo\"bar", "foo\\\"bar", "foo\\\"bar", "foo\\u0022bar"),
				new Case("foo'bar", "foo'bar", "foo'bar", "foo'bar"),

				// Emoji "Bridge at Night" - https://www.compart.com/en/unicode/U+1F309
				new Case("foo\ud83c\udf09bar", "foo\ud83c\udf09bar", "foo\ud83c\udf09bar", "foo\ud83c\udf09bar")
		);
	}

	void escapeUnescape(Case... cases) {
		for (Case c : cases) {
			// Escaping
			assertEquals(c.standard, escapeStandard(c.original));
			assertEquals(c.standardAndUnicode, escapeStandardAndUnicodeWhitespace(c.original));
			assertEquals(c.standardAndUnicodeAlt, escapeStandardAndUnicodeWhitespaceAlt(c.original));
			// Unescaping
			assertEquals(c.original, unescapeStandard(c.standard));
			assertEquals(c.original, unescapeStandardAndUnicodeWhitespace(c.standardAndUnicode));
			assertEquals(c.original, unescapeUnicode(c.standardAndUnicodeAlt));
		}
	}

	record Case(String original, String standard, String standardAndUnicode, String standardAndUnicodeAlt) {}
}