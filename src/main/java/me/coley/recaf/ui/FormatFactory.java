package me.coley.recaf.ui;

import com.github.javaparser.utils.StringEscapeUtils;
import me.coley.recaf.bytecode.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.objectweb.asm.Handle;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.insn.ParameterValInsnNode;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.ui.component.InsnHBox;
import me.coley.recaf.ui.component.TextHBox;
import me.coley.recaf.ui.component.constructor.TypeAnnotationNodeConstructor.RefType;

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
	 * @param text
	 *            Raw text.
	 * @return Text node of raw text.
	 */
	public static Node raw(String text) {
		TextHBox t = new TextHBox();
		addRaw(t, text);
		return t;
	}

	/**
	 * @param name
	 *            Name.
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
		add(t, insnNode(node.start, method));
		addRaw(t, " - ");
		add(t, insnNode(node.end, method));
		addRaw(t, " # ");
		add(t, insnNode(node.handler, method));
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
		annotation(t, item);
		return t;
	}

	private static void annotation(TextHBox t, AnnotationNode item) {
		if (item.desc != null) {
			addRaw(t, "@");
			addType(t, Type.getType(item.desc));
		}
		if (item instanceof TypeAnnotationNode) {
			TypeAnnotationNode itemType = (TypeAnnotationNode) item;
			addRaw(t, " Path:");
			addRaw(t, itemType.typePath.toString());
			addRaw(t, " Ref:");
			addRaw(t, RefType.fromSort(itemType.typeRef).name());
			addRaw(t, " ");
		}
		int max = item.values == null ? 0 : item.values.size();
		if (max == 0) {
			return;
		}
		addRaw(t, "(");
		for (int i = 0; i < max; i += 2) {
			String name = (String) item.values.get(i);
			Object value = item.values.get(i + 1);
			if (max > 2 || !"value".equals(name)) {
				addName(t, name);
				addRaw(t, "=");
			}
			if (value instanceof String) {
				addString(t, "\"" + value.toString() + "\"");
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
			} else if (value instanceof List) {
				List<?> l = (List<?>) value;
				if (l.isEmpty()) {
					addRaw(t, "[]");
				} else {
					addRaw(t, "[");
					Object first = l.get(0);
					Type type;
					if (first instanceof String[]) {
						type = Type.getType(Enum.class);
					} else if (first instanceof Type) {
						type = Type.getType(Class.class);
					} else if (first instanceof AnnotationNode) {
						type = Type.getType(Annotation.class);
					} else {
						type = Type.getType(first.getClass());
					}
					addType(t, type);
					addRaw(t, "...]");
				}
			} else if (value instanceof String[]) { // enum
				String[] str = (String[]) value;
				Type enumType = Type.getType(str[0]);
				addType(t, enumType);
				addRaw(t, ".");
				addName(t, str[1]);
			} else if (value instanceof AnnotationNode) {
				annotation(t, (AnnotationNode) value);
			} else {
				Logging.fine("Unknown annotation data-type: @" + i + " type: " + value.getClass());
			}
			if (i + 2 < max) {
				addRaw(t, ", ");
			}
		}
		addRaw(t, ")");
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
	public static InsnHBox insnNode(AbstractInsnNode ain, MethodNode method) {
		if (ain == null)
			throw new IllegalStateException("Attempted to display null instruction");
		InsnHBox t = new InsnHBox(ain);
		try {
			style(t, "opcode-wrapper");
			addInsn(t, ain, method);
		} catch (Exception e) {
			String type = ain.getClass().getSimpleName();
			String meth = method == null ? "<ISOLATED>" : method.name + method.desc;
			int index = InsnUtil.index(ain, method);
			Logging.error("Invalid instruction: " + type + "@" + meth + "@" + index, true);
			Logging.error(e, false);
		}
		return t;
	}

	/**
	 * VBox wrapper for multiple instructions populated via
	 * {@link #insnNode(AbstractInsnNode, MethodNode)}.
	 * 
	 * @param insns
	 *            Collection of instructions.
	 * @param method
	 *            Method containing the instructions.
	 * @return Wrapper for the instructions.
	 */
	public static Node insnsNode(Collection<AbstractInsnNode> insns, MethodNode method) {
		VBox box = new VBox();
		for (AbstractInsnNode ain : insns) {
			box.getChildren().add(insnNode(ain, method));
		}
		return box;
	}

	/**
	 * String representation for multiple instruction populated via
	 * {@link #insnNode(AbstractInsnNode, MethodNode)}.
	 * 
	 * @param insns
	 *            Collection of instructions.
	 * @param method
	 *            Method containing the instructions.
	 * @return String of the instructions representation.
	 */
	public static String insnsString(Collection<AbstractInsnNode> insns, MethodNode method) {
		StringBuilder sb = new StringBuilder();
		for (AbstractInsnNode ain : insns) {
			sb.append(insnNode(ain, method).getText() + "\n");
		}
		return sb.toString().trim();
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

	private static void addInsn(InsnHBox text, AbstractInsnNode ain, MethodNode method) {
		addInsn(text, ain, method, true);
	}

	private static void addInsn(InsnHBox text, AbstractInsnNode ain, MethodNode method, boolean includeIndex) {
		if (includeIndex && !InsnUtil.isolated(ain)) {
			// digit spaces
			int spaces = String.valueOf(method == null ? InsnUtil.getSize(ain) : method.instructions.size()).length();
			// index of instruction
			String index = pad(String.valueOf(InsnUtil.index(ain, method)), spaces);
			addRaw(text, index);
			addRaw(text, ": ");
		}
		// add instruction name (opcode)
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
		// add instruction-type specific content. {} for scoped variable names.
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
				String value = StringEscapeUtils.escapeJava(String.valueOf(ldc.cst));
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
				addRaw(text, ":");
				addInsn(text, line.start, method, false);
			}
			break;
		}
		case AbstractInsnNode.JUMP_INSN: {
			JumpInsnNode jin = (JumpInsnNode) ain;
			addInsn(text, jin.label, method, false);
			if (ConfDisplay.instance().jumpHelp) {
				//@formatter:off
				String help = " ";
				switch (ain.getOpcode()) {
				case IFEQ     : help += "[$0 == 0 -> offset]";   break;
				case IFNE     : help += "[$0 != 0 -> offset]";   break;
				case IFLE     : help += "[$0 <= 0 -> offset]";   break;
				case IFLT     : help += "[$0 < 0 -> offset]";    break;
				case IFGE     : help += "[$0 >= 0 -> offset]";   break;
				case IFGT     : help += "[$0 > 0 -> offset]";    break;
				case IF_ACMPNE: help += "[$1 != $0 -> offset]";  break;
				case IF_ACMPEQ: help += "[$1 == $0 -> offset]";  break;
				case IF_ICMPEQ: help += "[$1 == $0 -> offset]";  break;
				case IF_ICMPNE: help += "[$1 != $0 -> offset]";  break;
				case IF_ICMPLE: help += "[$1 <= $0 -> offset]";  break;
				case IF_ICMPLT: help += "[$0 < $0 -> offset]";   break;
				case IF_ICMPGE: help += "[$0 >= $0 -> offset]";  break;
				case IF_ICMPGT: help += "[$0 > $0 -> offset]";   break;
				case GOTO     : help += "[-> offset]";           break;
				case JSR      : help += "[-> offset, +address]"; break;
				case IFNULL   : help += "[$0 == null -> offset]";break;
				case IFNONNULL: help += "[$0 != null -> offset]";break;
				}
				addNote(text, help);
				//@formatter:on
			}
			break;
		}
		case AbstractInsnNode.IINC_INSN: {
			IincInsnNode iinc = (IincInsnNode) ain;
			LocalVariableNode lvn = InsnUtil.getLocal(method, iinc.var);
			if (lvn != null) {
				addValue(text, "$" + iinc.var);
				addRaw(text, ":");
				addName(text, lvn.name);
			} else {
				addValue(text, "$" + iinc.var);
			}
			if (iinc.incr > 0) {
				addRaw(text, " + ");
			} else {
				addRaw(text, " - ");
			}
			addValue(text, String.valueOf(Math.abs(iinc.incr)));
			break;
		}
		case AbstractInsnNode.VAR_INSN: {
			VarInsnNode vin = (VarInsnNode) ain;
			addValue(text, String.valueOf(vin.var));
			LocalVariableNode lvn = InsnUtil.getLocal(method, vin.var);
			if (lvn != null) {
				String type = TypeUtil.filter(Type.getType(lvn.desc));
				StringBuilder sb = new StringBuilder(" [");
				sb.append(lvn.name);
				sb.append(":");
				sb.append(type);
				sb.append("]");
				addNote(text, sb.toString());
			}
			break;
		}
		case AbstractInsnNode.TABLESWITCH_INSN: {
			TableSwitchInsnNode tsin = (TableSwitchInsnNode) ain;
			StringBuilder lbls = new StringBuilder();
			for (LabelNode label : tsin.labels) {
				String offset = InsnUtil.labelName(label);
				lbls.append(offset).append(", ");
			}
			if (lbls.toString().endsWith(", ")) {
				lbls = new StringBuilder(lbls.substring(0, lbls.length() - 2));
			}
			String dfltOff = InsnUtil.labelName(tsin.dflt);
			addNote(text, " range[" + tsin.min + "-" + tsin.max + "]");
			addNote(text, " offsets[" + lbls + "]");
			addNote(text, " default:" + dfltOff);
			break;
		}
		case AbstractInsnNode.LOOKUPSWITCH_INSN: {
			LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) ain;
			String lbls = "";
			int cap = Math.min(lsin.keys.size(), lsin.labels.size());
			for (int i = 0; i < cap; i++) {
				String offset = InsnUtil.labelName(lsin.labels.get(i));
				lbls += lsin.keys.get(i) + "=" + offset + ", ";
			}
			if (lbls.endsWith(", ")) {
				lbls = lbls.substring(0, lbls.length() - 2);
			}
			addNote(text, " mapping[" + lbls + "]");
			if (lsin.dflt != null) {
				String offset = InsnUtil.labelName(lsin.dflt);
				addNote(text,  " default:" + offset);
			}
			break;
		}
		case AbstractInsnNode.MULTIANEWARRAY_INSN: {
			MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) ain;
			addType(text, Type.getType(manain.desc));
			StringBuilder dims = new StringBuilder();
			for (int i = 0; i < manain.dims; i++)
				dims.append("[]");
			addRaw(text, dims.toString());
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
			addRaw(text, InsnUtil.labelName(ain));
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
				addValue(text, String.valueOf(param.getIndex()));
				addRaw(text, " ");
				// Attempt to get variable name
				if (param.getParameter() != null) {
					addRaw(text, "(");
					addName(text, param.getParameter().name);
					addRaw(text, ":");
					addType(text, param.getValueType());
					addRaw(text, ") ");
				} else {
					LocalVariableNode lvn = InsnUtil.getLocal(method, param.getIndex());
					if (lvn != null) {
						addRaw(text, "(");
						addName(text, lvn.name);
						addRaw(text, ":");
						addType(text, param.getValueType());
						addRaw(text, ") ");
					} else {
						addType(text, param.getValueType());
					}
				}

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
