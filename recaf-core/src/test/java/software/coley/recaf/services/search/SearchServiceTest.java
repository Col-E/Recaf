package software.coley.recaf.services.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BasicTextFileInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.TextFileInfoBuilder;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.CatchPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.InstructionPathNode;
import software.coley.recaf.path.LocalVariablePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.ThrowsPathNode;
import software.coley.recaf.services.search.match.NumberPredicateProvider;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.NumberQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.query.ReferenceQuery;
import software.coley.recaf.services.search.query.StringQuery;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.AnnotationImpl;
import software.coley.recaf.test.dummy.ClassWithAnnotation;
import software.coley.recaf.test.dummy.ClassWithExceptions;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.TypeAnnotationImpl;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static software.coley.recaf.test.TestClassUtils.*;

/**
 * Tests for {@link SearchService}
 */
public class SearchServiceTest extends TestBase {
	static NumberPredicateProvider numMatchProvider;
	static StringPredicateProvider strMatchProvider;
	static SearchService searchService;
	static Workspace classesWorkspace;
	static Workspace filesWorkspace;

	@BeforeAll
	static void setup() throws IOException {
		numMatchProvider = recaf.get(NumberPredicateProvider.class);
		strMatchProvider = recaf.get(StringPredicateProvider.class);
		searchService = recaf.get(SearchService.class);

		// Make workspace with just some classes
		classesWorkspace = fromBundle(fromClasses(
				fromRuntimeClass(AccessibleFields.class),
				fromRuntimeClass(HelloWorld.class),
				fromRuntimeClass(StringConsumer.class)
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
		filesWorkspace = fromBundle(fromFiles(fileHello, fileNumbers));
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
			Results results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(4)));
			assertEquals(5, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(32)));
			assertEquals(0, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newLessThanPredicate(1)));
			assertEquals(5, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newLessThanOrEqualPredicate(1)));
			assertEquals(12, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newGreaterThanPredicate(4)));
			assertEquals(9, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newGreaterThanOrEqualPredicate(4)));
			assertEquals(14, results.size());

			results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newNotEqualsPredicate(4)));
			assertEquals(26, results.size());
		}

		@Test
		void testClassStrings() {
			Results results = searchService.search(classesWorkspace, new StringQuery(strMatchProvider.newEqualPredicate("Hello world")));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(strMatchProvider.newContainsPredicate(":")));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(strMatchProvider.newStartsWithPredicate("Hello")));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(strMatchProvider.newEndsWithPredicate("world")));
			assertEquals(1, results.size());

			results = searchService.search(classesWorkspace, new StringQuery(strMatchProvider.newPartialRegexPredicate("\\w+\\s\\w+")));
			assertEquals(1, results.size());
		}

		@Test
		void testFieldPath() {
			// Used only in constant-value attribute for field 'CONSTANT_FIELD'
			Results results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(16)));
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
			Results results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(31)));
			for (Result<?> result : results) {
				if (result.getPath() instanceof InstructionPathNode instructionPath) {
					assertEquals(BIPUSH, instructionPath.getValue().getOpcode());

					ClassMemberPathNode memberPath = instructionPath.getPathOfType(ClassMember.class);
					assertNotNull(memberPath);
					ClassMember declaredMember = memberPath.getValue();
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
			Workspace workspace = fromBundle(fromClasses(
					fromRuntimeClass(ClassWithAnnotation.class)
			));

			Results results = searchService.search(workspace, new StringQuery(strMatchProvider.newEqualPredicate("Hello")));
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
			Workspace workspace = fromBundle(fromClasses(
					fromRuntimeClass(ClassWithAnnotation.class)
			));

			Results results = searchService.search(workspace, new StringQuery(strMatchProvider.newEqualPredicate("arg")));
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
					strMatchProvider.newEqualPredicate("java/lang/System"),
					strMatchProvider.newEqualPredicate("out"),
					strMatchProvider.newEqualPredicate("Ljava/io/PrintStream;")));
			assertEquals(2, results.size());
			Results baseline = results;

			// No other references should be of PrintStream as named 'out' in the workspace
			// so when we omit parameters the results should be the same.
			results = searchService.search(classesWorkspace, new ReferenceQuery(
					null,
					strMatchProvider.newEqualPredicate("out"),
					strMatchProvider.newEqualPredicate("Ljava/io/PrintStream;")
			));
			assertEquals(2, results.size());

			results = searchService.search(classesWorkspace, new ReferenceQuery(
					null,
					strMatchProvider.newEqualPredicate("out"),
					null
			));
			assertEquals(2, results.size());

			results = searchService.search(classesWorkspace, new ReferenceQuery(
					null,
					null,
					strMatchProvider.newEqualPredicate("Ljava/io/PrintStream;")
			));
			assertEquals(2, results.size());

			results = searchService.search(classesWorkspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("java/lang/System"),
					null, null
			));
			assertEquals(2, results.size());
		}

		@Test
		void testClassReferenceToNumberFormatException() throws IOException {
			Workspace workspace = fromBundle(fromClasses(
					fromRuntimeClass(ClassWithExceptions.class)
			));

			Results results = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("java/lang/NumberFormatException")
			));
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
			Results results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(0)));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(0xF)));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newAnyOfPredicate(0, 0xF)));
			assertEquals(2, results.size()); // combination of prior two

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newRangePredicate(0, 10)));
			assertEquals(3, results.size()); // should find 0, 1, and 7

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newLessThanPredicate(0)));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newLessThanOrEqualPredicate(0)));
			assertEquals(2, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newGreaterThanPredicate(0)));
			assertEquals(3, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newGreaterThanOrEqualPredicate(0)));
			assertEquals(4, results.size());

			results = searchService.search(filesWorkspace, new NumberQuery(numMatchProvider.newNotEqualsPredicate(0)));
			assertEquals(4, results.size());
		}

		@Test
		void testFileStrings() {
			Results results = searchService.search(filesWorkspace, new StringQuery(strMatchProvider.newEqualPredicate("Hello world")));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(strMatchProvider.newContainsPredicate("-")));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(strMatchProvider.newStartsWithPredicate("Hello")));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(strMatchProvider.newEndsWithPredicate("world")));
			assertEquals(1, results.size());

			results = searchService.search(filesWorkspace, new StringQuery(strMatchProvider.newPartialRegexPredicate("\\w+\\s\\w+")));
			assertEquals(1, results.size());
		}
	}
}
