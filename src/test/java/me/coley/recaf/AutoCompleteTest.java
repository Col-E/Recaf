package me.coley.recaf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import me.coley.recaf.parse.assembly.util.AutoComplete;
import me.coley.recaf.util.Classpath;

import static me.coley.recaf.parse.assembly.util.AutoComplete.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AutoComplete}.
 *
 * @author Andy Li
 */
public class AutoCompleteTest {
	@BeforeAll
	public static void initClasspath() {
		Classpath.getSystemClassIfExists(Object.class.getName());
	}

	@Nested
	@DisplayName("Internal Name Completion")
	class InternalNameCompletion {
		@ParameterizedTest
		@CsvSource({
				"java/lang/Str, java/lang/String",
				"java/util/L, java/util/List",
				"me/coley/recaf/AutoComplete, me/coley/recaf/AutoCompleteTest"
		})
		public void testSuccess(String token, String result) {
			assertThat("internal name should match", internalName(token), hasItem(result));
		}

		@ParameterizedTest
		@CsvSource({
				"java/lang/StringBu, java/lang/String",
				"java/io, java/lang/Object"
		})
		public void testNotMatch(String token, String result) {
			assertThat("internal name should not match", internalName(token), not(hasItem(result)));
		}

		@Test
		public void testInvalid() {
			assertThat("internal name should not match", internalName("java.lang.String"), emptyCollectionOf(String.class));
			assertDoesNotThrow(() -> internalName("  "));
		}
	}

	@Nested
	@DisplayName("Descriptor Name Completion")
	class DescriptorNameCompletion {
		@ParameterizedTest
		@CsvSource({
				"Ljava/lang/Str, Ljava/lang/String;",
				"[Ljava/lang/String, [Ljava/lang/String;",
				"[[[Ljava/lang/Obj, [[[Ljava/lang/Object;"
		})
		public void testSuccess(String token, String result) {
			List<String> actual = descriptorName(token);
			assertThat("descriptor should match", actual, hasItem(result));
		}

		@ParameterizedTest
		@CsvSource({
				"[Ljava/lang/Str, Ljava/lang/String;",
				"[[Ljava/lang/Str, [Ljava/lang/String;",
				"[Ljava/lang/Obj, [Ljava/lang/String"
		})
		public void testNotMatch(String token, String result) {
			assertThat("descriptor should not match", descriptorName(token), not(hasItem(result)));
		}

		@ParameterizedTest
		@ValueSource(strings = {"Ljava/not/exist", "java/lang/Str",
				"Ljava.lang", "[java/lang/", "[Ljava/lang/String;",
				";", "L", "[[L", "[L;", "L;"})
		public void testEmpty(String token) {
			assertThat("descriptor should not match", descriptorName(token), emptyCollectionOf(String.class));
		}

		@Test
		public void testInvalid() {
			assertDoesNotThrow(() -> internalName("  "));
		}
	}

	// TODO: more tests for fields and methods
}
