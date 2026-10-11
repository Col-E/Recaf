package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.target.TargetContext;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.assembler.AndroidAssemblerPipeline;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndexService;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for assembler completion resolution at a caret position.
 */
class AssemblerTabCompleterTest extends TestBase {
	private static final String DALVIK_REFERENCE_SOURCE = """
			.method public static sample ()V {
			    registers: 2,
			    code: {
			        goto target
			        new-instance v0 Lsample/Owner;
			        sget v0 sample/Owner.value I
			        invoke-static { v0 } sample/Owner.call ()V
			        invoke-polymorphic { v0 } sample/Owner.virtual ()V ()V
			        packed-switch v0 { first: 0, targets: { target, other } }
			        sparse-switch v0 { 1: target, 2: other }
			    target:
			    target:
			    other:
			        return-void
			    }
			}
			""";
	private static final String DALVIK_CONSTANTS_SOURCE = """
			.method public static sample ()V {
			    registers: 1,
			    code: {
			        const v0 .enum sample/Owner value I
			        const v0 .member sample/Owner value I
			        const-method-handle v0 { invokestatic, sample/Owner.call, ()V }
			        const-method-handle v0 LambdaMetafactory.metafactory
			        invoke-custom { v0 } callSite ()V { invokestatic, sample/Owner.call, ()V } { .enum sample/Owner value I, .member sample/Owner value I }
			        return-void
			    }
			}
			""";
	private static final String DALVIK_SWITCH_SOURCE = """
			.method public static sample (I)V {
			    registers: 1,
			    parameters: { key },
			    code: {
			        goto target
			        packed-switch v0 { first: 0, targets: { target, other } }
			        sparse-switch v0 { 1: target, 2: other }
			    target:
			    target:
			    other:
			        return-void
			    }
			}
			""";
	private static final String JVM_SWITCH_SOURCE = """
			.method public static sample (I)V {
			    parameters: { key },
			    code: {
			        goto target
			        tableswitch { min: 0, max: 1, default: target, cases: { target, other } }
			        lookupswitch { default: target, 1: other }
			    target:
			    other:
			        return
			    }
			}
			""";
	private static final String JVM_LOCALS_SOURCE = """
			.method public static sample (I)V {
			    parameters: { input },
			    code: {
			        iload input
			        iinc local 1
			        ldc { dynamicName, Lsample/Owner;, { invokestatic, sample/Owner.call, ()V }, { Lsample/Owner;, { invokestatic, sample/Owner.call, ()V } } }
			        ldc LambdaMetafactory.metafactory
			        invokedynamic site ()V { invokestatic, sample/Owner.call, ()V } { Lsample/Owner;, { invokestatic, sample/Owner.call, ()V } }
			        return
			    }
			}
			""";

	private static final String JVM_MEMBER_SOURCE = """
			.method public static sample ()V {
			    code: {
			        getstatic sample/Owner.value I
			        invokestatic sample/Owner.call ()V
			        invokevirtual sample/Owner.virtual ()V
			        new sample/Owner
			        checkcast sample/Owner
			        instanceof sample/Owner
			        return
			    }
			}
			""";

	private static final String HIERARCHY_SOURCE = """
			.method public static sample ()V {
			    code: {
			        invokevirtual sample/Child.ca ()V
			        return
			    }
			}
			""";

	@Test
	void dalvikRegisterOperandsOfferDeclaredRegisters() {
		// v0 and v1 are declared by the method, v2 is not.
		String source = DALVIK_REFERENCE_SOURCE;
		Workspace workspace = workspaceWithOwner();
		List<ASTElement> ast = parseDalvikElements(workspace, source);
		AssemblerCompletionEngine engine = newEngine(workspace, DalvikTargetContext.INSTANCE);

		int caret = source.indexOf("invoke-static { v0") + "invoke-static { v".length();
		List<AssemblerCompletion> completions = completionAt(engine, source, ast, caret);
		assertTrue(hasOperandCompletion(completions, "v0", AssemblerCompletion.OperandKind.REGISTER));
		assertTrue(hasOperandCompletion(completions, "v1", AssemblerCompletion.OperandKind.REGISTER));
		assertFalse(hasOperandCompletion(completions, "v2", AssemblerCompletion.OperandKind.REGISTER));
	}

	@Test
	void dalvikTypeOperandCompletesDeclaredOwner() {
		// "Lsample/Ow" is a partial descriptor for the owner in new-instance.
		String source = DALVIK_REFERENCE_SOURCE;
		Workspace workspace = workspaceWithOwner();
		List<ASTElement> ast = parseDalvikElements(workspace, source);
		AssemblerCompletionEngine engine = newEngine(workspace, DalvikTargetContext.INSTANCE);

		String typedType = "Lsample/Ow";
		List<AssemblerCompletion> completions = completionAt(engine, source, ast, source.indexOf(typedType) + typedType.length());
		assertTrue(hasTypeCompletion(completions, "Lsample/Owner;"));
	}

	@Test
	void dalvikFieldReferenceCompletesMemberName() {
		// "sample/Owner.va" is a partial field reference in sget.
		String source = DALVIK_REFERENCE_SOURCE;
		Workspace workspace = workspaceWithOwner();
		List<ASTElement> ast = parseDalvikElements(workspace, source);
		AssemblerCompletionEngine engine = newEngine(workspace, DalvikTargetContext.INSTANCE);

		String typedField = "sample/Owner.va";
		List<AssemblerCompletion> completions = completionAt(engine, source, ast, source.indexOf(typedField) + typedField.length());
		assertTrue(hasMemberCompletion(completions, "value I"));
	}

	@Test
	void dalvikInvokeReferenceCompletesMethodName() {
		// "sample/Owner.ca" is a partial method reference in invoke-static.
		String source = DALVIK_REFERENCE_SOURCE;
		Workspace workspace = workspaceWithOwner();
		List<ASTElement> ast = parseDalvikElements(workspace, source);
		AssemblerCompletionEngine engine = newEngine(workspace, DalvikTargetContext.INSTANCE);

		String typedMethod = "sample/Owner.ca";
		List<AssemblerCompletion> completions = completionAt(engine, source, ast, source.lastIndexOf(typedMethod) + typedMethod.length());
		assertTrue(hasMemberCompletion(completions, "call ()V"));
	}

	@Test
	void dalvikPolymorphicInvokeCompletesMethodName() {
		// "sample/Owner.vi" is a partial method reference in invoke-polymorphic.
		String source = DALVIK_REFERENCE_SOURCE;
		Workspace workspace = workspaceWithOwner();
		List<ASTElement> ast = parseDalvikElements(workspace, source);
		AssemblerCompletionEngine engine = newEngine(workspace, DalvikTargetContext.INSTANCE);

		String typedVirtualMethod = "sample/Owner.vi";
		List<AssemblerCompletion> completions = completionAt(engine, source, ast,
				source.lastIndexOf(typedVirtualMethod) + typedVirtualMethod.length());
		assertTrue(hasMemberCompletion(completions, "virtual ()V"));
	}

	@Test
	void dalvikBranchLabelsCompleteAtGotoAndSwitchTargets() {
		String source = DALVIK_REFERENCE_SOURCE;
		Workspace workspace = workspaceWithOwner();
		List<ASTElement> ast = parseDalvikElements(workspace, source);
		AssemblerCompletionEngine engine = newEngine(workspace, DalvikTargetContext.INSTANCE);

		// goto target
		int branchLabelStart = source.indexOf("target", source.indexOf("goto"));
		assertTrue(hasLabelCompletion(completionAt(engine, source, ast, branchLabelStart + 3), "target"));

		// packed-switch targets
		int packedLabelStart = source.indexOf("target", source.indexOf("targets:") + "targets:".length());
		assertTrue(hasLabelCompletion(completionAt(engine, source, ast, packedLabelStart + 3), "target"));

		// sparse-switch case targets
		int sparseLabelStart = source.indexOf("target", source.indexOf("sparse-switch"));
		assertTrue(hasLabelCompletion(completionAt(engine, source, ast, sparseLabelStart + 3), "target"));
	}

	@Test
	void dalvikEnumDeclarationCompletesOwnerAndFields() {
		String source = DALVIK_CONSTANTS_SOURCE;
		ASTMethod method = parseDalvikMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), DalvikTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// The owner after ".enum" completes as a type.
		int enumOwnerStart = source.indexOf("sample/Owner", source.indexOf(".enum"));
		assertTrue(hasTypeCompletion(completionAt(engine, source, ast, enumOwnerStart + "sample/Own".length()), "sample/Owner"));

		// The field after the owner is a declaration member, and it is the second argument.
		int enumFieldStart = source.indexOf("value", enumOwnerStart);
		var enumSite = AssemblerOperandLocator.locate(source, enumFieldStart + 2, ast, DalvikTargetContext.INSTANCE);
		assertNotNull(enumSite);
		assertEquals(1, enumSite.argumentIndex());
		assertEquals("DECLARATION_MEMBER", AssemblerStructuredOperands.structuredOperand(enumSite, DalvikTargetContext.INSTANCE).kind().name());
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, enumFieldStart + 2), "value I"));
	}

	@Test
	void dalvikMemberDeclarationCompletesFieldAndDescriptor() {
		String source = DALVIK_CONSTANTS_SOURCE;
		ASTMethod method = parseDalvikMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), DalvikTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// The name after ".member" is a declaration member of the owner.
		int memberStart = source.indexOf(".member", source.indexOf("invoke-custom"));
		int memberNameStart = source.indexOf("value", memberStart);
		var memberSite = AssemblerOperandLocator.locate(source, memberNameStart + 2, ast, DalvikTargetContext.INSTANCE);
		assertNotNull(memberSite);
		assertEquals("DECLARATION_MEMBER", AssemblerStructuredOperands.structuredOperand(memberSite, DalvikTargetContext.INSTANCE).kind().name());
		assertEquals("sample/Owner", AssemblerStructuredOperands.structuredOperand(memberSite, DalvikTargetContext.INSTANCE).memberReference());
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, memberNameStart + 2), "value I"));

		// The descriptor after the name completes as a member descriptor.
		int memberDescriptorStart = source.indexOf(" I", memberNameStart) + 1;
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, memberDescriptorStart), "I"));
	}

	@Test
	void dalvikHandleConstantCompletesKindMemberAndShortcut() {
		String source = DALVIK_CONSTANTS_SOURCE;
		ASTMethod method = parseDalvikMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), DalvikTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// The handle kind "invokestatic" completes as a structured constant.
		int handleKindStart = source.indexOf("invokestatic");
		assertTrue(hasOperandCompletion(completionAt(engine, source, ast, handleKindStart + "invoke".length()),
				"invokestatic", AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT));

		// The handle member completes as a method reference.
		int handleMemberStart = source.indexOf("sample/Owner.call", handleKindStart);
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, handleMemberStart + "sample/Owner.ca".length()), "call ()V"));

		// The bootstrap shortcut completes as a structured constant.
		String shortcut = "LambdaMetafactory.metafactory";
		assertTrue(hasOperandCompletion(completionAt(engine, source, ast,
						source.indexOf(shortcut) + "LambdaMeta".length()), shortcut,
				AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT));
	}

	@Test
	void dalvikCallSiteNameOffersNoMembers() {
		// The call site name is a plain identifier, not a member reference.
		String source = DALVIK_CONSTANTS_SOURCE;
		ASTMethod method = parseDalvikMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), DalvikTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		int callSiteStart = source.indexOf("callSite");
		assertFalse(completionAt(engine, source, ast, callSiteStart + 4).stream()
				.anyMatch(AssemblerCompletion.Member.class::isInstance));
	}

	@Test
	void dalvikCustomCallSiteUsesHandleAndEnumSchemas() {
		String source = DALVIK_CONSTANTS_SOURCE;
		ASTMethod method = parseDalvikMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), DalvikTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// The handle member inside invoke-custom completes as a method reference.
		int customHandleMemberStart = source.indexOf("sample/Owner.call", source.indexOf("invoke-custom"));
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, customHandleMemberStart + "sample/Owner.ca".length()), "call ()V"));

		// The enum field inside invoke-custom completes as a declaration member.
		int customEnumFieldStart = source.indexOf("value", source.indexOf(".enum", source.indexOf("invoke-custom")));
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, customEnumFieldStart + 2), "value I"));
	}

	@Test
	void dalvikRegisterCandidatesRespectBoundsAndWideIncomingSlots() {
		ASTMethod method = parseDalvikMethod("""
				.method public sample (JDLjava/lang/String;)V {
				    registers: 6,
				    parameters: { this, wide, precise, text },
				    code: {
				        invoke-static { v0, named } sample/Owner.call ()V
				        invoke-static/range { first, last } sample/Owner.call ()V
				        return-void
				    }
				}
				""");
		List<String> names = AssemblerOperandCandidates.dalvikRegisterCandidates(
				method, DalvikTargetContext.INSTANCE, "");

		// Registers v0 through v5 and their p-prefixed forms exist, but v6 does not.
		for (int i = 0; i < 6; i++) {
			assertTrue(names.contains("v" + i));
			assertTrue(names.contains("p" + i));
		}
		assertFalse(names.contains("v6"));
		assertFalse(names.contains("p6"));

		// Parameter names and named registers from the instructions are offered.
		assertTrue(names.contains("this"));
		assertTrue(names.containsAll(List.of("wide", "precise", "text", "named", "first", "last")));
	}

	@Test
	void dalvikRegisterFallbackUsesOnlyObservedNumericRegisters() {
		// Without a usable registers declaration, only registers seen in instructions are offered.
		for (String registers : List.of("", "registers: broken,")) {
			ASTMethod method = parseDalvikMethod("""
					.method public static sample ()V {
					    %s
					    parameters: {},
					    code: {
					        invoke-static { v0, v2 } sample/Owner.call ()V
					        return-void
					    }
					}
					""".formatted(registers));
			List<String> names = AssemblerOperandCandidates.dalvikRegisterCandidates(
					method, DalvikTargetContext.INSTANCE, "v");
			assertTrue(names.containsAll(List.of("v0", "v2")));
			assertFalse(names.contains("v1"));
		}
	}

	@Test
	void dalvikSwitchLabelsAreCollectedByTargetSchema() {
		ASTMethod method = parseDalvikMethod(DALVIK_SWITCH_SOURCE);

		// Only jump targets are collected as labels.
		assertEquals(List.of("target"), AssemblerOperandCandidates.labelCandidates(
				method, DalvikTargetContext.INSTANCE, "tar"));

		// Target labels in packed and sparse switches are label positions, the first case key is not.
		assertTrue(switchLabelAt(DALVIK_SWITCH_SOURCE, method, "targets: { target", DalvikTargetContext.INSTANCE));
		assertTrue(switchLabelAt(DALVIK_SWITCH_SOURCE, method, "1: target", DalvikTargetContext.INSTANCE));
		assertFalse(switchLabelAt(DALVIK_SWITCH_SOURCE, method, "first: 0", DalvikTargetContext.INSTANCE));
	}

	@Test
	void jvmSwitchLabelsAreCollectedByTargetSchema() {
		ASTMethod method = parseJvmMethod(JVM_SWITCH_SOURCE);

		// Only jump targets are collected as labels.
		assertEquals(List.of("target"), AssemblerOperandCandidates.labelCandidates(
				method, JvmTargetContext.INSTANCE, "tar"));

		// Targets in tableswitch cases, lookupswitch default, and lookupswitch cases are label positions.
		assertTrue(switchLabelAt(JVM_SWITCH_SOURCE, method, "cases: { target", JvmTargetContext.INSTANCE));
		assertTrue(switchLabelAt(JVM_SWITCH_SOURCE, method, "default: target", JvmTargetContext.INSTANCE));
		assertTrue(switchLabelAt(JVM_SWITCH_SOURCE, method, "1: other", JvmTargetContext.INSTANCE));
	}

	@Test
	void jvmSwitchLabelsCompleteOnlyAtLabelValues() {
		String source = JVM_SWITCH_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(new BasicWorkspace(new WorkspaceResourceBuilder().build()), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// Label values complete in tableswitch cases and lookupswitch.
		int tableLabelStart = source.indexOf("target", source.indexOf("cases:"));
		assertTrue(hasLabelCompletion(completionAt(engine, source, ast, tableLabelStart + 3), "target"));
		int lookupDefaultStart = source.indexOf("target", source.indexOf("lookupswitch"));
		assertTrue(hasLabelCompletion(completionAt(engine, source, ast, lookupDefaultStart + 3), "target"));
		int lookupCaseStart = source.indexOf("other", source.indexOf("lookupswitch"));
		assertTrue(hasLabelCompletion(completionAt(engine, source, ast, lookupCaseStart + 2), "other"));

		// A numeric min value is not a label position.
		int minValueStart = source.indexOf("min: 0") + "min: ".length();
		assertFalse(hasLabelCompletion(completionAt(engine, source, ast, minValueStart + 1), "target"));
	}

	@Test
	void jvmLocalVariablesCompleteFromParameterAndLocalNames() {
		String source = JVM_LOCALS_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// The parameter name in iload completes as a local.
		String inputPrefix = "input";
		assertTrue(hasOperandCompletion(completionAt(engine, source, ast,
				source.lastIndexOf(inputPrefix) + 2), "input", AssemblerCompletion.OperandKind.LOCAL));

		// The variable name in iinc completes as a local.
		String localPrefix = "local";
		assertTrue(hasOperandCompletion(completionAt(engine, source, ast,
				source.indexOf(localPrefix) + 2), "local", AssemblerCompletion.OperandKind.LOCAL));
	}

	@Test
	void jvmDynamicConstantTypesCompleteInsideBootstrapArguments() {
		String source = JVM_LOCALS_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);
		String condyType = "Lsample/Owner;";

		// The type naming the dynamic constant in ldc.
		int condyTypeStart = source.indexOf(condyType);
		assertTrue(hasTypeCompletion(completionAt(engine, source, ast, condyTypeStart + "Lsample/Ow".length()), condyType));

		// The type argument of the dynamic constant.
		int condyArgumentTypeStart = source.indexOf(condyType, condyTypeStart + condyType.length());
		assertTrue(hasTypeCompletion(completionAt(engine, source, ast, condyArgumentTypeStart + "Lsample/Ow".length()), condyType));

		// The type argument of invokedynamic, which is the last occurrence.
		int indyClassStart = source.lastIndexOf(condyType);
		assertTrue(hasTypeCompletion(completionAt(engine, source, ast, indyClassStart + "Lsample/Ow".length()), condyType));
	}

	@Test
	void jvmDynamicConstantHandlesCompleteMembersAndShortcuts() {
		String source = JVM_LOCALS_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// The handle kind "invokestatic" completes as a structured constant.
		int handleKindStart = source.indexOf("invokestatic");
		assertTrue(hasOperandCompletion(completionAt(engine, source, ast,
						handleKindStart + "invoke".length()), "invokestatic",
				AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT));

		// The handle member completes as a method reference.
		String handleMember = "sample/Owner.call";
		int handleMemberStart = source.indexOf(handleMember, handleKindStart);
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast,
				handleMemberStart + "sample/Owner.ca".length()), "call ()V"));

		// The bootstrap descriptor after the handle completes as a member descriptor.
		int bootstrapDescriptorStart = source.indexOf("()V", handleMemberStart);
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast, bootstrapDescriptorStart + 1), "()V"));

		// The bootstrap shortcut completes as a structured constant.
		String shortcut = "LambdaMetafactory.metafactory";
		assertTrue(hasOperandCompletion(completionAt(engine, source, ast,
						source.indexOf(shortcut) + "LambdaMeta".length()), shortcut,
				AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT));
	}

	@Test
	void jvmBootstrapArgumentHandlesCompleteMembers() {
		String source = JVM_LOCALS_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);
		String handleMember = "sample/Owner.call";

		// The handle inside the second ldc argument.
		int firstHandleStart = source.indexOf(handleMember);
		int secondHandleStart = source.indexOf(handleMember, firstHandleStart + handleMember.length());
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast,
				secondHandleStart + "sample/Owner.ca".length()), "call ()V"));

		// The handle inside the invokedynamic arguments, which is the last occurrence.
		int lastHandleStart = source.lastIndexOf(handleMember);
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast,
				lastHandleStart + "sample/Owner.ca".length()), "call ()V"));
	}

	@Test
	void jvmMemberReferencesCompleteFieldAndMethodNames() {
		String source = JVM_MEMBER_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// getstatic field reference.
		String typedField = "sample/Owner.va";
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast,
				source.indexOf(typedField) + typedField.length()), "value I"));

		// invokestatic method reference.
		String typedStatic = "sample/Owner.ca";
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast,
				source.indexOf(typedStatic) + typedStatic.length()), "call ()V"));

		// invokevirtual method reference.
		String typedVirtual = "sample/Owner.vi";
		assertTrue(hasMemberCompletion(completionAt(engine, source, ast,
				source.indexOf(typedVirtual) + typedVirtual.length()), "virtual ()V"));
	}

	@Test
	void jvmTypeOperandsCompleteForNewCheckcastAndInstanceof() {
		String source = JVM_MEMBER_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		List<ASTElement> ast = List.of(method);

		// JVM type operands use internal names rather than descriptors.
		for (String instruction : List.of("new ", "checkcast ", "instanceof ")) {
			int caret = source.indexOf(instruction + "sample/Owner") + instruction.length() + "sample/Ow".length();
			assertTrue(hasTypeCompletion(completionAt(engine, source, ast, caret), "sample/Owner"), instruction);
		}
	}

	@Test
	void simpleTypePrefixesCompleteToFullInternalNames() {
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		AssemblerCompletionEngine engine = newEngine(workspace, JvmTargetContext.INSTANCE);

		// In both cases we want to complete the full type name from the simple name prefix.
		//  - new Stri
		//  - invokevirtual Stri
		for (String instruction : List.of("new ", "invokevirtual ")) {
			String source = "code: {\n    " + instruction + "Stri";
			List<AssemblerCompletion> completions = engine.resolve(source, source.length(), List.of(), null).complete();
			AssemblerCompletion.Type stringType = completions.stream()
					.filter(AssemblerCompletion.Type.class::isInstance)
					.map(AssemblerCompletion.Type.class::cast)
					.filter(type -> type.text().equals("java/lang/String"))
					.findFirst()
					.orElse(null);

			assertNotNull(stringType, instruction);
			assertTrue(stringType.replacePartialText(), instruction);
		}
	}

	@Test
	void camelHumpTypePrefixesCompleteAtTheCaret() {
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		AssemblerCompletionEngine engine = newEngine(workspace, JvmTargetContext.INSTANCE);

		// In both cases we want to complete the full type name from camel-hump prefix.
		//  - new StrB
		//  - invokevirtual StriB
		for (String instruction : List.of("new ", "invokevirtual ")) {
			String source = "code: {\n    " + instruction + "StrB";
			List<AssemblerCompletion> completions = engine.resolve(source, source.length(), List.of(), null).complete();
			AssemblerCompletion.Type stringBuilder = completions.stream()
					.filter(AssemblerCompletion.Type.class::isInstance)
					.map(AssemblerCompletion.Type.class::cast)
					.filter(type -> type.text().equals("java/lang/StringBuilder"))
					.findFirst()
					.orElse(null);

			assertNotNull(stringBuilder, instruction);
			assertTrue(stringBuilder.replacePartialText(), instruction);
			assertEquals(2, stringBuilder.matchRank(), instruction);
		}

		// In both cases we want to complete the full type name from the acronym prefix.
		//  - new SB
		//  - invokevirtual SB
		List<AssemblerCompletion> acronymCompletions = engine.resolve("code: {\n    new SB", "code: {\n    new SB".length(), List.of(), null).complete();
		assertTrue(hasTypeCompletion(acronymCompletions, "java/lang/StringBuilder"));
		assertTrue(hasTypeCompletion(acronymCompletions, "java/lang/StringBuffer"));
		assertEquals(3, acronymCompletions.stream()
				.filter(AssemblerCompletion.Type.class::isInstance)
				.map(AssemblerCompletion.Type.class::cast)
				.filter(type -> type.text().equals("java/lang/StringBuilder"))
				.findFirst().orElseThrow().matchRank());
	}

	@Test
	void incompleteJvmHandleOperandsUseTheCursorScannerWithoutMethodAst() {
		String astSource = """
				.method public static sample ()V {
				    code: {
				        return
				    }
				}
				""";
		ASTMethod staleMethod = parseJvmMethod(astSource);
		String incompleteSource = """
				.method public static sample ()V {
				    code: {
				        ldc { invokestatic, sample/Owner.ca
				""";
		int caret = incompleteSource.indexOf("sample/Owner.ca") + "sample/Owner.ca".length();

		// Without a usable AST, the cursor scanner still finds the ldc handle operand.
		var site = AssemblerOperandLocator.locate(incompleteSource, caret, List.of(), JvmTargetContext.INSTANCE);
		assertNotNull(site);
		assertEquals("ldc", site.mnemonic());
		assertEquals(List.of(1), site.nestedIndices());
		assertEquals("HANDLE_MEMBER", AssemblerStructuredOperands.structuredOperand(site, JvmTargetContext.INSTANCE).kind().name());

		AssemblerCompletionEngine engine = newEngine(workspaceWithOwner(), JvmTargetContext.INSTANCE);
		assertTrue(hasMemberCompletion(completionAt(engine, incompleteSource, List.of(staleMethod), caret), "call ()V"));
	}

	@Test
	void visibilityFollowsTheEditedClass() {
		String source = """
				.method public static sample ()V {
				    code: {
				        new b/Shown
				        invokevirtual b/Other.run ()V
				        new a/Helper
				    }
				}
				""";
		ASTMethod method = parseJvmMethod(source);
		List<ASTElement> ast = List.of(method);
		AssemblerCompletionEngine engine = newEngine(visibilityWorkspace(), JvmTargetContext.INSTANCE);

		// From a/Caller, b/Shown is public and b/Hidden is package-private in package b.
		int newB = source.indexOf("new b/Shown") + "new b/".length();
		List<AssemblerCompletion> types = engine.resolve(source, newB, ast, "a/Caller").complete();
		assertTrue(hasTypeCompletion(types, "b/Shown"));
		assertFalse(hasTypeCompletion(types, "b/Hidden"));

		// b/Other.secret is private to b/Other, so only run is offered.
		int member = source.indexOf("invokevirtual b/Other.run") + "invokevirtual b/Other.".length();
		List<AssemblerCompletion> members = engine.resolve(source, member, ast, "a/Caller").complete();
		assertTrue(hasMemberCompletion(members, "run ()V"));
		assertFalse(hasMemberCompletion(members, "secret ()V"));

		// a/Helper is package-private, but shares the edited class's package.
		int newA = source.indexOf("new a/Helper") + "new a/".length();
		assertTrue(hasTypeCompletion(engine.resolve(source, newA, ast, "a/Caller").complete(), "a/Helper"));
	}

	@Test
	void assemblerSpecialCharactersAreThoseUsedInDescriptorsAndNames() {
		AssemblerTabCompleter completer = new AssemblerTabCompleter(mock(Workspace.class), mock(InheritanceGraph.class), testTypeIndexService(), mock(CellConfigurationService.class), new TabCompletionConfig());
		for (char character : "/[;-_()".toCharArray())
			assertTrue(completer.isSpecialCompletableCharacter(character), String.valueOf(character));
		for (char character : new char[]{',', ' ', '"'})
			assertFalse(completer.isSpecialCompletableCharacter(character), String.valueOf(character));
	}

	@Test
	void jvmTypeOperandCompletionsAreSortedByText() {
		String source = JVM_MEMBER_SOURCE;
		ASTMethod method = parseJvmMethod(source);
		// Classes are added out of order so the result only matches when the completions are sorted.
		Workspace workspace = workspaceWithClasses("sample/Owner", "sample/Owned", "sample/Owl");
		AssemblerCompletionEngine engine = newEngine(workspace, JvmTargetContext.INSTANCE);

		int caret = source.indexOf("new sample/Owner") + "new sample/Ow".length();
		List<String> typeTexts = completionAt(engine, source, List.of(method), caret).stream()
				.filter(AssemblerCompletion.Type.class::isInstance)
				.map(AssemblerCompletion::text)
				.toList();
		assertEquals(List.of("sample/Owl", "sample/Owned", "sample/Owner"), typeTexts);
	}

	@Test
	void resolvedCompletionsRespectMaxCompletionLength() {
		TabCompletionConfig config = new TabCompletionConfig();
		AssemblerTabCompleter completer = new AssemblerTabCompleter(mock(Workspace.class), mock(InheritanceGraph.class), testTypeIndexService(), mock(CellConfigurationService.class), config);
		completer.setTargetContext(DalvikTargetContext.INSTANCE);
		String source = "    code: {\n        invoke-sta";

		// 'invoke-static' is 13 characters long, so a limit of 5 removes it.
		config.setMaxCompletionLength(5);
		assertTrue(completer.resolve(source, source.length()).isEmpty());

		config.setMaxCompletionLength(200);
		assertTrue(completer.resolve(source, source.length()).contains(new AssemblerCompletion.Opcode("invoke-static")));
	}

	@Test
	void inheritedOverrideOffersOneCandidate() {
		String source = HIERARCHY_SOURCE;
		Workspace workspace = workspaceWithHierarchy();
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = new AssemblerCompletionEngine(testTypeIndexService(), workspace, new InheritanceGraph(workspace), JvmTargetContext.INSTANCE);

		// 'call ()V' is declared by both sample/Child and its parent sample/Base.
		String typed = "sample/Child.ca";
		long callCount = completionAt(engine, source, List.of(method), source.indexOf(typed) + typed.length()).stream()
				.filter(completion -> completion instanceof AssemblerCompletion.Member member && member.text().equals("call ()V"))
				.count();
		assertEquals(1, callCount);
	}

	@Test
	void inheritedMembersWithSameDescriptorKeepTheirOwnNames() {
		String source = HIERARCHY_SOURCE;
		Workspace workspace = workspaceWithHierarchy();
		ASTMethod method = parseJvmMethod(source);
		AssemblerCompletionEngine engine = new AssemblerCompletionEngine(testTypeIndexService(), workspace, new InheritanceGraph(workspace), JvmTargetContext.INSTANCE);

		// Both members share the descriptor '()V', so the name is what tells them apart.
		String typed = "sample/Child.";
		List<AssemblerCompletion> completions = completionAt(engine, source, List.of(method), source.indexOf(typed) + typed.length());
		assertTrue(hasMemberCompletion(completions, "call ()V"));
		assertTrue(hasMemberCompletion(completions, "other ()V"));
	}

	@Test
	void constructedCompleterHasNoCompletionsBeforeEditorInstall() {
		AssemblerTabCompleter completer = new AssemblerTabCompleter(mock(Workspace.class), mock(InheritanceGraph.class), testTypeIndexService(), mock(CellConfigurationService.class), new TabCompletionConfig());
		assertTrue(completer.computeCurrentCompletions().isEmpty());
	}

	@Test
	void resolvesOpcodeFromCrlfSourceWithoutAst() {
		// The partial mnemonic after a CRLF line break is still an opcode position.
		AssemblerCompletionEngine engine = newEngine(mock(Workspace.class), DalvikTargetContext.INSTANCE);
		String source = "    code: {\r\n        invoke-sta";
		AssemblerCompletionContext context = engine.resolve(source, source.length(), List.of(), null);
		assertEquals("invoke-sta", context.partialMatchedText());
		assertTrue(context.complete().contains(new AssemblerCompletion.Opcode("invoke-static")));
	}

	@Test
	void completerResolveReturnsOpcodesForTargetContext() {
		AssemblerTabCompleter completer = new AssemblerTabCompleter(mock(Workspace.class), mock(InheritanceGraph.class), testTypeIndexService(), mock(CellConfigurationService.class), new TabCompletionConfig());
		completer.setTargetContext(DalvikTargetContext.INSTANCE);
		String source = "    code: {\n        invoke-sta";
		assertTrue(completer.resolve(source, source.length()).contains(new AssemblerCompletion.Opcode("invoke-static")));
		assertTrue(completer.resolve(source, -1).isEmpty());
	}

	@Test
	void emptyLineResolvesToNoCompletions() {
		AssemblerCompletionEngine engine = newEngine(mock(Workspace.class), DalvikTargetContext.INSTANCE);
		String source = "    code: {\n        ";
		assertTrue(engine.resolve(source, source.length(), List.of(), null).complete().isEmpty());
	}

	@Test
	void outOfRangeCaretResolvesToNoCompletions() {
		AssemblerCompletionEngine engine = newEngine(mock(Workspace.class), DalvikTargetContext.INSTANCE);
		String source = "    code: {\n        invoke-sta";
		assertTrue(engine.resolve(source, -1, List.of(), null).complete().isEmpty());
		assertTrue(engine.resolve(source, source.length() + 1, List.of(), null).complete().isEmpty());
	}

	@Test
	void dalvikCompletionUsesDalvikInstructions() {
		var opcodes = AssemblerCompletionEngine.createOpcodeNames(DalvikTargetContext.INSTANCE);

		assertTrue(opcodes.contains("invoke-static"));
		assertTrue(opcodes.contains("sget-object"));
		assertTrue(AssemblerCompletionEngine.opcodeCompletions(opcodes, "invoke-sta").contains("invoke-static"));
		assertFalse(opcodes.contains("getstatic"));
	}

	@Test
	void jvmCompletionKeepsJvmOpcodes() {
		var opcodes = AssemblerCompletionEngine.createOpcodeNames(JvmTargetContext.INSTANCE);

		assertTrue(opcodes.contains("getstatic"));
		assertFalse(opcodes.contains("invoke-static"));

		// Short-form variants such as 'iload_0' are not offered, but the general form is.
		assertTrue(opcodes.contains("iload"));
		assertFalse(opcodes.contains("iload_0"));
	}

	/**
	 * @param internalNames
	 * 		Internal names of classes to add to the workspace.
	 *
	 * @return Workspace containing the given classes.
	 */
	@Nonnull
	private static Workspace workspaceWithClasses(String... internalNames) {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		for (String internalName : internalNames) {
			ClassWriter writer = new ClassWriter(0);
			writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
			writer.visitEnd();
			bundle.initialPut(new JvmClassInfoBuilder(writer.toByteArray()).build());
		}
		return new BasicWorkspace(new WorkspaceResourceBuilder().withJvmClassBundle(bundle).build());
	}

	/**
	 * @return Workspace containing a base class and a child class that overrides a method.
	 */
	@Nonnull
	private static Workspace workspaceWithHierarchy() {
		ClassWriter base = new ClassWriter(0);
		base.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/Base", null, "java/lang/Object", null);
		addInstanceMethod(base, "call");
		base.visitEnd();

		ClassWriter child = new ClassWriter(0);
		child.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/Child", null, "sample/Base", null);
		addInstanceMethod(child, "call");
		addInstanceMethod(child, "other");
		child.visitEnd();

		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		bundle.initialPut(new JvmClassInfoBuilder(base.toByteArray()).build());
		bundle.initialPut(new JvmClassInfoBuilder(child.toByteArray()).build());
		return new BasicWorkspace(new WorkspaceResourceBuilder().withJvmClassBundle(bundle).build());
	}

	/**
	 * @return Workspace containing a class in package a and a class in package b, with different visibility modifiers.
	 */
	@Nonnull
	private static Workspace visibilityWorkspace() {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		bundle.initialPut(new JvmClassInfoBuilder(classBytes("a/Caller", Opcodes.ACC_PUBLIC)).build());
		bundle.initialPut(new JvmClassInfoBuilder(classBytes("a/Helper", 0)).build());
		bundle.initialPut(new JvmClassInfoBuilder(classBytes("b/Shown", Opcodes.ACC_PUBLIC)).build());
		bundle.initialPut(new JvmClassInfoBuilder(classBytes("b/Hidden", 0)).build());
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "b/Other", null, "java/lang/Object", null);
		addMethod(cw, Opcodes.ACC_PUBLIC, "run");
		addMethod(cw, Opcodes.ACC_PRIVATE, "secret");
		cw.visitEnd();
		bundle.initialPut(new JvmClassInfoBuilder(cw.toByteArray()).build());
		return new BasicWorkspace(new WorkspaceResourceBuilder().withJvmClassBundle(bundle).build());
	}

	private static void addInstanceMethod(@Nonnull ClassWriter writer, @Nonnull String name) {
		MethodVisitor mw = writer.visitMethod(Opcodes.ACC_PUBLIC, name, "()V", null, null);
		mw.visitCode();
		mw.visitInsn(Opcodes.RETURN);
		mw.visitMaxs(0, 0);
		mw.visitEnd();
	}

	private static byte[] classBytes(@Nonnull String internalName, int access) {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V17, access, internalName, null, "java/lang/Object", null);
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static void addMethod(@Nonnull ClassWriter writer, int access, String name) {
		MethodVisitor mw = writer.visitMethod(access, name, "()V", null, null);
		mw.visitCode();
		mw.visitInsn(Opcodes.RETURN);
		mw.visitMaxs(0, 0);
		mw.visitEnd();
	}

	@Nonnull
	private static AssemblerCompletionEngine newEngine(@Nonnull Workspace workspace, @Nonnull TargetContext targetContext) {
		return new AssemblerCompletionEngine(testTypeIndexService(), workspace, mock(InheritanceGraph.class), targetContext);
	}

	@Nonnull
	private static TypeIndexService testTypeIndexService() {
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		return new TypeIndexService(workspaceManager);
	}

	@Nonnull
	private static List<AssemblerCompletion> completionAt(@Nonnull AssemblerCompletionEngine engine, @Nonnull String source,
	                                                      @Nonnull List<ASTElement> ast, int caret) {
		return engine.resolve(source, caret, ast, null).complete();
	}

	private static boolean hasTypeCompletion(@Nonnull List<AssemblerCompletion> completions, @Nonnull String text) {
		return completions.stream().anyMatch(completion ->
				completion instanceof AssemblerCompletion.Type type && type.text().equals(text));
	}

	private static boolean hasLabelCompletion(@Nonnull List<AssemblerCompletion> completions, @Nonnull String text) {
		return hasOperandCompletion(completions, text, AssemblerCompletion.OperandKind.LABEL);
	}

	private static boolean hasMemberCompletion(@Nonnull List<AssemblerCompletion> completions, @Nonnull String text) {
		return completions.stream().anyMatch(completion ->
				completion instanceof AssemblerCompletion.Member member && member.text().equals(text));
	}

	private static boolean hasOperandCompletion(@Nonnull List<AssemblerCompletion> completions, @Nonnull String text,
	                                            @Nonnull AssemblerCompletion.OperandKind kind) {
		return completions.stream().anyMatch(completion ->
				completion instanceof AssemblerCompletion.Operand operand
						&& operand.kind() == kind && operand.text().equals(text));
	}

	@Nonnull
	private List<ASTElement> parseDalvikElements(@Nonnull Workspace workspace, @Nonnull String source) {
		AndroidAssemblerPipeline pipeline = recaf.get(AssemblerPipelineManager.class).newAndroidAssemblerPipeline(workspace);
		return pipeline.fullParse(pipeline.tokenize(source, "<reference-test>").requireValue()).requireValue();
	}

	@Nonnull
	private ASTMethod parseJvmMethod(@Nonnull String source) {
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		var pipeline = recaf.get(AssemblerPipelineManager.class).newJvmAssemblerPipeline(workspace);
		List<ASTElement> parsed = pipeline.fullParse(pipeline.tokenize(source, "<jvm-operand-test>").requireValue()).requireValue();
		return parsed.stream()
				.filter(ASTMethod.class::isInstance)
				.map(ASTMethod.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected a method AST"));
	}

	@Nonnull
	private ASTMethod parseDalvikMethod(@Nonnull String source) {
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		AndroidAssemblerPipeline pipeline = recaf.get(AssemblerPipelineManager.class).newAndroidAssemblerPipeline(workspace);
		List<ASTElement> parsed = pipeline.fullParse(pipeline.tokenize(source, "<operand-test>").requireValue()).requireValue();
		return parsed.stream()
				.filter(ASTMethod.class::isInstance)
				.map(ASTMethod.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected a method AST"));
	}

	private static boolean switchLabelAt(String source, ASTMethod method, String text, TargetContext target) {
		int textStart = source.indexOf(text);
		if (textStart < 0)
			throw new AssertionError("Missing source text: " + text);

		var site = AssemblerOperandLocator.locate(source, textStart + text.length(), List.of(method), target);
		return site != null && AssemblerStructuredOperands.isSwitchLabelPosition(site);
	}

	private Workspace workspaceWithOwner() {
		ClassWriter writer = new ClassWriter(0);
		writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/Owner", null, "java/lang/Object", null);
		writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "I", null, null).visitEnd();

		MethodVisitor call = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V", null, null);
		call.visitCode();
		call.visitInsn(Opcodes.RETURN);
		call.visitMaxs(0, 0);
		call.visitEnd();

		MethodVisitor virtual = writer.visitMethod(Opcodes.ACC_PUBLIC, "virtual", "()V", null, null);
		virtual.visitCode();
		virtual.visitInsn(Opcodes.RETURN);
		virtual.visitMaxs(0, 0);
		virtual.visitEnd();
		writer.visitEnd();

		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		bundle.initialPut(new JvmClassInfoBuilder(writer.toByteArray()).build());
		return new BasicWorkspace(new WorkspaceResourceBuilder().withJvmClassBundle(bundle).build());
	}
}
