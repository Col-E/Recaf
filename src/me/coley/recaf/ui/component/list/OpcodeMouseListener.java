package me.coley.recaf.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.ReleaseListener;
import me.coley.recaf.ui.component.action.ActionCheckBox;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.internalframe.EditBox;
import me.coley.recaf.ui.component.internalframe.OpcodeCreationBox;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.ui.component.panel.LabelSwitcherPanel;
import me.coley.recaf.ui.component.panel.OpcodeTypeSwitchPanel;
import me.coley.recaf.ui.component.panel.TagTypeSwitchPanel;
import me.coley.recaf.ui.component.table.VariableTable;
import me.coley.recaf.util.Misc;

public class OpcodeMouseListener implements ReleaseListener {
	private final Recaf recaf = Recaf.getInstance();
	private final MethodNode method;
	private final OpcodeList list;
	private final ClassDisplayPanel display;

	public OpcodeMouseListener(MethodNode method, ClassDisplayPanel display, OpcodeList list) {
		this.method = method;
		this.display = display;
		this.list = list;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int button = e.getButton();

		if (list.getSelectedIndices().length == 1) {
			// If not left-click, enforce selection at the given location
			if (button != MouseEvent.BUTTON1) {
				int index = list.locationToIndex(e.getPoint());
				list.setSelectedIndex(index);
			}
		}

		Object value = list.getSelectedValue();
		if (value == null) {
			return;
		}
		if (button == MouseEvent.BUTTON3) {
			createContextMenu((AbstractInsnNode) value, e.getX(), e.getY());
		} else if (button == MouseEvent.BUTTON2) {
			createEdit((AbstractInsnNode) value, e.getX(), e.getY());
		}
	}

	private void createContextMenu(AbstractInsnNode ain, int x, int y) {
		JPopupMenu popup = new JPopupMenu();
		ActionMenuItem itemEdit = new ActionMenuItem("Edit", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createEdit(ain, x, y);
			}
		}));
		popup.add(itemEdit);
		ActionMenuItem itemNewBefore = new ActionMenuItem("New Opcode Before...", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.addWindow(new OpcodeCreationBox(true, list, method, ain));
			}
		}));
		popup.add(itemNewBefore);
		ActionMenuItem itemNewAfter = new ActionMenuItem("New Opcode After...", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.addWindow(new OpcodeCreationBox(false, list, method, ain));
			}
		}));
		popup.add(itemNewAfter);
		ActionMenuItem itemRemove = new ActionMenuItem("Remove", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (recaf.options.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, "You sure you want to delete that opcode?", "Warning",
									   JOptionPane.YES_NO_OPTION);
					if (dialogResult != JOptionPane.YES_OPTION) {
						return;
					}
				}
				DefaultListModel<AbstractInsnNode> model = (DefaultListModel<AbstractInsnNode>) list.getModel();
				int[] descending = new int[list.getSelectedIndices().length];
				if (descending.length > 1) {
					// sort the list and remove highest index objects first
					for (int i = 0; i < descending.length; i++) {
						descending[i] = list.getSelectedIndices()[i];
					}
					Arrays.sort(descending);
					for (int i = 0; i < descending.length; i++) {
						int j = descending[descending.length - 1 - i];
						model.remove(j);
						method.instructions.remove(method.instructions.get(j));
					}
				} else {
					model.remove(list.getSelectedIndex());
					method.instructions.remove(ain);
				}

			}
		}));
		popup.add(itemRemove);
		popup.show(list, x, y);
	}

	private void createEdit(AbstractInsnNode ain, int x, int y) {
		EditBox frame = new EditBox("Opcode: " + OpcodeUtil.opcodeToName(ain.getOpcode()));
		switch (ain.getType()) {
		case AbstractInsnNode.INT_INSN:
			IntInsnNode insnInt = (IntInsnNode) ain;
			frame.add(new LabeledComponent("Value: ", new ActionTextField(insnInt.operand, s -> {
				if (Misc.isInt(s)) {
					insnInt.operand = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.VAR_INSN:
			VarInsnNode insnVar = (VarInsnNode) ain;
			frame.add(new JScrollPane(VariableTable.create(list, method)));
			frame.add(new LabeledComponent("Variable Index: ", new ActionTextField(insnVar.var, s -> {
				if (Misc.isInt(s)) {
					insnVar.var = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.TYPE_INSN:
			TypeInsnNode insnType = (TypeInsnNode) ain;
			frame.add(new LabeledComponent("Type: ", new ActionTextField(insnType.desc, s -> insnType.desc = s)));
			break;
		case AbstractInsnNode.FIELD_INSN:
			FieldInsnNode insnField = (FieldInsnNode) ain;
			frame.add(new LabeledComponent("Owner: ", new ActionTextField(insnField.owner, s -> insnField.owner = s)));
			frame.add(new LabeledComponent("Name: ", new ActionTextField(insnField.name, s -> insnField.name = s)));
			frame.add(new LabeledComponent("Descriptor: ", new ActionTextField(insnField.desc, s -> insnField.desc = s)));
			break;
		case AbstractInsnNode.METHOD_INSN:
			MethodInsnNode insnMethod = (MethodInsnNode) ain;
			frame.add(new LabeledComponent("Owner: ", new ActionTextField(insnMethod.owner, s -> insnMethod.owner = s)));
			frame.add(new LabeledComponent("Name: ", new ActionTextField(insnMethod.name, s -> insnMethod.name = s)));
			frame.add(new LabeledComponent("Descriptor: ", new ActionTextField(insnMethod.desc, s -> insnMethod.desc = s)));
			// ITF is labeled so it's not a centered checkbox.
			frame.add(new LabeledComponent("", new ActionCheckBox("<html>Owner is Interface <i>(ITF)</i></html>", insnMethod.itf,
										   b -> insnMethod.itf = b)));
			break;
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			InvokeDynamicInsnNode insnIndy = (InvokeDynamicInsnNode) ain;
			if (insnIndy.bsmArgs.length > 2 && insnIndy.bsmArgs[1] instanceof Handle) {
				Handle h = (Handle) insnIndy.bsmArgs[1];
				frame.add(new LabeledComponent("Name: ", new ActionTextField(h.getName(), s -> Misc.set(h, "name", s))));
				frame.add(new LabeledComponent("Descriptor: ", new ActionTextField(h.getDesc(), s -> Misc.set(h, "desc", s))));
				frame.add(new LabeledComponent("Owner: ", new ActionTextField(h.getOwner(), s -> Misc.set(h, "owner", s))));
				frame.add(new LabeledComponent("IsInterface: ", new ActionTextField(h.isInterface(), s -> Misc.setBoolean(
												   insnIndy.bsm, "itf", s))));
				frame.add(new TagTypeSwitchPanel(list, h));
			}
			break;
		case AbstractInsnNode.JUMP_INSN:
			JumpInsnNode insnJump = (JumpInsnNode) ain;
			frame.add(new LabelSwitcherPanel(list, method, insnJump.label, l -> insnJump.label = l));
			break;
		case AbstractInsnNode.LDC_INSN:
			LdcInsnNode insnLdc = (LdcInsnNode) ain;
			frame.add(new LabeledComponent("Value: ", new ActionTextField(insnLdc.cst, s -> {
				if (insnLdc.cst instanceof String) {
					insnLdc.cst = s;
				} else if (Misc.isInt(s)) {
					insnLdc.cst = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.IINC_INSN:
			IincInsnNode insnIinc = (IincInsnNode) ain;
			frame.add(new JScrollPane(VariableTable.create(list, method)));
			frame.add(new LabeledComponent("Variable Index: ", new ActionTextField(insnIinc.var, s -> {
				if (Misc.isInt(s)) {
					insnIinc.var = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.TABLESWITCH_INSN:
			TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
			frame.add(new LabeledComponent("Default: ", new LabelSwitcherPanel(list, method, insnTableSwitch.dflt,
										   l -> insnTableSwitch.dflt = l)));
			for (int i = 0; i < insnTableSwitch.labels.size(); i++) {
				final int fi = i;
				LabelNode label = insnTableSwitch.labels.get(i);
				int j = insnTableSwitch.min + i;
				frame.add(new LabeledComponent(j + ": ", new LabelSwitcherPanel(list, method, label, l -> insnTableSwitch.labels
											   .set(fi, l))));
			}
			break;
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) ain;
			frame.add(new LabeledComponent("Default: ", new LabelSwitcherPanel(list, method, insnLookupSwitch.dflt,
										   l -> insnLookupSwitch.dflt = l)));
			for (int i = 0; i < insnLookupSwitch.labels.size(); i++) {
				final int fi = i;
				LabelNode label = insnLookupSwitch.labels.get(i);
				int j = insnLookupSwitch.keys.get(i);
				frame.add(new LabeledComponent(j + ": ", new LabelSwitcherPanel(list, method, label, l -> insnLookupSwitch.labels
											   .set(fi, l))));
			}
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			MultiANewArrayInsnNode insnArray = (MultiANewArrayInsnNode) ain;
			frame.add(new LabeledComponent("Descriptor: ", new ActionTextField(insnArray.desc, s -> insnArray.desc = s)));
			frame.add(new LabeledComponent("Dimensions: ", new ActionTextField(insnArray.dims, s -> {
				if (Misc.isInt(s)) {
					insnArray.dims = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.FRAME:
			// TODO
			FrameNode insnFrame = (FrameNode) ain;
			break;
		case AbstractInsnNode.LINE:
			LineNumberNode insnLine = (LineNumberNode) ain;
			frame.add(new LabeledComponent("Line: ", new ActionTextField(insnLine.line, s -> {
				if (Misc.isInt(s)) {
					insnLine.line = Integer.parseInt(s);
				}
			})));
			frame.add(new LabeledComponent("Start: ", new LabelSwitcherPanel(list, method, insnLine.start,
										   l -> insnLine.start = l)));
			break;
		}
		OpcodeTypeSwitchPanel opSelector = new OpcodeTypeSwitchPanel(list, ain);
		if (opSelector.getOptionCount() > 0) {
			frame.add(opSelector);
		}
		// Tell the user the empty box is intentional.
		if (!frame.hasContent()) {
			JLabel nothing = new JLabel("Nothing to edit");
			nothing.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			frame.add(nothing);
		}
		display.addWindow(frame);
		// TODO: Reliable way of positioning frame in a reasonable place (near
		// the mouse)
		// frame.setLocation(x, y);
		frame.setVisible(true);
	}
}
