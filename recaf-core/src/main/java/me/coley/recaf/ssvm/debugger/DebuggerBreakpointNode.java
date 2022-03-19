package me.coley.recaf.ssvm.debugger;

import dev.xdark.ssvm.asm.DelegatingInsnNode;
import dev.xdark.ssvm.asm.VMOpcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Debugger breakpoint.
 *
 * @param <I>
 * 		Instruction type to break at.
 *
 * @author xDark
 */
public class DebuggerBreakpointNode<I extends AbstractInsnNode> extends DelegatingInsnNode<I> {

	private final BreakpointObserver observer;

	/**
	 * @param delegate
	 * 		Backing instruction.
	 * @param observer
	 * 		Breakpoint observer.
	 */
	public DebuggerBreakpointNode(I delegate, BreakpointObserver observer) {
		super(delegate, VMOpcodes.LDC + 1);
		this.observer = observer;
	}

	/**
	 * @return breakpoint observer.
	 */
	public BreakpointObserver getObserver() {
		return observer;
	}
}
