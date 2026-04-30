package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacArgumentsBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.test.CompilerTestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.Handles;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicVersionedJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PhantomGenerator}.
 */
class PhantomGeneratorTest extends CompilerTestBase implements Opcodes {
	private static PhantomGenerator generator;
	private static PhantomGeneratorConfig generatorConfig;
	private static JavacCompiler compiler;

	@BeforeAll
	static void setup() {
		generator = recaf.get(PhantomGenerator.class);
		generatorConfig = recaf.get(PhantomGeneratorConfig.class);
		compiler = recaf.get(JavacCompiler.class);
	}

	@BeforeEach
	void resetGeneratorConfig() {
		generatorConfig.getLenientConflictingHierarchies().setValue(true);
	}

	@Test
	void testSuperAndImplements() {
		// class Example extends ClassDoesNotExist implements InterfaceDoesNotExist
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms("""
				.super ClassDoesNotExist
				.implements InterfaceDoesNotExist
				.class public abstract super Example {
				    .method public <init> ()V {
				        code: {
				        A:
				            aload this
				            invokespecial ClassDoesNotExist.<init> ()V
				            aload this
				            invokeinterface InterfaceDoesNotExist.doSomething ()V
				            return
				        B:
				        }
				    }
				}
				""", true);

		// The parent types should be created with the expected modifiers.
		JvmClassBundle phantomBundle = phantoms.getJvmClassBundle();
		JvmClassInfo missingClass = assertHasPhantom(phantomBundle, "ClassDoesNotExist");
		JvmClassInfo missingInterface = assertHasPhantom(phantomBundle, "InterfaceDoesNotExist");
		assertFalse(missingClass.hasInterfaceModifier());
		assertTrue(missingInterface.hasInterfaceModifier());

		// Parent constructor and interface methods should be created as well.
		assertNotNull(missingClass.getDeclaredMethod("<init>", "()V"));
		assertNotNull(missingInterface.getDeclaredMethod("doSomething", "()V"));

		// Compiling the class should fail without phantoms, but succeed with them.
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName("Example")
				.withClassSource("""
						public abstract class Example extends ClassDoesNotExist implements InterfaceDoesNotExist {
						    public Example() {
						        super();
						        doSomething();
						    }
						}
						""")
				.build();
		CompilerResult resultWithoutPhantoms = compiler.compile(args, null, null, null);
		CompilerResult resultWithPhantoms = compiler.compile(args, null, Collections.singletonList(phantoms), null);
		assertFalse(resultWithoutPhantoms.wasSuccess(), "Class should not compile without phantoms");
		assertTrue(resultWithPhantoms.wasSuccess(), "Class should compile with phantoms");
	}

	@Test
	void testMemberInsnReferences() {
		// class MemberInferenceExample {
		//     public static void example(MissingClass obj, MissingInterface iface) {
		//         obj.instanceMethod();
		//         int fieldValue = obj.instanceField;
		//         MissingClass.staticMethod();
		//         int staticFieldValue = MissingClass.STATIC_FIELD;
		//         iface.run();
		//         MissingInterface.staticRun();
		//     }
		// }
		JvmClassInfo jvmClassInfo = TestClassUtils.createClass("MemberInferenceExample", node -> {
			MethodNode method = new MethodNode(
					ACC_PUBLIC | ACC_STATIC,
					"example",
					"(LMissingClass;LMissingInterface;)V",
					null,
					null);
			method.visitVarInsn(ALOAD, 0);
			method.visitMethodInsn(INVOKEVIRTUAL, "MissingClass", "instanceMethod", "()V", false);
			method.visitVarInsn(ALOAD, 0);
			method.visitFieldInsn(GETFIELD, "MissingClass", "instanceField", "I");
			method.visitInsn(POP);
			method.visitMethodInsn(INVOKESTATIC, "MissingClass", "staticMethod", "()V", false);
			method.visitFieldInsn(GETSTATIC, "MissingClass", "STATIC_FIELD", "I");
			method.visitInsn(POP);
			method.visitVarInsn(ALOAD, 1);
			method.visitMethodInsn(INVOKEINTERFACE, "MissingInterface", "run", "()V", true);
			method.visitMethodInsn(INVOKESTATIC, "MissingInterface", "staticRun", "()V", true);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		JvmClassBundle phantomBundle = generatePhantoms(jvmClassInfo).getJvmClassBundle();

		// Referenced classes should be created.
		JvmClassInfo missingClass = assertHasPhantom(phantomBundle, "MissingClass");
		JvmClassInfo missingInterface = assertHasPhantom(phantomBundle, "MissingInterface");
		assertFalse(missingClass.hasInterfaceModifier());
		assertTrue(missingInterface.hasInterfaceModifier());

		// Referenced members should be created with the expected modifiers.
		MethodMember instanceMethod = missingClass.getDeclaredMethod("instanceMethod", "()V");
		MethodMember staticMethod = missingClass.getDeclaredMethod("staticMethod", "()V");
		FieldMember instanceField = missingClass.getDeclaredField("instanceField", "I");
		FieldMember staticField = missingClass.getDeclaredField("STATIC_FIELD", "I");
		MethodMember interfaceMethod = missingInterface.getDeclaredMethod("run", "()V");
		MethodMember interfaceStaticMethod = missingInterface.getDeclaredMethod("staticRun", "()V");
		assertNotNull(instanceMethod);
		assertNotNull(staticMethod);
		assertNotNull(instanceField);
		assertNotNull(staticField);
		assertNotNull(interfaceMethod);
		assertNotNull(interfaceStaticMethod);
		assertFalse(instanceMethod.hasStaticModifier());
		assertTrue(staticMethod.hasStaticModifier());
		assertFalse(instanceField.hasStaticModifier());
		assertTrue(staticField.hasStaticModifier());
		assertFalse(interfaceMethod.hasStaticModifier());
		assertTrue(interfaceMethod.hasAbstractModifier());
		assertTrue(interfaceStaticMethod.hasStaticModifier());
	}

	@Test
	void testDescInsnReferences() {
		// public MissingReturnType example(Object param1, MissingParameterType param2) {
		//     MissingCastType cast = (MissingCastType) param1;
		//     boolean isInstance = param1 instanceof MissingInstanceType;
		//     MissingArrayType[] array = new MissingArrayType[1];
		//     MissingMultiArrayType[][] multiArray = new MissingMultiArrayType[1][1];
		//     return null;
		// }
		JvmClassBundle phantomBundle = generatePhantoms("""
				.field public field LMissingFieldType;
				.method public static example (Ljava/lang/Object;LMissingParameterType;)LMissingReturnType; {
				    code: {
				    A:
				        aload 0
				        checkcast MissingCastType
				        pop
				        aload 0
				        instanceof MissingInstanceType
				        pop
				        iconst_1
				        anewarray MissingArrayType
				        pop
				        iconst_1
				        iconst_1
				        multianewarray [[LMissingMultiArrayType; 2
				        pop
				        aconst_null
				        areturn
				    B:
				    }
				}
				""", false).getJvmClassBundle();

		// All referenced types should be created as phantoms.
		assertHasPhantoms(phantomBundle,
				"MissingFieldType",
				"MissingParameterType",
				"MissingReturnType",
				"MissingCastType",
				"MissingInstanceType",
				"MissingArrayType",
				"MissingMultiArrayType");
	}

	@Test
	void testAnnotationReferences() {
		// @MissingClassAnnotation(number = 15, text = "Hello, world!", classValue = String.class)
		// public class EdgeReferenceExample {
		//     public MissingFieldType field;
		//
		//     @MissingMethodAnnotation(enabled = true, names = {"one", "two"})
		//     public static MissingReturnType example(MissingParameterType param) { ... }
		// }
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms("""
				.visible-annotation MissingClassAnnotation {
				    number: 15,
				    text: "Hello, world!",
				    classValue: java/lang/String
				}
				.super java/lang/Object
				.class public super EdgeReferenceExample {
				    .field public field LMissingFieldType;
				
				    .visible-annotation MissingMethodAnnotation {
				        enabled: true,
				        names: { "one", "two" }
				    }
				    .method public static example (LMissingParameterType;)LMissingReturnType; {
				        code: {
				        A:
				            aconst_null
				            areturn
				        B:
				        }
				    }
				}
				""", true);
		JvmClassBundle phantomBundle = phantoms.getJvmClassBundle();

		// All referenced types should be created as phantoms, including annotation types.
		assertHasPhantoms(phantomBundle,
				"MissingClassAnnotation",
				"MissingFieldType",
				"MissingParameterType",
				"MissingReturnType",
				"MissingMethodAnnotation");

		// Annotation element methods should be inferred from the annotation values.
		JvmClassInfo missingClassAnnotation = assertHasPhantom(phantomBundle, "MissingClassAnnotation");
		JvmClassInfo missingMethodAnnotation = assertHasPhantom(phantomBundle, "MissingMethodAnnotation");
		assertTrue(missingClassAnnotation.hasAnnotationModifier());
		assertTrue(missingMethodAnnotation.hasAnnotationModifier());
		assertNotNull(missingClassAnnotation.getDeclaredMethod("number", "()I"));
		assertNotNull(missingClassAnnotation.getDeclaredMethod("text", "()Ljava/lang/String;"));
		assertNotNull(missingClassAnnotation.getDeclaredMethod("classValue", "()Ljava/lang/Class;"));
		assertNotNull(missingMethodAnnotation.getDeclaredMethod("enabled", "()Z"));
		assertNotNull(missingMethodAnnotation.getDeclaredMethod("names", "()[Ljava/lang/String;"));

		// Compiling source with annotation values should succeed with generated phantoms.
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName("AnnotatedExample")
				.withClassSource("""
						@MissingClassAnnotation(number = 15, text = "Hello, world!", classValue = String.class)
						public class AnnotatedExample {
						    @MissingMethodAnnotation(enabled = true, names = {"one", "two"})
						    public MissingReturnType example(MissingParameterType param) {
						        return null;
						    }
						}
						""")
				.build();
		CompilerResult resultWithoutPhantoms = compiler.compile(args, null, null, null);
		CompilerResult resultWithPhantoms = compiler.compile(args, null, Collections.singletonList(phantoms), null);
		assertFalse(resultWithoutPhantoms.wasSuccess(), "Annotated class should not compile without phantoms");
		assertTrue(resultWithPhantoms.wasSuccess(), "Annotated class should compile with phantoms");

		// Validate the annotation values are correct on the class.
		JvmClassInfo compiledClass = new JvmClassInfoBuilder(resultWithPhantoms.getCompilations().get("AnnotatedExample")).build();
		AnnotationInfo classAnnotation = assertHasAnnotation(compiledClass, "LMissingClassAnnotation;");
		assertTrue(classAnnotation.isVisible());
		assertEquals(15, classAnnotation.getElements().get("number").getElementValue());
		assertEquals("Hello, world!", classAnnotation.getElements().get("text").getElementValue());
		assertEquals(Type.getType(String.class), classAnnotation.getElements().get("classValue").getElementValue());

		// Validate the annotation values are correct on the method.
		MethodMember compiledMethod = compiledClass.getDeclaredMethod("example", "(LMissingParameterType;)LMissingReturnType;");
		assertNotNull(compiledMethod);
		AnnotationInfo methodAnnotation = assertHasAnnotation(compiledMethod, "LMissingMethodAnnotation;");
		assertTrue(methodAnnotation.isVisible());
		assertEquals(true, methodAnnotation.getElements().get("enabled").getElementValue());
		assertEquals(List.of("one", "two"), methodAnnotation.getElements().get("names").getElementValue());
	}

	@Test
	void testInvisibleAnnotationRetentionOnCompiledClassInfo() {
		// Annotations with compile-time retention should be generated as phantoms with the annotation modifier,
		// but their values should be invisible when compiling source that references them.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms("""
				.invisible-annotation MissingInvisibleAnnotation {
				    value: "hidden"
				}
				.super java/lang/Object
				.class public super InvisibleAnnotationCarrier {
				}
				""", true);
		JvmClassBundle phantomBundle = phantoms.getJvmClassBundle();
		JvmClassInfo missingInvisibleAnnotation = assertHasPhantom(phantomBundle, "MissingInvisibleAnnotation");
		assertTrue(missingInvisibleAnnotation.hasAnnotationModifier());

		// Compiling source that references the invisible annotation should succeed, and the annotation values should not be visible
		// just like how the assembly above indicates.
		JvmClassInfo compiledClass = compileWithPhantoms("InvisibleAnnotatedExample", """
				@MissingInvisibleAnnotation("hidden")
				public class InvisibleAnnotatedExample {
				}
				""", phantoms);
		AnnotationInfo annotation = assertHasAnnotation(compiledClass, "LMissingInvisibleAnnotation;");
		assertFalse(annotation.isVisible());
		assertEquals("hidden", annotation.getElements().get("value").getElementValue());
	}

	@Test
	void testInnerAnnotationReferences() {
		// @InnerAnno.TheInner("example") implies the existence of both the outer and inner annotation types.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms("""
				.visible-annotation InnerAnno$TheInner {
				    value: "example"
				}
				.super java/lang/Object
				.class public super InnerAnnotationCarrier {
				}
				""", true);
		JvmClassBundle phantomBundle = phantoms.getJvmClassBundle();

		// Both classes should be created.
		// Based n the usage above we only have evidence for 'TheInner' being an annotation though,
		// so 'InnerAnno' should not have the annotation modifier.
		JvmClassInfo outerAnnotation = assertHasPhantom(phantomBundle, "InnerAnno");
		JvmClassInfo innerAnnotation = assertHasPhantom(phantomBundle, "InnerAnno$TheInner");
		assertFalse(outerAnnotation.hasAnnotationModifier());
		assertTrue(innerAnnotation.hasAnnotationModifier());

		// Inner should be aware of its outer class.
		assertEquals("InnerAnno", innerAnnotation.getOuterClassName());
		assertNotNull(outerAnnotation.getInnerClassByInnerName("TheInner"));
		assertNotNull(innerAnnotation.getDeclaredMethod("value", "()Ljava/lang/String;"));

		// We should be able to use the inner annotation in source now.
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName("InnerAnnotatedExample")
				.withClassSource("""
						@InnerAnno.TheInner("example")
						public class InnerAnnotatedExample {
						}
						""")
				.build();
		CompilerResult resultWithoutPhantoms = compiler.compile(args, null, null, null);
		CompilerResult resultWithPhantoms = compiler.compile(args, null, Collections.singletonList(phantoms), null);
		assertFalse(resultWithoutPhantoms.wasSuccess(), "Nested annotated class should not compile without phantoms");
		assertTrue(resultWithPhantoms.wasSuccess(),
				() -> "Nested annotated class should compile with phantoms: " + resultWithPhantoms.getDiagnostics());

		// Validate the annotation 'value' is correct.
		JvmClassInfo compiledClass = new JvmClassInfoBuilder(resultWithPhantoms.getCompilations().get("InnerAnnotatedExample")).build();
		AnnotationInfo annotation = assertHasAnnotation(compiledClass, "LInnerAnno$TheInner;");
		assertTrue(annotation.isVisible());
		assertEquals("example", annotation.getElements().get("value").getElementValue());
	}

	@Test
	void testLdcAndCatchReferences() {
		// public static void example() throws MissingExceptionType {
		//     Object ldc = MissingLdcType.class;
		//     Object dyn = /* dynamic bs here */;
		//     try {
		//         // junk placeholder
		//     } catch (MissingCatchType e) {
		//         return;
		//     }
		// }
		JvmClassInfo jvmClassInfo = TestClassUtils.createClass("LdcAndCatchReferenceExample", node -> {
			MethodNode method = new MethodNode(
					ACC_PUBLIC | ACC_STATIC,
					"example",
					"()V",
					null,
					new String[]{"MissingExceptionType"});
			method.instructions.add(new LdcInsnNode(Type.getObjectType("MissingLdcType")));
			method.instructions.add(new InsnNode(POP));
			method.instructions.add(new LdcInsnNode(new ConstantDynamic(
					"dyn",
					"LMissingDynamicType;",
					new Handle(H_INVOKESTATIC,
							"java/lang/invoke/StringConcatFactory",
							"makeConcatWithConstants",
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
							false),
					Type.getObjectType("MissingDynamicArgumentType"))));
			method.instructions.add(new InsnNode(POP));
			LabelNode start = new LabelNode();
			LabelNode end = new LabelNode();
			LabelNode handler = new LabelNode();
			method.instructions.add(start);
			method.instructions.add(new InsnNode(NOP));
			method.instructions.add(end);
			method.instructions.add(new JumpInsnNode(GOTO, handler));
			method.instructions.add(handler);
			method.instructions.add(new InsnNode(RETURN));
			method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "MissingCatchType"));
			node.methods.add(method);
		});
		JvmClassBundle phantomBundle = generatePhantoms(jvmClassInfo).getJvmClassBundle();

		// All referenced types should be created as phantoms, including exception types and dynamic constant types.
		assertHasPhantoms(phantomBundle,
				"MissingExceptionType",
				"MissingLdcType",
				"MissingDynamicType",
				"MissingDynamicArgumentType",
				"MissingCatchType");
	}

	@Test
	void testCatchHandlerReceiverConstraint() {
		// If a catch handler has a phantom type as its exception, then that phantom should be inferred to extend Throwable.
		// Additionally, any usage of the exception value should propagate constraints to the phantom as well.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    exceptions: {
				        { A, B, C, LCaughtThing; }
				    },
				    code: {
				    A:
				        new CaughtThing
				        dup
				        invokespecial CaughtThing.<init> ()V
				        athrow
				    B:
				        return
				    C:
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    }
				}
				""", false).getJvmClassBundle();
		JvmClassInfo caughtThing = assertHasPhantom(phantomBundle, "CaughtThing");
		assertEquals("java/lang/Throwable", caughtThing.getSuperName());

		// The exception value is used as the receiver of a call to List.add, so it should be inferred to implement List.
		assertTrue(caughtThing.getInterfaces().contains("java/util/List"));
	}

	@Test
	void testHandlerPropagatesTryLocalState() {
		// A local assigned from a known type in a try-catch block should allow
		// propagation of constraints beyond the handled range.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example (LLocalCaughtList;)V {
				    parameters: { list },
				    exceptions: {
				        { A, B, C, Ljava/lang/Throwable; }
				    },
				    code: {
				    A:
				        aload list
				        astore aliased
				        invokestatic Thrower.boom ()V
				    B:
				        return
				    C:
				        aload aliased
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    }
				}
				""", false).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "LocalCaughtList").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testCatchAllHandlerPropagatesThrowableAndLocals() {
		// Similar to the test above, but with a wildcard handler and a custom thrown exception type in the protected range.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example (LCatchAllList;)V {
				    parameters: { list },
				    exceptions: {
				        { A, B, C, * }
				    },
				    code: {
				    A:
				        aload list
				        astore aliased
				        new CatchAllThrown
				        dup
				        invokespecial CatchAllThrown.<init> ()V
				        athrow
				    B:
				        return
				    C:
				        astore ex
				        aload ex
				        invokevirtual java/lang/Throwable.getMessage ()Ljava/lang/String;
				        pop
				        aload aliased
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("java/lang/Throwable", assertHasPhantom(phantomBundle, "CatchAllThrown").getSuperName());
		assertTrue(assertHasPhantom(phantomBundle, "CatchAllList").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testIndyReferences() {
		// Invokedynamic contents should be visited and infer missing types too.
		JvmClassInfo jvmClassInfo = TestClassUtils.createClass("InvokeDynamicExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "()V", null, null);
			method.instructions.add(new InvokeDynamicInsnNode(
					"run",
					"()LMissingFunctionalInterface;",
					Handles.META_FACTORY,
					Type.getType("()V"),
					new Handle(H_INVOKESTATIC, "MissingLambdaOwner", "impl", "()V", false),
					Type.getType("()V")));
			method.instructions.add(new InsnNode(POP));
			method.instructions.add(new InsnNode(RETURN));
			node.methods.add(method);
		});
		JvmClassBundle phantomBundle = generatePhantoms(jvmClassInfo).getJvmClassBundle();
		assertHasPhantom(phantomBundle, "MissingFunctionalInterface");
		JvmClassInfo lambdaOwner = assertHasPhantom(phantomBundle, "MissingLambdaOwner");
		assertNotNull(lambdaOwner.getDeclaredMethod("impl", "()V"));
	}

	@Test
	void testNoGenerationOfKnownTypes() {
		// Make a workspace with 'KnownParent' then generate phantoms for
		// a class that extends it and references some JDK types.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(assemble("""
				.super java/lang/Object
				.class public super KnownParent {
				}
				""", true)));
		JvmClassInfo input = assemble("""
				.super KnownParent
				.class public super SkipKnownTypesExample {
				    .method public list ()Ljava/util/List; {
				        code: {
				        A:
				            aconst_null
				            areturn
				        B:
				        }
				    }
				}
				""", true);

		// Nothing should be generated since all types are already present in the workspace.
		JvmClassBundle phantomBundle = generatePhantoms(workspace, input).getJvmClassBundle();
		assertNoPhantom(phantomBundle, "KnownParent");
		assertNoPhantom(phantomBundle, "java/util/List");
	}

	@Test
	void testReferenceCycle() {
		// A class that has two references depending on each other should still allow both to be generated.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        getstatic CycleA.OTHER LCycleB;
				        pop
				        getstatic CycleB.OTHER LCycleA;
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		JvmClassInfo cycleA = assertHasPhantom(phantomBundle, "CycleA");
		JvmClassInfo cycleB = assertHasPhantom(phantomBundle, "CycleB");
		assertNotNull(cycleA.getDeclaredField("OTHER", "LCycleB;"));
		assertNotNull(cycleB.getDeclaredField("OTHER", "LCycleA;"));
	}

	@Test
	void testImpliedInterfaceConstraintFromReceiverOwner() {
		// If a phantom class (CustomList) is the receiver of a call of some other type (List)
		// then the phantom should be inferred to implement/extend that type, based on what we
		// know about the method's owner type.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        new CustomList
				        dup
				        invokespecial CustomList.<init> ()V
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    B:
				    }
				}
				""", false);

		// The CustomList type should be generated and usable as a List.
		JvmClassInfo customList = assertHasPhantom(phantoms.getJvmClassBundle(), "CustomList");
		assertTrue(customList.getInterfaces().contains("java/util/List"));
		compileWithPhantoms("UsesCustomList", """
				import java.util.*;
				public class UsesCustomList {
				    void use() {
				        new CustomList().add(null);
				        List list = new CustomList();
				        Collections.sort(list);
				    }
				}
				""", phantoms);
	}

	@Test
	void testImpliedClassConstraintFromReceiverFieldOwner() {
		// If a phantom class (Thing) is the receiver of a field instruction of some other type (ParentThing)
		// then the phantom should be inferred to extend that type.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        new Thing
				        dup
				        invokespecial Thing.<init> ()V
				        iconst_0
				        putfield ParentThing.someInt I
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();

		// Thing and ParentThing should both be generated, and Thing extends ParentThing.
		assertEquals("ParentThing", assertHasPhantom(phantomBundle, "Thing").getSuperName());

		// The field should be created on ParentThing.
		assertNotNull(assertHasPhantom(phantomBundle, "ParentThing").getDeclaredField("someInt", "I"));
	}

	@Test
	void testImpliedArgumentAndResultConstraints() {
		// If a phantom class is used as an argument or return type of a method call,
		// then the phantom should be inferred to extend/implement the method's parameter/return types.
		//
		// Here:
		//  - Y extends A because it's passed to a method that expects an A.
		//  - Z extends B because it's returned from a method that returns a B. Returned Z is used as B for calling baz().
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example (LX;LY;)V {
					parameters: { x, y },
				    code: {
				    A:
				        aload x
				        aload y
				        invokevirtual X.bar (LA;)LZ;
				        invokevirtual B.baz ()V
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("A", assertHasPhantom(phantomBundle, "Y").getSuperName());
		assertEquals("B", assertHasPhantom(phantomBundle, "Z").getSuperName());
	}

	@Test
	void testMergeAwareReturnConstraints() {
		// If a phantom class is returned from multiple return instructions that expect different types,
		// the phantom should be inferred to extend/implement both types if possible.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static choose (ZLB;LC;)LA; {
					parameters: { flag, b, c },
				    code: {
				    A:
				        iload flag
				        ifeq C
				    B:
				        aload b
				        astore result
				        goto D
				    C:
				        aload c
				        astore result
				        goto D
				    D:
				        aload result
				        areturn
				    E:
				    }
				}
				""", false).getJvmClassBundle();

		// Return type is 'A' and parameters 'B' and 'C' are both returned conditionally, so both should be inferred to extend 'A'.
		assertEquals("A", assertHasPhantom(phantomBundle, "B").getSuperName());
		assertEquals("A", assertHasPhantom(phantomBundle, "C").getSuperName());
	}

	@Test
	void testPutFieldAndStaticValueConstraints() {
		// If a phantom class is stored into a field of some other type,
		// then the phantom should be inferred to extend/implement that type.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static putField (LFieldHolder;LStoredFieldValue;)V {
					parameters: { holder, value },
				    code: {
				    A:
				        aload holder
				        aload value
				        putfield FieldHolder.field LFieldTarget;
				        return
				    B:
				    }
				}
				.method public static putStatic (LStoredStaticValue;)V {
					parameters: { value },
				    code: {
				    A:
				        aload value
				        putstatic StaticFieldHolder.field LStaticFieldTarget;
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("FieldTarget", assertHasPhantom(phantomBundle, "StoredFieldValue").getSuperName());
		assertEquals("StaticFieldTarget", assertHasPhantom(phantomBundle, "StoredStaticValue").getSuperName());
	}

	@Test
	void testArrayStoreConstraint() {
		// If a phantom class is stored into an array of some other type,
		// then the phantom should be inferred to extend/implement that type.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ([LArrayElementTarget;LStoredArrayValue;)V {
					parameters: { array, value },
				    code: {
				    A:
				        aload array
				        iconst_0
				        aload value
				        aastore
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("ArrayElementTarget", assertHasPhantom(phantomBundle, "StoredArrayValue").getSuperName());
	}

	@Test
	void testArrayLoadReceiverConstraint() {
		// If a phantom class is pushed from an array load and that value is used in such a way that infers some inheritance
		// relationship, then the phantom should be inferred to extend/implement that type.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ([LLoadedList;)V {
					parameters: { array },
				    code: {
				    A:
				        aload array
				        iconst_0
				        aaload
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "LoadedList").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testArrayLoadAliasConstraint() {
		// Variant of the above test where the vale is temporarily stored in a local.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ([LAliasedList;)V {
					parameters: { array },
				    code: {
				    A:
				        aload array
				        iconst_0
				        aaload
				        astore value
				        aload value
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "AliasedList").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testSwapPreservesPutFieldConstraint() {
		// If a phantom class is swapped with another value and then stored into a field,
		// the phantom should still be inferred to extend/implement the field type based on the putfield instruction.
		// The swap should not interfere with the constraint inference.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        new StoredSwapValue
				        dup
				        invokespecial StoredSwapValue.<init> ()V
				        new FieldHolder
				        dup
				        invokespecial FieldHolder.<init> ()V
				        swap
				        swap
				        swap
				        putfield FieldHolder.field LFieldTarget;
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("FieldTarget", assertHasPhantom(phantomBundle, "StoredSwapValue").getSuperName());
	}

	@Test
	void testDupX1PreservesPutFieldConstraint() {
		// Same idea as the above test but with dup_x1 instead of swap.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        new FieldHolder
				        dup
				        invokespecial FieldHolder.<init> ()V
				        new DuplicatedFieldValue
				        dup
				        invokespecial DuplicatedFieldValue.<init> ()V
				        dup_x1
				        putfield FieldHolder.field LFieldTarget;
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("FieldTarget", assertHasPhantom(phantomBundle, "DuplicatedFieldValue").getSuperName());
	}

	@Test
	void testPop2PreservesReceiverConstraint() {
		// Basic usage inference test with a redundant long + pop2 in the middle.
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        new WideStackList
				        dup
				        invokespecial WideStackList.<init> ()V
				        lconst_0
				        pop2
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "WideStackList").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testInvokeDynamicReturnFlowConstraint() {
		// If a phantom class is returned from an invokedynamic call and then used as some other type,
		// the phantom should be inferred to extend/implement that type based on the method call that uses the invokedynamic result.
		JvmClassInfo jvmClassInfo = TestClassUtils.createClass("InvokeDynamicReturnFlowExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "()V", null, null);
			method.visitInvokeDynamicInsn(
					"run",
					"()LIndyList;",
					Handles.META_FACTORY,
					Type.getType("()V"),
					new Handle(H_INVOKESTATIC, "IndyFactory", "impl", "()V", false),
					Type.getType("()V"));
			method.visitInsn(ACONST_NULL);
			method.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
			method.visitInsn(POP);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		JvmClassBundle phantomBundle = generatePhantoms(jvmClassInfo).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "IndyList").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testThrowConstraint() {
		// If a phantom class is thrown, then the phantom should be inferred to extend Throwable.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms("""
				.method public static example (LThrownType;)V {
					parameters: { thrown },
				    code: {
				    A:
				        aload thrown
				        athrow
				    B:
				    }
				}
				""", false);
		assertEquals("java/lang/Throwable", assertHasPhantom(phantoms.getJvmClassBundle(), "ThrownType").getSuperName());

		// We should be able to use the generated phantom as an exception type in source now.
		compileWithPhantoms("ThrowsThing", """
				public class ThrowsThing {
				    void fail() throws ThrownType {
				        throw new ThrownType();
				    }
				}
				""", phantoms);
	}

	@Test
	void testAstoreConstraintFromDeclaredLocalType() {
		// If a phantom class is stored into a local variable with a declared type,
		// then the phantom should be inferred to extend/implement that type.
		JvmClassInfo input = TestClassUtils.createClass("AstoreExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "(LStoreTarget;LStoredType;)V", null, null);
			method.visitVarInsn(ALOAD, 1);
			method.visitVarInsn(ASTORE, 0);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});

		// Validate that based on the assignment logic, we infer StoredType extends StoreTarget.
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		assertEquals("StoreTarget", assertHasPhantom(phantomBundle, "StoredType").getSuperName());
	}

	@Test
	void testAliasThroughLocalPropagation() {
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example ()V {
				    code: {
				    A:
				        new LocalThing
				        dup
				        invokespecial LocalThing.<init> ()V
				        astore t
				        aload t
				        aconst_null
				        invokeinterface java/util/List.add (Ljava/lang/Object;)Z
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "LocalThing").getInterfaces().contains("java/util/List"));
	}

	@Test
	void testConflictingClassConstraintsLenientMethodsCompile() {
		// With leniency disabled there is a conflict here. We have 'Child'
		// which should have two class parents, ParentA and ParentB.
		// There is no way to tell which one should be the immediate parent so we fail.
		generatorConfig.getLenientConflictingHierarchies().setValue(false);
		assertThrows(PhantomGenerationException.class,
				() -> generator.createPhantomsForClasses(EmptyWorkspace.get(), List.of(assemble("""
						.method public static example (LChild;)V {
							parameters: { child },
						    code: {
						    A:
						        aload child
						        invokevirtual ParentA.use ()V
						        aload child
						        invokevirtual ParentB.use ()V
						        return
						    B:
						    }
						}
						""", false))));

		// With leniency enabled, we just pick the first parent we see.
		generatorConfig.getLenientConflictingHierarchies().setValue(true);
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(assemble("""
				.method public static example (LChild;)V {
					parameters: { child },
				    code: {
				    A:
				        aload child
				        invokevirtual ParentA.useA ()V
				        aload child
				        invokevirtual ParentB.useB ()V
				        return
				    B:
				    }
				}
				""", false));

		// Doesn't matter which order we see, but Child -> [ParentA, ParentB] is what we wanna see.
		// It'll let the compilation later on emit the same 'invokevirtual' calls.
		JvmClassInfo child = assertHasPhantom(phantoms.getJvmClassBundle(), "Child");
		JvmClassInfo parentA = assertHasPhantom(phantoms.getJvmClassBundle(), "ParentA");
		JvmClassInfo parentB = assertHasPhantom(phantoms.getJvmClassBundle(), "ParentB");
		String childSuper = child.getSuperName();
		assertTrue("ParentA".equals(childSuper) || "ParentB".equals(childSuper),
				() -> "Child should extend one conflicting parent, got: " + childSuper);
		if ("ParentA".equals(childSuper)) {
			assertEquals("ParentB", parentA.getSuperName());
		} else {
			assertEquals("ParentA", parentB.getSuperName());
		}

		// Compilation should succeed, and both calls should be 'invokevirtual' since both parents are classes in the lenient chain.
		JvmClassInfo use = compileWithPhantoms("LenientUse", """
				public class LenientUse {
				    void run(Child child) {
				        ((ParentA)child).useA();
				        ((ParentB)child).useB();
				    }
				}
				""", phantoms);
		String useDis = disassemble(use, true);
		assertTrue(useDis.contains("invokevirtual ParentA.useA ()V"));
		assertTrue(useDis.contains("invokevirtual ParentB.useB ()V"));
	}

	@Test
	void testConflictingClassConstraintsLenientFieldsCompile() {
		// Same idea as above but with fields.
		generatorConfig.getLenientConflictingHierarchies().setValue(true);
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(assemble("""
				.method public static example (LChild;)I {
					parameters: { child },
				    code: {
				    A:
				        aload child
				        getfield ParentA.valueA I
				        aload child
				        getfield ParentB.valueB I
				        iadd
				        ireturn
				    B:
				    }
				}
				""", false));

		// Same case as above test.
		JvmClassInfo child = assertHasPhantom(phantoms.getJvmClassBundle(), "Child");
		JvmClassInfo parentA = assertHasPhantom(phantoms.getJvmClassBundle(), "ParentA");
		JvmClassInfo parentB = assertHasPhantom(phantoms.getJvmClassBundle(), "ParentB");
		String childSuper = child.getSuperName();
		assertTrue("ParentA".equals(childSuper) || "ParentB".equals(childSuper),
				() -> "Child should extend one conflicting parent, got: " + childSuper);
		if ("ParentA".equals(childSuper)) {
			assertEquals("ParentB", parentA.getSuperName());
		} else {
			assertEquals("ParentA", parentB.getSuperName());
		}

		// Compilation should succeed, and both field accesses should be 'getfield' since both parents are classes in the lenient chain.
		JvmClassInfo use = compileWithPhantoms("LenientFieldUse", """
				public class LenientFieldUse {
				    int read(Child child) {
				        return ((ParentA)child).valueA + ((ParentB)child).valueB;
				    }
				}
				""", phantoms);
		String useDis = disassemble(use, true);
		assertTrue(useDis.contains("getfield ParentA.valueA I"));
		assertTrue(useDis.contains("getfield ParentB.valueB I"));
	}

	@Test
	void testCheckcastDoesNotCreateSubtypeConstraint() {
		// Checkcast doesn't actually imply a subtype constraints. Sure, its common that there is one, but:
		//  - It's not required
		//  - It doesn't break compilation contracts
		JvmClassBundle phantomBundle = generatePhantoms("""
				.method public static example (LCastOnlySub;)V {
					parameters: { sub },
				    code: {
				    A:
				        aload sub
				        checkcast CastOnlySuper
				        pop
				        return
				    B:
				    }
				}
				""", false).getJvmClassBundle();
		assertEquals("java/lang/Object", assertHasPhantom(phantomBundle, "CastOnlySub").getSuperName());
		assertHasPhantom(phantomBundle, "CastOnlySuper");
	}

	@Test
	void testWorkspaceGenerationHandlesDuplicateClassesAcrossBundles() {
		// If the same class is present in both the base and a versioned bundle,
		// we should still be able to generate phantoms for missing types referenced by both versions of the class
		JvmClassInfo baseClass = TestClassUtils.createClass("DuplicateWorkspaceType", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "base", "(LMissingBaseType;)V", null, null);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		JvmClassInfo versionedClass = TestClassUtils.createClass("DuplicateWorkspaceType", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "versioned", "(LMissingVersionedType;)V", null, null);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});

		JvmClassBundle baseBundle = TestClassUtils.fromClasses(baseClass);
		BasicVersionedJvmClassBundle versionedBundle = new BasicVersionedJvmClassBundle(11);
		versionedBundle.initialPut(versionedClass);
		NavigableMap<Integer, VersionedJvmClassBundle> versionedBundles = new TreeMap<>();
		versionedBundles.put(versionedBundle.version(), versionedBundle);
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder()
				.withJvmClassBundle(baseBundle)
				.withVersionedJvmClassBundles(versionedBundles)
				.build());

		// Validate both types are generated.
		GeneratedPhantomWorkspaceResource phantoms =
				assertDoesNotThrow(() -> generator.createPhantomsForWorkspace(workspace));
		assertHasPhantoms(phantoms.getJvmClassBundle(), "MissingBaseType", "MissingVersionedType");
	}

	@Nonnull
	private GeneratedPhantomWorkspaceResource generatePhantoms(@Nonnull String assembly, boolean isFullBody) {
		return generatePhantoms(EmptyWorkspace.get(), assemble(assembly, isFullBody));
	}

	@Nonnull
	private GeneratedPhantomWorkspaceResource generatePhantoms(@Nonnull JvmClassInfo... classes) {
		return generatePhantoms(EmptyWorkspace.get(), classes);
	}

	@Nonnull
	private GeneratedPhantomWorkspaceResource generatePhantoms(@Nonnull Workspace workspace, @Nonnull JvmClassInfo... classes) {
		return assertDoesNotThrow(() -> generator.createPhantomsForClasses(workspace, List.of(classes)));
	}

	@Nonnull
	private JvmClassInfo compileWithPhantoms(@Nonnull String className, @Nonnull String source, @Nonnull GeneratedPhantomWorkspaceResource phantoms) {
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName(className)
				.withClassSource(source)
				.build();
		CompilerResult result = compiler.compile(args, null, Collections.singletonList(phantoms), null);
		assertTrue(result.wasSuccess(), () -> "Class should compile with phantoms: " + className
				+ "\nDiagnostics: " + result.getDiagnostics().stream()
				.map(CompilerDiagnostic::toString)
				.collect(Collectors.joining("\n")));
		byte[] bytecode = result.getCompilations().get(className);
		assertNotNull(bytecode, "Missing compilation output: " + className);
		return new JvmClassInfoBuilder(bytecode).build();
	}

	@Nonnull
	private static JvmClassInfo assertHasPhantom(@Nonnull JvmClassBundle bundle, @Nonnull String name) {
		JvmClassInfo phantom = bundle.get(name);
		assertNotNull(phantom, "Missing phantom: " + name);
		return phantom;
	}

	@Nonnull
	private static AnnotationInfo assertHasAnnotation(@Nonnull Annotated annotated, @Nonnull String descriptor) {
		return annotated.getAnnotations().stream()
				.filter(annotation -> descriptor.equals(annotation.getDescriptor()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing annotation: " + descriptor + " in " + annotated.getAnnotations()));
	}

	private static void assertHasPhantoms(@Nonnull JvmClassBundle bundle, @Nonnull String... names) {
		for (String name : names)
			assertHasPhantom(bundle, name);
	}

	private static void assertNoPhantom(@Nonnull JvmClassBundle bundle, @Nonnull String name) {
		assertNull(bundle.get(name), "Unexpected phantom: " + name);
	}
}
