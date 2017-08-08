package me.coley.recaf.ui.component.tree;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import me.coley.recaf.Program;

/**
 * Selection/Mouse listener for double-click actions related to the selected
 * ClassNode.
 * 
 * @author Matt
 */
public class JavaTreeListener implements TreeSelectionListener, MouseListener {
	private static final long CLICK_DELAY = 15;
	private final Program callback = Program.getInstance();
	private ASMTreeNode lastSelected;
	private long selectionTime;

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		// Set last selected node and the last time the selction changed.
		Object component = e.getPath().getLastPathComponent();
		if (component instanceof ASMTreeNode) {
			ASMTreeNode node = (ASMTreeNode) component;
			selectionTime = System.currentTimeMillis();
			lastSelected = node;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// If the last selection is not null and the selction contains a class
		// node, check if some time has elapsed from the initial selction time.
		// If so, select it. Basically a double-click hack.
		if (lastSelected != null && lastSelected.getNode() != null) {
			long now = System.currentTimeMillis();
			if (now - selectionTime > CLICK_DELAY) {
				callback.selectClass(lastSelected.getNode());
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
