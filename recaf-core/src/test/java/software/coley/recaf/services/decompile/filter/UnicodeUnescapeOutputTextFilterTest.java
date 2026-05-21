package software.coley.recaf.services.decompile.filter;

import org.junit.jupiter.api.Test;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

	@Test
	void leavesObfuscationWhitespaceEscapes() {
		assertEquals("a\\u200bb", filter.filter(workspace, classInfo, "a\\u200bb"));
		assertEquals("\\u202ex", filter.filter(workspace, classInfo, "\\u202ex"));
	}
}
