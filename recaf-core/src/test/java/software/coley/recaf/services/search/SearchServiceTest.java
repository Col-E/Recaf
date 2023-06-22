package software.coley.recaf.services.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.TestBase;
import software.coley.recaf.info.BasicTextFileInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.TextFileInfoBuilder;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.*;
import software.coley.recaf.services.search.builtin.NumberQuery;
import software.coley.recaf.services.search.builtin.ReferenceQuery;
import software.coley.recaf.services.search.builtin.StringQuery;
import software.coley.recaf.services.search.result.*;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.*;
import software.coley.recaf.util.NumberMatchMode;
import software.coley.recaf.util.TextMatchMode;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.BIPUSH;

/**
 * Tests for {@link SearchService}
 */
public class SearchServiceTest extends TestBase {
	static SearchService searchService;
	static Workspace classesWorkspace;
	static Workspace filesWorkspace;

	@BeforeAll
	static void setup() throws IOException {
		searchService = recaf.get(SearchService.class);

		// Make workspace with just some classes
		classesWorkspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.fromRuntimeClass(AccessibleFields.class),
				TestClassUtils.fromRuntimeClass(HelloWorld.class),
				TestClassUtils.fromRuntimeClass(StringConsumer.class)
		));

		// Make workspace with just some files
		BasicTextFileInfo fileHello = new TextFileInfoBuilder()
				.withName("hello.txt")
				.withRawContent("Hello world".getBytes(StandardCharsets.UTF_8))
				.build();
		BasicTextFileInfo fileNumbers = new TextFileInfoBuilder()
				.withName("numbers.txt")
				.withRawContent("1\n-1\n0xF\n0\n7".getBytes(StandardCharsets.UTF_8))
				.build();
		filesWorkspace = TestClassUtils.fromBundle(TestClassUtils.fromFiles(fileHello, fileNumbers));
	}

	@Test
	void testEmpty() {
		Results results = searchService.search(EmptyWorkspace.get(), new Query() {
			// empty query
		});
		assertTrue(results.isEmpty(), "No results should be found in an empty workspace");
	}

	@Nested
	class Jvm {
		@Test
		void testClassNumbers() {
			Results results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.EQUALS, 4));
			assertEquals(5, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.EQUALS, -32));
			assertEquals(0, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.LESS_THAN, 1));
			assertEquals(5, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.LESS_OR_EQUAL_THAN, 1));
			assertEquals(12, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.GREATER_THAN, 4));
			assertEquals(9, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.GREATER_OR_EQUAL_THAN, 4));
			assertEquals(14, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.NOT, 4));
			assertEquals(26, results.size());
		}

		@Test
		void testClassStrings() {
			Results results = searchService.search(classesWorkspace, new StringQuery(TextMatchMode.EQUALS, "Hello world"));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(TextMatchMode.CONTAINS, ":"));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(TextMatchMode.STARTS_WITH, "Hello"));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(TextMatchMode.ENDS_WITH, "world"));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(TextMatchMode.REGEX, "\\w+\\s\\w+"));
			assertEquals(1, results.size());
		}

		@Test
		void testFieldPath() {
			// Used only in constant-value attribute for field 'CONSTANT_FIELD'
			Results results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.EQUALS, 16));
			for (Result<?> result : results) {
				if (result.getPath() instanceof ClassMemberPathNode memberPath) {
					ClassMember declaredMember = memberPath.getValue();
					assertTrue(declaredMember.isField());
					assertEquals("CONSTANT_FIELD", declaredMember.getName());
					assertEquals("I", declaredMember.getDescriptor());
				} else {
					fail("Value[16] found in not CONSTANT_FIELD path");
				}
			}
		}

		@Test
		void testMethodPath() {
			// 31 is used in generated hashCode() implementations
			Results results = searchService.search(classesWorkspace, new NumberQuery(NumberMatchMode.EQUALS, 31));
			for (Result<?> result : results) {
				if (result.getPath() instanceof InstructionPathNode instructionPath) {
					assertEquals(BIPUSH, instructionPath.getValue().getOpcode());

					ClassMemberPathNode parentOfType = instructionPath.getParentOfType(ClassMember.class);
					assertNotNull(parentOfType);
					ClassMember declaredMember = parentOfType.getValue();
					assertTrue(declaredMember.isMethod());
					assertEquals("hashCode", declaredMember.getName());
					assertEquals("()I", declaredMember.getDescriptor());
				} else {
					fail("Value[31] found in not hashCode() path");
				}
			}
		}

		@Test
		void testAnnotationStrings() throws IOException {
			Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
					TestClassUtils.fromRuntimeClass(ClassWithAnnotation.class)
			));

			Results results = searchService.search(workspace, new StringQuery(TextMatchMode.EQUALS, "Hello"));
			assertEquals(1, results.size());

			Result<?> result = results.iterator().next();
			PathNode<?> path = result.getPath();
			if (path instanceof AnnotationPathNode annotationPath) {
				AnnotationInfo declaredAnnotation = annotationPath.getValue();
				assertTrue(declaredAnnotation.isVisible());
				assertEquals("L" + AnnotationImpl.class.getName().replace('.', '/') + ";",
						declaredAnnotation.getDescriptor());
			} else {
				fail("Text[arg] found not in annotation usage");
			}
		}

		@Test
		void testTypeAnnotationStrings() throws IOException {
			Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
					TestClassUtils.fromRuntimeClass(ClassWithAnnotation.class)
			));

			Results results = searchService.search(workspace, new StringQuery(TextMatchMode.EQUALS, "arg"));
			assertEquals(1, results.size());

			Result<?> result = results.iterator().next();
			PathNode<?> path = result.getPath();
			if (path instanceof AnnotationPathNode annotationPath) {
				AnnotationInfo declaredAnnotation = annotationPath.getValue();
				assertTrue(declaredAnnotation.isVisible());
				assertEquals("L" + TypeAnnotationImpl.class.getName().replace('.', '/') + ";",
						declaredAnnotation.getDescriptor());
			} else {
				fail("Text[Hello] found not in annotation usage");
			}
		}

		@Test
		void testMemberReferenceSearchSysOut() {
			// References to System.out
			Results results = searchService.search(classesWorkspace, new ReferenceQuery(
					TextMatchMode.EQUALS, "java/lang/System", "out", "Ljava/io/PrintStream;"));
			assertEquals(2, results.size());
			Results baseline = results;

			// No other references should be of PrintStream as named 'out' in the workspace
			// so when we omit parameters the results should be the same.
			results = searchService.search(classesWorkspace, new ReferenceQuery(
					TextMatchMode.EQUALS, null, "out", "Ljava/io/PrintStream;"));
			assertEquals(2, results.size());

			results = searchService.search(classesWorkspace, new ReferenceQuery(
					TextMatchMode.EQUALS, null, "out", null));
			assertEquals(2, results.size());

			results = searchService.search(classesWorkspace, new ReferenceQuery(
					TextMatchMode.EQUALS, null, null, "Ljava/io/PrintStream;"));
			assertEquals(2, results.size());

			results = searchService.search(classesWorkspace, new ReferenceQuery(
					TextMatchMode.EQUALS, "java/lang/System", null, null));
			assertEquals(2, results.size());
		}

		@Test
		void testClassReferenceToNumberFormatException() throws IOException {
			Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
					TestClassUtils.fromRuntimeClass(ClassWithExceptions.class)
			));

			Results results = searchService.search(workspace, new ReferenceQuery(
					TextMatchMode.EQUALS, "java/lang/NumberFormatException"));
			assertEquals(4, results.size());

			// Thrown type on method declaration
			Set<Result<?>> throwsMatches = results.stream()
					.filter(r -> r.getPath() instanceof ThrowsPathNode)
					.collect(Collectors.toSet());
			assertEquals(1, throwsMatches.size());

			// NEW instruction is used to create a NFE
			Set<Result<?>> insnMatches = results.stream()
					.filter(r -> r.getPath() instanceof InstructionPathNode)
					.collect(Collectors.toSet());
			assertEquals(1, insnMatches.size());

			// Type of catch block
			Set<Result<?>> catchMatches = results.stream()
					.filter(r -> r.getPath() instanceof CatchPathNode)
					.collect(Collectors.toSet());
			assertEquals(1, catchMatches.size());

			// Catch block has local variable
			Set<Result<?>> varMatches = results.stream()
					.filter(r -> r.getPath() instanceof LocalVariablePathNode)
					.collect(Collectors.toSet());
			assertEquals(1, varMatches.size());
		}
	}

	@Nested
	class File {
		@Test
		void testFileNumbers() {
			Results results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.EQUALS, 0));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.EQUALS, 0xF));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.LESS_THAN, 0));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.LESS_OR_EQUAL_THAN, 0));
			assertEquals(2, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.GREATER_THAN, 0));
			assertEquals(3, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.GREATER_OR_EQUAL_THAN, 0));
			assertEquals(4, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(NumberMatchMode.NOT, 0));
			assertEquals(4, results.size());
		}

		@Test
		void testFileStrings() {
			Results results = searchService.search(filesWorkspace, new StringQuery(TextMatchMode.EQUALS, "Hello world"));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(TextMatchMode.CONTAINS, "-"));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(TextMatchMode.STARTS_WITH, "Hello"));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(TextMatchMode.ENDS_WITH, "world"));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(TextMatchMode.REGEX, "\\w+\\s\\w+"));
			assertEquals(1, results.size());
		}
	}
}
