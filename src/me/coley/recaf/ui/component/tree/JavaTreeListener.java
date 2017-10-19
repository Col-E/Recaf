package me.coley.recaf.ui.component.tree;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.component.list.OpcodeList;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;

/**
 * Selection/Mouse listener for double-click actions related to the selected
 * ClassNode.
 *
 * @author Matt
 */
public class JavaTreeListener implements TreeSelectionListener, MouseListener, TreeExpansionListener {
	private final Recaf recaf = Recaf.INSTANCE;
	private ASMTreeNode lastSelected;
	private JTree tree;

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		tree = (JTree) e.getSource();
	}

	@Override
	public void treeExpanded(TreeExpansionEvent e) {
		// Reset selection, prevents expansion from opening the contained value.
		lastSelected = null;
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent e) {
		// Reset selection, prevents collapsing from opening the contained
		// value.
		lastSelected = null;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// Skip if nothing selected (tree null) or not a left click
		if (tree == null || e.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		// Skip if the press did not occur in the selection's bounds
		if (tree.getSelectionPath() == null || !tree.getPathBounds(tree.getSelectionPath()).contains(e.getX(), e.getY())) {
			return;
		}
		// Update selection, open if double clicked.
		Object selection = tree.getLastSelectedPathComponent();
		if (selection != null && selection instanceof ASMTreeNode) {
			ASMTreeNode node = (ASMTreeNode) selection;
			ClassNode cn = node.getNode();
			if (node == lastSelected && cn != null) {
				recaf.selectClass(cn);
				// Open method opcodes if applicable.
				//
				// TODO: Clean up.
				// This is incredibly hacky and needs refactoring later so 
				// interactive search and other things can be extended upon.
				if (node instanceof ASMInsnTreeNode) {
					ASMInsnTreeNode insn = (ASMInsnTreeNode) node;
					JComponent child = recaf.gui.getTabs().getChild(cn.name);
					if (child instanceof JScrollPane) {
						ClassDisplayPanel display = (ClassDisplayPanel) ((JScrollPane) child).getViewport().getView();
						OpcodeList list = display.openOpcodes(insn.getMethod()).list;
						list.setSelectedValue(insn.getInsn(), true);
					}
				}
			}
			lastSelected = node;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
