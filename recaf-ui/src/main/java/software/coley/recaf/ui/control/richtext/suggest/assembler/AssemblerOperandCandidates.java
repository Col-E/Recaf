package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.target.TargetContext;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static software.coley.recaf.ui.control.richtext.suggest.assembler.AssemblerOperandLocator.isDalvikRegisterOperand;

/**
 * Utilities for finding operand candidates for tab-completion.
 * <p>
 * Names an operand may refer to:
 * <ul>
 *     <li>Labels</li>
 *     <li>JVM locals</li>
 *     <li>Dalvik registers</li>
 *     <li>Handle kinds</li>
 *     <li>Handle shortcuts</li>
 * </ul>
 *
 * @author Matt Coley
 */
final class AssemblerOperandCandidates {
	private AssemblerOperandCandidates() {}

	/**
	 * @param method
	 * 		Method declaring the labels. {@code null} if the caret is not inside a method.
	 * @param targetContext
	 * 		Instruction language used to find label declarations.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Labels declared in the method, in declaration order, that extend the prefix.
	 */
	@Nonnull
	static List<String> labelCandidates(@Nullable ASTMethod method, @Nonnull TargetContext targetContext,
	                                    @Nonnull String prefix) {
		if (method == null)
			return List.of();

		Set<String> names = new LinkedHashSet<>();
		AssemblyQueries.labels(method, targetContext).declarations().forEach(label -> names.add(label.name()));
		return properPrefixMatches(names, prefix);
	}

	/**
	 * @param method
	 * 		Method declaring the locals. {@code null} if the caret is not inside a method.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Named JVM local variables of the method, in declaration order, that extend the prefix.
	 */
	@Nonnull
	static List<String> jvmLocalCandidates(@Nullable ASTMethod method, @Nonnull String prefix) {
		if (method == null)
			return List.of();

		Set<String> names = new LinkedHashSet<>();
		AssemblyQueries.variables(method, JvmTargetContext.INSTANCE).declarations()
				.forEach(variable -> names.add(variable.identity().name()));
		return properPrefixMatches(names, prefix);
	}

	/**
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Handle kind names, sorted, that extend the prefix.
	 */
	@Nonnull
	static List<String> handleKindCandidates(@Nonnull String prefix) {
		return properPrefixMatches(Handle.KINDS.keySet().stream().sorted().toList(), prefix);
	}

	/**
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Handle shortcut names, sorted, that extend the prefix.
	 */
	@Nonnull
	static List<String> handleShortcutCandidates(@Nonnull String prefix) {
		return properPrefixMatches(Handle.HANDLE_SHORTCUTS.keySet().stream().sorted().toList(), prefix);
	}

	/**
	 * @param names
	 * 		Candidate names, in the order they should be offered.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Names that start with the prefix and are not equal to it, in iteration order.
	 */
	@Nonnull
	private static List<String> properPrefixMatches(@Nonnull Collection<String> names, @Nonnull String prefix) {
		return names.stream().filter(name -> name.startsWith(prefix) && !name.equals(prefix)).toList();
	}

	/**
	 * Collects register names a Dalvik operand may use in {@code method}, filtered by the typed prefix.
	 *
	 * @param method
	 * 		Method to collect registers from.
	 * @param targetContext
	 * 		Instruction language used to find register operands in the body.
	 * @param prefix
	 * 		Typed text.
	 *
	 * @return Matching register names, de-duplicated, in the order above.
	 */
	@Nonnull
	static List<String> dalvikRegisterCandidates(@Nonnull ASTMethod method, @Nonnull TargetContext targetContext,
	                                             @Nonnull String prefix) {
		Set<String> used = new LinkedHashSet<>();
		if (method.getCode() != null) {
			for (ASTInstruction instruction : method.getCode().getInstructions()) {
				if (instruction instanceof ASTLabel || instruction.identifier() == null)
					continue;

				Instruction<?> definition = targetContext.instructions().get(instruction.identifier().content());
				if (definition == null)
					continue;

				// Collect register names from the instruction's operands, but only for operands that are actually registers.
				List<? extends ASTElement> arguments = instruction.arguments();
				for (int i = 0; i < Math.min(arguments.size(), definition.operandCount()); i++) {
					if (isDalvikRegisterOperand(definition, i))
						collectRegisterNames(arguments.get(i), used);
				}
			}
		}

		Set<String> candidates = new LinkedHashSet<>();
		Integer registerCount = declaredRegisterCount(method);
		if (registerCount != null) {
			// If the register count is known we can fill all the registers in order, even if they are not used in the method.
			for (int i = 0; i < registerCount; i++)
				candidates.add("v" + i);
		} else {
			// Otherwise we can only offer the registers that are actually used in the method, but we can sort them by their numeric index.
			TreeMap<Integer, String> usedNumericRegisters = new TreeMap<>();
			for (String name : used) {
				Integer index = registerIndex(name, 'v');
				if (index != null)
					usedNumericRegisters.putIfAbsent(index, name);
			}
			candidates.addAll(usedNumericRegisters.values());
		}

		// Parameter registers are always available.
		int incomingSlots = incomingRegisterSlots(method);
		for (int i = 0; i < incomingSlots; i++)
			candidates.add("p" + i);

		// Instance reference for virtual methods.
		boolean isStatic = method.getModifiers().hasModifier("static");
		if (!isStatic)
			candidates.add("this");

		// Name aliases for parameters and used registers.
		for (ASTIdentifier parameter : method.getParameters()) {
			String name = parameter.literal();
			if (!isStatic || !"this".equals(name))
				candidates.add(name);
		}
		for (String name : used) {
			if (registerIndex(name, 'v') == null && registerIndex(name, 'p') == null)
				candidates.add(name);
		}

		return candidates.stream().filter(name -> name.startsWith(prefix)).toList();
	}

	/**
	 * @param method
	 * 		Method to check for a declared register count.
	 *
	 * @return Declared register count, or {@code null} if not declared or invalid.
	 */
	@Nullable
	private static Integer declaredRegisterCount(@Nonnull ASTMethod method) {
		ASTElement value = method.getMethodAttributes().get("registers");
		try {
			if (value instanceof ASTNumber number)
				return Math.max(0, number.asInt());
			if (value instanceof ASTIdentifier identifier)
				return Math.max(0, Integer.parseInt(identifier.content()));
		} catch (Exception ex) {
			// Junk value
		}
		return null;
	}

	/**
	 * @param method
	 * 		Method to check for the number of incoming register slots.
	 *
	 * @return Number of incoming register slots, or {@code 0} if the method is invalid.
	 */
	private static int incomingRegisterSlots(@Nonnull ASTMethod method) {
		String descriptor = method.getDescriptor().literal();
		try {
			int slots = method.getModifiers().hasModifier("static") ? 0 : 1;
			for (Type argument : Type.getArgumentTypes(descriptor))
				slots += argument.getSize();
			return slots;
		} catch (RuntimeException ignored) {
			return 0;
		}
	}

	/**
	 * Collects register names from an AST element, recursively descending into arrays.
	 *
	 * @param element
	 * 		AST element to collect register names from.
	 * @param names
	 * 		Set to add register names to.
	 */
	private static void collectRegisterNames(@Nullable ASTElement element, @Nonnull Set<String> names) {
		if (element instanceof ASTIdentifier identifier) {
			names.add(identifier.literal());
		} else if (element instanceof ASTArray array) {
			for (ASTElement value : array.values())
				collectRegisterNames(value, names);
		}
	}

	/**
	 * @param name
	 * 		Register name to parse.
	 * @param prefix
	 * 		Expected prefix of the register name.
	 *
	 * @return Numeric index of the register, or {@code null} if the name is not a valid register with the given prefix.
	 */
	@Nullable
	private static Integer registerIndex(@Nonnull String name, char prefix) {
		// Too short for 'pN' / 'vN' / etc. or doesn't have the right prefix.
		if (name.length() < 2 || name.charAt(0) != prefix)
			return null;

		for (int i = 1; i < name.length(); i++)
			if (!Character.isDigit(name.charAt(i)))
				return null;

		try {
			return Integer.parseInt(name.substring(1));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
}
