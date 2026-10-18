package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.instructions.DalvikOperands;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Locates the instruction operand under the assembler caret.
 *
 * @author Matt Coley
 */
final class AssemblerOperandLocator {
	private AssemblerOperandLocator() {}

	/**
	 * @param source
	 * 		Full editor text.
	 * @param caret
	 * 		Caret offset into {@code source}.
	 * @param ast
	 * 		Parsed elements of the edited class, or {@code null} if no parse is available.
	 * @param targetContext
	 * 		Instruction language of the edited class.
	 *
	 * @return Operand under the caret, or {@code null} if the caret is not inside an instruction with a known mnemonic.
	 */
	@Nullable
	static Site locate(@Nonnull String source, int caret, @Nullable List<ASTElement> ast,
	                   @Nonnull TargetContext targetContext) {
		if (caret < 0 || caret > source.length())
			return null;

		ASTMethod method = findMethod(ast, caret);
		ASTInstruction instruction = findInstruction(method, caret);
		if (instruction != null && instruction.identifier() != null
				&& caret < instruction.identifier().range().end()
				&& contains(instruction.identifier().range(), caret))
			instruction = null;
		int instructionStart;
		int argumentsStart;
		String mnemonic;
		if (instruction != null && instruction.identifier() != null
				&& targetContext.instructions().get(instruction.identifier().literal()) != null) {
			Range mnemonicRange = instruction.identifier().range();
			instructionStart = mnemonicRange.start();
			argumentsStart = mnemonicRange.end() + 1;
			mnemonic = instruction.identifier().literal();
		} else {
			instruction = null;
			instructionStart = findInstructionLineStart(source, caret);
			while (instructionStart < caret && Character.isWhitespace(source.charAt(instructionStart)))
				instructionStart++;
			int mnemonicEnd = instructionStart;
			while (mnemonicEnd < caret && !Character.isWhitespace(source.charAt(mnemonicEnd))
					&& source.charAt(mnemonicEnd) != '{' && source.charAt(mnemonicEnd) != ',')
				mnemonicEnd++;
			if (mnemonicEnd == instructionStart)
				return null;
			mnemonic = source.substring(instructionStart, mnemonicEnd);
			argumentsStart = mnemonicEnd;
		}

		Instruction<?> definition = targetContext.instructions().get(mnemonic);
		if (definition == null)
			return null;
		while (argumentsStart < caret && Character.isWhitespace(source.charAt(argumentsStart)))
			argumentsStart++;
		Cursor cursor = scan(source, argumentsStart, caret);
		int argumentIndex = cursor.argumentIndex();
		ASTElement argument = null;
		ASTElement selected = null;
		if (instruction != null) {
			List<? extends ASTElement> arguments = instruction.arguments();
			for (int i = 0; i < arguments.size(); i++) {
				ASTElement candidate = arguments.get(i);
				if (candidate != null && contains(candidate.range(), caret)) {
					argumentIndex = i;
					argument = candidate;
					selected = candidate.pick(caret);
					break;
				}
			}
			if (argument == null && argumentIndex >= 0 && argumentIndex < arguments.size())
				argument = arguments.get(argumentIndex);
		}

		String linePrefix = source.substring(instructionStart, caret);
		return new Site(source, caret, method, instruction, mnemonic, definition, argumentIndex, argument, selected,
				cursor.nestedIndices(), cursor.containerStarts(), cursor.nestedKey(), cursor.tokenPrefix(), linePrefix);
	}

	@Nullable
	private static ASTMethod findMethod(@Nullable List<ASTElement> ast, int caret) {
		if (ast == null)
			return null;
		ASTMethod selected = null;
		for (ASTElement element : ast)
			selected = findMethod(element, caret, selected);
		return selected;
	}

	@Nullable
	private static ASTMethod findMethod(@Nonnull ASTElement element, int caret, @Nullable ASTMethod selected) {
		if (element instanceof ASTMethod method && contains(method.range(), caret)
				&& (selected == null || method.range().end() - method.range().start()
				< selected.range().end() - selected.range().start()))
			selected = method;
		for (ASTElement child : element.children())
			selected = findMethod(child, caret, selected);
		return selected;
	}

	@Nullable
	private static ASTInstruction findInstruction(@Nullable ASTMethod method, int caret) {
		if (method == null || method.getCode() == null)
			return null;
		ASTInstruction selected = null;
		for (ASTInstruction candidate : method.getCode().getInstructions()) {
			if (candidate instanceof ASTLabel || !contains(candidate.range(), caret))
				continue;
			if (selected == null || candidate.range().end() - candidate.range().start() < selected.range().end() - selected.range().start())
				selected = candidate;
		}
		return selected;
	}

	/**
	 * @param range
	 * 		Source range of an AST element.
	 * @param offset
	 * 		Caret offset.
	 *
	 * @return {@code true} if the range is known and contains the offset.
	 */
	static boolean contains(@Nonnull Range range, int offset) {
		return range != Range.EMPTY && range.within(offset);
	}

	private static int findInstructionLineStart(@Nonnull String source, int caret) {
		int nestedBraces = 0;
		char quote = 0;
		for (int i = caret - 1; i >= 0; i--) {
			char c = source.charAt(i);
			if (quote != 0) {
				if (c == quote && !isEscaped(source, i))
					quote = 0;
				continue;
			}
			if ((c == '\'' || c == '"') && !isEscaped(source, i)) {
				quote = c;
			} else if (c == '}') {
				nestedBraces++;
			} else if (c == '{' && nestedBraces > 0) {
				nestedBraces--;
			} else if (c == '\n' && nestedBraces == 0) {
				return i + 1;
			}
		}
		return 0;
	}

	private static boolean isEscaped(@Nonnull String source, int offset) {
		int slashes = 0;
		for (int i = offset - 1; i >= 0 && source.charAt(i) == '\\'; i--)
			slashes++;
		return (slashes & 1) != 0;
	}

	/**
	 * Source-level fallback for detecting a {@code code: { ... }} block around the caret, used when the AST does not
	 * cover the caret. Comments and quoted strings are skipped so braces inside them do not affect the depth.
	 *
	 * @param source
	 * 		Full editor text.
	 * @param caret
	 * 		Caret offset into {@code source}.
	 *
	 * @return {@code true} if the caret is inside the body of a {@code code:} block.
	 */
	static boolean isCaretInCodeBlock(@Nonnull String source, int caret) {
		int depth = 0;
		int codeDepth = -1;
		int codeBrace = -1;
		char quote = 0;
		boolean escaped = false;
		for (int i = 0; i < Math.min(caret, source.length()); i++) {
			char c = source.charAt(i);
			if (quote != 0) {
				if (escaped) {
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}
			if (c == '\'' || c == '"') {
				quote = c;
				continue;
			}
			if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
				while (i < source.length() && source.charAt(i) != '\n')
					i++;
				continue;
			}
			if (c == '#') {
				while (i < source.length() && source.charAt(i) != '\n')
					i++;
				continue;
			}
			if (codeBrace < 0 && c == 'c' && source.startsWith("code", i)
					&& (i == 0 || !isIdentifierPart(source.charAt(i - 1)))
					&& (i + 4 >= source.length() || !isIdentifierPart(source.charAt(i + 4))))
				codeBrace = codeBlockBrace(source, i + 4);
			if (c == '{') {
				depth++;
				if (i == codeBrace)
					codeDepth = depth;
			} else if (c == '}') {
				if (depth == codeDepth) {
					codeDepth = -1;
					codeBrace = -1;
				}
				depth = Math.max(0, depth - 1);
			}
		}
		return codeDepth >= 0;
	}

	private static int codeBlockBrace(@Nonnull String source, int index) {
		while (index < source.length() && Character.isWhitespace(source.charAt(index)))
			index++;
		if (index >= source.length() || source.charAt(index++) != ':')
			return -1;
		while (index < source.length() && Character.isWhitespace(source.charAt(index)))
			index++;
		return index < source.length() && source.charAt(index) == '{' ? index : -1;
	}

	private static boolean isIdentifierPart(char c) {
		return Character.isJavaIdentifierPart(c) || c == '-';
	}

	/**
	 * @param definition
	 * 		Instruction definition.
	 * @param index
	 * 		Operand index.
	 *
	 * @return {@code true} if the operand at the index is a Dalvik register, register pair, register array, or register range.
	 */
	static boolean isDalvikRegisterOperand(@Nonnull Instruction<?> definition, int index) {
		if (index < 0 || index >= definition.operandCount())
			return false;
		var operand = definition.operand(index);
		return operand == DalvikOperands.REGISTER.getOperand()
				|| operand == DalvikOperands.REGISTER_WIDE.getOperand()
				|| operand == DalvikOperands.REGISTER_ARRAY.getOperand()
				|| operand == DalvikOperands.INVOKE_REGISTERS.getOperand();
	}

	/**
	 * Scans operands through the caret, retaining the current top-level operand, nested element indexes, and token prefix.
	 * Delimiters inside braces and quoted strings are not treated as operand boundaries.
	 */
	@Nonnull
	private static Cursor scan(@Nonnull String source, int start, int caret) {
		List<Integer> nested = new ArrayList<>();
		List<Integer> containerStarts = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		int nextArgument = 0;
		int activeArgument = -1;
		int tokenStart = -1;
		boolean hasTopLevelValue = false;
		char quote = 0;
		boolean escaped = false;
		String nestedKey = null;

		for (int i = start; i < caret; i++) {
			char c = source.charAt(i);
			if (quote != 0) {
				if (escaped) {
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}

			if (c == '\'' || c == '"') {
				if (nested.isEmpty() && !hasTopLevelValue) {
					activeArgument = nextArgument++;
					hasTopLevelValue = true;
				}
				quote = c;
				tokenStart = -1;
				continue;
			}

			if (c == '{') {
				if (nested.isEmpty() && !hasTopLevelValue) {
					activeArgument = nextArgument++;
					hasTopLevelValue = true;
				}
				nested.add(0);
				containerStarts.add(i);
				keys.add(null);
				tokenStart = -1;
				continue;
			}
			if (c == '}') {
				if (!nested.isEmpty()) {
					nested.removeLast();
					containerStarts.removeLast();
					keys.removeLast();
				}
				tokenStart = -1;
				continue;
			}
			if (c == ',') {
				tokenStart = -1;
				if (nested.isEmpty()) {
					hasTopLevelValue = false;
					activeArgument = -1;
				} else {
					nested.set(nested.size() - 1, nested.getLast() + 1);
					keys.set(keys.size() - 1, null);
				}
				continue;
			}
			if (c == '=' || c == ':') {
				if (!nested.isEmpty() && tokenStart >= 0)
					keys.set(keys.size() - 1, source.substring(tokenStart, i).trim());
				tokenStart = -1;
				continue;
			}
			if (Character.isWhitespace(c)) {
				tokenStart = -1;
				if (nested.isEmpty()) {
					hasTopLevelValue = false;
					activeArgument = -1;
				}
				continue;
			}

			if (nested.isEmpty() && !hasTopLevelValue) {
				activeArgument = nextArgument++;
				hasTopLevelValue = true;
			}
			if (tokenStart < 0)
				tokenStart = i;
		}

		String prefix = quote == 0 && tokenStart >= 0 ? source.substring(tokenStart, caret) : "";
		int currentArgument = activeArgument >= 0 ? activeArgument : nextArgument;
		List<Integer> nestedIndices = nested.isEmpty() ? List.of() : List.copyOf(nested);
		for (int i = keys.size() - 1; i >= 0; i--) {
			if (keys.get(i) != null) {
				nestedKey = keys.get(i);
				break;
			}
		}
		return new Cursor(currentArgument, nestedIndices, List.copyOf(containerStarts), nestedKey, prefix);
	}

	/**
	 * Result of {@link #scan(String, int, int)}.
	 *
	 * @param argumentIndex
	 * 		Index of the top-level operand at the caret.
	 * @param nestedIndices
	 * 		Element index at each brace depth, outermost first.
	 * @param containerStarts
	 * 		Source offset of each open brace, outermost first.
	 * @param nestedKey
	 * 		Innermost {@code key:} or {@code key=} name before the caret, or {@code null} if there is none.
	 * @param tokenPrefix
	 * 		Unquoted token text immediately before the caret.
	 */
	private record Cursor(int argumentIndex, @Nonnull List<Integer> nestedIndices,
	                      @Nonnull List<Integer> containerStarts,
	                      @Nullable String nestedKey, @Nonnull String tokenPrefix) {}

	/**
	 * The instruction operand under the caret, with the information needed to choose completions for it.
	 *
	 * @param source
	 * 		Full editor text.
	 * @param caret
	 * 		Caret offset into {@code source}.
	 * @param method
	 * 		Method containing the caret, if the AST has one.
	 * @param instruction
	 * 		AST instruction for the caret, or {@code null} when only the source text is available.
	 * @param mnemonic
	 * 		Instruction mnemonic.
	 * @param definition
	 * 		Instruction definition for the mnemonic.
	 * @param argumentIndex
	 * 		Index of the operand under the caret.
	 * @param argument
	 * 		AST element for the operand, if one has been parsed.
	 * @param selectedElement
	 * 		Deepest AST element at the caret within {@code argument}.
	 * @param nestedIndices
	 * 		Element index at each brace depth within the operand.
	 * @param containerStarts
	 * 		Source offset of each open brace within the operand.
	 * @param nestedKey
	 * 		Innermost {@code key:} or {@code key=} name before the caret, if any.
	 * @param tokenPrefix
	 * 		Unquoted token text immediately before the caret.
	 * @param linePrefix
	 * 		Source text from the start of the instruction to the caret.
	 */
	record Site(@Nonnull String source,
	            int caret,
	            @Nullable ASTMethod method,
	            @Nullable ASTInstruction instruction,
	            @Nonnull String mnemonic,
	            @Nonnull Instruction<?> definition,
	            int argumentIndex,
	            @Nullable ASTElement argument,
	            @Nullable ASTElement selectedElement,
	            @Nonnull List<Integer> nestedIndices,
	            @Nonnull List<Integer> containerStarts,
	            @Nullable String nestedKey,
	            @Nonnull String tokenPrefix,
	            @Nonnull String linePrefix) {
		/**
		 * @param depth
		 * 		Depth of the array, {@code 0} for the outermost array.
		 * @param elementIndex
		 * 		Index of the element within the array at {@code depth}.
		 *
		 * @return Text of the array element at {@code depth} and {@code elementIndex},
		 * or {@code null} if the caret is not inside an array or the indexes are invalid.
		 */
		@Nullable
		String arrayElementPrefix(int depth, int elementIndex) {
			if (depth < 0 || depth >= containerStarts.size() || elementIndex < 0)
				return null;

			int start = containerStarts.get(depth) + 1;
			int index = 0;
			int elementStart = start;
			int nestedDepth = 0;
			char quote = 0;
			boolean escaped = false;
			for (int i = start; i < caret; i++) {
				char c = source.charAt(i);
				if (quote != 0) {
					if (escaped)
						escaped = false;
					else if (c == '\\')
						escaped = true;
					else if (c == quote)
						quote = 0;
					continue;
				}
				if (c == '\'' || c == '"') {
					quote = c;
				} else if (c == '{') {
					nestedDepth++;
				} else if (c == '}' && nestedDepth > 0) {
					nestedDepth--;
				} else if (c == ',' && nestedDepth == 0) {
					if (index == elementIndex)
						return source.substring(elementStart, i).trim();
					index++;
					elementStart = i + 1;
				}
			}

			return index == elementIndex ? source.substring(elementStart, caret).trim() : null;
		}

		/**
		 * @param index
		 * 		Index of the operand.
		 *
		 * @return Text of the operand at {@code index},
		 * or {@code null} if the caret is not inside an instruction or the index is invalid.
		 */
		@Nullable
		String argumentText(int index) {
			if (instruction == null || index < 0 || index >= instruction.arguments().size())
				return null;
			ASTElement value = instruction.arguments().get(index);
			return value == null ? null : value.content();
		}
	}
}
