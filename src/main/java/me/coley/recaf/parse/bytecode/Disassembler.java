package me.coley.recaf.parse.bytecode;

import me.coley.recaf.metadata.Comments;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.parser.HandleParser;
import me.coley.recaf.util.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

/**
 * Method instruction disassembler.
 *
 * @author Matt
 */
public class Disassembler {
	private final Map<LabelNode, String> labelToName = new HashMap<>();
	private final List<String> out = new ArrayList<>();
	private final Set<Integer> paramVariables = new HashSet<>();
	private Comments comments;
	private MethodNode method;
	private boolean useIndyAlias = true;
	private boolean doInsertIndyAlias;

	/**
	 * @param method
	 * 		Method to disassemble.
	 *
	 * @return Text of method instructions.
	 */
	public String disassemble(MethodNode method) {
		setup(method);
		visit(method);
		return String.join("\n", out);
	}

	/**
	 * @param field
	 * 		Field to disassemble.
	 *
	 * @return Text of field definition.
	 */
	public String disassemble(FieldNode field) {
		visit(field);
		return String.join("\n", out);
	}


	/**
	 * @param useIndyAlias
	 * 		Flag to determine if lambda handles should be simplified where possible.
	 */
	public void setUseIndyAlias(boolean useIndyAlias) {
		this.useIndyAlias = useIndyAlias;
	}

	private void setup(MethodNode value) {
		this.method = value;
		comments = new Comments(value);
		// Ensure there is a label before the first variable instruction and after the last usage.
		enforceLabelRanges(value);
		// Validate each named variable has the same type.
		splitSameIndexedVariablesOfDiffNames(value);
		// Validate each named variable has the same type.
		splitSameNamedVariablesOfDiffTypes(value);
		// Input validation
		if (value.instructions == null)
			throw new IllegalArgumentException("Method instructions list is null!");
		// Generate initial names for the labels
		int i = 0;
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for(AbstractInsnNode insn : value.instructions.toArray())
			if(insn instanceof LabelNode) {
				LabelNode lbl = (LabelNode) insn;
				labelToName.put(lbl, StringUtil.generateName(alphabet, i++));
			} else if (insn instanceof InvokeDynamicInsnNode) {
				InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
				if(useIndyAlias && HandleParser.DEFAULT_HANDLE.equals(indy.bsm))
					doInsertIndyAlias = true;
			}
		// Generate variable names
		if(!AccessFlag.isStatic(value.access))
			paramVariables.add(0);
		// Rename labels for catch ranges
		if (value.tryCatchBlocks == null)
			return;
		i = 1;
		int blocks = value.tryCatchBlocks.size();
		for (TryCatchBlockNode block : value.tryCatchBlocks)
			if (blocks > 1) {
				labelToName.put(block.start, "EX_START_" + i);
				labelToName.put(block.end, "EX_END_" + i);
				labelToName.put(block.handler, "EX_HANDLER_" + i);
				i++;
			} else {
				labelToName.put(block.start, "EX_START");
				labelToName.put(block.end, "EX_END");
				labelToName.put(block.handler, "EX_HANDLER");
			}
	}

	private void visit(MethodNode value) {
		// Visit definition
		MethodDefinitionAST def = new MethodDefinitionAST(0, 0,
				new NameAST(0, 0, value.name),
				new DescAST(0, 0, Type.getMethodType(value.desc).getReturnType().getDescriptor()));
		for (AccessFlag flag : AccessFlag.values())
			if (flag.getTypes().contains(AccessFlag.Type.METHOD) && (value.access & flag.getMask()) == flag.getMask())
				def.getModifiers().add(new DefinitionModifierAST(0, 0, flag.getName().toUpperCase()));
		Type[] argTypes = Type.getMethodType(value.desc).getArgumentTypes();
		int paramVar = AccessFlag.isStatic(value.access) ? 0 : 1;
		for (Type arg : argTypes) {
			String name = firstVarByIndex(paramVar);
			if (name == null)
				name = String.valueOf(paramVar);
			def.addArgument(new DefinitionArgAST(0, 0,
					new DescAST(0,0,arg.getDescriptor()),
					new NameAST(0,0,name)));
			paramVar += arg.getSize();
		}
		out.add(def.print());
		// Visit signature
		if (value.signature != null)
			out.add("SIGNATURE " + value.signature);
		// Visit aliases
		if(doInsertIndyAlias) {
			StringBuilder line = new StringBuilder("ALIAS H_META \"");
			visitHandle(line, HandleParser.DEFAULT_HANDLE, true);
			line.append('"');
			out.add(line.toString());
		}
		// Visit exceptions
		if(value.exceptions != null)
			for(String type : value.exceptions)
				out.add("THROWS " + type);
		// Visit try-catches
		if (value.tryCatchBlocks != null)
			for (TryCatchBlockNode block : value.tryCatchBlocks) {
				String start = labelToName.get(block.start);
				String end = labelToName.get(block.end);
				String handler = labelToName.get(block.handler);
				if (block.type != null)
					out.add(String.format("TRY %s %s CATCH(%s) %s", start, end, block.type, handler));
				else
					out.add(String.format("TRY %s %s CATCH(*) %s", start, end, handler));
			}
		// Visit instructions
		int offset = 0;
		for (AbstractInsnNode insn : value.instructions.toArray()) {
			// Prepend comments if found
			appendComment(offset);
			// Append instruction
			appendLine(insn);
			offset++;
		}
		// Append final comment if found
		appendComment(offset);
	}

	private void visit(FieldNode value) {
		// Visit definition
		FieldDefinitionAST def = new FieldDefinitionAST(0, 0,
				new NameAST(0, 0, value.name),
				new DescAST(0, 0, value.desc));
		for (AccessFlag flag : AccessFlag.values())
			if (flag.getTypes().contains(AccessFlag.Type.FIELD) && (value.access & flag.getMask()) == flag.getMask())
				def.getModifiers().add(new DefinitionModifierAST(0, 0, flag.getName().toUpperCase()));
		out.add(def.print());
		// Visit signature
		if (value.signature != null)
			out.add("SIGNATURE " + value.signature);
		// Visit default-value
		if(value.value != null) {
			StringBuilder line = new StringBuilder("VALUE ");
			Object o = value.value;
			if(o instanceof String) {
				String str = o.toString();
				str = str.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
				line.append('"').append(str).append('"');
			} else if(o instanceof Long)
				line.append(o).append('L');
			else if (o instanceof Double) {
				line.append(o);
				if (!(o.equals(Double.POSITIVE_INFINITY) || o.equals(Double.NEGATIVE_INFINITY)
						|| o.equals(Double.NaN)))
					line.append('D');
			} else if (o instanceof Float) {
				// Float has the same edge case items as double, but we want to denote them with
				// "F" suffix.
				line.append(o).append('F');
			} else
				line.append(o);
			out.add(line.toString());
		}
	}

	private void appendLine(AbstractInsnNode insn) {
		StringBuilder line = new StringBuilder(OpcodeUtil.opcodeToName(insn.getOpcode()));
		switch(insn.getType()) {
			case INSN:
				break;
			case INT_INSN:
				visitIntInsn(line, (IntInsnNode) insn);
				break;
			case VAR_INSN:
				visitVarInsn(line, (VarInsnNode) insn);
				break;
			case TYPE_INSN:
				visitTypeInsn(line, (TypeInsnNode) insn);
				break;
			case FIELD_INSN:
				visitFieldInsn(line, (FieldInsnNode) insn);
				break;
			case METHOD_INSN:
				visitMethodInsn(line, (MethodInsnNode) insn);
				break;
			case JUMP_INSN:
				visitJumpInsn(line, (JumpInsnNode) insn);
				break;
			case LABEL:
				visitLabel(line, (LabelNode) insn);
				break;
			case LDC_INSN:
				visitLdcInsn(line, (LdcInsnNode) insn);
				break;
			case IINC_INSN:
				visitIincInsn(line, (IincInsnNode) insn);
				break;
			case TABLESWITCH_INSN:
				visitTableSwitchInsn(line, (TableSwitchInsnNode) insn);
				break;
			case LOOKUPSWITCH_INSN:
				visitLookupSwitchInsn(line, (LookupSwitchInsnNode) insn);
				break;
			case MULTIANEWARRAY_INSN:
				visitMultiANewArrayInsn(line, (MultiANewArrayInsnNode) insn);
				break;
			case LINE:
				visitLine(line, (LineNumberNode) insn);
				break;
			case INVOKE_DYNAMIC_INSN:
				visitIndyInsn(line, (InvokeDynamicInsnNode) insn);
				break;
			case FRAME:
				// Do nothing
				break;
			default:
				throw new IllegalStateException("Unknown instruction type: " + insn.getType());
		}
		out.add(line.toString());
	}

	private void appendComment(int offset) {
		String prefix = "// ";
		String comment = comments.get(offset);
		if (comment != null)
			out.add(prefix + String.join("\n" + prefix, comment.split("\n")));
	}

	private void visitIntInsn(StringBuilder line, IntInsnNode insn) {
		if (insn.getOpcode() == Opcodes.NEWARRAY) {
			line.append(' ').append(TypeUtil.newArrayArgToType(insn.operand).getDescriptor());
		} else {
			line.append(' ').append(insn.operand);
		}
	}

	private void visitVarInsn(StringBuilder line, VarInsnNode insn) {
		String name = varInsnToName(insn);
		line.append(' ').append(name);
	}

	private void visitTypeInsn(StringBuilder line, TypeInsnNode insn) {
		// TypeInsnNode demands the descriptor be an internal name....
		// Sometimes its not though.
		Type type = insn.desc.contains(";") ?
				Type.getType(insn.desc) : Type.getObjectType(insn.desc);
		String name = EscapeUtil.escape(type.getInternalName());
		line.append(' ').append(name);
	}

	private void visitFieldInsn(StringBuilder line, FieldInsnNode insn) {
		String owner = EscapeUtil.escape(insn.owner);
		String name = EscapeUtil.escape(insn.name);
		String desc = EscapeUtil.escape(insn.desc);
		line.append(' ').append(owner).append('.').append(name).append(' ').append(desc);
	}

	private void visitMethodInsn(StringBuilder line, MethodInsnNode insn) {
		String owner = EscapeUtil.escapeCommon(insn.owner);
		String name = EscapeUtil.escapeCommon(insn.name);
		String desc = EscapeUtil.escapeCommon(insn.desc);
		line.append(' ').append(owner).append('.').append(name).append(desc);
		if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn.itf) {
			line.append(" itf");
		}
	}

	private void visitJumpInsn(StringBuilder line, JumpInsnNode insn) {
		String name = name(insn.label);
		line.append(' ').append(name);
	}

	private void visitLabel(StringBuilder line, LabelNode insn) {
		// insert modified opcode
		line.delete(0, line.length());
		// append name
		String name = name(insn);
		line.append(name).append(":");
	}

	private void visitLdcInsn(StringBuilder line, LdcInsnNode insn) {
		line.append(' ');
		if(insn.cst instanceof String) {
			String str = insn.cst.toString();
			str = EscapeUtil.escape(str);
			line.append('"').append(str).append('"');
		} else if (insn.cst instanceof Long)
			line.append(insn.cst).append('L');
		else if (insn.cst instanceof Double) {
			Double d = (Double) insn.cst;
			line.append(d);
			if (!(d.equals(Double.POSITIVE_INFINITY) || d.equals(Double.NEGATIVE_INFINITY) || d.equals(Double.NaN)))
				line.append('D');
		}
		else if (insn.cst instanceof Float)
			line.append(insn.cst).append('F');
		else if (insn.cst instanceof Handle)
			visitHandle(line, (Handle) insn.cst, false);
		else
			line.append(insn.cst);
	}

	private void visitIincInsn(StringBuilder line, IincInsnNode insn) {
		String name = varInsnToName(insn);
		line.append(' ').append(name).append(' ').append(insn.incr);
	}

	private void visitTableSwitchInsn(StringBuilder line, TableSwitchInsnNode insn) {
		line.append(" range[").append(insn.min).append(':').append(insn.max).append(']');
		line.append(" labels[");
		for(int i = 0; i < insn.labels.size(); i++) {
			String name = name(insn.labels.get(i));
			line.append(name);
			if(i < insn.labels.size() - 1)
				line.append(", ");
		}
		String name = name(insn.dflt);
		line.append("] default[").append(name).append(']');
	}

	private void visitLookupSwitchInsn(StringBuilder line, LookupSwitchInsnNode insn) {
		line.append(" mapping[");
		for(int i = 0; i < insn.keys.size(); i++) {
			String name = name(insn.labels.get(i));
			line.append(insn.keys.get(i)).append('=').append(name);
			if(i < insn.keys.size() - 1)
				line.append(", ");
		}
		String name = name(insn.dflt);
		line.append("] default[").append(name).append(']');
	}

	private void visitMultiANewArrayInsn(StringBuilder line, MultiANewArrayInsnNode insn) {
		line.append(' ').append(insn.desc).append(' ').append(insn.dims);
	}

	private void visitLine(StringBuilder line, LineNumberNode insn) {
		// insert modified opcode
		line.delete(0, line.length());
		line.append("LINE ");
		// append line info
		String name = name(insn.start);
		line.append(name).append(' ').append(insn.line);
	}

	private void visitIndyInsn(StringBuilder line, InvokeDynamicInsnNode insn) {
		// append nsmr & desc
		String name = EscapeUtil.escape(insn.name);
		String desc = EscapeUtil.escape(insn.desc);
		line.append(' ').append(name).append(' ').append(desc).append(' ');
		// append handle
		visitHandle(line, insn.bsm, false);
		// append args
		line.append(" args[");
		for(int i = 0; i < insn.bsmArgs.length; i++) {
			Object arg = insn.bsmArgs[i];
			if (arg instanceof Integer)
				line.append(arg);
			else if (arg instanceof Float)
				line.append(arg).append('F');
			else if (arg instanceof Long)
				line.append(arg).append('L');
			else if (arg instanceof Double)
				line.append(arg).append('D');
			else if (arg instanceof Type)
				line.append(arg);
			else if (arg instanceof Handle)
				visitHandle(line, (Handle) arg, false);
			else if (arg instanceof String)
				line.append("\"" + EscapeUtil.escape((String)arg) +"\"");
			if(i < insn.bsmArgs.length - 1)
				line.append(", ");
		}
		line.append(']');
	}

	private void visitHandle(StringBuilder line, Handle handle, boolean dontAlias) {
		if(!dontAlias && useIndyAlias && HandleParser.DEFAULT_HANDLE.equals(handle)) {
			line.append("${" + HandleParser.DEFAULT_HANDLE_ALIAS + "}");
			return;
		}
		String owner = EscapeUtil.escape(handle.getOwner());
		String name = EscapeUtil.escape(handle.getName());
		String desc = EscapeUtil.escape(handle.getDesc());
		line.append("handle[");
		line.append(OpcodeUtil.tagToName(handle.getTag()));
		line.append(' ').append(owner);
		line.append('.').append(name);
		if (handle.getTag() >= Opcodes.H_GETFIELD && handle.getTag() <= Opcodes.H_PUTSTATIC)
			line.append(' ');
		line.append(desc);
		line.append(']');
	}

	// ======================================================================= //

	/**
	 * @param insn
	 * 		Instruction to disassemble.
	 *
	 * @return Text of instruction.
	 */
	public static String insn(AbstractInsnNode insn) {
		Disassembler d = new Disassembler();
		// Populate label names if necessary for the given instruction type
		int type = insn.getType();
		boolean dbg = type == LABEL || type == LINE;
		boolean ref = type == JUMP_INSN  || type == LOOKUPSWITCH_INSN || type == TABLESWITCH_INSN;
		if (dbg || ref) {
			int i = 0;
			String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
			AbstractInsnNode tmp = InsnUtil.getFirst(insn);
			while(tmp != null) {
				if(insn instanceof LabelNode) {
					LabelNode lbl = (LabelNode) insn;
					d.labelToName.put(lbl, StringUtil.generateName(alphabet, i++));
				}
				tmp = tmp.getNext();
			}
		}
		// Disassemble the single insn
		d.appendLine(insn);
		return d.out.get(0);
	}

	// ======================================================================= //

	/**
	 * @param insn
	 * 		Variable instruction.
	 *
	 * @return {@code null} if no variable with the index exists. Otherwise, the variable's name.
	 */
	private String varInsnToName(AbstractInsnNode insn) {
		int varIndex = ((insn instanceof VarInsnNode) ?
				((VarInsnNode) insn).var : ((IincInsnNode) insn).var);
		if (method != null && method.localVariables != null) {
			int insnPos = InsnUtil.index(insn);
			List<LocalVariableNode> list =  method.localVariables.stream()
					.filter(v -> varIndex == v.index)
					.collect(Collectors.toList());
			String name = list.stream()
					.filter(v -> insnPos >= InsnUtil.index(v.start) - 1 && insnPos <= InsnUtil.index(v.end) + 1)
					.map(v -> v.name)
					.findFirst().orElse(String.valueOf(varIndex));
			// Override slot-0 for non-static methods to ALWAYS be 'this' just in case
			// an obfuscator has renamed the variable
			if (!AccessFlag.isStatic(method.access) && varIndex == 0)
				name = "this";
			return name;
		}
		return String.valueOf(varIndex);
	}

	/**
	 * @param index
	 * 		Index to search by.
	 *
	 * @return Name of first variable matching the index.
	 */
	private String firstVarByIndex(int index) {
		if (method != null && method.localVariables != null) {
			return method.localVariables.stream()
					.filter(v -> v.index == index)
					.min(Comparator.comparingInt(a -> InsnUtil.index(a.start)))
					.map(v -> v.name)
					.orElse(String.valueOf(index));
		}
		return String.valueOf(index);
	}

	/**
	 * Updates a method so that all variable instructions have a label before and after them.
	 * This allows the assembler to regenerate proper variable ranges.
	 *
	 * @param value
	 * 		Method to update.
	 */
	private static void enforceLabelRanges(MethodNode value) {
		AbstractInsnNode[] insns = value.instructions.toArray();
		// Iterate forwards to validate start ranges
		boolean varFound = false;
		boolean labelFound = false;
		for (int i = 0; i < insns.length; i++) {
			AbstractInsnNode ain = insns[i];
			// Check if we can abandon the search
			if (!varFound && labelFound) {
				break;
			}
			// Insert label if none exist before the variable instruction
			else if (varFound) {
				value.instructions.insert(new LabelNode());
				break;
			}
			// Update found items
			if (ain.getType() == LABEL)
				labelFound = true;
			else if (ain.getType() == VAR_INSN)
				varFound = true;
		}
		// Iterate backwards to validate end ranges
		varFound = false;
		labelFound = false;
		for (int i = insns.length - 1; i >= 0; i--) {
			AbstractInsnNode ain = insns[i];
			// Check if we can abandon the search
			if (!varFound && labelFound) {
				break;
			}
			// Insert label if none exist after the variable instruction
			else if (varFound) {
				value.instructions.add(new LabelNode());
				break;
			}
			// Update found items
			if (ain.getType() == LABEL)
				labelFound = true;
			else if (ain.getType() == VAR_INSN)
				varFound = true;
		}
		// If we have a VERY short method we may have to do an extra check
		if (varFound && !labelFound) {
			value.instructions.add(new LabelNode());
		}
	}

	/**
	 * Renames variables that share the same name but different types.
	 * This is done to prevent type conflicts of variables by the same name in
	 * obfuscated code.
	 * <br>
	 * There are cases where the same-type variables of the same name should
	 * be kept. For example, a method with multiple try-catch blocks and all
	 * exception variables are named <i>"e"</i>.
	 *
	 * @param node
	 * 		Method to update.
	 */
	public static void splitSameNamedVariablesOfDiffTypes(MethodNode node) {
		if (node.localVariables == null)
			return;
		Map<Integer, LocalVariableNode> indexToVar = new HashMap<>();
		Map<Integer, String> indexToName = new HashMap<>();
		Map<String, Integer> nameToIndex = new HashMap<>();
		boolean changed = false;
		for(LocalVariableNode lvn : node.localVariables) {
			int index = lvn.index;
			String name = lvn.name;
			if(indexToName.containsValue(name)) {
				// The variable name is NOT unique.
				// Set both variables names to <NAME + INDEX>
				// Even with 3+ duplicates, this method will give each a unique index-based name.
				int otherIndex = nameToIndex.get(name);
				LocalVariableNode otherLvn = indexToVar.get(otherIndex);
				if (!lvn.desc.equals(otherLvn.desc)) {
					if (index != otherIndex) {
						// Different indices are used
						lvn.name = name + index;
						otherLvn.name = name + otherIndex;
					} else {
						// Same index but other type?
						// Just give it a random name.
						// TODO: Naming instead off of types would be better.
						lvn.name = nextIndexName(name, node);
					}
					changed = true;
				}
				// Update maps
				indexToVar.put(index, lvn);
				indexToName.put(index, lvn.name);
				nameToIndex.put(lvn.name, index);
			} else {
				// The variable name is unique.
				// Update maps
				indexToVar.put(index, lvn);
				indexToName.put(index, name);
				nameToIndex.put(name, index);
			}
		}
		// Logging
		if (changed) {
			Log.warn("Separating variables of same name pointing to different indices: " + node.name + node.desc);
		}
	}

	/**
	 * Reallocates variable indices of variables that share the same index but names.
	 * This is done to prevent type conflicts of variables by the same index in different scopes.
	 * Recaf does not understand variable scope at the moment,
	 * so this is a hack to be removed in the future when it does.
	 *
	 * @param node
	 * 		Method to update.
	 *
	 * @see VariableGenerator Place to add scoped variable support later.
	 */
	public static void splitSameIndexedVariablesOfDiffNames(MethodNode node) {
		if (node.localVariables == null)
			return;
		Map<Integer, String> indexToName = new HashMap<>();
		boolean changed = false;
		int nextFreeVar = computeMavVar(AccessFlag.isStatic(node.access), node.localVariables);
		for(LocalVariableNode lvn : node.localVariables) {
			int index = lvn.index;
			String name = lvn.name;
			if(!name.equals(indexToName.getOrDefault(index, name))) {
				// Update the variables index and bump the next free index
				index = lvn.index = nextFreeVar;
				nextFreeVar += Type.getType(lvn.desc).getSize();
				indexToName.put(index, lvn.name);
				changed = true;
			} else {
				indexToName.put(index, name);
			}
		}
		// Logging
		if (changed) {
			Log.warn("Separating variables of same index reusing the same name: " + node.name + node.desc);
		}
	}

	/**
	 * @param name
	 * 		Base name.
	 * @param node
	 * 		Method with variable names to compare to.
	 *
	 * @return Base name + a number, yields first non-taken name.
	 */
	private static String nextIndexName(String name, MethodNode node) {
		int i = 1;
		String[] tmp = {name + i};
		while(node.localVariables.stream().anyMatch(lvn -> tmp[0].equals(lvn.name))) {
			i++;
			tmp[0] = name + i;
		}
		return tmp[0];
	}

	private static int computeMavVar(boolean isStatic, List<LocalVariableNode> vars) {
		int max = isStatic ? 0 : 1;
		for (LocalVariableNode lvn : vars) {
			if (lvn.index >= max) {
				try {
					max = lvn.index + Type.getType(lvn.desc).getSize();
				} catch (IllegalArgumentException ex) {
					// If there is garbage in the descriptor ASM's type parse will fail.
					// We don't know if its supposed to be a wide type (double/long) so we
					// will just increment the max-value by 2 to be safe.
					max = lvn.index + 2;
				}
			}
		}
		return max;
	}


	private String name(LabelNode label) {
		return labelToName.getOrDefault(label, "?");
	}
}
