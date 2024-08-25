package software.coley.recaf.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StringDiff}.
 */
class StringDiffTest {
	@Test
	void testIdentity() {
		String original = """
				void foo() {
					print("hello");
				}
				""";
		List<StringDiff.Diff> diffs = StringDiff.diff(original, original);
		assertEquals(0, diffs.size(), "No diffs expected");
	}

	@Test
	void testSingleUpdate() {
		String original = """
				void foo() {
					print("hello");
				}
				""";
		String modified = """
				void foo() {
					print("goodbye");
				}
				""";

		// Expect one CHANGE diff
		List<StringDiff.Diff> diffs = StringDiff.diff(original, modified);
		assertEquals(1, diffs.size(), "Mismatch number of expected diffs");
		StringDiff.Diff diff = diffs.getFirst();
		assertEquals(StringDiff.DiffType.CHANGE, diff.type());

		// Only the string content changed
		assertEquals("hello", diff.textA());
		assertEquals("goodbye", diff.textB());
		assertEquals(original.indexOf("hello"), diff.startA());
		assertEquals(original.indexOf("hello") + "hello".length(), diff.endA());
		assertEquals(modified.indexOf("goodbye"), diff.startB());
		assertEquals(modified.indexOf("goodbye") + "goodbye".length(), diff.endB());

		// Patch should equal modified
		String patched = diff.apply(original);
		assertEquals(modified, patched);
	}

	@Test
	void testSingleRemove() {
		String original = """
				void foo() {
					print("hello");
				}""";
		String modified = """
				void foo() {
								
				}""";

		// Expect one REMOVE diff
		List<StringDiff.Diff> diffs = StringDiff.diff(original, modified);
		assertEquals(1, diffs.size(), "Mismatch number of expected diffs");
		StringDiff.Diff diff = diffs.getFirst();
		assertEquals(StringDiff.DiffType.REMOVE, diff.type());

		// The print call got removed
		assertEquals("\tprint(\"hello\");", diff.textA());
		assertEquals("", diff.textB());

		// Patch should equal modified
		String patched = diff.apply(original);
		assertEquals(modified, patched);
	}

	@Test
	void testSingleRemoveCollapseLines() {
		String original = """
				void foo() {
					print("hello");
				}""";
		String modified = "void foo() {}";

		// Expect one REMOVE diff
		List<StringDiff.Diff> diffs = StringDiff.diff(original, modified);
		assertEquals(1, diffs.size(), "Mismatch number of expected diffs");
		StringDiff.Diff diff = diffs.getFirst();
		assertEquals(StringDiff.DiffType.REMOVE, diff.type());

		// We removed multiple "lines" to turn this into a single line
		assertEquals("\n\tprint(\"hello\");\n", diff.textA());
		assertEquals("", diff.textB());

		// Patch should equal modified
		String patched = diff.apply(original);
		assertEquals(modified, patched);
	}

	@Test
	void testExampleTernary() {
		String original = """
				void foo() {
					if (bar())
					   println("is bar");
					else
					   println("not bar");
				}""";
		String modified = """
				void foo() {
					println(bar() ? "is bar" : "not bar");
				}""";

		// Expect one CHANGE diff
		List<StringDiff.Diff> diffs = StringDiff.diff(original, modified);
		assertEquals(1, diffs.size(), "Mismatch number of expected diffs");
		StringDiff.Diff diff = diffs.getFirst();
		assertEquals(StringDiff.DiffType.CHANGE, diff.type());

		// The "not bar"); is the common suffix so the code before contains the part we need to swap out.
		assertEquals("""
				if (bar())
				\t   println("is bar");
				\telse
				\t   println(""", diff.textA());
		assertEquals("println(bar() ? \"is bar\" : ", diff.textB());

		// Patch should equal modified
		String patched = diff.apply(original);
		assertEquals(modified, patched);
	}
}