package software.coley.recaf.util.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTException;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utility methods for working with JASM.
 *
 * @author Matt Coley
 */
public final class JasmUtils {
	public static final Set<String> FLOW_CONTROL_INSNS = Set.of(
			"goto", "jsr",
			"ifnull", "ifnonnull", "ifeq", "ifne", "ifle", "ifge", "iflt", "ifgt",
			"if_acmpeq", "if_acmpne", "if_icmpeq", "if_icmpge", "if_icmpgt", "if_icmple", "if_icmplt", "if_icmpne"
	);
	public static final Set<String> SWITCH_INSNS = Set.of("tableswitch", "lookupswitch");
	public static final Set<String> TYPE_REFERENCE_INSNS = Set.of("new", "anewarray", "checkcast", "instanceof");

	private JasmUtils() {}

	/**
	 * @param instructionName
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a flow control instruction.
	 */
	public static boolean isFlowControlInstruction(@Nullable String instructionName) {
		return instructionName != null && FLOW_CONTROL_INSNS.contains(instructionName);
	}

	/**
	 * @param instructionName
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a switch instruction.
	 */
	public static boolean isSwitchInstruction(@Nullable String instructionName) {
		return instructionName != null && SWITCH_INSNS.contains(instructionName);
	}

	/**
	 * @param instructionName
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction references a variable in its first operand.
	 */
	public static boolean isVariableReferenceInstruction(@Nullable String instructionName) {
		if (instructionName == null)
			return false;

		boolean isLoad = instructionName.endsWith("load");
		boolean isStore = instructionName.endsWith("store");
		if ((isLoad || isStore) && instructionName.length() > 1 && instructionName.charAt(1) != 'a')
			return true;
		return "ret".equals(instructionName) || "iinc".equals(instructionName);
	}

	/**
	 * @param instructionName
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction references a type in its first operand.
	 */
	public static boolean isTypeReferenceInstruction(@Nullable String instructionName) {
		return instructionName != null && TYPE_REFERENCE_INSNS.contains(instructionName);
	}

	/**
	 * @param astElements
	 * 		AST elements to search through.
	 * @param position
	 * 		Position to check for.
	 * @param line
	 * 		Line to check for.
	 *
	 * @return Instruction at the given position and line, or {@code null} if not found.
	 */
	@Nullable
	public static ASTInstruction findInstruction(@Nullable List<ASTElement> astElements, int position, int line) {
		if (astElements == null)
			return null;

		ASTInstruction[] selected = new ASTInstruction[1];
		for (ASTElement element : astElements) {
			// Some weird edge cases where JASM can have null entries, so sanity check,
			if (element == null)
				continue;

			// Skip elements not within the given range.
			if (!element.range().within(position))
				continue;

			// Walk down the tree to find a deeper match.
			element.walk(ast -> {
				if (ast instanceof ASTInstruction instruction) {
					Location location = ast.location();
					if (location != null && location.line() == line) {
						selected[0] = instruction;
					} else {
						String identifier = instruction.identifier().content();
						if (isSwitchInstruction(identifier) && ast.range().within(position)) {
							selected[0] = instruction;
						}
					}
				}
				return selected[0] == null;
			});

			if (selected[0] != null)
				break;
		}

		return selected[0];
	}

	/**
	 * @param method
	 * 		Method to search through.
	 * @param target
	 * 		Label name to find.
	 *
	 * @return Label declaration with the given name, or {@code null} if not found.
	 */
	@Nullable
	public static ASTLabel getLabelDeclaration(@Nonnull ASTMethod method, @Nonnull String target) {
		for (ASTInstruction instruction : method.code().instructions())
			if (instruction instanceof ASTLabel label && Objects.equals(label.identifier().content(), target))
				return label;
		return null;
	}

	/**
	 * @param astElements
	 * 		AST elements to search through.
	 * @param methodName
	 * 		Method name to filter by, or {@code null} to ignore.
	 * @param methodDescriptor
	 * 		Method descriptor to filter by, or {@code null} to ignore.
	 *
	 * @return Map of variable names to their usages in the AST.
	 */
	@Nonnull
	public static Map<String, JasmAstUsages> collectVariableUsages(@Nullable List<ASTElement> astElements,
	                                                               @Nullable String methodName,
	                                                               @Nullable String methodDescriptor) {
		JasmAstUsages emptyUsage = JasmAstUsages.EMPTY_USAGE;
		Map<String, JasmAstUsages> variableUsages = new HashMap<>();
		visitMethods(astElements, methodName, methodDescriptor, astMethod -> {
			for (ASTIdentifier parameter : astMethod.parameters()) {
				String literalName = parameter.literal();
				variableUsages.putIfAbsent(literalName, emptyUsage.asParameter());
			}
			visitVariableMatches(astMethod, match -> {
				JasmAstUsages existing = variableUsages.getOrDefault(match.variableName(), emptyUsage);
				if (match.kind() == VariableReferenceKind.READ) {
					variableUsages.put(match.variableName(), existing.withNewRead(match.instruction()));
				} else {
					variableUsages.put(match.variableName(), existing.withNewWrite(match.instruction()));
				}
			});
		});
		return variableUsages;
	}

	/**
	 * @param method
	 * 		Method to search through.
	 * @param variableName
	 * 		Variable name to find references for.
	 *
	 * @return List of references to the variable, sorted by their position in the source.
	 */
	@Nonnull
	public static List<VariableReference> collectVariableReferences(@Nonnull ASTMethod method, @Nonnull String variableName) {
		List<VariableReference> references = new ArrayList<>();
		visitVariableMatches(method, match -> {
			if (match.argument() instanceof ASTIdentifier identifier && variableName.equals(identifier.literal()))
				references.add(new VariableReference(identifier, match.kind()));
		});
		references.sort(Comparator.comparing(reference -> reference.element().location()));
		return references;
	}

	/**
	 * @param astElements
	 * 		AST elements to search through.
	 * @param methodName
	 * 		Method name to filter by, or {@code null} to ignore.
	 * @param methodDescriptor
	 * 		Method descriptor to filter by, or {@code null} to ignore.
	 *
	 * @return Map of label names to their usages in the AST.
	 */
	@Nonnull
	public static Map<String, JasmAstUsages> collectLabelUsages(@Nullable List<ASTElement> astElements,
	                                                            @Nullable String methodName,
	                                                            @Nullable String methodDescriptor) {
		Map<String, JasmAstUsages> labelUsages = new HashMap<>();
		visitMethods(astElements, methodName, methodDescriptor, astMethod -> {
			visitLabelDeclarations(astMethod, label -> addRead(labelUsages, label.identifier().content(), label));
			visitInstructionLabelMatches(astMethod, match -> addWrite(labelUsages, match.labelName(), match.element()));
		});
		return labelUsages;
	}

	/**
	 * @param method
	 * 		Method to search through.
	 * @param labelName
	 * 		Label name to find references for.
	 *
	 * @return List of references to the label, sorted by their position in the source.
	 */
	@Nonnull
	public static List<LabelReference> collectLabelReferences(@Nonnull ASTMethod method, @Nonnull String labelName) {
		List<LabelReference> references = new ArrayList<>();
		visitExceptionLabelMatches(method, match -> {
			if (labelName.equals(match.labelName()))
				references.add(new LabelReference(match.element(), match.kind(), match.context()));
		});
		visitInstructionLabelMatches(method, match -> {
			if (labelName.equals(match.labelName()))
				references.add(new LabelReference(match.element(), match.kind(), match.context()));
		});
		references.sort(Comparator.comparing(reference -> reference.element().location()));
		return references;
	}

	private static void visitVariableMatches(@Nonnull ASTMethod method, @Nonnull Consumer<VariableMatch> consumer) {
		ASTCode code = method.code();
		if (code == null)
			return;

		for (ASTInstruction instruction : code.instructions()) {
			VariableReferenceKind kind = getVariableReferenceKind(instruction);
			if (kind == null)
				continue;

			List<ASTElement> arguments = instruction.arguments();
			if (arguments.isEmpty())
				continue;

			ASTElement argument = arguments.getFirst();
			String variableName = argument instanceof ASTIdentifier identifier ? identifier.literal() : argument.content();
			if (variableName != null)
				consumer.accept(new VariableMatch(variableName, instruction, argument, kind));
		}
	}

	private static void visitLabelDeclarations(@Nonnull ASTMethod method, @Nonnull Consumer<ASTLabel> consumer) {
		ASTCode code = method.code();
		if (code == null)
			return;

		for (ASTInstruction instruction : code.instructions()) {
			if (instruction instanceof ASTLabel label)
				consumer.accept(label);
		}
	}

	private static void visitExceptionLabelMatches(@Nonnull ASTMethod method, @Nonnull Consumer<LabelMatch> consumer) {
		for (ASTException exception : method.exceptions()) {
			consumer.accept(new LabelMatch(exception.start().literal(), exception.start(), LabelReferenceKind.TRY_START, null));
			consumer.accept(new LabelMatch(exception.end().literal(), exception.end(), LabelReferenceKind.TRY_END, null));
			consumer.accept(new LabelMatch(exception.handler().literal(), exception.handler(), LabelReferenceKind.HANDLER, null));
		}
	}

	private static void visitInstructionLabelMatches(@Nonnull ASTMethod method, @Nonnull Consumer<LabelMatch> consumer) {
		ASTCode code = method.code();
		if (code == null)
			return;

		for (ASTInstruction instruction : code.instructions()) {
			if (instruction instanceof ASTLabel)
				continue;

			String instructionName = instruction.identifier().content();
			List<ASTElement> arguments = instruction.arguments();
			if (instructionName == null || arguments.isEmpty())
				continue;

			if (isFlowControlInstruction(instructionName)) {
				ASTElement target = arguments.getFirst();
				String labelName = target.content();
				if (labelName != null)
					consumer.accept(new LabelMatch(labelName, target, LabelReferenceKind.FLOW, instructionName));
			} else if ("tableswitch".equals(instructionName)) {
				visitTableSwitchLabelMatches(instruction, consumer);
			} else if ("lookupswitch".equals(instructionName)) {
				visitLookupSwitchLabelMatches(instruction, consumer);
			}
		}
	}

	@Nullable
	private static VariableReferenceKind getVariableReferenceKind(@Nonnull ASTInstruction instruction) {
		String instructionName = instruction.identifier().content();
		if (!isVariableReferenceInstruction(instructionName))
			return null;

		if ("ret".equals(instructionName))
			return VariableReferenceKind.READ;
		if ("iinc".equals(instructionName))
			return VariableReferenceKind.INCREMENT;
		return instructionName.endsWith("load") ? VariableReferenceKind.READ : VariableReferenceKind.WRITE;
	}

	private static void visitTableSwitchLabelMatches(@Nonnull ASTInstruction instruction, @Nonnull Consumer<LabelMatch> consumer) {
		ASTObject switchObject = instruction.argumentObject(0);
		if (switchObject == null)
			return;

		ASTElement defaultCase = switchObject.value("default");
		if (defaultCase instanceof ASTIdentifier identifier)
			consumer.accept(new LabelMatch(identifier.literal(), identifier, LabelReferenceKind.SWITCH_DEFAULT, null));

		ASTArray cases = switchObject.value("cases");
		if (cases == null)
			return;

		int min = parseIntOrDefault(switchObject.value("min"), 0);
		List<ASTElement> values = cases.values();
		for (int i = 0; i < values.size(); i++) {
			ASTElement value = values.get(i);
			if (value instanceof ASTIdentifier identifier)
				consumer.accept(new LabelMatch(identifier.literal(), identifier, LabelReferenceKind.SWITCH_CASE, String.valueOf(min + i)));
		}
	}

	private static void visitLookupSwitchLabelMatches(@Nonnull ASTInstruction instruction, @Nonnull Consumer<LabelMatch> consumer) {
		ASTObject switchObject = instruction.argumentObject(0);
		if (switchObject == null)
			return;

		ASTElement defaultCase = switchObject.value("default");
		if (defaultCase instanceof ASTIdentifier identifier)
			consumer.accept(new LabelMatch(identifier.literal(), identifier, LabelReferenceKind.SWITCH_DEFAULT, null));

		for (int i = 0; i < switchObject.values().size(); i++) {
			ASTIdentifier key = switchObject.values().key(i);
			if ("default".equals(key.content()))
				continue;

			ASTElement value = switchObject.values().get(i);
			if (value instanceof ASTIdentifier identifier)
				consumer.accept(new LabelMatch(identifier.literal(), identifier, LabelReferenceKind.SWITCH_CASE, key.content()));
		}
	}

	private static void visitMethods(@Nullable List<ASTElement> astElements,
	                                 @Nullable String methodName,
	                                 @Nullable String methodDescriptor,
	                                 @Nonnull Consumer<ASTMethod> consumer) {
		if (astElements == null)
			return;

		for (ASTElement astElement : astElements) {
			if (astElement instanceof ASTMethod astMethod) {
				if (matchesMethod(astMethod, methodName, methodDescriptor))
					consumer.accept(astMethod);
			} else if (astElement instanceof ASTClass astClass) {
				for (ASTElement child : astClass.children()) {
					if (child instanceof ASTMethod astMethod && matchesMethod(astMethod, methodName, methodDescriptor))
						consumer.accept(astMethod);
				}
			}
		}
	}

	private static boolean matchesMethod(@Nonnull ASTMethod astMethod, @Nullable String methodName, @Nullable String methodDescriptor) {
		if (methodName != null && !Objects.equals(methodName, astMethod.getName().literal()))
			return false;
		return methodDescriptor == null || Objects.equals(methodDescriptor, astMethod.getDescriptor().literal());
	}

	private static void addRead(@Nonnull Map<String, JasmAstUsages> usages, @Nullable String name, @Nonnull ASTElement element) {
		if (name == null)
			return;
		JasmAstUsages existing = usages.getOrDefault(name, JasmAstUsages.EMPTY_USAGE);
		usages.put(name, existing.withNewRead(element));
	}

	private static void addWrite(@Nonnull Map<String, JasmAstUsages> usages, @Nullable String name, @Nonnull ASTElement element) {
		if (name == null)
			return;
		JasmAstUsages existing = usages.getOrDefault(name, JasmAstUsages.EMPTY_USAGE);
		usages.put(name, existing.withNewWrite(element));
	}

	private static int parseIntOrDefault(@Nullable ASTElement element, int defaultValue) {
		if (element == null || element.content() == null)
			return defaultValue;
		try {
			return Integer.parseInt(element.content());
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	/**
	 * Models a reference to a variable in the AST.
	 *
	 * @param element
	 * 		Referenced element.
	 * @param kind
	 * 		Kind of reference.
	 */
	public record VariableReference(@Nonnull ASTElement element, @Nonnull VariableReferenceKind kind) {}

	/**
	 * @param element
	 * 		Referenced element.
	 * @param kind
	 * 		Kind of reference.
	 * @param context
	 * 		Additional context for the reference, for example the case value for a switch case,
	 * 		or the instruction name for a flow control reference.
	 */
	public record LabelReference(@Nonnull ASTElement element, @Nonnull LabelReferenceKind kind,
	                             @Nullable String context) {}

	/**
	 * @param variableName
	 * 		Name of variable.
	 * @param instruction
	 * 		Instruction referencing the variable.
	 * @param argument
	 * 		Argument referencing the variable.
	 * @param kind
	 * 		Kind of reference.
	 */
	private record VariableMatch(@Nonnull String variableName, @Nonnull ASTInstruction instruction,
	                             @Nonnull ASTElement argument, @Nonnull VariableReferenceKind kind) {}

	/**
	 * @param labelName
	 * 		Name of label.
	 * @param element
	 * 		Element referencing the label.
	 * @param kind
	 * 		Kind of reference.
	 * @param context
	 * 		Additional context for the reference, for example the case value for a switch case,
	 */
	private record LabelMatch(@Nonnull String labelName, @Nonnull ASTElement element,
	                          @Nonnull LabelReferenceKind kind, @Nullable String context) {}

	/**
	 * Kinds of variable references.
	 */
	public enum VariableReferenceKind {
		READ,
		WRITE,
		INCREMENT
	}

	/**
	 * Kinds of label references.
	 */
	public enum LabelReferenceKind {
		FLOW,
		SWITCH_DEFAULT,
		SWITCH_CASE,
		TRY_START,
		TRY_END,
		HANDLER
	}
}
