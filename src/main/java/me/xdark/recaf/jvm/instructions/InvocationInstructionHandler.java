package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.Class;
import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import me.xdark.recaf.jvm.Method;
import me.xdark.recaf.jvm.VMException;
import me.xdark.recaf.jvm.classloading.ClassLoader;
import org.objectweb.asm.tree.MethodInsnNode;

abstract class InvocationInstructionHandler implements InstructionHandler<MethodInsnNode> {
	protected Method findMethod(MethodInsnNode instruction, ExecutionContext ctx) throws VMException {
		ClassLoader loader = ctx.getClassLoader();
		Class c = loader.loadClass(instruction.owner);
		Method method = c.getMethod(instruction.name, instruction.desc);
		if (method == null) {
			throw new VMException("No such method: " + instruction.name + instruction.desc);
		}
		return method;
	}
}
