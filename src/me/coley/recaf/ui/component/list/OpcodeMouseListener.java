package me.coley.recaf.ui.component.list;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

import org.objectweb.asm.tree.*;

import me.coley.recaf.Program;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.component.ClassDisplayPanel;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.ReleaseListener;
import me.coley.recaf.ui.component.VariableTable;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.util.Misc;

public class OpcodeMouseListener implements ReleaseListener {
	private final MethodNode method;
	// TODO: Use or delete?
	@SuppressWarnings("unused")
	private final Program callback;
	private final JList<AbstractInsnNode> list;
	private final ClassDisplayPanel display;

	public OpcodeMouseListener(MethodNode method, Program callback, ClassDisplayPanel display, JList<AbstractInsnNode> list) {
		this.method = method;
		this.callback = callback;
		this.display = display;
		this.list = list;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int button = e.getButton();
		// If not left-click, enforce selection at the given location
		if (button != MouseEvent.BUTTON1) {
			int index = list.locationToIndex(e.getPoint());
			list.setSelectedIndex(index);
		}
		Object value = list.getSelectedValue();
		if (value == null) {
			return;
		}
		if (button == MouseEvent.BUTTON3) {
			createContextMenu((AbstractInsnNode) value, e.getX(), e.getY());
		}
	}

	private void createContextMenu(AbstractInsnNode ain, int x, int y) {
		JPopupMenu popup = new JPopupMenu();
		ActionMenuItem itemEdit = new ActionMenuItem("Edit", (new ActionListener() {
			@SuppressWarnings({ "unused" })
			@Override
			public void actionPerformed(ActionEvent e) {
				XFrame frame = new XFrame("Opcode: " + OpcodeUtil.opcodeToName(ain.getOpcode()));
				switch (ain.getType()) {
				case AbstractInsnNode.INT_INSN:
					IntInsnNode insnInt = (IntInsnNode) ain;
					frame.add(new LabeledComponent("Value:", new ActionTextField(insnInt.operand, s -> {
						if (Misc.isInt(s)) {
							insnInt.operand = Integer.parseInt(s);
						}
					})));
					break;
				case AbstractInsnNode.VAR_INSN:
					VarInsnNode insnVar = (VarInsnNode) ain;
					frame.add(new JScrollPane(VariableTable.create(method)));
					frame.add(new LabeledComponent("Variable Index:", new ActionTextField(insnVar.var, s -> {
						if (Misc.isInt(s)) {
							insnVar.var = Integer.parseInt(s);
						}
					})));
					break;
				case AbstractInsnNode.TYPE_INSN:
					TypeInsnNode insnType = (TypeInsnNode) ain;
					frame.add(new LabeledComponent("Type:", new ActionTextField(insnType.desc, s -> insnType.desc = s)));
					break;
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode insnField = (FieldInsnNode) ain;
					frame.add(new LabeledComponent("Owner:", new ActionTextField(insnField.owner, s -> insnField.owner = s)));
					frame.add(new LabeledComponent("Name:", new ActionTextField(insnField.name, s -> insnField.name = s)));
					frame.add(new LabeledComponent("Descriptor:", new ActionTextField(insnField.desc, s -> insnField.desc = s)));
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode insnMethod = (MethodInsnNode) ain;
					frame.add(new LabeledComponent("Owner:", new ActionTextField(insnMethod.owner, s -> insnMethod.owner = s)));
					frame.add(new LabeledComponent("Name:", new ActionTextField(insnMethod.name, s -> insnMethod.name = s)));
					frame.add(new LabeledComponent("Descriptor:", new ActionTextField(insnMethod.desc,
							s -> insnMethod.desc = s)));
					// TODO: Add ITF?
					break;
				case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
					// TODO:
					break;
				case AbstractInsnNode.JUMP_INSN:
					JumpInsnNode insnJump = (JumpInsnNode) ain;
					break;
				case AbstractInsnNode.LDC_INSN:
					LdcInsnNode insnLdc = (LdcInsnNode) ain;
					frame.add(new LabeledComponent("Value:", new ActionTextField(insnLdc.cst, s -> {
						if (insnLdc.cst instanceof String) {
							insnLdc.cst = s;
						} else if (Misc.isInt(s)) {
							insnLdc.cst = Integer.parseInt(s);
						}
					})));
					break;
				case AbstractInsnNode.IINC_INSN:
					IincInsnNode insnIinc = (IincInsnNode) ain;
					frame.add(new JScrollPane(VariableTable.create(method)));
					frame.add(new LabeledComponent("Variable Index:", new ActionTextField(insnIinc.var, s -> {
						if (Misc.isInt(s)) {
							insnIinc.var = Integer.parseInt(s);
						}
					})));
					break;
				case AbstractInsnNode.TABLESWITCH_INSN:
					TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
					break;
				case AbstractInsnNode.LOOKUPSWITCH_INSN:
					LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) ain;
					break;
				case AbstractInsnNode.MULTIANEWARRAY_INSN:
					MultiANewArrayInsnNode insnArray = (MultiANewArrayInsnNode) ain;
					break;
				case AbstractInsnNode.FRAME:
					// TODO
					break;
				case AbstractInsnNode.LINE:
					LineNumberNode insnLine = (LineNumberNode) ain;
					frame.add(new LabeledComponent("Line:", new ActionTextField(insnLine.line, s -> {
						if (Misc.isInt(s)) {
							insnLine.line = Integer.parseInt(s);
						}
					})));
					break;
				}
				OpcodeTypeSwitchPanel opSelector = new OpcodeTypeSwitchPanel(list, ain);
				if (opSelector.getOptionCount() > 0) {
					frame.add(opSelector);
				}
				// Tell the user the empty box is intentional.
				if (!frame.hasContent) {
					JLabel nothing = new JLabel("Nothing to edit");
					nothing.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
					frame.add(nothing);
				}
				display.addWindow(frame);
				frame.setVisible(true);

			}
		}));
		popup.add(itemEdit);
		ActionMenuItem itemRemove = new ActionMenuItem("Remove", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultListModel<AbstractInsnNode> model = (DefaultListModel<AbstractInsnNode>) list.getModel();
				model.remove(list.getSelectedIndex());
				method.instructions.remove(ain);
			}
		}));
		popup.add(itemRemove);
		popup.show(list, x, y);
	}

	@SuppressWarnings("serial")
	public static class XFrame extends JInternalFrame {
		private boolean hasContent;

		public XFrame(String title) {
			super(title);
			setResizable(true);
			setIconifiable(true);
			setClosable(true);
			setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible) {
				pack();
				setMinimumSize(getSize());
			}
		}

		@Override
		public Component add(Component comp) {
			// Don't count internal swing components
			if (!(comp instanceof BasicInternalFrameTitlePane)) {
				hasContent = true;
			}
			return super.add(comp);
		}

		public boolean hasContent() {
			return hasContent;
		}

	}

}
