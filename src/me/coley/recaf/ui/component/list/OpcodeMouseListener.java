package me.coley.recaf.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JPopupMenu;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

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

	private void createContextMenu(AbstractInsnNode value, int x, int y) {
		JPopupMenu popup = new JPopupMenu();
		ActionMenuItem itemAccess = new ActionMenuItem("Edit", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
			}
		}));
	}

}
