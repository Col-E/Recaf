package me.coley.recaf.ssvm.debugger;

import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Breakpoint processor.
 *
 * @author xDark
 */
public class BreakpointInstructionProcessor implements InstructionProcessor<DebuggerBreakpointNode<?>> {
	@Override
	public Result execute(DebuggerBreakpointNode<?> insn, ExecutionContext ctx) {
		insn.getObserver().breakpointReached(ctx);
		AbstractInsnNode backing = insn.getDelegate();
		return ctx.getVM().getInterface().getProcessor(backing).execute(backing, ctx);
	}
}
