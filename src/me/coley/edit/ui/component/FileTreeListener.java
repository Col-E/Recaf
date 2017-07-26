package me.coley.edit.ui.component;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import me.coley.edit.Program;

public class FileTreeListener implements  TreeSelectionListener {
	private final Program callback;
	private ASMTreeNode lastSelected;

	public FileTreeListener(Program callback) {
		this.callback = callback;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		Object component = e.getPath().getLastPathComponent();
		if (component instanceof ASMTreeNode) {
			ASMTreeNode node = (ASMTreeNode) component;
			if (lastSelected == node) {
				callback.selectClass(node.getNode());
			}
			lastSelected = node;
		}
	}
}
