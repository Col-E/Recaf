package me.coley.recaf.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Program;
import me.coley.recaf.asm.Access;
import me.coley.recaf.ui.component.ReleaseListener;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.internalframe.AccessBox;
import me.coley.recaf.ui.component.internalframe.DefaultValueBox;
import me.coley.recaf.ui.component.internalframe.ExceptionsListBox;
import me.coley.recaf.ui.component.internalframe.OpcodeListBox;
import me.coley.recaf.ui.component.internalframe.TryCatchBox;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.util.Misc;

/**
 * Click listener for ClassNode members <i>(Fields / Methods)</i>. Used for
 * generation context menus and such.
 * 
 * @author Matt
 */
public class MemberNodeClickListener implements ReleaseListener {
	private final Program callback = Program.getInstance();
	private final ClassDisplayPanel display;
	private final JList<?> list;
	private final ClassNode node;

	public MemberNodeClickListener(ClassDisplayPanel display, ClassNode node, JList<?> list) {
		this.list = list;
		this.node = node;
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
				openDefaultValue((FieldNode) value);
			} else if (value instanceof MethodNode) {
				openOpcodes((MethodNode) value);
			}
		} else if (button == MouseEvent.BUTTON3) {
			createContextMenu(value, e.getX(), e.getY());
		}
	}

	private void createContextMenu(Object value, int x, int y) {
		JPopupMenu popup = new JPopupMenu();
		// Field/Method only actions
		if (value instanceof FieldNode) {
			FieldNode fn = (FieldNode) value;
			if (fn.desc.length() == 1 || fn.desc.equals("Ljava/lang/String;")) {
				popup.add(new ActionMenuItem("Edit DefaultValue", () -> openDefaultValue((FieldNode) value)));
			}
		} else {
			MethodNode mn = (MethodNode) value;
			if (!Access.isAbstract(mn.access)) {
				popup.add(new ActionMenuItem("Edit Opcodes", () -> openOpcodes(mn)));
			}
			if (mn.exceptions != null) {
				popup.add(new ActionMenuItem("Edit Exceptions", () -> openExceptions(mn)));
			}if (mn.tryCatchBlocks != null && !mn.tryCatchBlocks.isEmpty()) {
				popup.add(new ActionMenuItem("Edit Try-Catch Blocks", () -> openTryCatchBlocks(mn)));
			}
		}
		// General actions
		ActionMenuItem itemAccess = new ActionMenuItem("Edit Access", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (value instanceof FieldNode) {
						FieldNode fn = (FieldNode) value;
						display.addWindow(new AccessBox(fn, list));
					} else if (value instanceof MethodNode) {
						MethodNode mn = (MethodNode) value;
						display.addWindow(new AccessBox(mn, list));
					}
				} catch (Exception e1) {
					display.exception(e1);
				}
			}
		}));
		ActionMenuItem itemDeletThis = new ActionMenuItem("Remove", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Show confirmation
				if (callback.options.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, "You sure you want to delete that member?", "Warning",
							JOptionPane.YES_NO_OPTION);
					if (dialogResult != JOptionPane.YES_OPTION) {
						return;
					}
				}
				// remove from class node
				if (value instanceof FieldNode) {
					node.fields.remove(value);
				} else {
					node.methods.remove(value);
				}
				// remove from list
				DefaultListModel<?> model = (DefaultListModel<?>) list.getModel();
				model.removeElement(value);

			}
		}));
		popup.add(itemAccess);
		popup.add(itemDeletThis);
		// Display popup
		popup.show(list, x, y);
	}

	/**
	 * Open window for modifying default value of a field.
	 * 
	 * @param field
	 */
	private void openDefaultValue(FieldNode field) {
		try {
			display.addWindow(new DefaultValueBox(field.name, field.value, value -> {
				if (field.desc.length() == 1) {
					// Convert string value to int.
					if (Misc.isInt(value)) {
						field.value = Integer.parseInt(value);
					}
				} else {
					// Just set value as string
					field.value = value;
				}
			}));
		} catch (Exception e) {
			display.exception(e);
		}
	}

	/**
	 * Open window for modifying method opcodes.
	 * 
	 * @param method
	 */
	private void openOpcodes(MethodNode method) {
		try {
			display.addWindow(new OpcodeListBox(display, method));
		} catch (Exception e) {
			display.exception(e);
		}
	}

	/**
	 * Open window for modifying method exceptions.
	 * 
	 * @param method
	 */
	private void openExceptions(MethodNode method) {
		try {
			display.addWindow(new ExceptionsListBox(method));
		} catch (Exception e) {
			display.exception(e);
		}
	}
	
	/**
	 * Open window for modifying method try-catch blocks.
	 * 
	 * @param method
	 */
	private void openTryCatchBlocks(MethodNode method) {
		try {
			display.addWindow(new TryCatchBox(method));
		} catch (Exception e) {
			display.exception(e);
		}
	}
}
