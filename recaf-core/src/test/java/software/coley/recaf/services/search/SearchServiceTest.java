package software.coley.recaf.services.search;

import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.EnumConstant;
import me.darknet.dex.tree.definitions.constant.MemberConstant;
import me.darknet.dex.tree.definitions.constant.TypeConstant;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Invoke;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BasicTextFileInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.TextFileInfoBuilder;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.AndroidInstructionPathNode;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.CatchPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.JvmInstructionPathNode;
import software.coley.recaf.path.LocalVariablePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.ThrowsPathNode;
import software.coley.recaf.services.search.match.NumberPredicateProvider;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.DeclarationQuery;
import software.coley.recaf.services.search.query.InstructionQuery;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;
import static software.coley.recaf.test.TestClassUtils.*;
import static software.coley.recaf.test.TestDexUtils.newAndroidClass;
import static software.coley.recaf.test.TestDexUtils.newDexMethod;

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
		void testInsnSearch() {
			Results results = searchService.search(classesWorkspace, new InstructionQuery(List.of(
					strMatchProvider.newEqualPredicate("getstatic java/lang/System.out Ljava/io/PrintStream;"),
					strMatchProvider.newEqualPredicate("ldc \"Hello world\""),
					strMatchProvider.newEqualPredicate("invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V")
			)));
			assertEquals(1, results.size());
		}

		@Test
		void testFieldPath() {
			// Used only in constant-value attribute for field 'CONSTANT_FIELD'
			Results results = searchService.search(classesWorkspace, new NumberQuery(numMatchProvider.newEqualsPredicate(16)));
			for (Result<?> result : results) {
				ClassMember declaredMember = result.getPath().getValueOfType(ClassMember.class);
				if (declaredMember != null) {
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
				if (result.getPath() instanceof JvmInstructionPathNode instructionPath) {
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
					.filter(r -> r.getPath() instanceof JvmInstructionPathNode)
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

		@Test
		void testMemberDeclarations() {
			Results results = searchService.search(classesWorkspace, new DeclarationQuery(
					strMatchProvider.newEndsWithPredicate("AccessibleFields"),
					strMatchProvider.newEqualPredicate("CONSTANT_FIELD"),
					strMatchProvider.newEqualPredicate("I")
			));
			assertEquals(1, results.size());

			// Only 1 class in the provided workspace has an equals implemented
			results = searchService.search(classesWorkspace, new DeclarationQuery(
					null,
					strMatchProvider.newEqualPredicate("equals"),
					null
			));
			assertEquals(1, results.size());
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

	@Nested
	class Android {
		@Test
		void testMemberReferenceSearchInvoke() {
			// class AndroidCaller {
			//     static void call() {
			//         AndroidTarget.callee();
			//     }
			// }
			Workspace workspace = fromBundle(fromClasses(
					newAndroidClass("demo/AndroidCaller", newDexMethod("call", "()V", ACC_PUBLIC | ACC_STATIC, code -> code
							.arguments(0, 0)
							.registers(0)
							.invoke(Invoke.STATIC, Types.instanceTypeFromInternalName("demo/AndroidTarget"), "callee",
									Types.methodTypeFromDescriptor("()V"))
							.return_void()
					))
			));

			// Search for the 'AndroidTarget.callee()' reference.
			Results results = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("demo/AndroidTarget"),
					strMatchProvider.newEqualPredicate("callee"),
					strMatchProvider.newEqualPredicate("()V")));
			assertEquals(1, results.size());

			// Should be a single result with a path to the invoke instruction.
			Result<?> result = results.getFirst();
			assertInstanceOf(AndroidInstructionPathNode.class, result.getPath());

			// The path should indicate the result is from 'AndroidCaller.call()'.
			ClassMemberPathNode memberPath = result.getPath().getPathOfType(ClassMember.class);
			assertNotNull(memberPath);
			assertEquals("call", memberPath.getValue().getName());
			assertEquals("()V", memberPath.getValue().getDescriptor());
		}

		@Test
		void testClassReferenceSearchTargetType() {
			// class TargetType {}
			// class AndroidReferenceHolder {
			//     static void probe() throws TargetType {
			//         new TargetType();
			//     }
			// }
			String targetTypeName = "demo/TargetType";
			InstanceType targetType = Types.instanceTypeFromInternalName(targetTypeName);
			Label begin = new Label();
			Label end = new Label();
			Label handler = new Label();
			Code code = new Code(0, 0, 1);
			code.addInstruction(begin);
			code.addInstruction(new NewInstanceInstruction(0, targetType));
			code.addInstruction(end);
			code.addInstruction(new ReturnInstruction());
			code.addInstruction(handler);
			code.addInstruction(new ReturnInstruction());
			code.addTryCatch(new TryCatch(begin, end, List.of(new Handler(handler, targetType))));
			code.setDebugInfo(new DebugInformation(
					List.of(),
					List.of(),
					List.of(new DebugInformation.LocalVariable(0, "local", targetType, null, begin, end))
			));
			me.darknet.dex.tree.definitions.MethodMember method = newDexMethod("probe", "()V", ACC_PUBLIC | ACC_STATIC, code);
			method.addThrownType(targetTypeName);
			Workspace workspace = fromBundle(fromClasses(
					newAndroidClass("demo/AndroidReferenceHolder", method)
			));

			// Search for references to 'TargetType'.
			//  - Throws on method signature
			//  - NEW instruction
			//  - Catch block type
			//  - Local variable type in catch block
			Results results = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate(targetTypeName)
			));
			assertEquals(4, results.size());
			assertEquals(1, results.stream().filter(r -> r.getPath() instanceof ThrowsPathNode).count());
			assertEquals(1, results.stream().filter(r -> r.getPath() instanceof AndroidInstructionPathNode).count());
			assertEquals(1, results.stream().filter(r -> r.getPath() instanceof CatchPathNode).count());
			assertEquals(1, results.stream().filter(r -> r.getPath() instanceof LocalVariablePathNode).count());
		}

		@Test
		void testNestedAnnotationConstantReferences() {
			// @OuterAnno(nested = @InnerAnno(
			//    member = MemberOwner.helper(),
			//    enumValue = EnumType.ENTRY,
			//    typeValue = TypeRef.class
			// ))
			// class AndroidAnnotationHolder {}
			Workspace workspace = fromBundle(fromClasses(
					newAndroidClass("demo/AndroidAnnotationHolder", definition -> definition.addAnnotation(new Annotation(
							(byte) Annotation.VISIBILITY_RUNTIME,
							new AnnotationPart(
									Types.instanceTypeFromInternalName("demo/OuterAnno"),
									Map.of("nested", new AnnotationConstant(new AnnotationPart(
											Types.instanceTypeFromInternalName("demo/InnerAnno"),
											Map.of(
													"member", new MemberConstant(
															Types.instanceTypeFromInternalName("demo/MemberOwner"),
															new MemberIdentifier("helper", "()V")),
													"enumValue", new EnumConstant(
															Types.instanceTypeFromInternalName("demo/EnumType"),
															new MemberIdentifier("ENTRY", "Ldemo/EnumType;")),
													"typeValue", new TypeConstant(
															Types.instanceTypeFromInternalName("demo/TypeRef"))
											))
									))
							)
					)))
			));

			// Search for the nested annotation's 'MemberOwner.helper()' reference.
			Results memberResults = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("demo/MemberOwner"),
					strMatchProvider.newEqualPredicate("helper"),
					strMatchProvider.newEqualPredicate("()V")));
			assertEquals(1, memberResults.size());
			assertInstanceOf(AnnotationPathNode.class, memberResults.getFirst().getPath());

			// Search for the nested annotation's 'EnumType.ENTRY' reference.
			Results enumResults = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("demo/EnumType"),
					strMatchProvider.newEqualPredicate("ENTRY"),
					strMatchProvider.newEqualPredicate("Ldemo/EnumType;")));
			assertEquals(1, enumResults.size());
			assertInstanceOf(AnnotationPathNode.class, enumResults.getFirst().getPath());

			// Search for the nested annotation's 'TypeRef' reference.
			Results typeResults = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("demo/TypeRef")
			));
			assertEquals(1, typeResults.size());
			assertInstanceOf(AnnotationPathNode.class, typeResults.getFirst().getPath());

			// Search for the nested annotation itself.
			Results nestedAnnotationResults = searchService.search(workspace, new ReferenceQuery(
					strMatchProvider.newEqualPredicate("demo/InnerAnno")
			));
			assertEquals(1, nestedAnnotationResults.size());
			assertInstanceOf(AnnotationPathNode.class, nestedAnnotationResults.getFirst().getPath());
		}
	}
}
