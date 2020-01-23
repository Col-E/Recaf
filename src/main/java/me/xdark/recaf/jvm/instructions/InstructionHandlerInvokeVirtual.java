package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.Method;
import org.objectweb.asm.tree.MethodInsnNode;

public final class InstructionHandlerInvokeVirtual extends InvocationInstructionHandler {
	@Override
	public void process(MethodInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Method method = findMethod(instruction, ctx);
		Object v;
		int count = method.getArgumentsCount();
		if (count == 0) {
			v = method.invoke(ctx.pop());
		} else {
			Object[] array = new Object[count];
			for (int i = 0; i < count; array[i++] = ctx.pop()) ;
			v = method.invoke(ctx.pop(), array);
		}
		if (method.isReturnResult()) {
			ctx.push(v);
		}
	}
}
