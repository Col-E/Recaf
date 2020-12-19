package me.coley.recaf.parse.bytecode;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.coley.recaf.parse.bytecode.ast.DefinitionArgAST;
import me.coley.recaf.parse.bytecode.ast.VariableReference;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Variable analysis. Used to generate {@link #getVariables() variable nodes}.
 *
 * @author Matt
 */
public class VariableGenerator {
	private final VariableNameCache nameCache;
	private final MethodCompilation compilation;
	private final MethodNode node;
	private final List<LocalVariableNode> variableNodes = new ArrayList<>();
	private final int minimumVarIndex;

	VariableGenerator(VariableNameCache nameCache, MethodCompilation compilation, MethodNode node) {
		this.nameCache = nameCache;
		this.compilation = compilation;
		this.node = node;
		minimumVarIndex = computeArgUsedIndices();
	}

	private int computeArgUsedIndices() {
		Type methodType = Type.getMethodType(node.desc);
		int index = AccessFlag.isStatic(node.access) ? 0 : 1;
		int lastArgSize = 0;
		for (Type argType : methodType.getArgumentTypes()) {
			lastArgSize = argType.getSize();
			index += lastArgSize;
		}
		return index;
	}

	/**
	 * Visit the method with verifier information.
	 *
	 * @param verifier
	 * 		Verifier with analysis data. May be {@code null}.
	 */
	void computeVariables(MethodVerifier verifier) throws AssemblerException {
		// Compute the variable indices to their assigned types
		Multimap<Integer, Integer> indexToSort = MultimapBuilder.hashKeys().hashSetValues().build();
		Multimap<Integer, String> indexToName = MultimapBuilder.hashKeys().hashSetValues().build();
		for (VariableReference reference : compilation.getAst().getRoot().search(VariableReference.class)) {
			int index = reference.getVariableIndex(nameCache);
			int sort = reference.getVariableSort();
			String name = reference.getVariableName().getName();
			indexToSort.put(index, sort);
			// Only add variable refs that are not numeric
			if (!name.matches("\\d+"))
				indexToName.put(index, name);
		}
		// Compute the method argument variables first
		for (DefinitionArgAST reference : compilation.getAst().getRoot().search(DefinitionArgAST.class)) {
			int index = reference.getVariableIndex(nameCache);
			String name = reference.getVariableName().getName();
			// Only add variable refs that are not numeric
			if (!name.matches("\\d+"))
				computeArgument(index, name, reference.getDesc().getDesc());
		}
		// If a variable index only has one type, this is very easy.
		// Otherwise, if it has multiple types we must consider scope...
		for (int index : indexToSort.keySet()) {
			// Skip over variables already declared by arguments.
			// It is illegal to re-assign variables to argument-occupied indices.
			if (index < minimumVarIndex)
				continue;
			Collection<Integer> sorts = indexToSort.get(index);
			Collection<String> names = indexToName.get(index);
			// TODO: Support variable index reuse with same name via scope analysis
			if (names.size() > 1)
				throw new AssemblerException(
						"Named variables sharing the same index is not yet supported in Recaf's assembler\n" +
						"The disassembler should have de-duplicated the indices. Please open a bug report.");
			if (sorts.size() == 1) {
				// TODO: Don't pass in the name like this.
				//       See above note.
				if (!names.isEmpty())
					computeSimple(index, names.iterator().next());
			} else if (verifier != null) {
				computeScoped(index, verifier);
			}
		}
	}

	/**
	 * Simple entire-range scoped variable.
	 *
	 * @param index
	 * 		Arg variable index.
	 * @param name
	 * 		Arg name.
	 * @param desc
	 * 		Arg type.
	 */
	private void computeArgument(int index, String name, String desc) {
		LabelNode first = null;
		LabelNode last = null;
		for (AbstractInsnNode insn : node.instructions) {
			if (insn instanceof LabelNode) {
				LabelNode lbl = (LabelNode) insn;
				if (first == null) {
					first = lbl;
				} else {
					last = lbl;
				}
			}
		}
		addVariable(index, name, Type.getType(desc), first, last);
	}

	/**
	 * Simple min-max range check.
	 *
	 * @param index
	 * 		Variable to compute.
	 * @param name
	 * 		Variable name.
	 *
	 * @throws AssemblerException
	 * 		When name lookup fails.
	 */
	private void computeSimple(int index, String name) throws AssemblerException {
		boolean foundUsage = false;
		LabelNode first = null;
		LabelNode last = null;
		Type type = null;
		for (AbstractInsnNode insn : node.instructions) {
			// If variable usage, record that we saw it and the implied type. Invalidate the last label.
			// If label usage, record as first if not seen variable yet. Otherwise set last.
			if (insn instanceof VarInsnNode) {
				VarInsnNode vin = (VarInsnNode) insn;
				if (vin.var == index) {
					type = getType(vin.getOpcode());
					foundUsage = true;
					last = null;
				}
			} else if (insn instanceof IincInsnNode) {
				IincInsnNode iinc = (IincInsnNode) insn;
				if (iinc.var == index) {
					type = Type.INT_TYPE;
					foundUsage = true;
					last = null;
				}
			} else if (insn instanceof LabelNode) {
				LabelNode lbl = (LabelNode) insn;
				if (!foundUsage) {
					first = lbl;
				} else {
					last = lbl;
				}
			}
		}
		// Validate
		if (type == null)
			throw new AssemblerException("Could not determine type from variable: " + name);
		if (first == null)
			throw new AssemblerException("Could not determine end of range for variable: " + name);
		// Add variable
		addVariable(index, name, type, first, last);
	}

	private void computeScoped(int index, MethodVerifier verifier) throws AssemblerException {
		// TODO: Make scoped variable
	}

	/**
	 * Register variable.
	 *
	 * @param index
	 * 		Var index.
	 * @param name
	 * 		Var name.
	 * @param type
	 * 		Var type.
	 * @param start
	 * 		Starting label.
	 * @param end
	 * 		Ending label.
	 */
	private void addVariable(int index, String name, Type type, LabelNode start, LabelNode end) {
		variableNodes.add(new LocalVariableNode(name, type.getDescriptor(), null, start, end, index));
	}

	/**
	 * @return {@code null} when no variables used. A list of generated variables when they are declared.
	 */
	public List<LocalVariableNode> getVariables() {
		if (variableNodes.isEmpty())
			return null;
		return variableNodes;
	}

	/**
	 * @param opcode
	 * 		Var opcode.
	 *
	 * @return Type derived from the opcode.
	 *
	 * @throws AssemblerException
	 * 		When the opcode is not supported.
	 */
	private static Type getType(int opcode) throws AssemblerException {
		switch (opcode) {
			case ALOAD:
			case ASTORE:
				return TypeUtil.OBJECT_TYPE;
			case IINC:
			case ILOAD:
			case ISTORE:
				return Type.INT_TYPE;
			case FLOAD:
			case FSTORE:
				return Type.FLOAT_TYPE;
			case DLOAD:
			case DSTORE:
				return Type.DOUBLE_TYPE;
			case LLOAD:
			case LSTORE:
				return Type.LONG_TYPE;
			default:
				throw new AssemblerException("Unsupported opcode for variable reference: " + opcode);
		}
	}
}
