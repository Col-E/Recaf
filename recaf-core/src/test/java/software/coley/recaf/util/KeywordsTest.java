package software.coley.recaf.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Keywords}.
 */
class KeywordsTest {
	@Test
	void containsKeywordFindsWholeParts() {
		assertTrue(Keywords.containsKeyword("void"));
		assertTrue(Keywords.containsKeyword("class"));
		assertTrue(Keywords.containsKeyword("this"));
		assertTrue(Keywords.containsKeyword("int"));
		assertTrue(Keywords.containsKeyword("null"));

		// Names are split at characters that cannot be in an identifier.
		assertTrue(Keywords.containsKeyword("com/example/void"));
		assertTrue(Keywords.containsKeyword("void/example/Example"));
		assertTrue(Keywords.containsKeyword("a.b.class"));
		assertTrue(Keywords.containsKeyword("some name with while"));
		assertTrue(Keywords.containsKeyword("a-if-b"));
		assertTrue(Keywords.containsKeyword("<new>"));
	}

	@Test
	void containsKeywordIgnoresKeywordsInsideLargerParts() {
		assertFalse(Keywords.containsKeyword(""));
		assertFalse(Keywords.containsKeyword("hello"));
		assertFalse(Keywords.containsKeyword("classes"));
		assertFalse(Keywords.containsKeyword("a_class"));
		assertFalse(Keywords.containsKeyword("voidValue"));
		assertFalse(Keywords.containsKeyword("com/example/Example"));
		assertFalse(Keywords.containsKeyword("Class"), "Keywords are case-sensitive");
		assertFalse(Keywords.containsKeyword("/"));
	}

	@Test
	void containsKeywordIgnoresWordsThatCanBeVariableNames() {
		for (String name : new String[]{"var", "record", "yield", "sealed", "permits", "module", "open", "transitive"})
			assertFalse(Keywords.containsVariableKeyword(name), name + " is usable as a variable name");
	}

	@Test
	void specialClassNamesAreNotTreatedAsKeywords() {
		// 'package-info' splits into the keyword 'package' and 'info', but it is a legal class name.
		assertFalse(Keywords.containsClassKeyword("package-info"));
		assertFalse(Keywords.containsClassKeyword("module-info"));
		assertFalse(Keywords.containsClassKeyword("com/example/package-info"));
		assertFalse(Keywords.containsClassKeyword("com/example/module-info"));

		// A keyword next to the special name still counts.
		assertTrue(Keywords.containsClassKeyword("com/void/package-info"));
		assertTrue(Keywords.containsClassKeyword("package-info-void"));
		assertTrue(Keywords.containsClassKeyword("package-class"));
	}
}
