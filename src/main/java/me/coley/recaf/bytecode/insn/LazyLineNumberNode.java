package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

/**
 * Extension of LineNumberNode used by the assembler to be replaced with a dummy label/lineinsn.
 *
 * @author Matt
 */
public class LazyLineNumberNode extends LineNumberNode {
	public LazyLineNumberNode(int line) {
		super(line, null);
	}

	/**
	 * Update the instructions to replace lazy-line nodes with proper lines by inserting
	 * start-labels before the lazy instructions.
	 *
	 * @param method
	 * 		Method with instructions to update.
	 *
	 * @return Updated instructions.
	 */
	public static void clean(MethodNode method) {
		for(AbstractInsnNode ain : method.instructions.toArray()) {
			if(ain instanceof LazyLineNumberNode) {
				LazyLineNumberNode llnn = (LazyLineNumberNode) ain;
				LabelNode ln = new LabelNode();
				method.instructions.insertBefore(ain, ln);
				method.instructions.set(ain, new LineNumberNode(llnn.line, ln));
			}
		}
	}
}