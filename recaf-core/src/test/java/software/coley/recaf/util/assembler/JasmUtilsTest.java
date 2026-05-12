package software.coley.recaf.util.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.junit.jupiter.api.Test;
import software.coley.recaf.test.CompilerTestBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JasmUtils}.
 */
@SuppressWarnings("ConstantValue")
class JasmUtilsTest extends CompilerTestBase {
	@Test
	void instructionClassifiersHandleNullAndKnownOpcodes() {
		// Validate various inputs for 'isX' classifying methods.
		assertFalse(JasmUtils.isFlowControlInstruction(null));
		assertTrue(JasmUtils.isFlowControlInstruction("goto"));
		assertFalse(JasmUtils.isFlowControlInstruction("return"));

		assertFalse(JasmUtils.isSwitchInstruction(null));
		assertTrue(JasmUtils.isSwitchInstruction("tableswitch"));
		assertTrue(JasmUtils.isSwitchInstruction("lookupswitch"));
		assertFalse(JasmUtils.isSwitchInstruction("ifeq"));

		assertFalse(JasmUtils.isVariableReferenceInstruction(null));
		assertTrue(JasmUtils.isVariableReferenceInstruction("iload"));
		assertTrue(JasmUtils.isVariableReferenceInstruction("istore"));
		assertTrue(JasmUtils.isVariableReferenceInstruction("ret"));
		assertTrue(JasmUtils.isVariableReferenceInstruction("iinc"));
		assertTrue(JasmUtils.isVariableReferenceInstruction("aload"));
		assertTrue(JasmUtils.isVariableReferenceInstruction("astore"));
		assertFalse(JasmUtils.isVariableReferenceInstruction("nop"));

		assertFalse(JasmUtils.isTypeReferenceInstruction(null));
		assertTrue(JasmUtils.isTypeReferenceInstruction("new"));
		assertTrue(JasmUtils.isTypeReferenceInstruction("checkcast"));
		assertFalse(JasmUtils.isTypeReferenceInstruction("invokevirtual"));
	}

	@Test
	void findInstructionReturnsNullForNullAst() {
		// Validates lookup fails gracefully when given a null AST.
		assertNull(JasmUtils.findInstruction(null, 0, 1));
	}

	@Test
	void findInstructionMatchesByPositionAndLine() {
		String assembly = """
				.super java/lang/Object
				.class public super FindInstructionClass {
				    .method public static example ()V {
				        code: {
				            iconst_0
				            istore counter
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = assembleAst(assembly, true);

		// Validates a normal instruction can be looked-up from its line/position.
		ASTInstruction instruction = JasmUtils.findInstruction(ast,
				offsetOf(assembly, "istore counter"),
				lineOf(assembly, "istore counter"));
		assertNotNull(instruction);
		assertEquals("istore", instruction.identifier().content());
	}

	@Test
	void findInstructionSkipsNullElementsAndResolvesSwitchBodySelections() {
		String assembly = """
				.super java/lang/Object
				.class public super SwitchInstructionClass {
				    .method public static example (I)V {
				        parameters: { key },
				        code: {
				            iload key
				            tableswitch {
				                min: 1,
				                max: 2,
				                cases: { caseOne, caseTwo },
				                default: defaultCase
				            }
				        caseOne:
				            return
				        caseTwo:
				            return
				        defaultCase:
				            return
				        }
				    }
				}
				""";
		// There was a bug a while ago originating from JASM reporting null values somewhere in the AST.
		// I don't have a reliable repro for that, so we simulate it here.
		List<ASTElement> ast = new ArrayList<>(assembleAst(assembly, true));
		ast.addFirst(null);

		// Validates that if we do see a null we still can resolve other existing instructions.
		ASTInstruction instruction = JasmUtils.findInstruction(ast, offsetOf(assembly, "defaultCase"), lineOf(assembly, "defaultCase"));
		assertNotNull(instruction);
		assertEquals("tableswitch", instruction.identifier().content());
	}

	@Test
	void getLabelDeclarationFindsPresentAndMissingLabels() {
		ASTMethod method = firstMethod(assembleAst("""
				.super java/lang/Object
				.class public super LabelLookupClass {
				    .method public static example ()V {
				        code: {
				        alpha:
				            return
				        beta:
				            return
				        }
				    }
				}
				""", true), "example");

		// Validate we can find a label by name.
		ASTLabel alpha = JasmUtils.getLabelDeclaration(method, "alpha");
		assertNotNull(alpha);
		assertEquals("alpha", alpha.identifier().content());

		// And that a bogus label name returns null.
		assertNull(JasmUtils.getLabelDeclaration(method, "missing"));
	}

	@Test
	void collectVariableUsagesAndReferencesTrackParametersReadsWritesAndIncrements() {
		String assembly = """
				.super java/lang/Object
				.class public super VariableUsageClass {
				    .method public static target (I)V {
				        parameters: { methodParam },
				        code: {
				            iload methodParam
				            istore localCopy
				            iinc localCopy 1
				            iload localCopy
				            return
				        }
				    }
				    .method public static other ()V {
				        code: {
				            istore otherLocal
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = assembleAst(assembly, true);
		ASTMethod target = firstMethod(ast, "target");

		// Validate parameter and local variable usages are tracked properly, and that usages from other methods are not included.
		Map<String, JasmAstUsages> usages = JasmUtils.collectVariableUsages(ast, "target", "(I)V");
		JasmAstUsages methodParam = usages.get("methodParam");
		JasmAstUsages localCopy = usages.get("localCopy");
		assertTrue(methodParam.isParameter());
		assertEquals(1, methodParam.readers().size());
		assertEquals(0, methodParam.writers().size());
		assertNotNull(localCopy);
		assertFalse(localCopy.isParameter());
		assertEquals(1, localCopy.readers().size());
		assertEquals(2, localCopy.writers().size());

		// Since we looked at 'target' the usage from 'other' should not be included.
		assertFalse(usages.containsKey("otherLocal"));

		// Validate that the references collected for 'localCopy' are correct and in the right order.
		List<JasmUtils.VariableReference> references = JasmUtils.collectVariableReferences(target, "localCopy");
		assertEquals(List.of(
				JasmUtils.VariableReferenceKind.WRITE,
				JasmUtils.VariableReferenceKind.INCREMENT,
				JasmUtils.VariableReferenceKind.READ
		), references.stream().map(JasmUtils.VariableReference::kind).toList());
		assertEquals(List.of("localCopy", "localCopy", "localCopy"),
				references.stream().map(reference -> reference.element().content()).toList());
	}

	@Test
	void collectLabelUsagesAndReferencesCoverFlowSwitchAndTryCatchReferences() {
		String assembly = """
				.super java/lang/Object
				.class public super LabelUsageClass {
				    .method public static target (I)V {
				        parameters: { key },
				        exceptions: {
				            { tryStart, tryEnd, catchLabel, * }
				        },
				        code: {
				        tryStart:
				            iload key
				            ifeq flowTarget
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
				    .method public static other ()V {
				        code: {
				        ignored:
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = assembleAst(assembly, true);
		ASTMethod target = firstMethod(ast, "target");

		// Validate that label usages are tracked properly, and that usages from other methods are not included.
		Map<String, JasmAstUsages> usages = JasmUtils.collectLabelUsages(ast, "target", "(I)V");
		assertFalse(usages.containsKey("ignored"));
		assertEquals(1, usages.get("tryStart").readers().size());
		assertEquals(0, usages.get("tryStart").writers().size());
		assertEquals(1, usages.get("flowTarget").readers().size());
		assertEquals(1, usages.get("flowTarget").writers().size());
		assertEquals(1, usages.get("tableDefault").readers().size());
		assertEquals(1, usages.get("tableDefault").writers().size());

		// The flow target label should have a single flow control reference from the 'ifeq' instruction.
		List<JasmUtils.LabelReference> flowReferences = JasmUtils.collectLabelReferences(target, "flowTarget");
		assertEquals(1, flowReferences.size());
		assertEquals(JasmUtils.LabelReferenceKind.FLOW, flowReferences.getFirst().kind());
		assertEquals("ifeq", flowReferences.getFirst().context());

		// The table case label should have a single switch case reference from the 'tableswitch' instruction.
		List<JasmUtils.LabelReference> switchCaseReferences = JasmUtils.collectLabelReferences(target, "tableCaseA");
		assertEquals(1, switchCaseReferences.size());
		assertEquals(JasmUtils.LabelReferenceKind.SWITCH_CASE, switchCaseReferences.getFirst().kind());
		assertEquals("7", switchCaseReferences.getFirst().context());

		// The table/lookup-switch default labels should both have a single reference from their respective instructions.
		List<JasmUtils.LabelReference> switchDefaultReferences = JasmUtils.collectLabelReferences(target, "lookupDefault");
		assertEquals(1, switchDefaultReferences.size());
		assertEquals(JasmUtils.LabelReferenceKind.SWITCH_DEFAULT, switchDefaultReferences.getFirst().kind());
		assertNull(switchDefaultReferences.getFirst().context());
		switchDefaultReferences = JasmUtils.collectLabelReferences(target, "tableDefault");
		assertEquals(1, switchDefaultReferences.size());
		assertEquals(JasmUtils.LabelReferenceKind.SWITCH_DEFAULT, switchDefaultReferences.getFirst().kind());
		assertNull(switchDefaultReferences.getFirst().context());

		// The try-catch labels should have references from the exception table,
		// and the try start/end labels should also have references from the try-catch block.
		List<JasmUtils.LabelReference> tryReferences = JasmUtils.collectLabelReferences(target, "catchLabel");
		List<JasmUtils.LabelReference> orderedTryRefs = JasmUtils.collectLabelReferences(target, "tryEnd");
		assertEquals(List.of(JasmUtils.LabelReferenceKind.HANDLER),
				tryReferences.stream().map(JasmUtils.LabelReference::kind).toList());
		assertEquals(List.of(JasmUtils.LabelReferenceKind.TRY_START),
				JasmUtils.collectLabelReferences(target, "tryStart").stream().map(JasmUtils.LabelReference::kind).toList());
		assertEquals(List.of(JasmUtils.LabelReferenceKind.TRY_END),
				orderedTryRefs.stream().map(JasmUtils.LabelReference::kind).toList());
	}

	private static int offsetOf(String text, String token) {
		int offset = text.indexOf(token);
		if (offset < 0)
			throw new IllegalArgumentException("Missing token: " + token);
		return offset;
	}

	private static int lineOf(String text, String token) {
		int offset = offsetOf(text, token);
		int line = 1;
		for (int i = 0; i < offset; i++) {
			if (text.charAt(i) == '\n')
				line++;
		}
		return line;
	}

	private static ASTMethod firstMethod(List<ASTElement> ast, String name) {
		for (ASTElement element : ast) {
			if (element instanceof ASTClass klass) {
				for (ASTElement child : klass.children()) {
					if (child instanceof ASTMethod method && Objects.equals(name, method.getName().literal()))
						return method;
				}
			} else if (element instanceof ASTMethod method && Objects.equals(name, method.getName().literal())) {
				return method;
			}
		}
		throw new IllegalArgumentException("Missing method: " + name + " in " +
				ast.stream().map(ASTElement::content).collect(Collectors.joining(", ")));
	}
}
