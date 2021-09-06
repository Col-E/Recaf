package me.coley.recaf.ui.control.tree.item;

/**
 * Item for representing method instructions in the workspace.
 *
 * @author Matt Coley
 */
public class InsnItem extends BaseTreeItem {
	private final int opcode;

	public InsnItem(int opcode) {
		this.opcode = opcode;
		init();
	}


	@Override
	protected BaseTreeValue createTreeValue() {
		// TODO: Replace with instruction name, and more data when possible
		return new BaseTreeValue(this, String.valueOf(opcode), false);
	}
}
