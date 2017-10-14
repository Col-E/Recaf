package me.coley.recaf.ui.component.list;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Options;
import me.coley.recaf.asm.Access;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.FontUtil;
import me.coley.recaf.ui.HtmlRenderer;

public class OpcodeCellRenderer implements HtmlRenderer, ListCellRenderer<AbstractInsnNode>, Opcodes {
	private final MethodNode method;
	private final Options options;

	public OpcodeCellRenderer(MethodNode method, Options options) {
		this.method = method;
		this.options = options;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends AbstractInsnNode> list, AbstractInsnNode value, int index,
			boolean isSelected, boolean cellHasFocus) {
		OpcodeList opcodeCastedList = (OpcodeList) list;
		JLabel label = new JLabel(getOpcodeText(opcodeCastedList, value));
		label.setFont(FontUtil.monospace);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createEtchedBorder());
		if (isSelected) {
			label.setBackground(Color.white);
		} else {
			label.setBackground(opcodeCastedList.getColorFor(index, value));
		}
		return label;
	}

	public String getOpcodeText(OpcodeList list, AbstractInsnNode ain) {
		int ainIndex = method.instructions.indexOf(ain);
		int zeros = String.valueOf(method.instructions.size()).length() - String.valueOf(ainIndex).length() + 1;
		String ss = "";
		for (int i = 0; i < zeros; i++) {
			ss += "&nbsp;";
		}
		String s = "<html>" + color(colGray, ainIndex + ".") + ss + "<b>" + OpcodeUtil.opcodeToName(ain.getOpcode()) + "</b>";
		switch (ain.getType()) {
		case AbstractInsnNode.INT_INSN:
			// Add int value to string
			IntInsnNode insnInt = (IntInsnNode) ain;
			s += " " + insnInt.operand;
			break;
		case AbstractInsnNode.VAR_INSN:
			// Add local variable index to string
			VarInsnNode insnVar = (VarInsnNode) ain;
			s += " " + insnVar.var;
			// Add local variable name if possible
			if (insnVar.var < method.localVariables.size()) {
				LocalVariableNode var = method.localVariables.get(insnVar.var);
				String varStr = var.name;
				s += color(colBlueDark, italic(" (" + varStr + ") - " + getTypeStr(Type.getType(var.desc), options)));
			} else if (insnVar.var == 0 && !Access.isStatic(method.access)) {
				// If the local variable doesn't have a name, we can assume at
				// index = 0 that it is 'this'.
				s += color(colBlueDark, italic(" (this)"));
			}
			break;
		case AbstractInsnNode.TYPE_INSN:
			// Add type name to string
			TypeInsnNode insnType = (TypeInsnNode) ain;
			String typeDeclaredStr = getTypeStr(Type.getType(insnType.desc), options);
			s += color(colBlueDark, italic(" " + typeDeclaredStr));
			break;
		case AbstractInsnNode.FIELD_INSN:
			FieldInsnNode insnField = (FieldInsnNode) ain;
			s += " " + italic(color(colBlueDark, getTypeStr(Type.getType(insnField.desc), options))) + " ";
			s += color(colRedDark, getTypeStr(Type.getObjectType(insnField.owner), options)) + "." + escape(insnField.name);
			break;
		case AbstractInsnNode.METHOD_INSN:
			MethodInsnNode insnMethod = (MethodInsnNode) ain;
			Type typeMethod = Type.getMethodType(insnMethod.desc);
			// Args string
			String args = "";
			for (Type t : typeMethod.getArgumentTypes()) {
				args += getTypeStr(t, options) + ", ";
			}
			if (args.endsWith(", ")) {
				args = args.substring(0, args.length() - 2);
			}
			s += " " + italic(color(colBlueDark, getTypeStr(typeMethod.getReturnType(), options))) + " ";
			s += color(colRedDark, getTypeStr(Type.getObjectType(insnMethod.owner), options)) + "." + escape(insnMethod.name)
				 + "(";
			s += color(colTealDark, args);
			s += ")";
			break;
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			InvokeDynamicInsnNode insnIndy = (InvokeDynamicInsnNode) ain;
			if (insnIndy.bsmArgs.length >= 2 && insnIndy.bsmArgs[1] instanceof Handle) {
				Handle handle = (Handle) insnIndy.bsmArgs[1];
				Type typeIndyOwner = Type.getObjectType(handle.getOwner());
				Type typeIndyDesc = Type.getMethodType(handle.getDesc());
				// args string
				String argsIndy = "";
				for (Type t : typeIndyDesc.getArgumentTypes()) {
					argsIndy += getTypeStr(t, options) + ", ";
				}
				if (argsIndy.endsWith(", ")) {
					argsIndy = argsIndy.substring(0, argsIndy.length() - 2);
				}
				s += " " + italic(color(colBlueDark, getTypeStr(typeIndyDesc.getReturnType(), options))) + " ";
				s += color(colRedDark, getTypeStr(typeIndyOwner, options)) + "." + escape(handle.getName()) + "(";
				s += color(colTealDark, argsIndy);
				s += ")";
			} else {
				s += " " + italic(color(colGray, "(unknown indy format)"));
			}
			break;
		case AbstractInsnNode.JUMP_INSN:
			JumpInsnNode insnJump = (JumpInsnNode) ain;
			if (insnJump.label != null) {
				s += " " + method.instructions.indexOf(insnJump.label);
			}
			if (options.opcodeShowJumpHelp) {
				//@formatter:off
				String z = "";
				switch (ain.getOpcode()) {
				case IFEQ     :
					z = "($0 == 0 -> offset)";
					break;
				case IFNE     :
					z = "($0 != 0 -> offset)";
					break;
				case IFLE     :
					z = "($0 <= 0 -> offset)";
					break;
				case IFLT     :
					z = "($0 < 0 -> offset)";
					break;
				case IFGE     :
					z = "($0 >= 0 -> offset)";
					break;
				case IFGT     :
					z = "($0 > 0 -> offset)";
					break;
				case IF_ACMPNE:
					z = "($1 != $0 -> offset)";
					break;
				case IF_ACMPEQ:
					z = "($1 == $0 -> offset)";
					break;
				case IF_ICMPEQ:
					z = "($1 == $0 -> offset)";
					break;
				case IF_ICMPNE:
					z = "($1 != $0 -> offset)";
					break;
				case IF_ICMPLE:
					z = "($1 <= $0 -> offset)";
					break;
				case IF_ICMPLT:
					z = "($1 < $0 -> offset)";
					break;
				case IF_ICMPGE:
					z = "($1 >= $0 -> offset)";
					break;
				case IF_ICMPGT:
					z = "($1 > $0 -> offset)";
					break;
				case GOTO     :
					z = "(-> offset)";
					break;
				case JSR      :
					z = "(-> offset, +address)";
					break;
				case IFNULL   :
					z = "($0 == null -> offset)";
					break;
				case IFNONNULL:
					z = "($0 != null -> offset)";
					break;
				}
				//@formatter:on
				s += " " + italic(color(colGray, escape(z)));
			}
			break;
		case AbstractInsnNode.LDC_INSN:
			LdcInsnNode insnLdc = (LdcInsnNode) ain;
			String v = escape(insnLdc.cst.toString());
			boolean extended = false;
			if (v.length() > options.ldcMaxLength) {
				v = v.substring(0, options.ldcMaxLength);
				extended = true;
			}
			String x = italic(color(colGreenDark, v));
			if (insnLdc.cst instanceof String) {
				x = "\"" + x;
				if (extended) {
					x += italic(color(colGray, " (too long)"));
				}
				x += "\"";
			}
			s += " " + x;
			break;
		case AbstractInsnNode.IINC_INSN:
			// Add local variable index to string
			IincInsnNode insnIinc = (IincInsnNode) ain;
			s += " " + insnIinc.var;
			// Add local variable name if possible
			if (insnIinc.var < method.localVariables.size()) {
				LocalVariableNode var = method.localVariables.get(insnIinc.var);
				String varStr = var.name;
				s += color(colBlueDark, italic(" (" + varStr + ") "));
			} else if (insnIinc.var == 0 && !Access.isStatic(method.access)) {
				// If the local variable doesn't have a name, we can assume at
				// index = 0 that it is 'this'.
				s += color(colBlueDark, italic(" (this) "));
			}
			if (insnIinc.incr > 0) {
				s += color(colRedDark, "+" + insnIinc.incr);
			} else {
				s += color(colRedDark, "-" + insnIinc.incr);
			}
			break;
		case AbstractInsnNode.TABLESWITCH_INSN:
			TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
			String o = "";
			for (LabelNode label : insnTableSwitch.labels) {
				o += method.instructions.indexOf(label) + ", ";
			}
			if (o.endsWith(", ")) {
				o = o.substring(0, o.length() - 2);
			}
			int tableDefaultOffset = method.instructions.indexOf(insnTableSwitch.dflt);
			s += color(colGray, " range[" + insnTableSwitch.min + "-" + insnTableSwitch.max + "] offsets:[" + o + "] default:"
					   + tableDefaultOffset);
			break;
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) ain;
			String u = "";
			for (int i = 0; i < insnLookupSwitch.keys.size(); i++) {
				int offset = method.instructions.indexOf(insnLookupSwitch.labels.get(i));
				u += insnLookupSwitch.keys.get(i) + "->" + offset + ", ";
			}
			if (insnLookupSwitch.dflt != null) {
				int offset = method.instructions.indexOf(insnLookupSwitch.dflt);
				u += "default:" + offset;
			}
			if (u.endsWith(", ")) {
				u = u.substring(0, u.length() - 2);
			}
			s += color(colGray, italic(" (" + u + ")"));
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			// MultiANewArrayInsnNode insnArray = (MultiANewArrayInsnNode) ain;
			// TODO
			break;
		case AbstractInsnNode.FRAME:
			FrameNode fn = (FrameNode) ain;
			s = s.replaceAll("F_NEW", OpcodeUtil.opcodeToName(fn.type));
			break;
		case AbstractInsnNode.LABEL:
			if (options.opcodeSimplifyDescriptors) {
				s = s.replace("F_NEW", "");
			} else {
				s += " ";
			}
			s += color(colGray, italic("Label " + bold(list.getLabelName(ain))));
			break;
		case AbstractInsnNode.LINE:
			LineNumberNode line = (LineNumberNode) ain;
			if (options.opcodeSimplifyDescriptors) {
				s = s.replace("F_NEW", "");
			} else {
				s += " ";
			}
			s += color(colGray, italic("line #" + line.line));
			break;

		}
		return s + color(colGray, italic(list.getAppendFor(ainIndex, ain))) + "</html>";
	}
}
