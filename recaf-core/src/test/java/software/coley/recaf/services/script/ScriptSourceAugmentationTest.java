package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ScriptSourceAugmentation}.
 */
class ScriptSourceAugmentationTest {
	@Test
	void addsSyntheticPackageAndDefaultImportsToDefaultPackageClassScript() {
		// Basic class model script. Run method, no imports/package.
		String source = """
				public class Demo {
					List<String> names;
					void run() {}
				}
				""";

		// Apply post-processing/augmentation to the source.
		AugmentedSource augmented = ScriptSourceAugmentation.augmentClassScript(source);
		String augmentedText = augmented.augmentedSource();

		// Validate the augmented source has:
		//  - A synthetic package declaration.
		//  - Default imports for util packages + recaf services.
		assertEquals("software.coley.recaf.generated", augmented.packageName());
		assertTrue(augmentedText.startsWith("package software.coley.recaf.generated;"));
		assertTrue(augmentedText.contains("import java.util.*;"));
		assertTrue(augmentedText.contains("import software.coley.recaf.services.workspace.*;"));

		// Validate that tokens from the original source are still present in the augmented source, and that their offsets map correctly.
		assertMappedToken(augmented, "List<String> names");
		assertMappedToken(augmented, "void run()");

		// Validate that line mappings are correct for tokens from the original source.
		assertLineMapping(augmented, "public class Demo");
		assertLineMapping(augmented, "List<String> names");
		assertLineMapping(augmented, "void run()");
	}

	@Test
	void keepsExplicitPackageAndOriginalImportsAligned() {
		// Similar to the prior test, but we have an explicit package declaration
		// and an import that should be preserved in the augmented source.
		String source = """
				package example.demo;
				import java.time.Instant;
				
				public class Demo {
					Instant now;
					List<String> names;
				}
				""";

		// Apply post-processing/augmentation to the source.
		AugmentedSource augmented = ScriptSourceAugmentation.augmentClassScript(source);
		String augmentedText = augmented.augmentedSource();

		// Validate the augmented source has:
		//  - The original package declaration is preserved.
		//  - The original import is preserved and appears after the package declaration.
		//  - But also the utils/service imports.
		assertEquals("example.demo", augmented.packageName());
		assertTrue(augmentedText.startsWith("package example.demo;"));
		assertTrue(augmentedText.contains("import java.util.*;"));
		assertTrue(augmentedText.indexOf("import java.util.*;") > augmentedText.indexOf("package example.demo;"));
		assertTrue(augmentedText.indexOf("import java.time.Instant;") > augmentedText.indexOf("import java.util.*;"));
		assertTrue(augmentedText.contains("import software.coley.recaf.services.workspace.*;"));

		// Validate that tokens from the original source are still present in the augmented source, and that their offsets map correctly.
		assertMappedToken(augmented, "import java.time.Instant;");
		assertMappedToken(augmented, "Instant now;");
		assertMappedToken(augmented, "List<String> names");

		// Validate that line mappings are correct for tokens from the original source.
		assertLineMapping(augmented, "import java.time.Instant;");
		assertLineMapping(augmented, "Instant now;");
		assertLineMapping(augmented, "List<String> names");
	}

	@Test
	void snippetScriptsMapOffsetsIntoGeneratedMethodBody() {
		// Snippet script that assumes a much simpler Workspace + void run shape.
		String source = """
				System.out.println("hello");
				int value = 42;
				""";

		// Apply post-processing/augmentation to the source.
		AugmentedSource augmented = ScriptSourceAugmentation.augmentSnippetClass("Script123", source).source();

		// Validate that tokens from the original source are still present in the augmented source, and that their offsets map correctly.
		assertMappedToken(augmented, "System.out.println(\"hello\");");
		assertMappedToken(augmented, "int value = 42;");
		assertLineMapping(augmented, "System.out.println(\"hello\");");
		assertLineMapping(augmented, "int value = 42;");
	}

	/**
	 * @param augmented
	 * 		Model to check.
	 * @param token
	 * 		Token from the original source to check for in the augmented source.
	 */
	private static void assertMappedToken(@Nonnull AugmentedSource augmented,
	                                      @Nonnull String token) {
		int originalIndex = augmented.originalSource().indexOf(token);
		int augmentedIndex = augmented.augmentedSource().indexOf(token);
		assertEquals(augmentedIndex, augmented.mapOriginalToAugmented(originalIndex),
				"Unexpected augmented offset for token: " + token);
	}

	/**
	 * @param augmented
	 * 		Model to check.
	 * @param token
	 * 		Token from the original source to check for in the augmented source.
	 */
	private static void assertLineMapping(@Nonnull AugmentedSource augmented,
	                                      @Nonnull String token) {
		String originalSource = augmented.originalSource();
		int originalIndex = originalSource.indexOf(token);
		int augmentedIndex = augmented.augmentedSource().indexOf(token);
		assertEquals(lineNumberAt(originalSource, originalIndex),
				augmented.mapAugmentedLineToOriginal(lineNumberAt(augmented.augmentedSource(), augmentedIndex)),
				"Unexpected original line mapping for token: " + token);
	}

	/**
	 * @param text
	 * 		Text to check.
	 * @param offset
	 * 		Offset in the text to find the line number of.
	 *
	 * @return 1-based line number at the given offset in the text.
	 */
	private static int lineNumberAt(String text, int offset) {
		int line = 1;
		for (int i = 0; i < offset; i++)
			if (text.charAt(i) == '\n')
				line++;
		return line;
	}
}
