package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Item for representing method instructions in the workspace.
 *
 * @author Matt Coley
 */
public class InsnItem extends BaseTreeItem {
	private final AbstractInstruction instruction;

	/**
	 * @param instruction
	 * 		Instruction of item.
	 */
	public InsnItem(AbstractInstruction instruction) {
		this.instruction = instruction;
		init();
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, instruction.print(), false) {
			@Override
			protected void validatePathElement(String pathElementValue) {
				// no-op
			}
		};
	}
}
