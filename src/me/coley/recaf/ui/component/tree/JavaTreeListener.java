package me.coley.recaf.ui.component.tree;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import me.coley.recaf.Recaf;

/**
 * Selection/Mouse listener for double-click actions related to the selected
 * ClassNode.
 *
 * @author Matt
 */
public class JavaTreeListener implements TreeSelectionListener, MouseListener, TreeExpansionListener {
	private final Recaf recaf = Recaf.getInstance();
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
		if (selection instanceof ASMTreeNode) {
			ASMTreeNode node = (ASMTreeNode) selection;
			if (node != null && node == lastSelected && node.getNode() != null) {
				recaf.selectClass(node.getNode());
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
