package me.coley.recaf.ui.component.tree;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import me.coley.recaf.Program;

public class FileTreeListener implements TreeSelectionListener, MouseListener {
	private static final long CLICK_DELAY = 15;
	private final Program callback;
	private ASMTreeNode lastSelected;
	private long selectionTime;

	public FileTreeListener(Program callback) {
		this.callback = callback;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		Object component = e.getPath().getLastPathComponent();
		if (component instanceof ASMTreeNode) {
			ASMTreeNode node = (ASMTreeNode) component;
			selectionTime = System.currentTimeMillis();
			lastSelected = node;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (lastSelected != null && lastSelected.getNode() != null) {
			long now = System.currentTimeMillis();
			if (now - selectionTime > CLICK_DELAY) {
				callback.selectClass(lastSelected.getNode());
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}
}
