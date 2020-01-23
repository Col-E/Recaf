package me.xdark.recaf.jvm;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.reflect.Modifier;
import java.util.List;

public final class Method extends Member {
	private final InsnList instructions;
	private final int maxStack;
	private final int maxLocals;
	private final List<TryCatchBlockNode> tryCatchBlockNodes;
	private final boolean nonQStatic;

	protected Method(VirtualMachine vm, String name, String descriptor, Class declaringClass, int modifiers, boolean synthetic, InsnList instructions, int maxStack, int maxLocals, List<TryCatchBlockNode> tryCatchBlockNodes) {
		super(vm, name, descriptor, declaringClass, modifiers, synthetic);
		this.instructions = instructions;
		this.nonQStatic = !Modifier.isStatic(modifiers);
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
		this.tryCatchBlockNodes = tryCatchBlockNodes;
	}

	public Object invoke(Object instance, Object... args) throws VMException {
		if (nonQStatic && instance == null) {
			throw new VMException("Attempted to invoke non-static method with null instance");
		}
		ExecutionContext<? extends Object> ctx = new ExecutionContext<>(maxStack, maxLocals, instructions, tryCatchBlockNodes);
		int load = 0;
		if (nonQStatic) {
			ctx.store(load++, instance);
		}
		for (int i = load - 1; i < args.length; i++) {
			ctx.store(i, args[i]);
		}
		return ctx.run();
	}
}
