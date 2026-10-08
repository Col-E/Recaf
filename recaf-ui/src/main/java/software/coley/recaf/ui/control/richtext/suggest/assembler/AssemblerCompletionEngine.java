package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.instructions.DalvikOperands;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.util.JvmOpcodes;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.target.TargetContext;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.ui.control.richtext.suggest.assembler.AssemblerCompletionContext.OperandToken;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndexService;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Resolves the completion context at a caret position.
 *
 * @author Matt Coley
 */
final class AssemblerCompletionEngine {
	private final TargetContext targetContext;
	private final List<String> opcodes;
	private final TypeIndexService typeIndexService;
	private final Workspace workspace;
	private final InheritanceGraph graph;
	private final AssemblerReferenceCompletions references;

	/**
	 * @param typeIndexService
	 * 		Type index service to use for type lookups.
	 * @param workspace
	 * 		Workspace to pull type and member information from.
	 * @param inheritanceGraph
	 * 		Inheritance graph for member lookups.
	 * @param targetContext
	 * 		Instruction language used by the currently edited class.
	 */
	AssemblerCompletionEngine(@Nonnull TypeIndexService typeIndexService,
	                          @Nonnull Workspace workspace,
	                          @Nonnull InheritanceGraph inheritanceGraph,
	                          @Nonnull TargetContext targetContext) {
		this.typeIndexService = typeIndexService;
		this.workspace = workspace;
		this.graph = inheritanceGraph;
		this.targetContext = targetContext;
		this.opcodes = createOpcodeNames(targetContext);
		this.references = new AssemblerReferenceCompletions(targetContext);
	}

	/**
	 * @return Instruction language this engine resolves completions for.
	 */
	@Nonnull
	TargetContext targetContext() {
		return targetContext;
	}

	/**
	 * @param source
	 * 		Full source text.
	 * @param caret
	 * 		Caret index into the source text.
	 * @param ast
	 * 		AST of the source text, or {@code null} if unavailable.
	 * @param editedClassName
	 * 		Internal class name of the class being edited,
	 * 		or {@code null} if the source is not attached to a class.
	 *
	 * @return Context describing what completions to offer at the caret.
	 */
	@Nonnull
	AssemblerCompletionContext resolve(@Nonnull String source, int caret, @Nullable List<ASTElement> ast,
	                                   @Nullable String editedClassName) {
		if (caret < 0 || caret > source.length())
			return new AssemblerCompletionContext.Empty();

		// Prefer the AST when it has a code block at the caret. The source scan covers code that has not parsed yet.
		boolean inCode = false;
		if (ast != null && !ast.isEmpty()) {
			ASTElement element = ast.getFirst().pick(caret);
			while (element != null) {
				if (element instanceof ASTCode code) {
					inCode = code.range().within(caret);
					break;
				}
				element = element.parent();
			}
		}
		if (!inCode)
			inCode = AssemblerOperandLocator.isCaretInCodeBlock(source, caret);
		if (!inCode)
			return new AssemblerCompletionContext.Empty();

		// The line content up to the caret is what we complete. Text after the caret is not part of the token.
		int lineStart = source.lastIndexOf('\n', caret - 1) + 1;
		String linePrefix = source.substring(lineStart, caret);
		String partialText = linePrefix.trim();

		// Skip if there is nothing to complete.
		if (partialText.isEmpty())
			return new AssemblerCompletionContext.Empty();

		// Resolved instructions are routed by their active operand role and schema. Incomplete mnemonics fall
		// through to opcode completion because they do not yet have an instruction definition.
		AssemblerOperandLocator.Site operandSite = AssemblerOperandLocator.locate(source, caret, ast, targetContext);
		if (operandSite != null) {
			AssemblerCompletionScope scope = new AssemblerCompletionScope(workspace,
					typeIndexService.getIndex(workspace), graph, editedClassName);
			return createOperandContext(scope, operandSite);
		}
		if (RegexUtil.matches("^\\s*[\\w/-]+\\s*$", linePrefix))
			return new AssemblerCompletionContext.OpcodeToken(partialText, opcodes);

		return new AssemblerCompletionContext.Empty();
	}

	/**
	 * Create a context for an operand completion.
	 *
	 * @param scope
	 * 		Completion scope to use for type and member lookups.
	 * @param site
	 * 		Operand site to create a context for.
	 *
	 * @return Context for the operand.
	 */
	@Nonnull
	private AssemblerCompletionContext createOperandContext(@Nonnull AssemblerCompletionScope scope,
	                                                        @Nonnull AssemblerOperandLocator.Site site) {
		String prefix = site.tokenPrefix();

		// Handle register operands for Dalvik,
		if (targetContext == DalvikTargetContext.INSTANCE
				&& AssemblerOperandLocator.isDalvikRegisterOperand(site.definition(), site.argumentIndex())) {
			List<String> names = site.method() == null ? List.of() :
					AssemblerOperandCandidates.dalvikRegisterCandidates(site.method(), targetContext, prefix);
			return new OperandToken(prefix, operandCompletions(names, AssemblerCompletion.OperandKind.REGISTER, prefix));
		}

		int operandIndex = site.argumentIndex();
		OperandRole role = site.definition().role(operandIndex);

		// Handle label operands for both.
		if (role != null && role.kind() == OperandRole.RoleKind.LABEL)
			return labelContext(site, prefix);

		// Handle switch payload label operands for both.
		if (role != null && role.kind() == OperandRole.RoleKind.SWITCH_PAYLOAD && AssemblerStructuredOperands.isSwitchLabelPosition(site))
			return labelContext(site, prefix);

		// Handle type operands for both.
		if (role != null && role.kind() == OperandRole.RoleKind.TYPE)
			return new OperandToken(prefix, typeCompletions(scope, site, prefix));

		// Handle local variable operands for JVM.
		if (role != null && role.kind() == OperandRole.RoleKind.VARIABLE && targetContext == JvmTargetContext.INSTANCE) {
			List<String> names = AssemblerOperandCandidates.jvmLocalCandidates(site.method(), prefix);
			return new OperandToken(prefix, operandCompletions(names, AssemblerCompletion.OperandKind.LOCAL, prefix));
		}

		// Handle member reference operands for both.
		int memberIndex = site.definition().memberReferenceOperandIndex();
		if (memberIndex >= 0 && (operandIndex == memberIndex || operandIndex == memberIndex + 1)) {
			boolean isMethod = site.definition().hasTrait(InstructionTrait.METHOD_REFERENCE);
			boolean isField = site.definition().hasTrait(InstructionTrait.FIELD_REFERENCE);
			if (isMethod || isField)
				return toOperandContext(references.memberReference(scope, site, memberIndex, isMethod, prefix));
		}

		// Handle structured operands (method handles and such) for both.
		if (targetContext == JvmTargetContext.INSTANCE || targetContext == DalvikTargetContext.INSTANCE)
			return structuredContext(scope, site, prefix);

		// Nothing to offer.
		return new OperandToken(prefix, List.of());
	}

	/**
	 * @param site
	 * 		Operand site to create a context for.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Context for the operand.
	 */
	@Nonnull
	private OperandToken labelContext(@Nonnull AssemblerOperandLocator.Site site, @Nonnull String prefix) {
		List<String> names = AssemblerOperandCandidates.labelCandidates(site.method(), targetContext, prefix);
		return new OperandToken(prefix, operandCompletions(names, AssemblerCompletion.OperandKind.LABEL, prefix));
	}

	/**
	 * Handle structured operands (method handles and such) for both.
	 *
	 * @param scope
	 * 		Workspace state for the completion request.
	 * @param site
	 * 		Operand site to create a context for.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Context for the operand.
	 */
	@Nonnull
	private OperandToken structuredContext(@Nonnull AssemblerCompletionScope scope,
	                                       @Nonnull AssemblerOperandLocator.Site site, @Nonnull String prefix) {
		AssemblerStructuredOperands.StructuredOperand position = AssemblerStructuredOperands.structuredOperand(site, targetContext);
		return switch (position.kind()) {
			case CONSTANT_IDENTIFIER -> {
				List<AssemblerCompletion> candidates = new ArrayList<>(operandCompletions(
						AssemblerOperandCandidates.handleShortcutCandidates(prefix),
						AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT, prefix));
				if (targetContext == DalvikTargetContext.INSTANCE)
					candidates.addAll(AssemblerTypeCompletions.complete(scope, prefix, false, false));
				else if (prefix.isEmpty() || prefix.startsWith("L") || prefix.startsWith("["))
					candidates.addAll(AssemblerTypeCompletions.complete(scope, prefix, true, false));
				yield new OperandToken(prefix, List.copyOf(candidates));
			}
			case HANDLE_SHORTCUT -> new OperandToken(prefix, operandCompletions(
					AssemblerOperandCandidates.handleShortcutCandidates(prefix),
					AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT, prefix));
			case HANDLE_KIND -> new OperandToken(prefix, operandCompletions(
					AssemblerOperandCandidates.handleKindCandidates(prefix),
					AssemblerCompletion.OperandKind.STRUCTURED_CONSTANT, prefix));
			case HANDLE_MEMBER ->
					toOperandContext(references.handleReference(scope, site, prefix, null, position.handleKind(), false));
			case HANDLE_DESCRIPTOR ->
					toOperandContext(references.handleReference(scope, site, prefix, position.memberReference(), position.handleKind(), true));
			case DYNAMIC_TYPE ->
					new OperandToken(prefix, AssemblerTypeCompletions.complete(scope, prefix, true, false));
			case DECLARATION_OWNER ->
					new OperandToken(prefix, AssemblerTypeCompletions.complete(scope, prefix, false, false));
			case DECLARATION_MEMBER ->
					toOperandContext(references.declarationField(scope, site, prefix, position.memberReference(), false));
			case DECLARATION_DESCRIPTOR ->
					toOperandContext(references.declarationField(scope, site, prefix, position.memberReference(), true));
			case NONE -> new OperandToken(prefix, List.of());
		};
	}

	/**
	 * @param scope
	 * 		Workspace state for the completion request.
	 * @param site
	 * 		Operand site to create a context for.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Type completions for the operand.
	 */
	@Nonnull
	private List<AssemblerCompletion> typeCompletions(@Nonnull AssemblerCompletionScope scope,
	                                                  @Nonnull AssemblerOperandLocator.Site site,
	                                                  @Nonnull String prefix) {
		boolean dalvik = targetContext == DalvikTargetContext.INSTANCE;
		boolean requireArray = dalvik && site.argumentIndex() >= 0
				&& site.argumentIndex() < site.definition().operandCount()
				&& site.definition().operand(site.argumentIndex()) == DalvikOperands.FILLED_NEW_ARRAY_TYPE.getOperand();
		return AssemblerTypeCompletions.complete(scope, prefix, dalvik && prefix.isEmpty(), requireArray);
	}

	/**
	 * @param result
	 * 		Reference completion result to convert to an operand context.
	 *
	 * @return Operand context for the reference completion result.
	 */
	@Nonnull
	private static OperandToken toOperandContext(@Nonnull AssemblerReferenceCompletions.Result result) {
		return new OperandToken(result.matchedText(), result.candidates());
	}

	/**
	 * @param names
	 * 		Candidate names, in the order they should be offered.
	 * @param kind
	 * 		Operand kind to use for the completions.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Completions that start with the prefix and are not equal to it, in iteration order.
	 */
	@Nonnull
	private static List<AssemblerCompletion> operandCompletions(@Nonnull List<String> names,
	                                                            @Nonnull AssemblerCompletion.OperandKind kind,
	                                                            @Nonnull String prefix) {
		return names.stream()
				.filter(name -> name.startsWith(prefix) && !name.equals(prefix))
				.map(name -> new AssemblerCompletion.Operand(name, kind))
				.map(AssemblerCompletion.class::cast)
				.toList();
	}

	/**
	 * @param targetContext
	 * 		Target context to pull opcode names from.
	 *
	 * @return Opcode names for the given target context.
	 */
	@Nonnull
	public static List<String> createOpcodeNames(@Nonnull TargetContext targetContext) {
		SortedSet<String> names = new TreeSet<>();
		if (targetContext == JvmTargetContext.INSTANCE) {
			names.addAll(JvmOpcodes.getFilteredOpcodes().keySet());
			names.removeIf(opcode -> (opcode.contains("store") || opcode.contains("load")) && opcode.indexOf('_') > -1);
		} else {
			for (String keyword : targetContext.keywordNames())
				if (targetContext.instructions().get(keyword) != null)
					names.add(keyword);
		}
		return List.copyOf(names);
	}

	/**
	 * @param opcodes
	 * 		All possible opcode names.
	 * @param partialInput
	 * 		Partial input text to match against.
	 *
	 * @return Opcodes that start with the partial input, but are not equal to it.
	 */
	@Nonnull
	public static List<String> opcodeCompletions(@Nonnull List<String> opcodes, @Nonnull String partialInput) {
		String partial = partialInput.trim();
		return opcodes.stream()
				.filter(opcode -> opcode.startsWith(partial) && !opcode.equals(partial))
				.toList();
	}
}
