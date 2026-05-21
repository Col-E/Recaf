package software.coley.recaf.services.decompile.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.workspace.model.Workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class UnicodeUnescapeOutputTextFilterTest {
	private final UnicodeUnescapeOutputTextFilter filter = new UnicodeUnescapeOutputTextFilter();
	private final Workspace workspace = mock(Workspace.class);
	private final ClassInfo classInfo = mock(ClassInfo.class);

	@Test
	void decodesCyrillicUnicodeEscapes() {
		String escaped = "return \"\\u0414\\u043b\\u044f \\u0440\\u0430\\u0431\\u043e\\u0442\\u044b\";";
		String expected = "return \"Для работы\";";
		assertEquals(expected, filter.filter(workspace, classInfo, escaped));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"\\u200b", // zero-width space
			"\\u200c", // ZWNJ
			"\\u200d", // ZWJ
			"\\u2028", // line separator
			"\\u2029", // paragraph separator
			"\\u202a", // LRE
			"\\u202b", // RLE
			"\\u202c", // PDF
			"\\u202d", // LRO
			"\\u202e", // RLO / bidi override
			"\\u2060", // word joiner
			"\\uFEFF", // BOM / ZWNBSP
			"\\u1680", // ogham space
			"\\u3000", // ideographic space
			"\\u2800"  // braille blank
	})
	void leavesObfuscationAndTrickUnicodeEscapes(String escape) {
		String input = "name = \"" + escape + "x\";";
		assertEquals(input, filter.filter(workspace, classInfo, input));
	}

	@Test
	void blanketUnescapeWouldExposeTricks_selectiveDoesNot() {
		String input = "a\\u200bb\\u202ec";
		assertNotEquals(input, EscapeUtil.unescapeUnicode(input), "blanket unescape exposes trick characters");
		assertEquals(input, filter.filter(workspace, classInfo, input));
	}

	@Test
	void whitespaceOnlyLiteralStaysEscaped() {
		String input = "\"\\u200b\\u200b\\u200b\"";
		assertEquals(input, filter.filter(workspace, classInfo, input));
	}
}
