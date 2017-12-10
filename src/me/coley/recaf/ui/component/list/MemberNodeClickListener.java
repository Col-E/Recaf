package me.coley.recaf.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckMethodAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Access;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.ui.component.panel.SearchPanel.SearchType;

/**
 * Click listener for ClassNode members <i>(Fields / Methods)</i>. Used for
 * generation context menus and such.
 *
 * @author Matt
 */
public class MemberNodeClickListener extends MouseAdapter {
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
		// Middle-click to open editor
		// Right-click to open context menu
		if (value != null && button == MouseEvent.BUTTON2) {
			// TODO: Allow users to choose custom middle-click actions
			if (value instanceof FieldNode) {
				display.openDefinition((FieldNode) value);
			} else if (value instanceof MethodNode) {
				display.openOpcodes((MethodNode) value);
			}
		} else if (button == MouseEvent.BUTTON3) {
			createContextMenu(value, e.getX(), e.getY(), isMethodList(list));
		}
	}

	private void createContextMenu(Object value, int x, int y, boolean isMethod) {
		JPopupMenu popup = new JPopupMenu();
		// Field/Method only actions
		if (value != null && isMethod) {
			MethodNode mn = (MethodNode) value;
			if (!Access.isAbstract(mn.access)) {
				popup.add(new ActionMenuItem("Show Decompilation", () -> display.decompile(node, mn)));
				if (mn.localVariables != null) {
					popup.add(new ActionMenuItem("Show Variables", () -> display.openVariables(mn)));
				}
				popup.add(new ActionMenuItem("Edit Opcodes", () -> display.openOpcodes(mn)));
				popup.add(new ActionMenuItem("Edit Try-Catch Blocks", () -> display.openTryCatchBlocks(mn)));
				ActionMenuItem itemVerify = new ActionMenuItem("Verify code", (new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							Printer printer = new Textifier();
							TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(printer);
							CheckMethodAdapter check = new CheckMethodAdapter(mn.access, mn.name, mn.desc, traceMethodVisitor,
									new HashMap<>());
							mn.accept(check);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							PrintWriter pw = new PrintWriter(baos, false);
							printer.print(pw);
							pw.close();
							String content = new String(baos.toByteArray());
							content = "Verified bytecode: \n\n" + content;
							// TODO: Format / stylize bytecode output
							Recaf.INSTANCE.ui.openMessage("Verification: " + mn.name, content);
						} catch (Exception ee) {
							Recaf.INSTANCE.ui.openException(ee);							
						}
					}
				}));
				popup.add(itemVerify);
			}
		}
		// General actions
		ActionMenuItem itemDefine = new ActionMenuItem("Edit Definition", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.openDefinition(value);
			}
		}));
		ActionMenuItem itemSearch = new ActionMenuItem("Find references", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (value instanceof FieldNode) {
						FieldNode fn = (FieldNode) value;
						Recaf.INSTANCE.ui.openSearch(SearchType.REFERENCES, new String[] { node.name, fn.name, fn.desc, "true" });
					} else if (value instanceof MethodNode) {
						MethodNode mn = (MethodNode) value;
						Recaf.INSTANCE.ui.openSearch(SearchType.REFERENCES, new String[] { node.name, mn.name, mn.desc, "true" });
					}
				} catch (Exception e1) {
					display.exception(e1);
				}
			}
		}));
		ActionMenuItem itemNewMember = new ActionMenuItem("Add new member", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.openNewMember(isMethod);
			}
		}));
		ActionMenuItem itemDeletThis = new ActionMenuItem("Remove", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Show confirmation
				if (Recaf.INSTANCE.configs.ui.confirmDeletions) {
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
		if (value != null) {
			popup.add(itemDefine);
			popup.add(itemSearch);
			popup.add(itemDeletThis);
		}
		popup.add(itemNewMember);
		// Display popup
		popup.show(list, x, y);
	}

	private boolean isMethodList(JList<?> l) {
		return l.equals(display.getMethods());
	}
}