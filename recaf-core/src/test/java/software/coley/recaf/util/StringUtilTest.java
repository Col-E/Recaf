package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.recaf.test.TestBase;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StringUtil}
 */
class StringUtilTest {
	@Test
	void indicesOf() {
		assertArrayEquals(new int[]{0}, StringUtil.indicesOf("\n", '\n'));
		assertArrayEquals(new int[]{0, 1, 2}, StringUtil.indicesOf("\n\n\n", '\n'));
		assertArrayEquals(new int[]{1, 3, 4}, StringUtil.indicesOf("a\na\n\n", '\n'));
		assertArrayEquals(new int[]{0, 1, 3}, StringUtil.indicesOf("\n\na\na", '\n'));
	}

	@Test
	void testFastSplit() {
		// Empty discarded
		assertEquals(List.of("a", "b", "c", "d"), StringUtil.fastSplit("a/b/c/d", false, '/'));
		assertEquals(List.of("a", "b", "c"), StringUtil.fastSplit("a/b/c/", false, '/'));
		assertEquals(List.of("b", "c", "d"), StringUtil.fastSplit("/b/c/d", false, '/'));
		assertEquals(Collections.emptyList(), StringUtil.fastSplit("/", false, '/'));
		assertEquals(Collections.emptyList(), StringUtil.fastSplit("//", false, '/'));
		assertEquals(Collections.emptyList(), StringUtil.fastSplit("///", false, '/'));

		// Empty included
		assertEquals(List.of("a", "b", "c", ""), StringUtil.fastSplit("a/b/c/", true, '/'));
		assertEquals(List.of("", "b", "c", "d"), StringUtil.fastSplit("/b/c/d", true, '/'));
		assertEquals(List.of("", ""), StringUtil.fastSplit("/", true, '/'));
		assertEquals(List.of("", "", ""), StringUtil.fastSplit("//", true, '/'));
		assertEquals(List.of("", "", "", ""), StringUtil.fastSplit("///", true, '/'));
	}

	@Test
	void testGetTabAdjustedLength() {
		assertEquals(4, StringUtil.getTabAdjustedLength("\t", 4));
		assertEquals(4, StringUtil.getTabAdjustedLength(" \t", 4));
		assertEquals(4, StringUtil.getTabAdjustedLength("  \t", 4));
		assertEquals(4, StringUtil.getTabAdjustedLength("   \t", 4));
		assertEquals(4, StringUtil.getTabAdjustedLength("    ", 4));
		assertEquals(10, StringUtil.getTabAdjustedLength("\t\t", 5));
		assertEquals(10, StringUtil.getTabAdjustedLength(" \t\t", 5));
		assertEquals(10, StringUtil.getTabAdjustedLength("  \t\t", 5));
		assertEquals(10, StringUtil.getTabAdjustedLength("   \t\t", 5));
		assertEquals(10, StringUtil.getTabAdjustedLength("     \t", 5));
	}

	@Test
	void testGetWhitespacePrefixLength() {
		assertEquals(4, StringUtil.getWhitespacePrefixLength("    text", 4));
		assertEquals(4, StringUtil.getWhitespacePrefixLength("   \ttext", 4));
		assertEquals(4, StringUtil.getWhitespacePrefixLength("  \ttext", 4));
		assertEquals(4, StringUtil.getWhitespacePrefixLength(" \ttext", 4));
		assertEquals(4, StringUtil.getWhitespacePrefixLength("\ttext", 4));
		assertEquals(8, StringUtil.getWhitespacePrefixLength("    \ttext", 4));
		assertEquals(8, StringUtil.getWhitespacePrefixLength("   \t \ttext", 4));
		assertEquals(8, StringUtil.getWhitespacePrefixLength("  \t   \ttext", 4));
		assertEquals(8, StringUtil.getWhitespacePrefixLength("\t \ttext", 4));
		assertEquals(8, StringUtil.getWhitespacePrefixLength(" \t   \ttext", 4));
	}

	@Test
	void testSplitNewline() {
		assertEquals(1, StringUtil.splitNewline("").length);
		assertEquals(1, StringUtil.splitNewline("a").length);
		assertEquals(2, StringUtil.splitNewline("a\nb").length);
		assertEquals(2, StringUtil.splitNewline("a\n\rb").length);
		assertEquals(2, StringUtil.splitNewline("a\r\nb").length);
		assertEquals(3, StringUtil.splitNewline("a\n\nb").length);
		assertEquals(3, StringUtil.splitNewline("a\n\r\r\nb").length);
		assertEquals(3, StringUtil.splitNewline("a\r\n\r\nb").length);
		assertEquals(3, StringUtil.splitNewline("a\r\n\n\rb").length);
		assertEquals(3, StringUtil.splitNewline("a\nb\nc").length);
	}

	@Test
	void testSplitNewlineSkipEmpty() {
		assertEquals(1, StringUtil.splitNewlineSkipEmpty("").length);
		assertEquals(1, StringUtil.splitNewlineSkipEmpty("a").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\nb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\n\rb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\r\nb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\n\nb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\n\r\r\nb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\r\n\r\nb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\r\n\n\rb").length);
		assertEquals(2, StringUtil.splitNewlineSkipEmpty("a\r\n\n\n\n\n\rb").length);
	}

	@Test
	void testCutOffAtFirst() {
		// chars
		assertEquals("", StringUtil.cutOffAtFirst("", 'd'));
		assertEquals("abc", StringUtil.cutOffAtFirst("abcdefg", 'd'));
		assertEquals("abc", StringUtil.cutOffAtFirst("abcdefgd", 'd'));
		// strings
		assertEquals("", StringUtil.cutOffAtFirst("", "d"));
		assertEquals("abc", StringUtil.cutOffAtFirst("abcdefg", "d"));
		assertEquals("abc", StringUtil.cutOffAtFirst("abcdefgd", "d"));
	}

	@Test
	void testCutOffAtLast() {
		// chars
		assertEquals("", StringUtil.cutOffAtLast("", 'd'));
		assertEquals("abc", StringUtil.cutOffAtLast("abcdefg", 'd'));
		assertEquals("abcdefg", StringUtil.cutOffAtLast("abcdefgd", 'd'));
		// strings
		assertEquals("", StringUtil.cutOffAtLast("", "d"));
		assertEquals("abc", StringUtil.cutOffAtLast("abcdefg", "d"));
		assertEquals("abcdefg", StringUtil.cutOffAtLast("abcdefgd", "d"));
	}

	@Test
	void testCutOffAtNth() {
		// chars
		assertEquals("", StringUtil.cutOffAtNth("", 'a', -1));
		assertEquals("", StringUtil.cutOffAtNth("", 'a', 0));
		assertEquals("", StringUtil.cutOffAtNth("", 'a', 1));
		assertEquals("aaa", StringUtil.cutOffAtNth("aaa", 'a', -1));
		assertEquals("aaa", StringUtil.cutOffAtNth("aaa", 'a', 0));
		assertEquals("", StringUtil.cutOffAtNth("aaa", 'a', 1));
		assertEquals("a", StringUtil.cutOffAtNth("aaa", 'a', 2));
		assertEquals("aa", StringUtil.cutOffAtNth("aaa", 'a', 3));
		assertEquals("aaa", StringUtil.cutOffAtNth("aaa", 'a', 4));
		// strings
		assertEquals("", StringUtil.cutOffAtNth("", "a", -1));
		assertEquals("", StringUtil.cutOffAtNth("", "a", 0));
		assertEquals("", StringUtil.cutOffAtNth("", "a", 1));
		assertEquals("aaa", StringUtil.cutOffAtNth("aaa", "a", -1));
		assertEquals("aaa", StringUtil.cutOffAtNth("aaa", "a", 0));
		assertEquals("", StringUtil.cutOffAtNth("aaa", "a", 1));
		assertEquals("a", StringUtil.cutOffAtNth("aaa", "a", 2));
		assertEquals("aa", StringUtil.cutOffAtNth("aaa", "a", 3));
		assertEquals("aaa", StringUtil.cutOffAtNth("aaa", "a", 4));
		assertEquals("b", StringUtil.cutOffAtNth("bbbbbbbbbb", "bb", 2));
		assertEquals("bb", StringUtil.cutOffAtNth("bbbbbbbbbb", "bb", 3));
		assertEquals("bbb", StringUtil.cutOffAtNth("bbbbbbbbbb", "bb", 4));
	}

	@Test
	void testGetAfter() {
		assertEquals("", StringUtil.getAfter("", ""));
		assertEquals("d", StringUtil.getAfter("a b c d", "c "));
		assertEquals("a b c d", StringUtil.getAfter("a b c d", "z"));
	}

	@Test
	void testInsert() {
		assertEquals("in", StringUtil.insert("in", -1, "prefix"));
		assertEquals("in", StringUtil.insert("in", 0, null));
		assertEquals("in", StringUtil.insert("in", 0, ""));
		assertEquals("in", StringUtil.insert("in", 999, "suffix"));
		assertEquals("prefixin", StringUtil.insert("in", 0, "prefix"));
		assertEquals("insuffix", StringUtil.insert("in", 2, "suffix"));
	}

	@Test
	void testRemove() {
		assertEquals("demo", StringUtil.remove("demo", -1, 4));
		assertEquals("demo", StringUtil.remove("demo", 999, 4));
		assertEquals("do", StringUtil.remove("demo", 1, 2));
		assertEquals("", StringUtil.remove("demo", 0, 4));
		assertEquals("", StringUtil.remove("demo", 0, 999));
	}

	@Test
	void testWithEmptyFallback() {
		assertEquals("fall", StringUtil.withEmptyFallback(null, "fall"));
		assertEquals("fall", StringUtil.withEmptyFallback("", "fall"));
		assertEquals("fall", StringUtil.withEmptyFallback(" ", "fall"));
		assertEquals("fall", StringUtil.withEmptyFallback("\n", "fall"));
		assertEquals("demo", StringUtil.withEmptyFallback("demo", "fall"));
	}

	@Test
	void testIsDecimal() {
		assertTrue(StringUtil.isDecimal("1"));
		assertTrue(StringUtil.isDecimal("1.0"));
		assertTrue(StringUtil.isDecimal("-1.0"));
		assertTrue(StringUtil.isDecimal("1" + "0".repeat(Short.MAX_VALUE)));
		assertFalse(StringUtil.isDecimal(""));
		assertFalse(StringUtil.isDecimal(" "));
		assertFalse(StringUtil.isDecimal("\0"));
	}

	@Test
	void testReplaceLast() {
		assertEquals("a a x", StringUtil.replaceLast("a a a", "a", "x"));
		assertEquals("a a a", StringUtil.replaceLast("a a a", "z", "x"));
	}

	@Test
	void testReplacePrefix() {
		assertEquals("xa a a", StringUtil.replacePrefix("a a a", null, "x"));
		assertEquals("xa a a", StringUtil.replacePrefix("a a a", "", "x"));
		assertEquals("x a a", StringUtil.replacePrefix("a a a", "a", "x"));
		assertEquals("a a a", StringUtil.replacePrefix("a a a", "z", "x"));
	}

	@Test
	void testReplaceRange() {
		assertEquals("_cdefg", StringUtil.replaceRange("abcdefg", 0, 2, "_"));
	}

	@Test
	void testCount() {
		assertEquals(0, StringUtil.count('a', null));
		assertEquals(0, StringUtil.count("a", null));
		assertEquals(0, StringUtil.count('a', ""));
		assertEquals(0, StringUtil.count("a", ""));
		assertEquals(0, StringUtil.count('a', "foo"));
		assertEquals(0, StringUtil.count("a", "foo"));
		assertEquals(2, StringUtil.count('a', "example bar"));
		assertEquals(2, StringUtil.count("a", "example bar"));
	}

	@Test
	void testPathToString() {
		assertEquals("foo" + File.separator + "bar.txt", StringUtil.pathToString(Paths.get("foo/bar.txt")));
	}

	@Test
	void testPathToNameString() {
		assertEquals("bar.txt", StringUtil.pathToNameString(Paths.get("foo/bar.txt")));
	}

	@Test
	void testShortenPath() {
		assertEquals("bar.txt", StringUtil.shortenPath("a/b/c/d/bar.txt"));
	}

	@Test
	void testRemoveExtension() {
		assertEquals("bar", StringUtil.removeExtension("bar.txt"));
		assertEquals("bar.tar", StringUtil.removeExtension("bar.tar.gz"));
	}

	@Test
	void testLimit() {
		assertEquals("abcdefg", StringUtil.limit("abcdefg", 999));
		assertEquals("abc", StringUtil.limit("abcdefg", 3));
		assertEquals("", StringUtil.limit("abcdefg", 0));
		assertEquals("", StringUtil.limit("abcdefg", -1));
		assertEquals("abc...", StringUtil.limit("abcdefg", "...", 3));
	}

	@Test
	void testLowercaseFirstChar() {
		assertEquals("", StringUtil.lowercaseFirstChar(""));
		assertEquals("f", StringUtil.lowercaseFirstChar("F"));
		assertEquals("foo", StringUtil.lowercaseFirstChar("Foo"));
		assertEquals("fOO", StringUtil.lowercaseFirstChar("FOO"));
	}

	@Test
	void testUppercaseFirstChar() {
		assertEquals("", StringUtil.uppercaseFirstChar(""));
		assertEquals("F", StringUtil.uppercaseFirstChar("f"));
		assertEquals("Foo", StringUtil.uppercaseFirstChar("foo"));
		assertEquals("FOO", StringUtil.uppercaseFirstChar("fOO"));
	}

	@Test
	void testGetCommonPrefix() {
		assertEquals("", StringUtil.getCommonPrefix("", ""));
		assertEquals("1", StringUtil.getCommonPrefix("123", "1bc"));
		assertEquals("", StringUtil.getCommonPrefix("aaa", "bbb"));
		assertEquals("com/", StringUtil.getCommonPrefix("com/foo", "com/bar"));
	}

	@Test
	void testGetCommonSuffix() {
		assertEquals("", StringUtil.getCommonSuffix("", ""));
		assertEquals("1", StringUtil.getCommonSuffix("321", "ab1"));
		assertEquals("1", StringUtil.getCommonSuffix("31", "ab1"));
		assertEquals("1", StringUtil.getCommonSuffix("1", "ab1"));
		assertEquals("", StringUtil.getCommonSuffix("", "ab1"));
		assertEquals("", StringUtil.getCommonSuffix("ab1", ""));
		assertEquals("", StringUtil.getCommonSuffix("aaa", "bbb"));
		String codeIfElse = """
				if (bar())
				   println("is bar");
				else
				   println("not bar");""";
		String codeTernary = """
				println(bar() ? "is bar" : "not bar");""";
		assertEquals("\"not bar\");", StringUtil.getCommonSuffix(codeIfElse, codeTernary));
	}

	@Test
	void testFillLeft() {
		assertEquals("aaab", StringUtil.fillLeft(4, "a", "b"));
		assertEquals("aaaa", StringUtil.fillLeft(4, "a", null));
	}

	@Test
	void testFillRight() {
		assertEquals("baaa", StringUtil.fillRight(4, "a", "b"));
		assertEquals("aaaa", StringUtil.fillRight(4, "a", null));
	}

	@Test
	void testIsAnyNullOrEmpty() {
		assertTrue(StringUtil.isAnyNullOrEmpty("", "a", null));
		assertTrue(StringUtil.isAnyNullOrEmpty("", "a"));
		assertTrue(StringUtil.isAnyNullOrEmpty("a", null));
		assertTrue(StringUtil.isAnyNullOrEmpty(""));
		assertTrue(StringUtil.isAnyNullOrEmpty((String) null));
		assertFalse(StringUtil.isAnyNullOrEmpty("a", "b", "c"));
	}

	@Test
	void testGetEntropy() {
		assertEquals(0, StringUtil.getEntropy(null));
		assertEquals(0, StringUtil.getEntropy(""));
		assertEquals(0, StringUtil.getEntropy("a"));
		assertEquals(0, StringUtil.getEntropy("aa"));
		assertEquals(0, StringUtil.getEntropy("aaa"));
		//
		assertEquals(1, StringUtil.getEntropy("ab"));
		assertEquals(1, StringUtil.getEntropy("\u0000\uFFFF"));
		//
		assertEquals(1.5849625, StringUtil.getEntropy("abc"), 0.0001);
		assertEquals(1.5849625, StringUtil.getEntropy("aabbcc"), 0.0001);
		assertEquals(0.6500224, StringUtil.getEntropy("aaaaax"), 0.0001);
	}

	@Test
	void testTraceToString() {
		String trace = StringUtil.traceToString(new Throwable("Message"));
		assertTrue(trace.startsWith("java.lang.Throwable: Message"));
		assertTrue(trace.contains("at " + getClass().getName() + ".testTraceToString"));
	}

	@Test
	void testToHexString() {
		assertEquals("0", StringUtil.toHexString(0));
		assertEquals("ffffffff", StringUtil.toHexString(-1));
		assertEquals("10", StringUtil.toHexString(16));
		assertEquals("fffffff0", StringUtil.toHexString(-16));
	}

	@Test
	void testGenerateIncrementingName() {
		assertEquals("a", StringUtil.generateIncrementingName("abc", 0));
		assertEquals("b", StringUtil.generateIncrementingName("abc", 1));
		assertEquals("c", StringUtil.generateIncrementingName("abc", 2));
		assertEquals("aa", StringUtil.generateIncrementingName("abc", 3));
		assertEquals("ab", StringUtil.generateIncrementingName("abc", 4));
		assertEquals("ac", StringUtil.generateIncrementingName("abc", 5));
		assertEquals("ba", StringUtil.generateIncrementingName("abc", 6));
	}

	@Test
	void testGenerateName() {
		String generated = StringUtil.generateName("abcdefg", 10, 0);
		assertNotNull(generated);
		assertEquals(10, generated.length());
	}

	@Nested
	class StringDecoding {
		@Test
		void testDecodeString() {
			// Text only
			var result = StringUtil.decodeString("hello".getBytes(StandardCharsets.UTF_8));
			assertTrue(result.couldDecode(), "Failed to decode 'hello' in UTF8");
			assertEquals("hello", result.text(), "Decoded result did not yield original content");
			assertEquals(StandardCharsets.UTF_8, result.charset(), "Unexpected charset for decoding 'hello' in UTF8");

			result = StringUtil.decodeString("hello".getBytes(StandardCharsets.ISO_8859_1));
			assertTrue(result.couldDecode(), "Failed to decode 'hello' in ISO_8859_1");
			assertEquals("hello", result.text(), "Decoded result did not yield original content");
			assertEquals(StandardCharsets.UTF_8, result.charset(), "Unexpected charset for decoding 'hello' in ISO_8859_1");

			result = StringUtil.decodeString("12345".getBytes(StandardCharsets.UTF_8));
			assertTrue(result.couldDecode(), "Failed to decode '12345' in UTF8");
			assertEquals("12345", result.text(), "Decoded result did not yield original content");

			// Ensure that long text doesn't get decoded wrong.
			String longString = "\"the quick brown fox jumps over the lazy dog\"".repeat(2000);
			result = StringUtil.decodeString(longString.getBytes(StandardCharsets.ISO_8859_1));
			assertTrue(result.couldDecode(), "Failed to decode 'the-quick-brown-fox' in ISO_8859_1");
			assertEquals(longString, result.text(), "Decoded result did not yield original content");

			// Empty
			result = StringUtil.decodeString(new byte[0]);
			assertFalse(result.couldDecode(), "Should fail to decode empty data");

			// Control category only
			result = StringUtil.decodeString("\0\1\2\3".getBytes(StandardCharsets.UTF_8));
			assertFalse(result.couldDecode(), "Should fail to decode control category chars");

			// Format category only
			result = StringUtil.decodeString("\u00AD\u0600\u202E\uFFFA".getBytes(StandardCharsets.UTF_8));
			assertFalse(result.couldDecode(), "Should fail to decode format category chars");

			// Private use category only
			result = StringUtil.decodeString("\uE000\uE001\uE002\uE003".getBytes(StandardCharsets.UTF_8));
			assertFalse(result.couldDecode(), "Should fail to decode private-use category chars");

			// Surrogate category only
			result = StringUtil.decodeString("\uD83C\uDF09\uD83C\uDF09".getBytes(StandardCharsets.UTF_8));
			assertFalse(result.couldDecode(), "Should fail to decode surrogate category chars");

			// Unassigned category only
			result = StringUtil.decodeString("\u0378\u0378\u0378\u0378".getBytes(StandardCharsets.UTF_8));
			assertEquals(StandardCharsets.ISO_8859_1, result.charset(), "Should fail to decode unassigned category chars as UTF8 - fall back to ISO_8859_1");

			// Chars outside the acceptable range won't get decoded, which tells us our input has non-text chars.
			result = StringUtil.decodeString(new byte[]{0, 1, 15, 20, 26, 31});
			assertFalse(result.couldDecode(), "Should fail to decode with control chars (char < 32)");
		}

		@ParameterizedTest
		@ValueSource(strings = {
			 	"lorem-long-ascii.txt",
			 	"lorem-long-cn.txt",
			 	"lorem-long-ru.txt",
			 	"lorem-short-ascii.txt",
			 	"lorem-short-cn.txt",
				"lorem-short-ru.txt"
		})
		void testDecodeLoremIpsum(@Nonnull String name) throws Exception {
			decodeFromPath(name);
		}

		void decodeFromPath(@Nonnull String path) throws Exception {
			byte[] bytes = Files.readAllBytes(Paths.get("src/testFixtures/resources/" + path));
			var result = StringUtil.decodeString(bytes);
			assertTrue(result.couldDecode());
		}
	}
}