package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.parser.HandleParser;
import me.coley.recaf.util.*;
import org.objectweb.asm.*;
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
	private Map<LabelNode, String> labelToName = new HashMap<>();
	private List<String> out = new ArrayList<>();
	private Set<Integer> paramVariables = new HashSet<>();
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
				out.add(String.format("TRY %s %s CATCH(%s) %s", start, end, block.type, handler));
			}
		// Visit instructions
		for(AbstractInsnNode insn : value.instructions.toArray())
			appendLine(insn);
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
			else if(o instanceof Double)
				line.append(o).append('D');
			else if(o instanceof Float)
				line.append(o).append('F');
			else
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

	private void visitIntInsn(StringBuilder line, IntInsnNode insn) {
		line.append(' ').append(insn.operand);
	}

	private void visitVarInsn(StringBuilder line, VarInsnNode insn) {
		String name = varInsnToName(insn);
		line.append(' ').append(name);
	}

	private void visitTypeInsn(StringBuilder line, TypeInsnNode insn) {
		line.append(' ').append(insn.desc);
	}

	private void visitFieldInsn(StringBuilder line, FieldInsnNode insn) {
		line.append(' ').append(insn.owner).append('.').append(insn.name).append(' ').append(insn.desc);
	}

	private void visitMethodInsn(StringBuilder line, MethodInsnNode insn) {
		line.append(' ').append(insn.owner).append('.').append(insn.name).append(insn.desc);
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
			str = str.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
			line.append('"').append(str).append('"');
		} else if (insn.cst instanceof Long)
			line.append(insn.cst).append('L');
		else if (insn.cst instanceof Double)
			line.append(insn.cst).append('D');
		else if (insn.cst instanceof Float)
			line.append(insn.cst).append('F');
		else
			line.append(insn.cst);
	}

	private void visitIincInsn(StringBuilder line, IincInsnNode insn) {
		String name = varInsnToName(insn);
		line.append(' ').append(name).append(' ').append(insn.incr);
	}

	private void visitTableSwitchInsn(StringBuilder line, TableSwitchInsnNode insn) {
		line.append(" range[").append(insn.min).append('-').append(insn.max).append(']');
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
		line.append(' ').append(insn.name).append(' ').append(insn.desc).append(' ');
		// append handle
		visitHandle(line, insn.bsm, false);
		// append args
		line.append(" args[");
		for(int i = 0; i < insn.bsmArgs.length; i++) {
			Object arg = insn.bsmArgs[i];
			// int
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
		line.append("handle[");
		line.append(OpcodeUtil.tagToName(handle.getTag()));
		line.append(' ').append(handle.getOwner());
		line.append('.').append(handle.getName());
		if (handle.getTag() >= Opcodes.H_GETFIELD && handle.getTag() <= Opcodes.H_PUTSTATIC)
			line.append(' ');
		line.append(handle.getDesc());
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

	// ======================================================================= //

	private String name(LabelNode label) {
		return labelToName.getOrDefault(label, "?");
	}
}
