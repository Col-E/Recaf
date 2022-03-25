package me.coley.recaf.ssvm.debugger;

import dev.xdark.ssvm.execution.ExecutionContext;

/**
 * Breakpoint observer.
 *
 * @author xDark
 */
@FunctionalInterface
public interface BreakpointObserver {
	/**
	 * Called when breakpoint is hit.
	 *
	 * @param ctx
	 * 		Currently executing method.
	 */
	void breakpointReached(ExecutionContext ctx);
}
