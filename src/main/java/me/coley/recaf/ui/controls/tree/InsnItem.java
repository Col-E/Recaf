package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.parse.bytecode.Disassembler;
import me.coley.recaf.util.InsnUtil;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Item to represent instructions.
 *
 * @author Matt
 */
public class InsnItem extends DirectoryItem {
	private final AbstractInsnNode insn;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param insn
	 * 		The instruction.
	 */
	public InsnItem(JavaResource resource, AbstractInsnNode insn) {
		super(resource, Disassembler.insn(insn));
		this.insn = insn;
	}

	/**
	 * @return Instruction value.
	 */
	public AbstractInsnNode getInsn() {
		return insn;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof InsnItem) {
			InsnItem c = (InsnItem) o;
			return Integer.compare(InsnUtil.index(insn), InsnUtil.index(c.insn));
		}
		return 1;
	}
}