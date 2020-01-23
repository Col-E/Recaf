package me.xdark.recaf.jvm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.reflect.Modifier;
import java.util.List;

public final class Method extends Member {
	private final VirtualMachine vm;
	final InsnList instructions;
	final int maxStack;
	final int maxLocals;
	final List<TryCatchBlockNode> tryCatchBlockNodes;
	final boolean nonStatic;
	private final boolean returnResult;
	private final int argumentsCount;

	protected Method(VirtualMachine vm, String name, String descriptor, Class declaringClass, int modifiers, boolean synthetic, InsnList instructions, int maxStack, int maxLocals, List<TryCatchBlockNode> tryCatchBlockNodes) {
		super(name, descriptor, declaringClass, modifiers, synthetic);
		this.vm = vm;
		this.instructions = instructions;
		this.nonStatic = !Modifier.isStatic(modifiers);
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
		this.tryCatchBlockNodes = tryCatchBlockNodes;
		Type type = Type.getMethodType(descriptor);
		this.returnResult = type.getReturnType().getSort() != Type.VOID;
		this.argumentsCount = type.getArgumentTypes().length;
	}

	public boolean isNonStatic() {
		return nonStatic;
	}

	public boolean isReturnResult() {
		return returnResult;
	}

	public int getArgumentsCount() {
		return argumentsCount;
	}

	public Object invoke(Object instance, Object... args) throws VMException {
		return vm.execute(instance, this, args);
	}
}
