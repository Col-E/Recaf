package software.coley.recaf.util.assembler.resolve;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import org.junit.jupiter.api.Test;
import software.coley.recaf.test.CompilerTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AssemblyResolver}.
 */
class AssemblyResolverTest extends CompilerTestBase {
	private static final String MAIN_ASSEMBLY = """
			.visible-annotation ClassAnno {
			    value: "class-level"
			}
			.super java/lang/Object
			.implements java/io/Serializable
			.inner public static {
			    name: NestedSimple,
			    inner: ResolverCarrier$NestedSimple,
			    outer: ResolverCarrier
			}
			.class public super ResolverCarrier {
			    .field public plainField I
			
			    .visible-annotation FieldAnno {
			        value: "field-level"
			    }
			    .field public annotatedField I
			
			    .visible-annotation MethodAnno {
			        value: "method-level"
			    }
			    .method public resolverMethod (I)V {
			        parameters: { methodParam },
			        exceptions: {
			            { tryStart, tryEnd, catchLabel, * }
			        },
			        code: {
			        declOnly:
			            nop
			        tryStart:
			            istore tempVar
			            new created/Type
			            ifeq flowTarget
			            iload methodParam
			            tableswitch {
			                min: 7,
			                max: 8,
			                cases: { tableCaseA, tableCaseB },
			                default: tableDefault
			            }
			        flowTarget:
			            lookupswitch {
			                11: lookupCaseA,
			                12: lookupCaseB,
			                default: lookupDefault
			            }
			        tryEnd:
			            return
			        catchLabel:
			            return
			        tableCaseA:
			            return
			        tableCaseB:
			            return
			        tableDefault:
			            return
			        lookupCaseA:
			            return
			        lookupCaseB:
			            return
			        lookupDefault:
			            return
			        }
			    }
			
			    .method public plainMethod ()V {
			        code: {
			            return
			        }
			    }
			}
			""";

	@Test
	void resolveAtReturnsEmptyWhenAstIsUnset() {
		// Validates the resolver stays stable when nothing has been parsed yet.
		AssemblyResolver resolver = new AssemblyResolver();
		assertSame(AssemblyResolution.EMPTY, resolver.resolveAt(0));
	}

	@Test
	void resolveAtRecognizesStandaloneAnnotations() {
		// Validates top-level annotations resolve properly.
		AssemblyResolution resolution = resolve("""
				.visible-annotation LooseAnno {
				    value: "loose"
				}
				""", "LooseAnno");
		IndependentAnnotationResolution annotationResolution = assertInstanceOf(IndependentAnnotationResolution.class, resolution);
		assertEquals("LooseAnno", annotationResolution.annotation().classType().content());
	}

	@Test
	void resolveAtRecognizesClassLevelSelections() {
		// Validates class-level attributes resolve to the specific class-related resolution types.
		ClassAnnotationResolution classAnnotation = assertInstanceOf(ClassAnnotationResolution.class, resolve(MAIN_ASSEMBLY, "ClassAnno"));
		assertEquals("ClassAnno", classAnnotation.annotation().classType().content());

		// Type extended
		ClassExtends classExtends = assertInstanceOf(ClassExtends.class, resolve(MAIN_ASSEMBLY, "java/lang/Object"));
		assertEquals("java/lang/Object", classExtends.superName().content());

		// Type implemented
		ClassImplements classImplements = assertInstanceOf(ClassImplements.class, resolve(MAIN_ASSEMBLY, "java/io/Serializable"));
		assertEquals("java/io/Serializable", classImplements.implemented().content());

		// Inner class
		InnerClassResolution innerClass = assertInstanceOf(InnerClassResolution.class, resolve(MAIN_ASSEMBLY, "ResolverCarrier$NestedSimple"));
		assertEquals("ResolverCarrier$NestedSimple", innerClass.inner().innerClass().content());
	}

	@Test
	void resolveAtRecognizesFieldSelections() {
		// Validates field selections resolve properly.
		FieldResolution fieldResolution = assertInstanceOf(FieldResolution.class, resolve(MAIN_ASSEMBLY, "plainField"));
		assertEquals("plainField", fieldResolution.field().getName().content());

		// And their annotations resolve properly as well.
		FieldAnnotationResolution fieldAnnotation = assertInstanceOf(FieldAnnotationResolution.class, resolve(MAIN_ASSEMBLY, "FieldAnno"));
		assertEquals("FieldAnno", fieldAnnotation.annotation().classType().content());
		assertEquals("annotatedField", fieldAnnotation.targetField().getName().content());
	}

	@Test
	void resolveAtRecognizesMethodSelections() {
		// Validates method selections resolve properly.
		MethodResolution methodResolution = assertInstanceOf(MethodResolution.class, resolve(MAIN_ASSEMBLY, "plainMethod"));
		assertEquals("plainMethod", methodResolution.method().getName().content());

		// And their annotations resolve properly as well.
		MethodAnnotationResolution methodAnnotation = assertInstanceOf(MethodAnnotationResolution.class, resolve(MAIN_ASSEMBLY, "MethodAnno"));
		assertEquals("MethodAnno", methodAnnotation.annotation().classType().content());
		assertEquals("resolverMethod", methodAnnotation.targetMethod().getName().content());
	}

	@Test
	void resolveAtRecognizesVariableSelections() {
		// Validates both parameter names and instruction operands resolve as variable declarations.
		VariableDeclarationResolution parameterResolution = assertInstanceOf(VariableDeclarationResolution.class, resolve(MAIN_ASSEMBLY, "methodParam"));
		VariableDeclarationResolution operandResolution = assertInstanceOf(VariableDeclarationResolution.class, resolve(MAIN_ASSEMBLY, "tempVar"));
		assertEquals("methodParam", parameterResolution.variableName().content());
		assertEquals("tempVar", operandResolution.variableName().content());
	}

	@Test
	void resolveAtRecognizesLabelSelections() {
		// Validates label declarations can be resolved from varying contexts.
		LabelDeclarationResolution declarationResolution = assertInstanceOf(LabelDeclarationResolution.class, resolve(MAIN_ASSEMBLY, "declOnly"));
		assertEquals("declOnly", declarationResolution.label().identifier().content());

		LabelReferenceResolution exceptionResolution = assertInstanceOf(LabelReferenceResolution.class, resolve(MAIN_ASSEMBLY, "tryStart"));
		assertEquals("tryStart", exceptionResolution.labelName().content());

		LabelReferenceResolution flowResolution = assertInstanceOf(LabelReferenceResolution.class, resolve(MAIN_ASSEMBLY, "flowTarget"));
		assertEquals("flowTarget", flowResolution.labelName().content());

		LabelReferenceResolution tableResolution = assertInstanceOf(LabelReferenceResolution.class, resolve(MAIN_ASSEMBLY, "tableDefault"));
		assertEquals("tableDefault", tableResolution.labelName().content());

		LabelReferenceResolution lookupResolution = assertInstanceOf(LabelReferenceResolution.class, resolve(MAIN_ASSEMBLY, "lookupCaseA"));
		assertEquals("lookupCaseA", lookupResolution.labelName().content());
	}

	@Test
	void resolveAtRecognizesTypeSelections() {
		// Validates both wildcard catch types and explicit instruction type operands resolve as type references.
		TypeReferenceResolution catchTypeResolution = assertInstanceOf(TypeReferenceResolution.class, resolve(MAIN_ASSEMBLY, "*"));
		assertEquals("*", catchTypeResolution.typeName().content());

		TypeReferenceResolution instructionTypeResolution = assertInstanceOf(TypeReferenceResolution.class, resolve(MAIN_ASSEMBLY, "created/Type"));
		assertEquals("created/Type", instructionTypeResolution.typeName().content());
	}

	@Test
	void resolveAtFallsBackToInstructionResolutionForOrdinaryInstructions() {
		// Validates that instructions that don't have any special handling for operands and such still resolve to a generic instruction resolution.
		InstructionResolution resolution = assertInstanceOf(InstructionResolution.class, resolve(MAIN_ASSEMBLY, "nop"));
		assertEquals("nop", resolution.instruction().identifier().content());
		assertNotNull(resolution.parentClass());
	}

	@Nonnull
	private AssemblyResolution resolve(@Nonnull String assembly, @Nonnull String token) {
		AssemblyResolver resolver = new AssemblyResolver();
		List<ASTElement> ast = assembleAst(assembly, true);
		resolver.setAst(ast);
		return resolver.resolveAt(offsetOf(assembly, token));
	}

	private static int offsetOf(@Nonnull String text, @Nonnull String token) {
		int offset = text.indexOf(token);
		if (offset < 0)
			throw new IllegalArgumentException("Missing token: " + token);
		return offset;
	}
}
