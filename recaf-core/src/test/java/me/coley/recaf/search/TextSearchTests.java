package me.coley.recaf.search;

import me.coley.recaf.TestUtils;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.JarContentSource;
import me.coley.recaf.workspace.resource.source.SingleFileContentSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextSearchTests extends TestUtils {
	private static Resource sample;
	private static Resource annotations;

	@BeforeAll
	static void setup() throws IOException {
		sample = new Resource(new SingleFileContentSource(sourcesDir.resolve("Sample.class")));
		sample.read();
		annotations = new Resource(new JarContentSource(jarsDir.resolve("DemoAnnotations.jar")));
		annotations.read();
	}

	@Nested
	@DisplayName("DemoAnnotations.jar")
	class Annotations {
		@Test
		void testAnnotationDefaultValue() {
			List<Result> results = search(annotations, "DEFAULT", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match default value string in annotation declaration");
		}

		@Test
		void testAnnotationOnClass() {
			List<Result> results = search(annotations, "Class Definition", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match annotation value on class declaration");
		}

		@Test
		void testAnnotationOnTypeParameter() {
			List<Result> results = search(annotations, "Type Parameter", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match annotation value on class type parameter declaration");
		}

		@Test
		void testAnnotationOnTypeParameterSupertype() {
			List<Result> results = search(annotations, "Type Parameter Super", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match annotation value on class type parameter super-type");
		}

		@Test
		void testAnnotationOnConstructor() {
			// Some annotations will be duplicated in both the visible and invisible lists in ASM
			List<Result> results = search(annotations, "Constructor", TextMatchMode.EQUALS);
			assertTrue(results.size() >= 1, "Failed to match annotation value on class constructor");
		}

		@Test
		void testAnnotationOnMethod() {
			// Some annotations will be duplicated in both the visible and invisible lists in ASM
			List<Result> results = search(annotations, "Method", TextMatchMode.EQUALS);
			assertTrue(results.size() >= 1, "Failed to match annotation value on method");
		}

		@Test
		void testAnnotationOnMethodArg() {
			// Some annotations will be duplicated in both the visible and invisible lists in ASM
			List<Result> results = search(annotations, "Method Argument", TextMatchMode.EQUALS);
			assertTrue(results.size() >= 1, "Failed to match annotation value on method argument");
		}

		@Test
		void testAnnotationOnMethodLocal() {
			List<Result> results = search(annotations, "Local type", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match annotation value on local variable type declaration");
		}

		@Test
		void testAnnotationOnMethodCast() {
			List<Result> results = search(annotations, "Local cast type", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match annotation value on casted type");
		}

		@Test
		void testAnnotationOnMethodInstanceOf() {
			List<Result> results = search(annotations, "Instanceof Type", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Failed to match annotation value on instanceof type");
		}
	}

	@Nested
	@DisplayName("Sample.class")
	class Sample {
		@Test
		void testEquals() {
			List<Result> results = search(sample, "Sample", TextMatchMode.EQUALS);
			assertEquals(1, results.size(), "Should contain exactly one match for 'Sample' as a string");
		}

		@Test
		void testContains() {
			List<Result> results = search(sample, "a", TextMatchMode.CONTAINS);
			assertEquals(1, results.size(), "Should contain exactly one match for 'Sample' as a string");
		}

		@Test
		void testRegex() {
			List<Result> results = search(sample, "\\w+", TextMatchMode.REGEX);
			assertEquals(1, results.size(), "Should contain exactly one match for 'Sample' as a string");
		}

		@Test
		void testStartsWith() {
			List<Result> results = search(sample, "S", TextMatchMode.STARTS_WITH);
			assertEquals(1, results.size(), "Should contain exactly one match for 'Sample' as a string");
		}

		@Test
		void testEndsWith() {
			List<Result> results = search(sample, "e", TextMatchMode.ENDS_WITH);
			assertEquals(1, results.size(), "Should contain exactly one match for 'Sample' as a string");
		}
	}

	private static List<Result> search(Resource resource, String text, TextMatchMode mode) {
		return new Search().text(text, mode).run(resource);
	}
}
