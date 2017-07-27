package me.coley.edit.ui.component.list;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.ui.component.AccessBox;
import me.coley.edit.ui.component.ClassDisplayPanel;

public class NodeClickListener implements MouseListener {
	private final ClassDisplayPanel display;
	private final JList<?> list;

	public NodeClickListener(ClassDisplayPanel display, JList<?> list) {
		this.list = list;
		this.display = display;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		Object value = list.getSelectedValue();
		// Middle-click to open editor
		// Right-click to open context menu
		if (e.getButton() == MouseEvent.BUTTON2) {
			open(value);
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			context(value, e.getX(), e.getY());
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	private void context(Object value, int x, int y) {
		Popup popup = new Popup();
		JMenuItem itemAccess = new JMenuItem("Edit Access");
		itemAccess.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// public AccessBox(String title, int init, Consumer<Integer>
				// action)
				try {
					if (value instanceof FieldNode) {
						FieldNode fn = (FieldNode) value;
						display.addWindow(new AccessBox(AccessBox.TITLE_FIELD + ": " + fn.name, fn.access,
								acc -> fn.access = acc));
					} else if (value instanceof MethodNode) {
						MethodNode mn = (MethodNode) value;
						display.addWindow(new AccessBox(AccessBox.TITLE_METHOD + ": " + mn.name, mn.access,
								acc -> mn.access = acc));
					}
				} catch (Exception e1) {
					display.exception(e1);
				}
			}
		});
		popup.add(itemAccess);
		if (value instanceof FieldNode) {
			FieldNode fn = (FieldNode) value;
			if (fn.desc.length() == 1 || fn.desc.equals("Ljava/lang/String;")) {
				JMenuItem itemValue = new JMenuItem("Edit DefaultValue");
				itemValue.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO: Open default value editor
					}
				});
				popup.add(itemValue);
			}
		} else {
			JMenuItem itemOpcodes = new JMenuItem("Edit Opcodes");
			itemOpcodes.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					open(value);
				}
			});
			popup.add(itemOpcodes);
		}
		popup.show(list, x, y);
	}

	@SuppressWarnings("serial")
	public class Popup extends JPopupMenu {

	}

	private void open(Object value) {
		JPanel content = new JPanel();
		if (value instanceof FieldNode) {

		} else if (value instanceof MethodNode) {

		}
		JInternalFrame frameMethods = new JInternalFrame("Methods");
		frameMethods.setResizable(true);
		frameMethods.setIconifiable(true);
		frameMethods.setBounds(445, 11, 180, 120);
		frameMethods.setVisible(true);
		frameMethods.setLayout(new BorderLayout());
		frameMethods.add(content, BorderLayout.CENTER);
	}
}
