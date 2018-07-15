package me.coley.recaf.ui;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.Set;

import org.objectweb.asm.Handle;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.Asm;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.bytecode.insn.ParameterValInsnNode;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.ui.component.OpcodeHBox;
import me.coley.recaf.ui.component.TextHBox;

/**
 * Text formatting.
 * 
 * @author Matt
 */
public class FormatFactory {
	/**
	 * Max length of LDC string constant. Larger strings cut off and end
	 * replaced with "..."
	 */
	private static final int MAX_LDC_LENGTH = 100;

	/**
	 * @param name
	 *            Name
	 * @return Text node of name.
	 */
	public static TextHBox name(String name) {
		TextHBox t = new TextHBox();
		addName(t, name);
		return t;
	}

	/**
	 * @param type
	 *            ASM Type descriptor. Does not allow method types.
	 * @return Text node of type.
	 */
	public static TextHBox type(Type type) {
		TextHBox t = new TextHBox();
		addType(t, type);
		return t;
	}

	/**
	 * @param type
	 *            ASM Type descriptor. Only allows method types.
	 * @return Text node of type.
	 */
	public static TextHBox typeMethod(Type type) {
		TextHBox t = new TextHBox();
		addMethodType(t, type);
		return t;
	}

	/**
	 * @param types
	 *            Array of ASM Type descriptor.
	 * @return Text node of array.
	 */
	public static TextHBox typeArray(Type[] types) {
		TextHBox t = new TextHBox();
		addArray(t, types);
		return t;
	}

	/**
	 * @param node
	 *            Try-catch node.
	 * @param method
	 *            Method containing try-catch.
	 * @return Text of try-catch block.
	 */
	public static TextHBox exception(TryCatchBlockNode node, MethodNode method) {
		TextHBox t = new TextHBox();
		String type = node.type;
		if (type == null) {
			addRaw(t, "*");
		} else {
			addType(t, Type.getObjectType(node.type));
		}
		addRaw(t, " - [");
		add(t, opcode(node.start, method));
		addRaw(t, " - ");
		add(t, opcode(node.end, method));
		addRaw(t, " # ");
		add(t, opcode(node.handler, method));
		addRaw(t, "]");
		return t;
	}

	/**
	 * @param item
	 *            Annotation.
	 * @return Text of annotation.
	 */
	public static TextHBox annotation(AnnotationNode item) {
		TextHBox t = new TextHBox();
		addRaw(t, "@");
		addName(t, item.desc);
		int max = item.values == null ? 0 : item.values.size();
		if (max > 0) {
			addRaw(t, "(");
			for (int i = 0, n = max; i < n; i += 2) {
				String name = (String) item.values.get(i);
				Object value = item.values.get(i + 1);
				addName(t, name);
				addRaw(t, "=");
				if (value instanceof String) {
					addString(t, value.toString());
				} else if (value instanceof Type) {
					Type type = (Type) value;
					if (TypeUtil.isInternal(type)) {
						addName(t, type.getInternalName());
					} else if (TypeUtil.isStandard(type.toString())) {
						addType(t, type);
					} else {
						Logging.warn("Unknown annotation type format in value: @" + (i + 1) + " type: " + value.toString());
						addRaw(t, type.getDescriptor());
					}
				} else if (value instanceof Number) {
					addValue(t, value.toString());
				} else {
					Logging.warn("Unknown annotation data: @" + i + " type: " + value.getClass());
					addRaw(t, value.toString());
				}
				if (i + 1 < max) {
					addRaw(t, ", ");
				}
			}
			addRaw(t, ")");
		}
		return t;
	}

	/**
	 * Text representation of a local variable.
	 * 
	 * @param node
	 *            Local variable.
	 * @param method
	 *            Method containing the variable.
	 * @return
	 */
	public static TextHBox variable(LocalVariableNode node, MethodNode method) {
		TextHBox t = new TextHBox();
		addName(t, node.name);
		addRaw(t, " - ");
		addType(t, Type.getType(node.desc));
		return t;
	}

	/**
	 * Text representation of an instruction.
	 * 
	 * @param ain
	 *            Instruction.
	 * @param method
	 *            Method containing the instruction.
	 * @return Text representation of an instruction.
	 */
	public static OpcodeHBox opcode(AbstractInsnNode ain, MethodNode method) {
		OpcodeHBox t = new OpcodeHBox(ain);
		try {
			style(t, "opcode-wrapper");
			addOpcode(t, ain, method);
		} catch (Exception e) {
			String type = ain.getClass().getSimpleName();
			String meth = method.name + method.desc;
			int index = OpcodeUtil.index(ain, method);
			Logging.error("Invalid opcode: " + type + "@" + meth + "@" + index, true);
		}
		return t;
	}

	/**
	 * VBox wrapper for multiple opcodes populated via
	 * {@link #opcode(AbstractInsnNode, MethodNode)}.
	 * 
	 * @param insns
	 *            Set of instructions.
	 * @param method
	 *            Method containing the instructions.
	 * @return Wrapper for the instructions.
	 */
	public static Node opcodeSet(Set<AbstractInsnNode> insns, MethodNode method) {
		VBox box = new VBox();
		for (AbstractInsnNode ain : insns) {
			box.getChildren().add(opcode(ain, method));
		}
		return box;
	}

	/// ================================================================= ///
	/// ========================= CONSTRUCTION ========================== ///
	/// ================================================================= ///

	private static void addName(TextHBox text, String name) {
		text.append(name);
		Node t = text(name);
		style(t, "op-name");
		add(text, t);
	}

	private static void addType(TextHBox text, Type type) {
		String content = TypeUtil.filter(type);
		text.append(content);
		Node t = text(content);
		style(t, "op-type");
		add(text, t);
	}

	private static void addMethodType(TextHBox text, Type type) {
		addRaw(text, "(");
		int len = type.getArgumentTypes().length;
		for (int i = 0; i < len; i++) {
			Type param = type.getArgumentTypes()[i];
			addType(text, param);
			if (i != len - 1) {
				addRaw(text, ", ");
			}
		}
		addRaw(text, ")");
		addType(text, type.getReturnType());
	}

	private static void addArray(TextHBox text, Type[] types) {
		int len = types.length;
		for (int i = 0; i < len; i++) {
			addType(text, types[i]);
			// Add separator between types
			if (i != len - 1) {
				addRaw(text, ", ");
			}
		}
	}

	private static void addOpcode(OpcodeHBox text, AbstractInsnNode ain, MethodNode method) {
		if (!OpcodeUtil.isolated(ain)) {
			// digit spaces
			int spaces = String.valueOf(OpcodeUtil.getSize(ain, method)).length();
			// index in opcode
			String index = pad(String.valueOf(OpcodeUtil.index(ain, method)), spaces);
			addRaw(text, index);
			addRaw(text, ": ");
		}
		// add opcode name
		String opName = OpcodeUtil.opcodeToName(ain.getOpcode());
		if (ain.getType() == AbstractInsnNode.LINE) {
			// F_NEW is the opcode for LineInsn's, so make an exception here.
			opName = "LINE";
		} else if (ain.getType() == AbstractInsnNode.LABEL) {
			opName = "LABEL";
		}
		text.append(opName);
		Label op = text(opName);
		style(op, "op-opcode");
		add(text, op);
		// add space for following content
		if (ain.getType() != AbstractInsnNode.INSN) {
			addRaw(text, " ");
		}
		// add opcode-type specific content. {} for scoped variable names.
		switch (ain.getType()) {
		case AbstractInsnNode.FIELD_INSN: {
			FieldInsnNode fin = (FieldInsnNode) ain;
			addType(text, Type.getObjectType(fin.owner));
			addRaw(text, ".");
			addName(text, fin.name);
			addRaw(text, " ");
			addType(text, Type.getType(fin.desc));
			break;
		}
		case AbstractInsnNode.METHOD_INSN: {
			MethodInsnNode min = (MethodInsnNode) ain;
			addType(text, Type.getObjectType(min.owner));
			addRaw(text, ".");
			addName(text, min.name);
			addMethodType(text, Type.getType(min.desc));
			break;
		}
		case AbstractInsnNode.TYPE_INSN: {
			TypeInsnNode tin = (TypeInsnNode) ain;
			String desc = tin.desc;
			addType(text, Type.getObjectType(desc));
			// TODO: make this part of the type. However due to conflicts
			// between object-type and arrays, merging crashes.
			// This is a "hack" to show the array value.
			if (ain.getOpcode() == Opcodes.ANEWARRAY) {
				addRaw(text, "[]");
			}
			break;
		}
		case AbstractInsnNode.INT_INSN: {
			IntInsnNode iin = (IntInsnNode) ain;
			addValue(text, String.valueOf(iin.operand));
			break;
		}
		case AbstractInsnNode.LDC_INSN: {
			LdcInsnNode ldc = (LdcInsnNode) ain;
			if (ldc.cst instanceof String) {
				String value = String.valueOf(ldc.cst);
				if (value.length() > MAX_LDC_LENGTH) {
					value = value.substring(0, MAX_LDC_LENGTH) + "...";
				}
				addString(text, "\"" + value + "\"");
			} else if (ldc.cst instanceof Type) {
				addType(text, TypeUtil.parse(ldc.cst.toString()));
			} else {
				addValue(text, String.valueOf(ldc.cst));
			}
			break;
		}
		case AbstractInsnNode.LINE: {
			LineNumberNode line = (LineNumberNode) ain;
			addValue(text, String.valueOf(line.line));
			if (line.start != null) {
				addRaw(text, " (");
				addOpcode(text, line.start, method);
				addRaw(text, ")");
			}
			break;
		}
		case AbstractInsnNode.JUMP_INSN: {
			JumpInsnNode jin = (JumpInsnNode) ain;
			if (ConfDisplay.instance().jumpHelp) {
				//@formatter:off
				String z = " ";
				switch (ain.getOpcode()) {
				case IFEQ     : z += "[$0 == 0 -> offset]";   break;
				case IFNE     : z += "[$0 != 0 -> offset]";   break;
				case IFLE     : z += "[$0 <= 0 -> offset]";   break;
				case IFLT     : z += "[$0 < 0 -> offset]";    break;
				case IFGE     : z += "[$0 >= 0 -> offset]";   break;
				case IFGT     : z += "[$0 > 0 -> offset]";    break;
				case IF_ACMPNE: z += "[$1 != $0 -> offset]";  break;
				case IF_ACMPEQ: z += "[$1 == $0 -> offset]";  break;
				case IF_ICMPEQ: z += "[$1 == $0 -> offset]";  break;
				case IF_ICMPNE: z += "[$1 != $0 -> offset]";  break;
				case IF_ICMPLE: z += "[$1 <= $0 -> offset]";  break;
				case IF_ICMPLT: z += "[$0 < $0 -> offset]";   break;
				case IF_ICMPGE: z += "[$0 >= $0 -> offset]";  break;
				case IF_ICMPGT: z += "[$0 > $0 -> offset]";   break;
				case GOTO     : z += "[-> offset]";           break;
				case JSR      : z += "[-> offset, +address]"; break;
				case IFNULL   : z += "[$0 == null -> offset]";break;
				case IFNONNULL: z += "[$0 != null -> offset]";break;
				}
				addNote(text, z);
				//@formatter:on
			}
			addRaw(text, " (");
			addOpcode(text, jin.label, method);
			addRaw(text, ")");
			break;
		}
		case AbstractInsnNode.VAR_INSN: {
			VarInsnNode vin = (VarInsnNode) ain;
			addValue(text, String.valueOf(vin.var));
			if (method != null && method.localVariables != null && vin.var < method.localVariables.size()) {
				LocalVariableNode lvn = Asm.getLocal(method, vin.var);
				addRaw(text, " (");
				addName(text, lvn.name);
				addRaw(text, ":");
				addType(text, Type.getType(lvn.desc));
				addRaw(text, ")");
			}
			break;
		}
		case AbstractInsnNode.TABLESWITCH_INSN: {
			TableSwitchInsnNode tsin = (TableSwitchInsnNode) ain;
			StringBuilder lbls = new StringBuilder();
			for (LabelNode label : tsin.labels) {
				lbls.append(OpcodeUtil.index(label, method)).append(", ");
			}
			if (lbls.toString().endsWith(", ")) {
				lbls = new StringBuilder(lbls.substring(0, lbls.length() - 2));
			}
			int dfltOff = OpcodeUtil.index(tsin.dflt, method);
			addNote(text, " range[" + tsin.min + "-" + tsin.max + "]");
			addNote(text, " offsets[" + lbls + "]");
			addNote(text, " dflt:" + dfltOff);
			break;
		}
		case AbstractInsnNode.LOOKUPSWITCH_INSN: {
			LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) ain;
			String lbls = "";
			int cap = Math.min(lsin.keys.size(), lsin.labels.size());
			for (int i = 0; i < cap; i++) {
				int offset = OpcodeUtil.index(lsin.labels.get(i), method);
				lbls += lsin.keys.get(i) + "->" + offset + ", ";
			}
			if (lsin.dflt != null) {
				int offset = OpcodeUtil.index(lsin.dflt, method);
				lbls += "dflt:" + offset;
			}
			if (lbls.endsWith(", ")) {
				lbls = lbls.substring(0, lbls.length() - 2);
			}
			addNote(text, " [" + lbls + "]");
			break;
		}
		case AbstractInsnNode.MULTIANEWARRAY_INSN: {
			MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) ain;
			addType(text, Type.getType(manain.desc));
			addNote(text, " x" + manain.dims);
			break;
		}
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
			InvokeDynamicInsnNode insnIndy = (InvokeDynamicInsnNode) ain;
			if (insnIndy.bsmArgs.length >= 2 && insnIndy.bsmArgs[1] instanceof Handle) {
				Handle handle = (Handle) insnIndy.bsmArgs[1];
				Type typeIndyOwner = Type.getObjectType(handle.getOwner());
				Type typeIndyDesc = Type.getMethodType(handle.getDesc());
				addType(text, typeIndyDesc.getReturnType());
				addRaw(text, " ");
				addType(text, typeIndyOwner);
				addRaw(text, ".");
				addName(text, handle.getName());
				addRaw(text, "(");
				Type[] args = typeIndyDesc.getArgumentTypes();
				for (int i = 0; i < args.length; i++) {
					Type t = args[i];
					addType(text, t);
					if (i < args.length - 1) {
						addRaw(text, ", ");
					}
				}
				addRaw(text, ")");
			} else {
				addNote(text, " (unknown indy format)");
			}
			break;
		}
		case AbstractInsnNode.LABEL: {
			addRaw(text, OpcodeUtil.labelName(ain));
			break;
		}
		case AbstractInsnNode.FRAME: {
			// FrameNode frame = (FrameNode) ain;
			break;
		}
		case OpcodeUtil.CUSTOM: {
			// custom opcodes
			if (ain.getOpcode() == ParameterValInsnNode.PARAM_VAL) {
				ParameterValInsnNode param = (ParameterValInsnNode) ain;
				if (param.getParameter() != null) {
					addName(text, param.getParameter().name);
					addRaw(text, ":");
				}
				addType(text, param.getValueType());

			}
			break;
		}
		}
	}

	private static Label addValue(TextHBox text, String content) {
		text.append(content);
		Label lbl = text(content);
		style(lbl, "op-value");
		add(text, lbl);
		return lbl;
	}

	private static Label addString(TextHBox text, String content) {
		text.append(content);
		Label lbl = text(content);
		style(lbl, "op-value-string");
		add(text, lbl);
		return lbl;
	}

	private static Label addRaw(TextHBox text, String content) {
		text.append(content);
		Label lbl = text(content);
		style(lbl, "op-raw");
		add(text, lbl);
		return lbl;
	}

	private static Label addNote(TextHBox text, String content) {
		text.append(content);
		Label lbl = text(content);
		style(lbl, "op-note");
		add(text, lbl);
		return lbl;
	}

	/// ================================================================= ///
	/// ============================ UTILITY ============================ ///
	/// ================================================================= ///

	/**
	 * Add {@code Node} to {@code HTextBox}.
	 * 
	 * @param text
	 * @param node
	 */
	private static void add(TextHBox text, Node node) {
		text.getChildren().add(node);
	}

	/**
	 * Adds a css class to the given text.
	 * 
	 * @param text
	 * @param clazz
	 */
	private static void style(Node text, String clazz) {
		text.getStyleClass().add(clazz);
	}

	/**
	 * Create label from string.
	 * 
	 * @param value
	 *            String value.
	 * @return Label of string.
	 */
	private static Label text(String value) {
		Label text = new Label(value);
		text.getStyleClass().add("code-fmt");
		return text;
	}

	/**
	 * Pad to the right.
	 * 
	 * @param text
	 * @param padding
	 * @return
	 */
	private static String pad(String text, int padding) {
		return String.format("%-" + padding + "s", text);
	}
}
