package me.coley.recaf.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JPopupMenu;

import org.objectweb.asm.tree.*;

import me.coley.recaf.Program;
import me.coley.recaf.ui.component.ReleaseListener;
import me.coley.recaf.ui.component.action.ActionMenuItem;

public class OpcodeMouseListener implements ReleaseListener {
	private final MethodNode method;
	private final Program callback;
	private final JList<AbstractInsnNode> list;

	public OpcodeMouseListener(MethodNode method, Program callback, JList<AbstractInsnNode> list) {
		this.method = method;
		this.callback = callback;
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
		ActionMenuItem itemAccess = new ActionMenuItem("Edit", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (ain.getType()) {
				case AbstractInsnNode.INT_INSN:
					IntInsnNode insnInt = (IntInsnNode) ain;
					break;
				case AbstractInsnNode.VAR_INSN:
					VarInsnNode insnVar = (VarInsnNode) ain;
					break;
				case AbstractInsnNode.TYPE_INSN:
					TypeInsnNode insnType = (TypeInsnNode) ain;
					break;
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode insnField = (FieldInsnNode) ain;
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode insnMethod = (MethodInsnNode) ain;
					break;
				case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
					break;
				case AbstractInsnNode.JUMP_INSN:
					JumpInsnNode insnJump = (JumpInsnNode) ain;
					break;
				case AbstractInsnNode.LDC_INSN:
					LdcInsnNode insnLdc = (LdcInsnNode) ain;
					break;
				case AbstractInsnNode.IINC_INSN:
					IincInsnNode insnIinc = (IincInsnNode) ain;
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
					break;
				case AbstractInsnNode.LINE:
					break;
				}
			}
		}));
	}

}
