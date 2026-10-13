package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationEnumReference;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacArgumentsBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.compile.JavacCompilerConfig;
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

import java.util.ArrayList;
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
	private static JavacCompiler compiler;

	@BeforeAll
	static void setup() {
		generator = recaf.get(PhantomGenerator.class);
		compiler = recaf.get(JavacCompiler.class);
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
	void testSuperHierarchyReferences() {
		// class InheritedInterfaceExample extends KnownSuperclass implements MissingInheritedInterface
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(assemble("""
				.super java/lang/Object
				.implements MissingInheritedInterface
				.class public super KnownSuperclass {
				}
				""", true)));
		JvmClassInfo input = assemble("""
				.super KnownSuperclass
				.class public super InheritedInterfaceExample {
				}
				""", true);

		// The indirect interface should be created as a phantom.
		JvmClassBundle phantomBundle = generatePhantoms(workspace, input).getJvmClassBundle();
		JvmClassInfo missingInterface = assertHasPhantom(phantomBundle, "MissingInheritedInterface");
		assertTrue(missingInterface.hasInterfaceModifier());
		assertNoPhantom(phantomBundle, "KnownSuperclass");
	}

	@Test
	void testKnownSuperclassConstructorDescriptor() {
		// A generated subclass must call a constructor that actually exists on a known parent.
		JvmClassInfo knownParent = assemble("""
				.super java/lang/Object
				.class public super KnownConstructorParent {
				    .method protected <init> (I)V {
				        code: {
				        A:
				            aload this
				            invokespecial java/lang/Object.<init> ()V
				            return
				        B:
				        }
				    }
				    .method public use ()V {
				        code: {
				        A:
				            return
				        B:
				        }
				    }
				}
				""", true);
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super KnownConstructorUse {
				    .method public static example ()V {
				        code: {
				        A:
				            new MissingConstructorChild
				            dup
				            invokespecial MissingConstructorChild.<init> ()V
				            invokevirtual KnownConstructorParent.use ()V
				            return
				        B:
				        }
				    }
				}
				""", true);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownParent));
		GeneratedPhantomWorkspaceResource phantoms = assertDoesNotThrow(() -> generator.createPhantomsForClasses(workspace, List.of(input), 8));
		JvmClassInfo child = assertHasPhantom(phantoms.getJvmClassBundle(), "MissingConstructorChild");
		assertNotNull(child.getDeclaredMethod("<init>", "()V"));
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
	void testFinalObjectMethodsAreNotAddedToPhantomOwners() {
		// class FinalObjectMethodUse {
		//      public static Class<?> getClass(FinalObjectMethodInterface iface) {
		//	        return iface.getClass();
		//	    }
		//      public static Class<?> getClassFromClass(FinalObjectMethodClass clazz) {
		//	        return clazz.getClass();
		//	    }
		//      public static void waitHandle() {
		//	        Handle handle = new Handle(H_INVOKEINTERFACE, "FinalObjectHandleInterface", "wait", "()V", true);
		//	    }
		// }
		JvmClassInfo input = TestClassUtils.createClass("FinalObjectMethodUse", node -> {
			MethodNode method = new MethodNode(
					ACC_PUBLIC | ACC_STATIC,
					"getClass",
					"(LFinalObjectMethodInterface;)Ljava/lang/Class;",
					null,
					null);
			method.maxStack = 1;
			method.visitVarInsn(ALOAD, 0);
			method.visitMethodInsn(INVOKEINTERFACE, "FinalObjectMethodInterface", "getClass", "()Ljava/lang/Class;", true);
			method.visitInsn(ARETURN);
			node.methods.add(method);

			method = new MethodNode(
					ACC_PUBLIC | ACC_STATIC,
					"getClassFromClass",
					"(LFinalObjectMethodClass;)Ljava/lang/Class;",
					null,
					null);
			method.maxStack = 1;
			method.visitVarInsn(ALOAD, 0);
			method.visitMethodInsn(INVOKEVIRTUAL, "FinalObjectMethodClass", "getClass", "()Ljava/lang/Class;", false);
			method.visitInsn(ARETURN);
			node.methods.add(method);

			method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "waitHandle", "()V", null, null);
			method.maxStack = 1;
			method.visitLdcInsn(new Handle(H_INVOKEINTERFACE, "FinalObjectHandleInterface", "wait", "()V", true));
			method.visitInsn(POP);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});

		// The phantom generator should not add the final methods of java.lang.Object to the phantom owners.
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		JvmClassInfo getClassOwner = assertHasPhantom(phantomBundle, "FinalObjectMethodInterface");
		JvmClassInfo classOwner = assertHasPhantom(phantomBundle, "FinalObjectMethodClass");
		JvmClassInfo waitOwner = assertHasPhantom(phantomBundle, "FinalObjectHandleInterface");
		assertTrue(getClassOwner.hasInterfaceModifier());
		assertFalse(classOwner.hasInterfaceModifier());
		assertTrue(waitOwner.hasInterfaceModifier());
		assertNull(getClassOwner.getDeclaredMethod("getClass", "()Ljava/lang/Class;"));
		assertNull(classOwner.getDeclaredMethod("getClass", "()Ljava/lang/Class;"));
		assertNull(waitOwner.getDeclaredMethod("wait", "()V"));
	}

	@Test
	void testSpecialMemberInsnReference() {
		// void example(MissingSource param) {
		//     ((MissingTarget) param).privateCall();
		// }
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super InvokeSpecialReceiverExample {
				    .method public static example (LMissingSource;)V {
				        parameters: { source },
				        code: {
				        A:
				            aload source
				            invokespecial MissingTarget.privateCall ()V
				            return
				        B:
				        }
				    }
				}
				""", true);

		// Using invokespecial on an unknown receiver type should infer that the receiver type is a subclass of the method owner type.
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		assertEquals("MissingTarget", assertHasPhantom(phantomBundle, "MissingSource").getSuperName());
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
		MethodMember namesElement = missingMethodAnnotation.getDeclaredMethod("names", "()[Ljava/lang/String;");
		assertNotNull(namesElement);
		assertEquals(List.of(), namesElement.getAnnotationDefault().getElementValue());

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
		compileWithPhantoms("OmittedAnnotationValues", "@MissingMethodAnnotation class OmittedAnnotationValues {}", phantoms);

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
	void testEnumAnnotationReference() {
		// @EnumAnnotationCarrier(mode = MissingMode.ENABLED)
		JvmClassInfo input = assemble("""
				.visible-annotation MissingAnnotation {
				    mode: .enum MissingMode ENABLED
				}
				.super java/lang/Object
				.class public super EnumAnnotationCarrier {
				}
				""", true);

		// Annotations referencing enums should generate the enum type and its constants.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(input);
		JvmClassInfo mode = assertHasPhantom(phantoms.getJvmClassBundle(), "MissingMode");
		assertTrue(mode.hasEnumModifier());
		assertEquals("java/lang/Enum", mode.getSuperName());
		assertNotNull(mode.getDeclaredField("ENABLED", "LMissingMode;"));

		// We should be able to use the enum in source now.
		compileWithPhantoms("EnumAnnotationUse", """
				@MissingAnnotation(mode = MissingMode.ENABLED)
				class EnumAnnotationUse {
				}
				""", phantoms);
	}

	@Test
	void testEnumConstantFieldRequirementIsNotWrittenTwice() {
		// The annotation discovers VALUE as an enum constant, while GETSTATIC also records it as a regular field.
		JvmClassInfo input = TestClassUtils.createClass("EnumConstantFieldUse", node -> {
			AnnotationVisitor annotation = node.visitAnnotation("LMissingEnumAnnotation;", true);
			annotation.visitEnum("value", "LMissingEnum;", "VALUE");
			annotation.visitEnd();

			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "value", "()LMissingEnum;", null, null);
			method.maxStack = 1;
			method.visitFieldInsn(GETSTATIC, "MissingEnum", "VALUE", "LMissingEnum;");
			method.visitInsn(ARETURN);
			node.methods.add(method);
		});

		// The phantom should only have the 'VALUE' field once, and it should be an enum constant.
		JvmClassInfo phantom = assertHasPhantom(generatePhantoms(input).getJvmClassBundle(), "MissingEnum");
		assertTrue(phantom.hasEnumModifier());
		assertEquals(1, phantom.getFields().size());
		FieldMember value = phantom.getFirstDeclaredFieldByName("VALUE");
		assertNotNull(value);
		assertTrue(value.hasEnumModifier());
	}

	@Test
	void testInnerClassesFlagsInferPhantomInterface() {
		// A class that references an inner class of a missing outer class should
		// infer the inner class is an interface if the inner class has the interface modifier.
		JvmClassInfo input = TestClassUtils.createClass("InnerKindEvidence", node ->
				node.visitInnerClass("MissingOuter$Nested", "MissingOuter", "Nested",
						ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE));

		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(input);
		assertTrue(assertHasPhantom(phantoms.getJvmClassBundle(), "MissingOuter$Nested").hasInterfaceModifier());
		compileWithPhantoms("InnerKindUse", "class InnerKindUse implements MissingOuter.Nested {}", phantoms);
	}

	@Test
	void testTopLevelDollarPrefixedClassNameIsNotNested() {
		// We don't want the phantom generator to get fooled by being over-eager on inner name $ splitting.
		// Things like com.example.Outer$Inner are 99% of the time gonna be an inner class.
		// But sometimes you have funky names like com.example.$Thing which are normal top-level classes just with a $ in the name.
		String topLevelName = "foo/$Bar";
		String nestedName = topLevelName + "$Types";
		JvmClassInfo input = TestClassUtils.createClass("DollarPrefixedTypeUse", node -> {
			node.fields.add(new FieldNode(ACC_PUBLIC, "bar", "L" + topLevelName + ";", null, null));
			node.fields.add(new FieldNode(ACC_PUBLIC, "types", "L" + nestedName + ";", null, null));
		});
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		JvmClassInfo topLevel = assertHasPhantom(phantomBundle, topLevelName);
		JvmClassInfo nested = assertHasPhantom(phantomBundle, nestedName);
		assertNull(phantomBundle.get("foo/"));
		assertNull(topLevel.getOuterClassName());
		assertEquals(topLevelName, nested.getOuterClassName());
	}

	@Test
	void testSpecialInterfaceHandleInfersPhantomInterface() {
		// A LDC handle to an interface method should infer the owner type is an interface.
		JvmClassInfo input = TestClassUtils.createClass("SpecialHandleKindEvidence", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "()V", null, null);
			method.maxStack = 1;
			method.visitLdcInsn(new Handle(H_INVOKESPECIAL, "MissingSpecialInterface", "run", "()V", true));
			method.visitInsn(POP);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(input);

		assertTrue(assertHasPhantom(phantoms.getJvmClassBundle(), "MissingSpecialInterface").hasInterfaceModifier());
		compileWithPhantoms("SpecialHandleKindUse", "class SpecialHandleKindUse implements MissingSpecialInterface { public void run() {} }", phantoms);
	}

	@Test
	void testPlatformJdkTypeLookupKeepsAbsentTypesPhantom() {
		// A class that references a platform type should not generate a phantom for the platform type.
		JvmClassInfo input = TestClassUtils.createClass("PlatformTypeReference", node -> {
			node.fields.add(new FieldNode(ACC_PUBLIC, "present", "Ljavax/xml/transform/Source;", null, null));
			node.fields.add(new FieldNode(ACC_PUBLIC, "absent", "Ljavax/recaf/phantom/MissingType;", null, null));
		});
		JvmClassBundle phantoms = generatePhantoms(input).getJvmClassBundle();

		assertNoPhantom(phantoms, "javax/xml/transform/Source");
		assertHasPhantom(phantoms, "javax/recaf/phantom/MissingType");
	}

	@Test
	void testAnnotationElementsReceiveNeutralDefaults() {
		// We have a class with two annotations of the same type.
		//
		// - One specified values (we shove all types in here for the sake of coverage)
		// - One with only one value specified
		//
		// This implies all the other elements have default-values since otherwise
		// the code wouldn't compile. So when we generate phantoms, they get neutral default values for the unspecified elements.
		JvmClassInfo input = TestClassUtils.createClass("NeutralDefaultCarrier", node -> {
			AnnotationVisitor values = node.visitAnnotation("LMissingDefaults;", true);
			values.visit("enabled", true);
			values.visit("byteValue", (byte) 2);
			values.visit("charValue", 'x');
			values.visit("shortValue", (short) 3);
			values.visit("count", 42);
			values.visit("longValue", 42L);
			values.visit("floatValue", 42.0f);
			values.visit("doubleValue", 42.0d);
			values.visit("text", "observed");
			values.visit("type", Type.getType(String.class));
			values.visitEnum("mode", "LMissingMode;", "ENABLED");
			values.visitEnum("policy", "Ljava/lang/annotation/RetentionPolicy;", "RUNTIME");

			AnnotationVisitor nested = values.visitAnnotation("nested", "LMissingNested;");
			nested.visit("value", "observed");
			nested.visitEnd();
			values.visitEnd();
		});
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(input);

		JvmClassInfo annotation = assertHasPhantom(phantoms.getJvmClassBundle(), "MissingDefaults");
		assertEquals(false, annotation.getDeclaredMethod("enabled", "()Z").getAnnotationDefault().getElementValue());
		assertEquals((byte) 0, annotation.getDeclaredMethod("byteValue", "()B").getAnnotationDefault().getElementValue());
		assertEquals('\0', annotation.getDeclaredMethod("charValue", "()C").getAnnotationDefault().getElementValue());
		assertEquals((short) 0, annotation.getDeclaredMethod("shortValue", "()S").getAnnotationDefault().getElementValue());
		assertEquals(0, annotation.getDeclaredMethod("count", "()I").getAnnotationDefault().getElementValue());
		assertEquals(0L, annotation.getDeclaredMethod("longValue", "()J").getAnnotationDefault().getElementValue());
		assertEquals(0.0f, annotation.getDeclaredMethod("floatValue", "()F").getAnnotationDefault().getElementValue());
		assertEquals(0.0d, annotation.getDeclaredMethod("doubleValue", "()D").getAnnotationDefault().getElementValue());
		assertEquals("", annotation.getDeclaredMethod("text", "()Ljava/lang/String;").getAnnotationDefault().getElementValue());
		assertEquals(Type.getType(Object.class), annotation.getDeclaredMethod("type", "()Ljava/lang/Class;").getAnnotationDefault().getElementValue());

		AnnotationEnumReference enumDefault = assertInstanceOf(AnnotationEnumReference.class, annotation.getDeclaredMethod("mode", "()LMissingMode;").getAnnotationDefault().getElementValue());
		assertEquals("ENABLED", enumDefault.getValue());

		AnnotationEnumReference platformEnumDefault = assertInstanceOf(AnnotationEnumReference.class, annotation.getDeclaredMethod("policy", "()Ljava/lang/annotation/RetentionPolicy;").getAnnotationDefault().getElementValue());
		assertEquals("CLASS", platformEnumDefault.getValue());

		AnnotationInfo nestedDefault = assertInstanceOf(AnnotationInfo.class, annotation.getDeclaredMethod("nested", "()LMissingNested;").getAnnotationDefault().getElementValue());
		assertEquals("LMissingNested;", nestedDefault.getDescriptor());

		compileWithPhantoms("NeutralDefaultUse", "@MissingDefaults class NeutralDefaultUse {}", phantoms);
	}

	@Test
	void testJetBrainsLocalVariableAnnotationReference() {
		// Contrived case where you have a @NotNull annotation on a local variable.
		// People really put this shit on variables. I get it, but man. Type annotations are such cringe.
		JvmClassInfo input = TestClassUtils.createClass("JetBrainsLocalAnnotationExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "()V", null, null);
			LabelNode start = new LabelNode();
			LabelNode end = new LabelNode();
			method.instructions.add(start);
			method.visitInsn(RETURN);
			method.instructions.add(end);
			method.localVariables.add(new LocalVariableNode("value", "Ljava/lang/String;", null, start, end, 0));
			AnnotationVisitor annotation = method.visitLocalVariableAnnotation(
					TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue(),
					null, new Label[]{start.getLabel()}, new Label[]{end.getLabel()}, new int[]{0},
					"Lorg/jetbrains/annotations/NotNull;", true);
			annotation.visitEnd();
			node.methods.add(method);
		});

		// Anyways, we should be able to generate the @NotNull annotation as a phantom.
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		JvmClassInfo annotation = assertHasPhantom(phantomBundle, "org/jetbrains/annotations/NotNull");
		assertTrue(annotation.hasAnnotationModifier());
	}

	@Test
	void testJetBrainsRecordComponentAnnotationReference() {
		// Same annotation as above, but on a more normal case like a record component.
		JvmClassInfo input = assemble("""
				.super java/lang/Record
				.visible-annotation org/jetbrains/annotations/NotNull {
				}
				.record-component value Ljava/lang/String;
				.class public super record JetBrainsRecordAnnotationExample {
				}
				""", true);
		assertHasPhantom(generatePhantoms(input).getJvmClassBundle(), "org/jetbrains/annotations/NotNull");
	}

	@Test
	void testReferencedKnownTypeHierarchy() {
		// Create two types:
		//  - KnownChild implements MissingParentInterface
		//  - KnownTypeReference uses KnownChild as a method parameter
		JvmClassInfo knownInterface = TestClassUtils.createClass("KnownChild", node -> {
			node.access = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
			node.interfaces.add("MissingParentInterface");
		});
		JvmClassInfo input = TestClassUtils.createClass("KnownTypeReference", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC, "use", "(LKnownChild;)V", null, null);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownInterface, input));

		// When generating phantoms for KnownTypeReference, the missing parent interface should be generated as a phantom.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(workspace, input);
		assertTrue(assertHasPhantom(phantoms.getJvmClassBundle(), "MissingParentInterface").hasInterfaceModifier());
	}

	@Test
	void testInstructionReferenceFollowingFindsAnnoOnMethod() {
		// interface KnownAnnotatedOwner { @NotNull void run() {} }
		JvmClassInfo knownOwner = assemble("""
				.super java/lang/Object
				.class public abstract interface KnownAnnotatedOwner {
				    .visible-annotation org/jetbrains/annotations/NotNull {
				    }
				    .method public abstract run ()V {
				    }
				}
				""", true);

		// class KnownOwnerUse { void example() { KnownAnnotatedOwner.run(); } }
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super KnownOwnerUse {
				    .method public static example ()V {
				        code: {
				        A:
				            aconst_null
				            invokeinterface KnownAnnotatedOwner.run ()V
				            return
				        B:
				        }
				    }
				}
				""", true);

		// When we analyze phantoms for KnownOwnerUse, the underlying reference following logic
		// should trigger analysis of KnownAnnotatedOwner, which should result in the @NotNull annotation being generated as a phantom.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownOwner));
		assertHasPhantom(generatePhantoms(workspace, input).getJvmClassBundle(), "org/jetbrains/annotations/NotNull");
	}

	@Test
	void testInstructionReferenceFollowingFindsAnnoOnLocal() {
		// Same test as above more or less, but the @NotNull annotation is on a local variable instead of a method.
		// Not super obvious to represent the annotation in JASM for this case...
		JvmClassInfo knownOwner = TestClassUtils.createClass("KnownLocalAnnotatedOwner", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", "()V", null, null);
			LabelNode start = new LabelNode();
			LabelNode end = new LabelNode();
			method.instructions.add(start);
			method.visitInsn(RETURN);
			method.instructions.add(end);
			method.localVariables.add(new LocalVariableNode("value", "Ljava/lang/String;", null, start, end, 0));
			AnnotationVisitor annotation = method.visitLocalVariableAnnotation(
					TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue(),
					null, new Label[]{start.getLabel()}, new Label[]{end.getLabel()}, new int[]{0},
					"Lorg/jetbrains/annotations/NotNull;", true);
			annotation.visitEnd();
			node.methods.add(method);
		});
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super KnownLocalAnnotationUse {
				    .method public static example ()V {
				        code: {
				        A:
				            invokestatic KnownLocalAnnotatedOwner.run ()V
				            return
				        B:
				        }
				    }
				}
				""", true);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownOwner));
		assertHasPhantom(generatePhantoms(workspace, input).getJvmClassBundle(), "org/jetbrains/annotations/NotNull");
	}

	@Test
	void testGenericArityIsRetainedInPhantoms() {
		// class GenericTypeReference implements MissingProvider<Pair<String, Integer>> { }
		JvmClassInfo input = TestClassUtils.createClass("GenericTypeReference", node -> {
			node.interfaces.add("MissingProvider");
			node.signature = "Ljava/lang/Object;LMissingProvider<Landroid/util/Pair<Ljava/lang/String;Ljava/lang/Integer;>;>;";
		});

		// Validate that source referencing the generic phantom can compile.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(input);
		CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
				.withClassName("GenericPairUse")
				.withClassSource("class GenericPairUse { android.util.Pair<String, Integer> value; }")
				.build(), null, Collections.singletonList(phantoms), null);
		assertTrue(result.wasSuccess(), () -> "Generic phantom should compile: " + result.getDiagnostics());
	}

	@Test
	void testGenericNestedTypeReferencesAreCollected() {
		// All three referenced types in the signature should be generated:
		//  - missing/Bound
		//  - missing/MissingContainer
		//    - missing/Element
		JvmClassInfo input = assemble("""
				.signature "<T:Lmissing/Bound;>Ljava/lang/Object;Lmissing/MissingContainer<Lmissing/Element;>;"
				.super java/lang/Object
				.class public super GenericNestedTypeReference {
				}
				""", true);
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		assertHasPhantoms(phantomBundle, "missing/Bound", "missing/MissingContainer", "missing/Element");
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
		assertEquals("java/lang/Throwable", assertHasPhantom(phantomBundle, "MissingExceptionType").getSuperName());
		assertEquals("java/lang/Throwable", assertHasPhantom(phantomBundle, "MissingCatchType").getSuperName());
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
		JvmClassInfo lambdaOwner = assertHasPhantom(phantomBundle, "MissingLambdaOwner");
		assertHasPhantom(phantomBundle, "MissingFunctionalInterface");
		assertNotNull(lambdaOwner.getDeclaredMethod("impl", "()V"));
	}

	@Test
	void testIndyArgs() {
		// Values in invokedynamic arguments should also be visited and infer missing types.
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super InvokeDynamicArgumentExample {
				    .method public static example (LMissingSource;)V {
				        parameters: { source },
				        code: {
				        A:
				            aload source
				            invokedynamic accept (LMissingTarget;)V { invokestatic, java/lang/invoke/LambdaMetafactory.metafactory, (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; } {}
				            return
				        B:
				        }
				    }
				}
				""", true);

		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		assertEquals("MissingTarget", assertHasPhantom(phantomBundle, "MissingSource").getSuperName());
	}

	@Test
	void testBootstrapTypesDoNotRequireWorkspaceRuntime() {
		// If somehow the user makes a workspace with no runtime resource as an internal supporting resource
		// then we shouldn't end up generating phantoms for JDK classes like 'Object' or 'List'.
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build(), Collections.emptyList(), false);
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super NoRuntimeResourceExample {
				    .method public static use (Ljava/util/List;)Ljava/lang/String; {
				        parameters: { list },
				        code: {
				        A:
				            aconst_null
				            areturn
				        B:
				        }
				    }
				}
				""", true);

		JvmClassBundle phantomBundle = generatePhantoms(workspace, input).getJvmClassBundle();
		assertNoPhantom(phantomBundle, "java/lang/Object");
		assertNoPhantom(phantomBundle, "java/util/List");
		assertNoPhantom(phantomBundle, "java/lang/String");
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
			method.maxStack = 2;
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
	void testConflictingClassConstraintsMethodsCompile() {
		// 'Child' is used as both a 'ParentA' and a 'ParentB', and both are classes. A class has one direct parent,
		// but both parents are phantoms, so they can be chained to satisfy both uses.
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

		// Compilation should succeed, and both calls should be 'invokevirtual' since both parents are classes in the chain.
		JvmClassInfo use = compileWithPhantoms("ChainedParentsUse", """
				public class ChainedParentsUse {
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
	void testKnownClassKeepsItsPhantomParent() {
		// 'KnownChild' exists in the workspace and extends 'MissingBase', which does not. 'PhantomSource' is returned as a
		// 'KnownChild', so it must extend it. The known class's own parent cannot change, so it still needs a phantom.
		JvmClassInfo knownChild = TestClassUtils.createClass("KnownChild", node -> node.superName = "MissingBase");
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownChild));
		JvmClassInfo input = assemble("""
				.method public static asKnown (LPhantomSource;)LKnownChild; {
					parameters: { source },
				    code: {
				    A:
				        aload source
				        areturn
				    B:
				    }
				}
				""", false);

		// The known class is not regenerated, the phantom extends it, and its missing parent is generated.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(workspace, input);
		JvmClassBundle bundle = phantoms.getJvmClassBundle();
		assertNoPhantom(bundle, "KnownChild");
		assertEquals("KnownChild", assertHasPhantom(bundle, "PhantomSource").getSuperName());
		assertHasPhantom(bundle, "MissingBase");
	}

	@Test
	void testFinalSuperclassFlowRequirementIsDropped() {
		// 'Child' is returned as both a 'FinalParent' and an 'OtherParent'. 'FinalParent' is a known final class,
		// so nothing can extend it.
		JvmClassInfo finalParent = TestClassUtils.createClass("FinalParent", node -> {
			node.access = ACC_PUBLIC | ACC_FINAL | ACC_SUPER;
			node.superName = "java/lang/Object";
		});
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(finalParent));
		JvmClassInfo input = assemble("""
				.method public static toFinal (LChild;)LFinalParent; {
					parameters: { child },
				    code: {
				    A:
				        aload child
				        areturn
				    B:
				    }
				}
				.method public static toOther (LChild;)LOtherParent; {
					parameters: { child },
				    code: {
				    A:
				        aload child
				        areturn
				    B:
				    }
				}
				""", false);

		// The final superclass is a soft flow requirement, so it is dropped rather than aborting generation.
		JvmClassBundle bundle = generatePhantoms(workspace, input).getJvmClassBundle();
		assertNoPhantom(bundle, "FinalParent");
		assertNotEquals("FinalParent", assertHasPhantom(bundle, "Child").getSuperName());
	}

	@Test
	void testFinalInheritedMethodIsNotRedeclared() {
		// Object.getClass() is final, so a phantom that receives the call must not declare it again.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(assemble("""
				.method public static example (LGz;)Ljava/lang/Class; {
					parameters: { gz },
				    code: {
				    A:
				        aload gz
				        invokevirtual Gz.getClass ()Ljava/lang/Class;
				        areturn
				    B:
				    }
				}
				""", false));

		// The phantom inherits 'getClass()' from Object rather than declaring it.
		JvmClassInfo gz = assertHasPhantom(phantoms.getJvmClassBundle(), "Gz");
		assertTrue(gz.getMethods().stream().noneMatch(method -> method.getName().equals("getClass")));
	}

	@Test
	void testAbstractNestedEnumIsNotEmittedFinal() {
		// javac records an enum with abstract members as ABSTRACT (not FINAL) in its InnerClasses entry.
		// A class cannot be both, so the generated inner-class record must not combine them.
		JvmClassInfo outer = TestClassUtils.createClass("Outer", node -> node.innerClasses.add(
				new InnerClassNode("Outer$Kind", "Outer", "Kind", ACC_PUBLIC | ACC_STATIC | ACC_ENUM | ACC_ABSTRACT)));
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(outer);
		JvmClassInfo kind = assertHasPhantom(phantoms.getJvmClassBundle(), "Outer$Kind");

		// Read the access flags of the phantom's own inner-class record.
		int[] innerAccess = {-1};
		new ClassReader(kind.getBytecode()).accept(new ClassVisitor(ASM9) {
			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				if ("Outer$Kind".equals(name))
					innerAccess[0] = access;
			}
		}, 0);
		assertTrue(innerAccess[0] != -1, "Missing inner class record for Outer$Kind");
		assertFalse((innerAccess[0] & ACC_FINAL) != 0 && (innerAccess[0] & ACC_ABSTRACT) != 0,
				"Illegal combined FINAL and ABSTRACT: 0x" + Integer.toHexString(innerAccess[0]));
	}

	@Test
	void testHintSetsSuperclassOfUnevidencedPhantom() {
		// Nothing in the input says what 'MissingRel' extends. A hint supplies the relation.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("KnownBase", node -> {})));
		JvmClassInfo input = TestClassUtils.createClass("HintUse", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", "(LMissingRel;)V", null, null);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		// The hinted class should become the phantom's superclass.
		GeneratedPhantomWorkspaceResource phantoms = assertDoesNotThrow(() ->
				generator.createPhantomsForClasses(workspace, List.of(input), 8,
						List.of(new PhantomHint.Supertype("MissingRel", "KnownBase"))));
		assertEquals("KnownBase", assertHasPhantom(phantoms.getJvmClassBundle(), "MissingRel").getSuperName(),
				"Hint not honored");
	}

	@Test
	void testEnumConstructorIsNotDeclaredTwice() {
		// Constructing an enum constant body invokes the (String, int) constructor. The writer already declares that
		// constructor for enums, so the requirement must not add a second copy, which the JVM rejects as a duplicate.
		JvmClassInfo input = TestClassUtils.createClass("EnumBodyUse", node -> {
			node.innerClasses.add(new InnerClassNode("MissingBody", null, null, ACC_ENUM | ACC_STATIC | ACC_FINAL));
			MethodNode make = new MethodNode(ACC_PUBLIC | ACC_STATIC, "make", "()V", null, null);
			make.visitTypeInsn(NEW, "MissingBody");
			make.visitInsn(DUP);
			make.visitLdcInsn("CONSTANT");
			make.visitInsn(ICONST_0);
			make.visitMethodInsn(INVOKESPECIAL, "MissingBody", "<init>", "(Ljava/lang/String;I)V", false);
			make.visitInsn(POP);
			make.visitInsn(RETURN);
			node.methods.add(make);
		});

		// The enum constructor should be declared exactly once.
		JvmClassInfo body = assertHasPhantom(generatePhantoms(input).getJvmClassBundle(), "MissingBody");
		long declarations = body.getMethods().stream()
				.filter(method -> method.getName().equals("<init>") && method.getDescriptor().equals("(Ljava/lang/String;I)V"))
				.count();
		assertEquals(1, declarations, "Enum constructor declared " + declarations + " times");
	}

	@Test
	void testConflictingHintDoesNotCostUnrelatedHint() {
		// 'Conflict' is hinted to two incomparable known classes, which no single chain can satisfy.
		// 'Other' shares no type with it, so its hint must still be honored.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("KnownLeft", node -> {}),
				TestClassUtils.createClass("KnownRight", node -> {})));
		JvmClassInfo input = TestClassUtils.createClass("HintScope", node -> {
			MethodNode conflict = new MethodNode(ACC_PUBLIC | ACC_STATIC, "a", "(LConflict;)V", null, null);
			conflict.visitInsn(RETURN);
			node.methods.add(conflict);
			MethodNode other = new MethodNode(ACC_PUBLIC | ACC_STATIC, "b", "(LOther;)V", null, null);
			other.visitInsn(RETURN);
			node.methods.add(other);
		});

		// Generation should not fail on the conflict, and the unrelated hint should hold.
		GeneratedPhantomWorkspaceResource phantoms = assertDoesNotThrow(() ->
				generator.createPhantomsForClasses(workspace, List.of(input), 8, List.of(
						new PhantomHint.Supertype("Conflict", "KnownLeft"),
						new PhantomHint.Supertype("Conflict", "KnownRight"),
						new PhantomHint.Supertype("Other", "KnownLeft"))));
		assertEquals("KnownLeft", assertHasPhantom(phantoms.getJvmClassBundle(), "Other").getSuperName());
	}

	@Test
	void testHintAddsInterfaceToUnevidencedPhantom() {
		// Nothing in the input says 'MissingImpl' implements 'KnownIface'. A hint naming an interface must add it as an
		// interface, not as a superclass.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("KnownIface", node -> node.access = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT),
				TestClassUtils.createClass("ImplUse", node -> {
					MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", "(LMissingImpl;)V", null, null);
					method.visitInsn(RETURN);
					node.methods.add(method);
				})));

		// The interface should be implemented, and the superclass left as Object.
		GeneratedPhantomWorkspaceResource phantoms = assertDoesNotThrow(() ->
				generator.createPhantomsForWorkspace(workspace, List.of(new PhantomHint.Supertype("MissingImpl", "KnownIface"))));
		JvmClassInfo impl = assertHasPhantom(phantoms.getJvmClassBundle(), "MissingImpl");
		assertTrue(impl.getInterfaces().contains("KnownIface"), "Interface hint not honored");
		assertEquals("java/lang/Object", impl.getSuperName(), "Interface hint leaked into superclass");
	}

	@Test
	void testCompileRefinesUnevidencedInterfaceFromJavacRelationErrors() {
		// 'IfaceUse' references 'MissingRel' with no evidence about its interfaces. The source assigns it to 'KnownIface',
		// which javac rejects until an interface hint is applied.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("KnownIface", node -> node.access = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT),
				TestClassUtils.createClass("IfaceUse", node -> {
					MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", "(LMissingRel;)V", null, null);
					method.visitInsn(RETURN);
					node.methods.add(method);
				})));
		String source = """
				public class IfaceUse {
				    void run(MissingRel r) {
				        KnownIface iface = r;
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once javac's relation error becomes an interface hint.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("IfaceUse")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Refinement should satisfy the interface"
					+ "\nDiagnostics: " + result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testTypeOnlyHintCreatesPhantomWithoutRelations() {
		// No input class is scanned, so 'OnlyNamed' is only known through the hint naming it.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("KnownBase", node -> {})));
		GeneratedPhantomWorkspaceResource phantoms = assertDoesNotThrow(() ->
				generator.createPhantomsForClasses(workspace, List.of(), 8, List.of(new PhantomHint.TypeOnly("OnlyNamed"))));

		// The phantom should exist with no relation to any other type.
		JvmClassInfo named = assertHasPhantom(phantoms.getJvmClassBundle(), "OnlyNamed");
		assertEquals("java/lang/Object", named.getSuperName());
		assertTrue(named.getInterfaces().isEmpty());
	}

	@Test
	void testCompileGeneratesMissingClassReferencedOnlyBySource() {
		// 'ClassHost' exists in the workspace with no references. 'MissingPlain' is named only by the source, so no
		// phantom exists for it until javac reports it missing and the compiler adds a type-only hint.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("ClassHost", node -> {})));
		String source = """
				public class ClassHost {
				    void run() {
				        MissingPlain m = null;
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once the missing class is generated.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("ClassHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Missing class should be generated"
					+ "\nDiagnostics: " + result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testCompileGeneratesMissingClassInSourcePackage() {
		// A simple name inside a class resolves in that class's package, so 'pkg/Missing' is the phantom to create.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("pkg/ClassHost", node -> {})));
		String source = """
				package pkg;
				public class ClassHost {
				    void run() {
				        Missing m = null;
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once 'pkg/Missing' is generated.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("pkg/ClassHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Missing class in source package should be generated\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testSingleStarImportNamesMissingClassInThatPackage() {
		// The host lives in 'app', but its only on-demand import is 'lib', so the missing name belongs to 'lib'. 'lib'
		// has a surviving class, so the import itself is valid and javac goes on to report the missing name.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("app/ClassHost", node -> {}),
				TestClassUtils.createClass("lib/Present", node -> {})));

		// The compiled host should reference the phantom in 'lib', not in its own package.
		String compiled = compileHost(workspace, """
				package app;
				import lib.*;
				public class ClassHost {
				    Object run() { return Missing.class; }
				}
				""");
		assertTrue(compiled.contains("lib/Missing"), "Missing class should resolve through the star import");
		assertFalse(compiled.contains("app/Missing"), "Missing class should not be placed in the host's package");
	}

	@Test
	void testMultipleStarImportsPreferPackageTheWorkspaceHasClassesIn() {
		// Both 'lib' and 'other' could hold 'Missing'. Hinting both would make javac report it as ambiguous, so one is
		// chosen. 'other' has more classes in the workspace, so it is the likelier home. 'lib' is listed first, so
		// only the count can pick 'other'.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("app/ClassHost", node -> {}),
				TestClassUtils.createClass("lib/Present", node -> {}),
				TestClassUtils.createClass("other/Present", node -> {}),
				TestClassUtils.createClass("other/Another", node -> {})));

		// The compiled host should reference the phantom in 'other' only.
		String compiled = compileHost(workspace, """
				package app;
				import lib.*;
				import other.*;
				public class ClassHost {
				    Object run() { return Missing.class; }
				}
				""");
		assertTrue(compiled.contains("other/Missing"), "Missing class should prefer the populated star package");
		assertFalse(compiled.contains("lib/Missing"), "Missing class should be placed in only one star package");
	}

	@Test
	void testAbsentSingleStarImportIsDiscoveredThroughItsNames() {
		// 'lib' has no classes, so javac stops at the import and reports nothing used from it. The names are found by
		// compiling again without the import, then resolved to 'lib' because it is the only on-demand import.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("app/ClassHost", node -> {})));

		// The compiled host should reference the phantom in 'lib'.
		String compiled = compileHost(workspace, """
				package app;
				import lib.*;
				public class ClassHost {
				    Object run() { return Missing.class; }
				}
				""");
		assertTrue(compiled.contains("lib/Missing"), "Name should be discovered through the absent star import");
	}

	@Test
	void testAbsentStarImportRanksBelowPopulatedOne() {
		// 'lib' is absent and 'other' is populated, so the name goes to 'other' even though 'lib' is listed first.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("app/ClassHost", node -> {}),
				TestClassUtils.createClass("other/Present", node -> {}),
				TestClassUtils.createClass("other/Another", node -> {})));

		// The compiled host should reference the phantom in 'other' only.
		String compiled = compileHost(workspace, """
				package app;
				import lib.*;
				import other.*;
				public class ClassHost {
				    Object run() { return Missing.class; }
				}
				""");
		assertTrue(compiled.contains("other/Missing"), "Name should prefer the populated star package");
		assertFalse(compiled.contains("lib/Missing"), "Name should not be placed in the absent star package");
	}

	@Test
	void testQualifiedReferenceIntoAbsentPackageIsDiscoveredFromItsLine() {
		// 'lib' has no classes, so javac reports the package alone. The class name is read from the reported line.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("app/ClassHost", node -> {})));

		// The compiled host should reference the phantom in 'lib'.
		String compiled = compileHost(workspace, """
				package app;
				public class ClassHost {
				    Object run() { return lib.Missing.class; }
				}
				""");
		assertTrue(compiled.contains("lib/Missing"), "Qualified reference should name the class in its absent package");
	}

	@Test
	void testSingleTypeImportFromAbsentPackageIsDiscoveredFromItsLine() {
		// The import names 'lib.Missing' directly, so no discovery pass is needed.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("app/ClassHost", node -> {})));

		// The compiled host should reference the phantom in 'lib'.
		String compiled = compileHost(workspace, """
				package app;
				import lib.Missing;
				public class ClassHost {
				    Object run() { return Missing.class; }
				}
				""");
		assertTrue(compiled.contains("lib/Missing"), "Single-type import should name the class in its absent package");
	}

	@Test
	void testCompileDeclaresMissingMembersOnPhantomReceiver() {
		// 'MissingMember' is named only by the source. Once it exists, the instance method, field, and static method used
		// on it are missing too, so javac reports each one. Their arguments and receivers come from those reports.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("MemberHost", node -> {})));
		String source = """
				public class MemberHost {
				    void run(MissingMember r) {
				        r.doThing(1, "text");
				        Object value = r.value;
				        MissingMember.make();
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once each reported member is declared.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("MemberHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Missing members should be declared on the phantom\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testCompileRefinesMemberReturnTypeFromUse() {
		// 'count()' is used as an int and 'name()' as a String. Each is first declared as returning Object, then javac's
		// incompatible-types errors on those lines give the types they must return.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("ReturnHost", node -> {})));
		String source = """
				public class ReturnHost {
				    void run(MissingRet r) {
				        int count = r.count();
				        String name = r.name();
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once both return types are refined.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("ReturnHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Return types should be refined from their uses\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testMultiSourceDiagnosticsRetainTheirCompilationUnit() {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("UnrelatedError", node -> {}),
				TestClassUtils.createClass("MemberSource", node -> {})));
		String unrelatedSource = """
				public class UnrelatedError {
				    void run() {
				        String value = new Object();
				    }
				}
				""";
		String memberSource = """
				public class MemberSource {
				    Object run(MissingMember member) {
				        Object value = member.read();
				        return value;
				    }
				}
				""";

		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("UnrelatedError")
					.withClassSource(unrelatedSource)
					.withClassSources(Collections.singletonMap("MemberSource", memberSource))
					.build(), workspace, null);

			// Both sources have a diagnostic or member use on line 3. The recorded source must identify only the
			// compilation unit that javac reported for the unrelated Object-to-String error.
			CompilerDiagnostic unrelatedError = result.getDiagnostics().stream()
					.filter(diagnostic -> diagnostic.message().contains("cannot be converted"))
					.findFirst()
					.orElseThrow();
			assertEquals("UnrelatedError", unrelatedError.sourceName());
			assertEquals(3, unrelatedError.line());
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testCompileDeclaresMissingConstructorOnPhantom() {
		// 'MissingCtor' is named only by the source, so its phantom has just the default constructor at first. The call
		// with an int and a String is then reported as matching no constructor, and its argument types declare one.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("CtorHost", node -> {})));
		String source = """
				public class CtorHost {
				    void run() {
				        MissingCtor made = new MissingCtor(1, "text");
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once the (int, String) constructor is declared.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("CtorHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Missing constructor should be declared on the phantom\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testCompileDeclaresExceptionForCatchOfMissingMember() {
		// 'read()' is called on a phantom and caught as IOException. A phantom method declares nothing at first, so javac
		// reports the catch clause as never thrown. The method on the try block's line then declares IOException.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(TestClassUtils.createClass("CatchHost", node -> {})));
		String source = """
				public class CatchHost {
				    void run(MissingIo r) {
				        try {
				            r.read();
				        } catch (java.io.IOException e) {
				        }
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once 'read()' declares IOException.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("CatchHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Catch should be satisfied by a declared exception\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testCompileGeneratesNestedClassWhoseClassFileIsMissing() {
		// 'Outer' declares member 'Inner' in its InnerClasses attribute, but 'Outer$Inner' has no class file. javac
		// reports "class file for Outer$Inner not found" rather than "cannot find symbol", so only the binary name
		// in that message can supply the phantom.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("Outer", node -> node.innerClasses.add(
						new InnerClassNode("Outer$Inner", "Outer", "Inner", ACC_PUBLIC | ACC_STATIC))),
				TestClassUtils.createClass("NestedHost", node -> {})));
		String source = """
				public class NestedHost {
				    Object run() { return Outer.Inner.class; }
				}
				""";

		// Compiling with phantom generation enabled should succeed once 'Outer$Inner' is generated.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("NestedHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Nested class should be generated from its class-file error\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testCompileRefinesUnevidencedSuperclassFromJavacRelationErrors() {
		// 'RelUse' is in the workspace and references 'MissingRel', so a phantom is generated for it with no superclass
		// evidence. The source then needs 'MissingRel' to be a 'KnownBase', which javac rejects until a hint is applied.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				TestClassUtils.createClass("KnownBase", node -> {}),
				TestClassUtils.createClass("RelUse", node -> {
					MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", "(LMissingRel;)V", null, null);
					method.visitInsn(RETURN);
					node.methods.add(method);
				})));
		String source = """
				public class RelUse {
				    void run(MissingRel r) {
				        KnownBase base = r;
				    }
				}
				""";

		// Compiling with phantom generation enabled should succeed once javac's relation error becomes a superclass hint.
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("RelUse")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Refinement should satisfy the relation"
					+ "\nDiagnostics: " + result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
	}

	@Test
	void testConflictingClassConstraintsFieldsCompile() {
		// Same idea as 'testConflictingClassConstraintsMethodsCompile', but with fields.
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

		// Doesn't matter which order we see, but 'Child' should extend one parent, which extends the other.
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

		// Compilation should succeed, and both field accesses should be 'getfield' since both parents are classes in the chain.
		JvmClassInfo use = compileWithPhantoms("ChainedParentsFieldUse", """
				public class ChainedParentsFieldUse {
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
	void testTargetVersionSelectsVersionedJvmTypes() throws PhantomGenerationException {
		// Set up a workspace with a Java 11 versioned bundle/class.
		JvmClassInfo versionedOwner = assemble("""
				.super java/lang/Object
				.class public super VersionedOwner {
				    .method public static run ()V {
				        code: {
				        A:
				            return
				        B:
				        }
				    }
				}
				""", true);
		BasicVersionedJvmClassBundle versionedBundle = new BasicVersionedJvmClassBundle(11);
		versionedBundle.initialPut(versionedOwner);
		NavigableMap<Integer, VersionedJvmClassBundle> versionedBundles = new TreeMap<>();
		versionedBundles.put(versionedBundle.version(), versionedBundle);
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder()
				.withVersionedJvmClassBundles(versionedBundles)
				.build());

		// The input class references VersionedOwner, which is only present in the Java 11 versioned bundle.
		JvmClassInfo input = assemble("""
				.super java/lang/Object
				.class public super VersionedTypeUse {
				    .method public static example ()V {
				        code: {
				        A:
				            invokestatic VersionedOwner.run ()V
				            return
				        B:
				        }
				    }
				}
				""", true);

		// Generating phantoms for Java 8 should create a phantom for VersionedOwner since it's not present in the Java 8 context.
		// Generating phantoms for Java 11 should not create a phantom since the type is present in the versioned bundle.
		GeneratedPhantomWorkspaceResource java8Phantoms = generator.createPhantomsForClasses(workspace, List.of(input), 8);
		assertHasPhantom(java8Phantoms.getJvmClassBundle(), "VersionedOwner");
		GeneratedPhantomWorkspaceResource java11Phantoms = generator.createPhantomsForClasses(workspace, List.of(input), 11);
		assertNoPhantom(java11Phantoms.getJvmClassBundle(), "VersionedOwner");
		JvmClassInfo emitted = assertHasPhantom(generatePhantoms(input).getJvmClassBundle(), "VersionedOwner");
		assertEquals(52, emitted.getVersion());
		JvmClassInfo java7Emitted = assertHasPhantom(
				generator.createPhantomsForClasses(EmptyWorkspace.get(), List.of(input), 7).getJvmClassBundle(),
				"VersionedOwner");
		assertEquals(51, java7Emitted.getVersion());
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

	@Test
	void testThrownKnownTypeInspectsItsHierarchy() {
		// KnownError extends MissingBase. Declaring 'throws KnownError' references a known type,
		// so the known type's own hierarchy should be inspected for the missing parent.
		JvmClassInfo knownError = TestClassUtils.createClass("KnownError", node -> {
			node.superName = "MissingBase";
		});
		JvmClassInfo input = TestClassUtils.createClass("ThrowsKnownError", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "()V", null, new String[]{"KnownError"});
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownError, input));

		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(workspace, input);
		assertFalse(assertHasPhantom(phantoms.getJvmClassBundle(), "MissingBase").hasInterfaceModifier());
	}

	@Test
	void testAnnotationTypeInspectsItsOwnAnnotations() {
		// KnownAnnotation is itself annotated with MissingMeta.
		// An input class annotated with KnownAnnotation references the known annotation type,
		// so the annotation's own annotations should be inspected.
		JvmClassInfo knownAnnotation = TestClassUtils.createClass("KnownAnnotation", node -> {
			node.access = ACC_PUBLIC | ACC_ANNOTATION | ACC_INTERFACE | ACC_ABSTRACT;
			node.interfaces.add("java/lang/annotation/Annotation");
			node.visitAnnotation("LMissingMeta;", true);
		});
		JvmClassInfo input = TestClassUtils.createClass("AnnotatedWithKnown", node -> {
			node.visitAnnotation("LKnownAnnotation;", true);
		});
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownAnnotation, input));

		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(workspace, input);
		assertTrue(assertHasPhantom(phantoms.getJvmClassBundle(), "MissingMeta").hasAnnotationModifier());
	}

	@Test
	void testKnownTypeReferencesDoNotInspectTheirOwnReferences() {
		// KnownMid is a known field type of the input class.
		//
		// Its method descriptor names KnownDeep, and it is annotated with KnownAnnotation,
		// which is annotated with MissingMeta.
		//
		// Known types reached only through those references are not inspected,
		// so neither their missing hierarchy nor their missing annotations should be generated.
		JvmClassInfo knownDeep = TestClassUtils.createClass("KnownDeep", node -> {
			node.superName = "MissingDeep";
		});
		JvmClassInfo knownAnnotation = TestClassUtils.createClass("KnownAnnotation", node -> {
			node.access = ACC_PUBLIC | ACC_ANNOTATION | ACC_INTERFACE | ACC_ABSTRACT;
			node.interfaces.add("java/lang/annotation/Annotation");
			node.visitAnnotation("LMissingMeta;", true);
		});
		JvmClassInfo knownMid = TestClassUtils.createClass("KnownMid", node -> {
			node.access = ACC_PUBLIC | ACC_ABSTRACT;
			node.visitAnnotation("LKnownAnnotation;", true);
			node.methods.add(new MethodNode(ACC_PUBLIC | ACC_ABSTRACT, "make", "()LKnownDeep;", null, null));
		});
		JvmClassInfo input = TestClassUtils.createClass("KnownMidUse", node -> {
			node.fields.add(new FieldNode(ACC_PUBLIC, "mid", "LKnownMid;", null, null));
		});
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownDeep, knownAnnotation, knownMid, input));

		// The missing types referenced only through known types do not need to be generated.
		JvmClassBundle phantomBundle = generatePhantoms(workspace, input).getJvmClassBundle();
		assertNoPhantom(phantomBundle, "MissingDeep");
		assertNoPhantom(phantomBundle, "MissingMeta");
	}

	@Test
	void testPermittedSubclassExtendsItsSealedType() {
		// KnownSealed permits MissingPermitted.
		JvmClassInfo knownSealed = TestClassUtils.createClass("KnownSealed", node -> {
			node.access = ACC_PUBLIC | ACC_ABSTRACT;
			node.permittedSubclasses = new ArrayList<>(List.of("MissingPermitted"));
		});
		JvmClassInfo input = TestClassUtils.createClass("SealedUse", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "use", "(LKnownSealed;)V", null, null);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(knownSealed, input));

		// The missing permitted subclass should be generated and extend the known sealed type.
		GeneratedPhantomWorkspaceResource phantoms = generatePhantoms(workspace, input);
		assertEquals("KnownSealed", assertHasPhantom(phantoms.getJvmClassBundle(), "MissingPermitted").getSuperName());
	}

	@Test
	void testStrippedDebugSlotDoesNotTakeParameterType() {
		// static void example(FirstValue first, SecondValue second) {
		//     SlotInterface slot = second;
		//     slot.run();
		// }
		JvmClassInfo input = TestClassUtils.createClass("StrippedSlotExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "(LFirstValue;LSecondValue;)V", null, null);
			method.maxStack = 1;

			// SecondValue --> slot
			method.visitVarInsn(ALOAD, 1);
			method.visitVarInsn(ASTORE, 2);

			// slot.run() --> slot must be SlotInterface
			// SecondValue must implement SlotInterface
			method.visitVarInsn(ALOAD, 2);
			method.visitMethodInsn(INVOKEINTERFACE, "SlotInterface", "run", "()V", true);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});

		// The phantom for SecondValue should implement SlotInterface.
		JvmClassInfo second = assertHasPhantom(generatePhantoms(input).getJvmClassBundle(), "SecondValue");
		assertEquals("java/lang/Object", second.getSuperName());
		assertTrue(second.getInterfaces().contains("SlotInterface"));
	}

	@Test
	void testHandlerReachableViaInitializationException() {
		// The only instruction in the protected range is a static field read, which can fail during class initialization.
		// The handler should still receive the local state from that read, so the local's phantom type should flow into the handler.
		//
		// static void example(HandledValue value) {
		//     try {
		//         Object popped = StaticHolder.FIELD; // can throw if StaticHolder.<clinit> fails
		//     } catch (Throwable t) {
		//         HandlerSink.take(value); // value should be inferred to extend HandlerTarget
		//     }
		// }
		JvmClassInfo input = TestClassUtils.createClass("StaticReadHandlerExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "(LHandledValue;)V", null, null);
			method.maxStack = 1;
			Label start = new Label();
			Label end = new Label();
			Label handler = new Label();
			method.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
			method.visitLabel(start);
			method.visitFieldInsn(GETSTATIC, "StaticHolder", "FIELD", "Ljava/lang/Object;"); // Can throw if StaticHolder.<clinit> fails
			method.visitInsn(POP);
			method.visitLabel(end);
			method.visitInsn(RETURN);
			method.visitLabel(handler);
			method.visitVarInsn(ASTORE, 1);
			method.visitVarInsn(ALOAD, 0);
			method.visitMethodInsn(INVOKESTATIC, "HandlerSink", "take", "(LHandlerTarget;)V", false);
			method.visitInsn(RETURN);
			node.methods.add(method);
		});
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		assertEquals("HandlerTarget", assertHasPhantom(phantomBundle, "HandledValue").getSuperName());
	}

	@Test
	void testConflictingStackHeightsAtLoopJoinTerminate() {
		// Junk method with invalid stack heights at a loop join.
		//
		// We should still be able to generate phantoms for types used in the portion of the
		// method with valid stack heights, such as LoopValue implementing LoopIface.
		JvmClassInfo input = TestClassUtils.createClass("StackConflictLoopExample", node -> {
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "example", "(LLoopValue;)V", null, null);
			method.maxStack = 2;
			Label header = new Label();
			Label oneValueBranch = new Label();

			method.visitVarInsn(ALOAD, 0);
			method.visitMethodInsn(INVOKEINTERFACE, "LoopIface", "run", "()V", true);

			method.visitJumpInsn(GOTO, header); // Empty stack --> header
			method.visitLabel(header); // <-- Receives flow of {}, {0}, and {0, 0}

			method.visitInsn(ICONST_0);
			method.visitJumpInsn(IFEQ, oneValueBranch); // {0} --> oneValueBranch

			method.visitInsn(ICONST_0);
			method.visitInsn(ICONST_0);
			method.visitJumpInsn(GOTO, header); // {0, 0} --> header

			method.visitLabel(oneValueBranch);
			method.visitInsn(ICONST_0);
			method.visitJumpInsn(GOTO, header); // {0} --> header
			node.methods.add(method);
		});
		JvmClassBundle phantomBundle = generatePhantoms(input).getJvmClassBundle();
		assertTrue(assertHasPhantom(phantomBundle, "LoopValue").getInterfaces().contains("LoopIface"));
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
		return assertDoesNotThrow(() -> generator.createPhantomsForClasses(workspace, List.of(classes), 8));
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
	private String compileHost(@Nonnull Workspace workspace, @Nonnull String source) {
		JavacCompilerConfig javacConfig = recaf.get(JavacCompilerConfig.class);
		javacConfig.getGeneratePhantoms().setValue(true);
		try {
			CompilerResult result = compiler.compile(new JavacArgumentsBuilder()
					.withClassName("app/ClassHost")
					.withClassSource(source)
					.build(), workspace, null);
			assertTrue(result.wasSuccess(), () -> "Compile should succeed after refinement\nDiagnostics: "
					+ result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n")));
			byte[] bytes = result.getCompilations().values().iterator().next();
			return new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
		} finally {
			javacConfig.getGeneratePhantoms().setValue(false);
		}
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
