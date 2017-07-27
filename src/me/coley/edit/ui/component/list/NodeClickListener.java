package me.coley.edit.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.ui.component.ClassDisplayPanel;
import me.coley.edit.ui.component.internalframe.AccessBox;
import me.coley.edit.ui.component.internalframe.DefaultValueBox;
import me.coley.edit.ui.component.internalframe.OpcodesBox;
import me.coley.edit.util.Misc;

public class NodeClickListener implements MouseListener {
	private final ClassDisplayPanel display;
	private final JList<?> list;

	public NodeClickListener(ClassDisplayPanel display, JList<?> list) {
		this.list = list;
		this.display = display;
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
		// Middle-click to open editor
		// Right-click to open context menu
		if (button == MouseEvent.BUTTON2) {
			// TODO: Allow users to choose custom middle-click actions
			if (value instanceof FieldNode) {
				openDefaultValue((FieldNode)value);
			} else if (value instanceof MethodNode) {
				openOpcodes((MethodNode) value);
			}
		} else if (button == MouseEvent.BUTTON3) {
			createContextMenu(value, e.getX(), e.getY());
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

	private void createContextMenu(Object value, int x, int y) {
		JPopupMenu popup = new JPopupMenu();
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
						openDefaultValue((FieldNode) value);
					}

				});
				popup.add(itemValue);
			}
		} else {
			JMenuItem itemOpcodes = new JMenuItem("Edit Opcodes");
			itemOpcodes.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					openOpcodes((MethodNode) value);
				}
			});
			popup.add(itemOpcodes);
		}
		popup.show(list, x, y);
	}

	private void openDefaultValue(FieldNode fn) {
		try {
			display.addWindow(new DefaultValueBox(fn.name, fn.value, value -> {
				if (fn.desc.length() == 1) {
					// Convert string value to int.
					if (Misc.isInt(value)) {
						fn.value = Integer.parseInt(value);
					}
				} else {
					// Just set value as string
					fn.value = value;
				}
			}));
		} catch (Exception e) {
			display.exception(e);
		}
	}

	private void openOpcodes(MethodNode mn) {
		try {
			display.addWindow(new OpcodesBox(mn));
		} catch (Exception e) {
			display.exception(e);
		}
	}
}
