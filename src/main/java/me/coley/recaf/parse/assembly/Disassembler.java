package me.coley.recaf.parse.assembly;

import me.coley.recaf.parse.assembly.parsers.HandleParser;
import me.coley.recaf.util.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

/**
 * Method instruction disassembler.
 *
 * @author Matt
 */
public class Disassembler implements Visitor<MethodNode> {
	private Map<LabelNode, String> labelToName = new HashMap<>();
	private Map<Integer, String> varToName = new HashMap<>();
	private List<String> out = new ArrayList<>();
	private boolean useIndyAlias = true;

	/**
	 * @param method
	 * 		Method to disassemble.
	 *
	 * @return Text of method instructions.
	 *
	 * @throws LineParseException
	 * 		n/a.
	 */
	public String disassemble(MethodNode method) throws LineParseException {
		visitPre(method);
		visit(method);
		return String.join("\n", out);
	}

	/**
	 * @param useIndyAlias
	 * 		Flag to determine if lambda handles should be simplified where possible.
	 */
	public void setUseIndyAlias(boolean useIndyAlias) {
		this.useIndyAlias = useIndyAlias;
	}

	@Override
	public void visitPre(MethodNode value) throws LineParseException {
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
			}
		// Generate variable names
		if (value.localVariables != null)
			for (LocalVariableNode lvn : value.localVariables)
				varToName.put(lvn.index, lvn.name);
		if (!AccessFlag.isStatic(value.access))
			varToName.put(0, "this");
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

	@Override
	public void visit(MethodNode value) throws LineParseException {
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
				out.add(String.format("CATCH %s %s %s %s", block.type, start, end, handler));
			}
		// Visit instructions
		for(AbstractInsnNode insn : value.instructions.toArray())
			appendLine(insn);
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
				throw new IllegalStateException("Unknown opcode type: " + insn.getType());
		}
		out.add(line.toString());
	}

	private void visitIntInsn(StringBuilder line, IntInsnNode insn) {
		line.append(' ').append(insn.operand);
	}

	private void visitVarInsn(StringBuilder line, VarInsnNode insn) {
		String name = name(insn.var);
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
		line.append("LABEL ");
		// append name
		String name = name(insn);
		line.append(name);
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
		String name = name(insn.var);
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
		line.append(insn.line).append(' ').append(name);
	}

	private void visitIndyInsn(StringBuilder line, InvokeDynamicInsnNode insn) {
		// append nsmr & desc
		line.append(' ').append(insn.name).append(' ').append(insn.desc).append(' ');
		// append handle
		visitHandle(line, insn.bsm);
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
				visitHandle(line, (Handle) arg);
			if(i < insn.bsmArgs.length - 1)
				line.append(", ");
		}
		line.append(']');
	}

	private void visitHandle(StringBuilder line, Handle handle) {
		if(useIndyAlias && HandleParser.DEFAULT_HANDLE.equals(handle)) {
			line.append(HandleParser.DEFAULT_HANDLE_ALIAS);
			return;
		}
		line.append("handle[");
		line.append(OpcodeUtil.tagToName(handle.getTag()));
		line.append(' ').append(handle.getOwner());
		line.append(' ').append(handle.getName());
		line.append(' ').append(handle.getDesc());
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
		d.appendLine(insn);
		return d.out.get(0);
	}

	// ======================================================================= //

	private String name(int index) {
		return varToName.getOrDefault(index, String.valueOf(index));
	}

	private String name(LabelNode label) {
		return labelToName.getOrDefault(label, "?");
	}
}
