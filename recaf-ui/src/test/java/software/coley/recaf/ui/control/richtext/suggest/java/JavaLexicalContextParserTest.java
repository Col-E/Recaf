package software.coley.recaf.ui.control.richtext.suggest.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JavaLexicalContextParser}.
 */
class JavaLexicalContextParserTest {
	private final JavaLexicalContextParser parser = new JavaLexicalContextParser();

	@Test
	void parsesImportContext() {
		// For this and the other tests we want to take a partial input see that it can
		// be mapped to the correct context.
		JavaLexicalContext context = parser.parse("import java.ut", "import java.ut".length());
		assertEquals(ContextKind.IMPORT, context.kind());
		assertEquals("java.ut", context.partialText());
	}

	@Test
	void parsesPackageContext() {
		JavaLexicalContext context = parser.parse("package software.col", "package software.col".length());
		assertEquals(ContextKind.PACKAGE, context.kind());
		assertEquals("software.col", context.partialText());
	}

	@Test
	void parsesMemberContext() {
		JavaLexicalContext context = parser.parse("value.ge", "value.ge".length());
		assertEquals(ContextKind.MEMBER, context.kind());
		assertEquals("value", context.receiverText());
		assertEquals("ge", context.partialText());
	}

	@Test
	void parsesMemberContextAfterMethodInvocation() {
		JavaLexicalContext context = parser.parse("strings.getFirst().sub", "strings.getFirst().sub".length());
		assertEquals(ContextKind.MEMBER, context.kind());
		assertEquals("strings.getFirst()", context.receiverText());
		assertEquals("sub", context.partialText());
	}

	@Test
	void parsesMethodReferenceContext() {
		JavaLexicalContext context = parser.parse("stream::ma", "stream::ma".length());
		assertEquals(ContextKind.METHOD_REFERENCE, context.kind());
		assertEquals("stream", context.receiverText());
		assertEquals("ma", context.partialText());
	}

	@Test
	void parsesAnnotationTypeContext() {
		JavaLexicalContext context = parser.parse("@Overr", "@Overr".length());
		assertEquals(ContextKind.TYPE, context.kind());
		assertTrue(context.annotationOnly());
		assertEquals("Overr", context.partialText());
	}

	@Test
	void parsesTypeHeaderKeywordSite() {
		JavaLexicalContext context = parser.parse("public class De", "public class De".length());
		assertEquals(ContextKind.IDENTIFIER, context.kind());
		assertEquals(KeywordSite.TYPE_HEADER, context.keywordSite());
	}

	@Test
	void parsesTypeBodyDeclarationKeywordSite() {
		String source = """
				class Demo {
					pri
				}
				""";
		int caret = source.indexOf("pri") + "pri".length();
		JavaLexicalContext context = parser.parse(source, caret);
		assertEquals(ContextKind.IDENTIFIER, context.kind());
		assertEquals(KeywordSite.TYPE_BODY_DECLARATION, context.keywordSite());
	}

	@Test
	void parsesMethodHeaderKeywordSite() {
		String source = """
				class Demo {
					void run() thro
				}
				""";
		int caret = source.indexOf("thro") + "thro".length();
		JavaLexicalContext context = parser.parse(source, caret);
		assertEquals(ContextKind.IDENTIFIER, context.kind());
		assertEquals(KeywordSite.METHOD_HEADER, context.keywordSite());
	}

	@Test
	void parsesExecutableKeywordSite() {
		String source = """
				class Demo {
					void run() {
						ret
					}
				}
				""";
		int caret = source.indexOf("ret") + "ret".length();
		JavaLexicalContext context = parser.parse(source, caret);
		assertEquals(ContextKind.IDENTIFIER, context.kind());
		assertEquals(KeywordSite.METHOD_BODY, context.keywordSite());
	}

	@Test
	void suppressesCompletionInsideCommentsAndStrings() {
		assertEquals(ContextKind.NONE, parser.parse("// String", "// String".length()).kind());
		assertEquals(ContextKind.NONE, parser.parse("\"String\"", "\"String\"".length()).kind());
		assertEquals(ContextKind.NONE, parser.parse("'c'", "'c'".length()).kind());
	}

	@Test
	void completionSuffixResolvesAcrossSeparators() {
		assertEquals("r", parser.completionSuffix("foo.ba", "foo.bar"));
		assertEquals("r", parser.completionSuffix("ba", "foo.bar"));
		assertNull(parser.completionSuffix("foo", "bar"));
	}
}
